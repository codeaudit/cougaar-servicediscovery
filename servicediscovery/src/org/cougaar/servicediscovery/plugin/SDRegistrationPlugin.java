/*
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA)
 *  and the Defense Logistics Agency (DLA).
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 *
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.servicediscovery.plugin;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.blackboard.PrivilegedClaimant;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.mts.MessageAddress;

/** Left in so that we can decide at a future point to include quiescence
 * reporting
 */
//import org.cougaar.core.service.AgentIdentificationService;
//import org.cougaar.core.service.QuiescenceReportService;

import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.Disposition;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.planning.ldm.plan.ScheduleElement;
import org.cougaar.planning.ldm.plan.ScheduleElementImpl;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.planning.plugin.util.PluginHelper;

import org.cougaar.servicediscovery.Constants;
import org.cougaar.servicediscovery.description.AvailabilityChangeMessage;
import org.cougaar.servicediscovery.description.LineageList;
import org.cougaar.servicediscovery.description.LineageListWrapper;
import org.cougaar.servicediscovery.description.ProviderCapability;
import org.cougaar.servicediscovery.description.ProviderCapabilities;
import org.cougaar.servicediscovery.description.ProviderDescription;
import org.cougaar.servicediscovery.description.ProviderDescriptionImpl;
import org.cougaar.servicediscovery.description.ServiceCategory;
import org.cougaar.servicediscovery.description.ServiceClassification;
import org.cougaar.servicediscovery.description.ServiceClassificationImpl;
import org.cougaar.servicediscovery.description.ServiceProfile;
import org.cougaar.servicediscovery.service.RegistrationService;
import org.cougaar.servicediscovery.util.UDDIConstants;
import org.cougaar.servicediscovery.transaction.DAMLReadyRelay;
import org.cougaar.servicediscovery.SDFactory;
import org.cougaar.servicediscovery.SDDomain;

import org.cougaar.util.TimeSpan;
import org.cougaar.util.UnaryPredicate;



import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Read local agent DAML profile file. Use the listed roles and register this agent with those
 * roles in the YP.
 **/
public class SDRegistrationPlugin extends SimplePlugin implements PrivilegedClaimant {
  private static final long DEFAULT_START = TimeSpan.MIN_VALUE;
  private static final long DEFAULT_END = TimeSpan.MAX_VALUE;

  private static final String DAML_IDENTIFIER = ".profile.daml";

  private static String ypAgent = 
  System.getProperty("org.cougaar.yp.ypAgent", "NCA");

  private LoggingService log;

  private RegistrationService registrationService = null;

  /**
   * Quiescence preparedness
   */
  //private QuiescenceReportService qrs;
  //private AgentIdentificationService ais;

  private IncrementalSubscription supportLineageSubscription;
  private IncrementalSubscription availabilityChangeSubscription;
  private IncrementalSubscription registerTaskSubscription;

  private boolean isRegistered = false;
  private static int WARNING_SUPPRESSION_INTERVAL = 2;
  private long warningCutoffTime = 0;

  private Alarm retryAlarm;
  private int outstandingSCAUpdates = 0;
  private int knownSCAs = 0;

  private boolean rehydrated;

  private ProviderDescription provD = null;
  private boolean publishProviderCapabilities = true;


  /** true iff we need to set up a retry **/
  private boolean failed = false;
  /** true iff we have submitted a registration request which hasn't yet been observed to complete **/
  private boolean inProgress = false;
  /** true iff we've got the callback successfully, but need to notify everyone else **/
  private boolean isPending = false;

  private UnaryPredicate supportLineagePredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return ((o instanceof LineageListWrapper) &&
	      (((LineageListWrapper) o).getType() == LineageList.SUPPORT));
    }
  };

  private UnaryPredicate availabilityChangePredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof AvailabilityChangeMessage);
    }
  };

  private UnaryPredicate registerTaskPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return ((o instanceof Task) &&
	      (((Task) o).getVerb().equals(org.cougaar.glm.ldm.Constants.Verb.RegisterServices)));
    }
  };

  private UnaryPredicate providerCapabilitiesPredicate = 
  new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof ProviderCapabilities) {
	ProviderCapabilities providerCapabilities = (ProviderCapabilities) o;
	return (providerCapabilities.getProviderName().equals(getAgentIdentifier().toString()));
      } else {
	return false;
      }
    }
  };


  public void setLoggingService(LoggingService log) {
    this.log = log;
  }


  /* Quiescence reporting support should we decide we need it */
//   public void load() {
//     super.load();
//     // Set up the QuiescenceReportService so that while waiting for the YP we
//     // dont go quiescent by mistake
//     this.ais = (AgentIdentificationService) getBindingSite().getServiceBroker().getService(this, AgentIdentificationService.class, null);
//     this.qrs = (QuiescenceReportService) getBindingSite().getServiceBroker().getService(this, QuiescenceReportService.class, null);

//     if (qrs != null)
//       qrs.setAgentIdentificationService(ais);
//   }

  public void unload() {
    if (registrationService != null) {
      getBindingSite().getServiceBroker().releaseService(this,
                                                         RegistrationService.class,
                                                         registrationService);
      registrationService = null;
    }

  /* Quiescence reporting support should we decide we need it */
//     if (qrs != null) {
//       getBindingSite().getServiceBroker().releaseService(this,
//                                                          QuiescenceReportService.class,
//                                                          qrs);
//       qrs = null;
//     }

//     if (ais != null) {
//       getBindingSite().getServiceBroker().releaseService(this,
//                                                          AgentIdentificationService.class,
//                                                          ais);
//       ais = null;
//     }

    if ((log != null) && (log != LoggingService.NULL)) {
      getBindingSite().getServiceBroker().releaseService(this, LoggingService.class, log);
      log = null;
    }
    super.unload();
  }

  protected void setupSubscriptions() {
    supportLineageSubscription =
      (IncrementalSubscription) subscribe(supportLineagePredicate);
    availabilityChangeSubscription =
      (IncrementalSubscription) subscribe(availabilityChangePredicate);
    registerTaskSubscription =
      (IncrementalSubscription) subscribe(registerTaskPredicate);

    registrationService = (RegistrationService) getBindingSite().
      getServiceBroker().getService(this, RegistrationService.class, null);

    // For now re-register every time.
    isRegistered = false;

    rehydrated = didRehydrate();

    Collection params = getDelegate().getParameters();

    if (params.size() > 0) {
	String numStr = (String) params.iterator().next();
      try {
	knownSCAs = Integer.parseInt(numStr);
      } catch (NumberFormatException nfe) {
        knownSCAs = 0;
	log.error(getAgentIdentifier() + " invalid SCA count parameter - " + numStr,
		  nfe);
      }
    } else {
      knownSCAs = 0;
    }

  }

  protected void execute () {
    if (isProvider()) {
      if (publishProviderCapabilities) {
	publishAdd(createProviderCapabilities());
	
	publishProviderCapabilities = false;
      }

      boolean registrationInitiated = false;

      if (isPending && !inProgress) {
        isRegistered = true;
        isPending = false;

	if (log.isInfoEnabled()) {
	  log.info("Completed initial registration of "+getAgentIdentifier());
	}

	if (outstandingSCAUpdates != 0) {
	  Collection scas = supportLineageSubscription.getCollection();
	  if (log.isInfoEnabled()) {
	    log.info(getAgentIdentifier() + " post pending added new SCAs - " + scas);
	  }

	  // Set outstanding count to 1 because we're adding all known SCAs
	  outstandingSCAUpdates = 1;
	  updateRegistration(scas);
	}
      } else if (!isRegistered) {
	if (supportLineageSubscription.size() >= knownSCAs) {
	  if (log.isDebugEnabled()) {
	    log.debug("Registering: " + getAgentIdentifier() +
		      " with SCAs " + supportLineageSubscription);
	  }
	  registrationInitiated = true;
	  initialRegister();
	} else {
	  if (log.isDebugEnabled()) {
	    log.debug("Waiting to register: " + getAgentIdentifier() +
		      " need " + knownSCAs + " SCAs, have " +
		      supportLineageSubscription.size());
	  }
	}
      }

      if (supportLineageSubscription.hasChanged()) {
	Collection adds = supportLineageSubscription.getAddedCollection();

	if (!adds.isEmpty()) {

	  if ((log.isInfoEnabled()) &&
	      (supportLineageSubscription.size() > knownSCAs)) {
	      log.info(getAgentIdentifier() + " expected " + knownSCAs +
		       " received " + supportLineageSubscription.size() + " " +
		       supportLineageSubscription);
	  }

	  if(isRegistered){
	    outstandingSCAUpdates++;
	    if (log.isInfoEnabled()) {
	      log.info(getAgentIdentifier() + " added new SCAs - " + adds);
	    }
	    updateRegistration(adds);
	  } else if (inProgress && !registrationInitiated) {
	    outstandingSCAUpdates++;
	    if (log.isInfoEnabled()) {
	      log.info(getAgentIdentifier() + " skip adding new SCAs - " + adds);
	    }
	  }
	}
      }

      if (availabilityChangeSubscription.hasChanged()) {
	Collection adds = availabilityChangeSubscription.getAddedCollection();
	handleAvailabilityChange(adds);
      }
    }


    // No harm since publish change only occurs if the conf rating has changed.
    updateRegisterTaskDispositions(registerTaskSubscription);
    
    // Not enabling this quiescent reporting yet -- currently
    // configuration change to ignore SDRegistrationPlugin appears to avoid
    // quiescence toggling. See bug# 3289

//     // Quiescence reporting
//     // Providers with outstanding registration attempts, or who will be waking themselves
//     // up to try to register again later, or who still need to register, are nonquiescent.
//     // Others are quiescent.
//     if (qrs != null) {
//       if (isProvider() && (!registrationComplete() || outstandingSCAUpdates > 0)) {
// 	qrs.clearQuiescentState();
// 	if (log.isInfoEnabled())
// 	  log.info(getAgentIdentifier() + " has outstanding YP Registrations. Not quiescent.");
// 	if (log.isDebugEnabled()) {
// 	  if (outstandingSCAUpdates > 0)
// 	    log.debug("                " + outstandingSCAUpdates + " SCA updates outstanding");
// 	  if (!registrationComplete()) 
// 	    log.debug("                 Registration is not complete.");
// 	}
//       } else {
// 	qrs.setQuiescentState();
// 	if (log.isInfoEnabled())
// 	  log.info(getAgentIdentifier() + " finished any YP Registrations. Now quiescent.");
//       }
//     }
  }

  private void updateRegistration(Collection scas) {
    final Collection adds = scas;

    // IMPORTANT - currently no code to retry. Failed transactions == lost SCAs
    registrationService.updateServiceDescription(ypAgent,
					         getAgentIdentifier().toString(),
                                                 scaServiceClassifications(adds),
                                                 new RegistrationService.Callback() {
      public void invoke(Object o) {
	// Success or not, we've completed
	// handling this transaction.
	outstandingSCAUpdates--;
	boolean success = ((Boolean)o).booleanValue();
	if (!success) {
	  log.error(getAgentIdentifier() +
		    " unable to update registry with new SCAs - " + adds);
	} else {
	  
	  if (log.isInfoEnabled()) {
	    log.info(getAgentIdentifier() +
		     " updated registry with new SCAs - " +
		     adds);
	  }
	}
	wake();
      }
      public void handle(Exception e) {
	// Success or not, we've completed
	// handling this transaction.
	outstandingSCAUpdates--;
	log.error("UpdateServiceDescription", e);
	wake();
      }
    });
    
  }


  ProviderDescription getPD() {
    if (provD == null) {
      ProviderDescription pd = new ProviderDescriptionImpl(log);
      try {
	boolean ok = pd.parseDAML(getAgentIdentifier() + DAML_IDENTIFIER);
	if (!ok) {
	  throw new RuntimeException("parseDAML failed " + getAgentIdentifier());
	}
      } catch (java.util.ConcurrentModificationException cme) {
	// Jena can do a concurrent mod exception. See bug 3052
 	// catch this above.
 	throw new RuntimeException("getPD() failed in " + 
				   getAgentIdentifier() + 
				   ". ConcurrentModException in Jena. See bug 3052.");
      }
      if (pd.getProviderName() == null) {
	throw new RuntimeException("getPD() failed to parse a provider name " + 
				   getAgentIdentifier());
      }
      provD = pd;
    }
    return provD;
  }

  private void initialRegister() {
    if (isPending) return;      // skip if we're just waiting to notify - of course, then we shouldn't be here
    if (isProvider()) {
      try {
        final ProviderDescription pd = getPD();

        if (failed) {
          // last try failed - set up another try.
          failed = false;
	  retryErrorLog("Problem adding ProviderDescription now, try again later.");
        } else {
          if (inProgress) {
            if (log.isInfoEnabled()) {
              log.info(getAgentIdentifier() + "Still waiting for register");
            }
          } else {
            inProgress = true;      // don't allow another one to start
	    outstandingSCAUpdates = 0;    // will pick up all known SCAs

            // callback will be executed by another thread
            RegistrationService.Callback cb =
              new RegistrationService.Callback() {
                public void invoke(Object o) {
                  boolean success = ((Boolean) o).booleanValue();
                  if (log.isInfoEnabled()) {
                    log.info(pd.getProviderName()+" initialRegister success = "+success);
                  }

                  isPending = true; // let the plugin set isRegistered
                  retryAlarm = null;   // probably not safe
                  inProgress = false; // ok to update
                  wake();
                }
                public void handle(Exception e) {
                  log.error("initialRegister", e);
                  failed = true;
                  inProgress = false; // ok to update
                  wake();
                }
              };

            // actually submit the request.
	    if (log.isInfoEnabled()) {
	      log.info(getAgentIdentifier() + " initial registration with SCAs " +
		       supportLineageSubscription);
	    }
            registrationService.addProviderDescription(ypAgent,
						       pd,
						       scaServiceClassifications(supportLineageSubscription),
                                                       cb);
          }
        }
      } catch (RuntimeException e) {
	//if the parsing failed, it may be because one of the url's was down, so try again later
	retryErrorLog("ProviderDescription registration failed.  Will Try again.", e);
      }
    } else {
      if (log.isDebugEnabled()) {
	log.debug("Agent " + getAgentIdentifier() + " Not Registering, no daml file.");
      }
    }

  }

  private void retryErrorLog(String message) {
    retryErrorLog(message, null);
  }

  // When an error occurs, but we'll be retrying later, treat it as a DEBUG
  // at first. After a while it becomes an error.
  private void retryErrorLog(String message, Throwable e) {
    if(warningCutoffTime == 0) {
      WARNING_SUPPRESSION_INTERVAL = (Integer.valueOf(System.getProperty(
									 "org.cougaar.servicediscovery.plugin.RegistrationGracePeriod",
									 String.valueOf(WARNING_SUPPRESSION_INTERVAL)))).intValue();
      warningCutoffTime = System.currentTimeMillis() + WARNING_SUPPRESSION_INTERVAL*60000;
    }
    int rand = (int)(Math.random()*10000) + 1000;
    retryAlarm = this.wakeAfter(rand);
    if(System.currentTimeMillis() > warningCutoffTime) {
      if (e == null)
	log.error(getAgentIdentifier() + message);
      else
	log.error(getAgentIdentifier() + message, e);
    } else if (log.isDebugEnabled()) {
      if (e == null)
	log.debug(getAgentIdentifier() + message);
      else
	log.debug(getAgentIdentifier() + message, e);
    }
  }

  private Collection scaServiceClassifications(Collection supportLineageCollection) {
    Collection serviceClassifications =
      new ArrayList(supportLineageCollection.size());
    for (Iterator iterator = supportLineageCollection.iterator();
	 iterator.hasNext();) {
      LineageListWrapper wrapper = (LineageListWrapper) iterator.next();
      ServiceClassification sca =
	new ServiceClassificationImpl(wrapper.getRoot(),
				      wrapper.getRoot(),
				      UDDIConstants.SUPPORT_COMMAND_ASSIGNMENT);
      serviceClassifications.add(sca);
    }
    return serviceClassifications;
  }

  private boolean registrationComplete() {
    //return ((outstandingSCAUpdates == 0) && (isRegistered));
    return (isRegistered);
  }

  private ProviderCapabilities createProviderCapabilities() {
    ProviderDescription pd = getPD();
    Collection serviceProfiles = pd.getServiceProfiles();

    PlanningFactory planningFactory = 
	(PlanningFactory) getFactory("planning");
    SDFactory sdFactory = (SDFactory) getFactory(SDDomain.SD_NAME);
    ProviderCapabilities providerCapabilities = 
      sdFactory.newProviderCapabilities(getAgentIdentifier().toString());

    for (Iterator iterator = serviceProfiles.iterator();
	 iterator.hasNext();) {
      ServiceProfile serviceProfile = (ServiceProfile) iterator.next();
      
      Collection serviceCategories = serviceProfile.getServiceCategories();

      Role role = null;
      String echelon = null;

      for (Iterator scIterator = serviceCategories.iterator();
	   scIterator.hasNext();) {
	ServiceCategory serviceCategory = (ServiceCategory) scIterator.next();
	
	String scheme = serviceCategory.getCategorySchemeName();
	if (scheme.equals(UDDIConstants.MILITARY_ECHELON_SCHEME)) {
	  echelon = serviceCategory.getCategoryName();
	} else if (scheme.equals(UDDIConstants.MILITARY_SERVICE_SCHEME)) {
	  role = Role.getRole(serviceCategory.getCategoryName());
	}

	if ((role != null) &&
	    (echelon != null)) {
	  Schedule defaultSchedule = 
	    planningFactory.newSimpleSchedule(DEFAULT_START, DEFAULT_END);
	  providerCapabilities.addCapability(role, echelon, defaultSchedule);
	  break;
	}
      }
    }

    return providerCapabilities;
  }


  private void updateRegisterTaskDispositions(Collection registerTasks) {
    for (Iterator iterator = registerTasks.iterator();
	 iterator.hasNext();) {
      Task task = (Task) iterator.next();
      PlanElement pe = task.getPlanElement();
      double conf;
      if (isProvider() && !registrationComplete()) {
	// BOZO - special rehydration kludge for quiesence monitor.
	// Plugin will reregister because it doesn't know whether the previous
	// registration exists but does not downgrade the confidence as the  
	// change would propagate via GLSExpander back to NCA. 
	if ((rehydrated) && 
	    (pe != null) &&
	    (pe.getEstimatedResult().getConfidenceRating() == 1.0)) {
	  if (log.isWarnEnabled()) {
	    log.warn(getAgentIdentifier() + 
		     ": updateRegisterTaskDisposition() " +
		     "leaving confidence at 1.0 after rehydration even though " +
		     "reregistration is not complete.");
	  }

	  conf = 1.0;
	} else {
	  if ((pe != null) &&
	      (pe.getEstimatedResult().getConfidenceRating() == 1.0)) {
	    if (log.isDebugEnabled()) {
	      log.debug(getAgentIdentifier() + 
		       ": updateRegisterTaskDisposition() " +
		       "changing confidence back to 0.0." +
		       " didRehydrate() == " + didRehydrate() + 
		       " rehydrated == " + rehydrated);
	    }
	  }
	  conf = 0.0;
	}
      } else {
	conf = 1.0;
      }

      AllocationResult estResult =
	PluginHelper.createEstimatedAllocationResult(task,
						     theLDMF,
						     conf,
						     true);

      if (pe == null) {
	if (log.isInfoEnabled()) {
	  log.info(getAgentIdentifier() +
		   " adding a disposition to RegisterServices task, confidence rating is  " +
		   conf);
	}
	Disposition disposition =
	  theLDMF.createDisposition(task.getPlan(), task, estResult);
	publishAdd(disposition);
      } else if (pe.getEstimatedResult().getConfidenceRating() != conf) {
	double previousConf = pe.getEstimatedResult().getConfidenceRating();
	
	// If we're backing up from a Confidence of 1.0 to lower, like at rehydration
	// when we don't know whether the YP lost our registration,
	// be a little more verbose.
        if (previousConf == 1.0) {
	  log.warn(getAgentIdentifier() + " SDRegistrationPlugin is " +
		   " changing RegisterServices confidence rating from " +
		   previousConf + " to " +
		   conf + ". Rehydrated?");
	} else if (log.isInfoEnabled()) {
	  log.info(getAgentIdentifier() +
		   " changing RegisterServices confidence rating from " +
		   previousConf + " to " +
		   conf);
	}
	pe.setEstimatedResult(estResult);
	publishChange(pe);
      }
    }
  }

  // Handle a change to our registration status
  protected void handleAvailabilityChange(AvailabilityChangeMessage availabilityChange) {
    synchronized (availabilityChange) {
      if (!isProvider()) {
	if (log.isDebugEnabled()) {
	  log.debug(getAgentIdentifier() + " not a provider. " +
		    "Ignoring AvailabiltyChangeMessage - " + 
		    availabilityChange);
	}
	return;
      }

      if (log.isDebugEnabled()) {
	log.debug(getAgentIdentifier() + 
		  " handling AvailabiltyChangeMessage. Status = " + 
		  availabilityChange.getStatus());
      }

      switch (availabilityChange.getStatus()) {
      case AvailabilityChangeMessage.REQUESTED:
	  updateProviderCapability(availabilityChange);
	  break;
      case AvailabilityChangeMessage.PENDING:
        // let it go.  might want to check to see if it takes a very long time...
        break;
      case AvailabilityChangeMessage.COMPLETED:
        availabilityChange.setRegistryUpdated(true);
        publishChange(availabilityChange);
        break;
      case AvailabilityChangeMessage.DONE:
        // should drop it from the list;
        break;
      case AvailabilityChangeMessage.ERROR:
        // retry, perhaps?
        break;
      }
    }
  }

  private void handleAvailabilityChange(Collection availabilityChanges) {
    for (Iterator iterator = availabilityChanges.iterator(); iterator.hasNext();) {
      AvailabilityChangeMessage availabilityChange = (AvailabilityChangeMessage) iterator.next();
      handleAvailabilityChange(availabilityChange);
    }
    
    // Handle availabilityChangeMessage as trumping the presence of DAML file.
    // Don't change isRegistered flag because that would assume that we
    // should reregister.
    //isRegistered = false;
    return;
  }
  
  private void removeRegisteredRole(final AvailabilityChangeMessage availabilityChange) {
    Collection serviceClassifications = new ArrayList(1);
    ServiceClassification sca =
      new ServiceClassificationImpl(availabilityChange.getRole().toString(),
				    availabilityChange.getRole().toString(),
				    UDDIConstants.MILITARY_SERVICE_SCHEME);
    serviceClassifications.add(sca);
    
    RegistrationService.Callback cb =
      new RegistrationService.Callback() {
      public void invoke(Object o) {
	boolean success = ((Boolean) o).booleanValue();
	synchronized(availabilityChange) {
	  availabilityChange.setStatus(AvailabilityChangeMessage.COMPLETED);
	}
	wake();
      }
      public void handle(Exception e) {
	log.error("handleAvailabilityChange", e);
	synchronized(availabilityChange) {
	  availabilityChange.setStatus(AvailabilityChangeMessage.ERROR);
	}
	wake();
      }
    };
    registrationService.deleteServiceDescription(ypAgent,
						 getAgentIdentifier().toString(),
						 serviceClassifications,
						 cb);
  }

  private void addRegisteredRole(final AvailabilityChangeMessage availabilityChange) {
    Collection serviceClassifications = new ArrayList(1);
    ServiceClassification sca =
      new ServiceClassificationImpl(availabilityChange.getRole().toString(),
				    availabilityChange.getRole().toString(),
				    UDDIConstants.MILITARY_SERVICE_SCHEME);
    serviceClassifications.add(sca);
    
    RegistrationService.Callback cb =
      new RegistrationService.Callback() {
      public void invoke(Object o) {
	boolean success = ((Boolean) o).booleanValue();
	synchronized(availabilityChange) {
	  availabilityChange.setStatus(AvailabilityChangeMessage.COMPLETED);
	}
	wake();
      }
      public void handle(Exception e) {
	log.error("handleAvailabilityChange", e);
	synchronized(availabilityChange) {
	  availabilityChange.setStatus(AvailabilityChangeMessage.ERROR);
	}
	wake();
      }
    };
    registrationService.updateServiceDescription(ypAgent,
						 getAgentIdentifier().toString(),
						 serviceClassifications,
						 cb);
  }

  private void updateProviderCapability(AvailabilityChangeMessage availabilityChange) {
    Collection pcCollection = query(providerCapabilitiesPredicate);

    if (log.isDebugEnabled()) {
      log.debug(getAgentIdentifier() + ": updateProviderCapability handling " +
		availabilityChange);
    }


    for (Iterator iterator = pcCollection.iterator();
	 iterator.hasNext();) {
      ProviderCapabilities capabilities = 
	(ProviderCapabilities) iterator.next();

      ProviderCapability capability = 
	  capabilities.getCapability(availabilityChange.getRole());

      if (capability != null) {
	TimeSpan timeSpan = availabilityChange.getTimeSpan();
	Schedule currentAvailability = 
	  capability.getAvailableSchedule();

	if (log.isDebugEnabled()) {
	  log.debug(getAgentIdentifier() + 
		    ": found matching ProviderCapability handling " +
		    capability + 
		    " with current availability " + currentAvailability);
	}
	
	PlanningFactory planningFactory = 
	  (PlanningFactory) getFactory("planning");
	Schedule newAvailability = 
	  planningFactory.newSchedule(currentAvailability.getAllScheduleElements());
	
	Collection overlaps = 
	  currentAvailability.getOverlappingScheduleElements(timeSpan.getStartTime(),
							     timeSpan.getEndTime());

	if (log.isDebugEnabled()) {
	  log.debug(getAgentIdentifier() + 
		    ": overlaps found - " + overlaps);
	}
	
	boolean change = false;
	if (overlaps.size() == 0) {
	  if (availabilityChange.isAvailable()) {

	    newAvailability.add(timeSpan);
	    change = true;
	  }
	} else {
	  change = true;

	  ScheduleElement earliest = null;
	  ScheduleElement latest = null;

	  for (Iterator overlap = overlaps.iterator();
	       overlap.hasNext();) {
	    latest = (ScheduleElement) overlap.next();
	    if (earliest == null) {
	      earliest = latest;
	    }
	    newAvailability.remove(latest);
	  }
	 
	  if (availabilityChange.isAvailable()) {
	    // Construct ScheduleElement to fill the entire period.
	    long newStart = Math.min(earliest.getStartTime(), 
				     timeSpan.getStartTime());
	    long newEnd = Math.max(latest.getEndTime(), 
				   timeSpan.getEndTime());
	    ScheduleElementImpl newScheduleElement = 
	      new ScheduleElementImpl(newStart, newEnd);
	    newAvailability.add(newScheduleElement);
	  } else {
	    // Construct ScheduleElements to bracket the unavailable time
	    
	    if (earliest.getStartTime() < timeSpan.getStartTime()) {
	      ScheduleElementImpl newEarliest = 
		new ScheduleElementImpl(earliest.getStartTime(), 
					timeSpan.getStartTime());
	      newAvailability.add(newEarliest);
	    }

	    if (latest.getEndTime() > timeSpan.getEndTime()) {
	      ScheduleElementImpl newLatest = 
		new ScheduleElementImpl(timeSpan.getEndTime(), 
					latest.getEndTime());
	      newAvailability.add(newLatest);
	    }
	  }
	}


	if (change) {
	  if (log.isDebugEnabled()) {
	    log.debug(getAgentIdentifier() + 
		      " provider availability after change " + 
		      newAvailability);
	  }


	  capability.setAvailableSchedule(newAvailability);
	  publishChange(capabilities);

	  
	  if (newAvailability.size() == 0) {
	    // Provider never available so remove registration
	    removeRegisteredRole(availabilityChange);
	  } else if (currentAvailability.size() == 0) {
	    // Provider changing from never available so add registration
	    addRegisteredRole(availabilityChange);	
	  }

	} else {
	  if (log.isDebugEnabled()) {
	    log.debug(getAgentIdentifier() + 
		      " ignoring AvailabilityChangeMessage " + 
		      availabilityChange +
		      " information already included in ProviderCapability " +
		      capability);
	  }
	}
      }
    }
  }

  // Is this Agent a service provider?
  private boolean isProvider() {
    return getProviderFile().exists();
  }
  
  // Get the DAML service provider file
  private File getProviderFile() {
    String damlFileName = getAgentIdentifier().toString() + DAML_IDENTIFIER;
    return new File(org.cougaar.servicediscovery.Constants.getServiceProfileURL().getFile() +
		    damlFileName);
  }
}



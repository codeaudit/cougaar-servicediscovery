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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.Disposition;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.planning.ldm.plan.ScheduleElement;
import org.cougaar.planning.ldm.plan.ScheduleElementImpl;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.servicediscovery.SDDomain;
import org.cougaar.servicediscovery.SDFactory;
import org.cougaar.servicediscovery.description.AvailabilityChangeMessage;
import org.cougaar.servicediscovery.description.LineageList;
import org.cougaar.servicediscovery.description.LineageListWrapper;
import org.cougaar.servicediscovery.description.ProviderCapabilities;
import org.cougaar.servicediscovery.description.ProviderCapability;
import org.cougaar.servicediscovery.description.ProviderDescription;
import org.cougaar.servicediscovery.description.ProviderDescriptionImpl;
import org.cougaar.servicediscovery.description.ServiceCategory;
import org.cougaar.servicediscovery.description.ServiceClassification;
import org.cougaar.servicediscovery.description.ServiceClassificationImpl;
import org.cougaar.servicediscovery.description.ServiceProfile;
import org.cougaar.servicediscovery.service.RegistrationService;
import org.cougaar.servicediscovery.util.UDDIConstants;
import org.cougaar.util.TimeSpan;
import org.cougaar.util.UnaryPredicate;

/**
 * Read local agent DAML profile file. Use the listed roles and register this agent with those
 * roles in the YP.
 **/
public abstract class SDRegistrationPluginBase extends ComponentPlugin {

  protected static final long DEFAULT_START = TimeSpan.MIN_VALUE;
  protected static final long DEFAULT_END = TimeSpan.MAX_VALUE;

  protected static final String DAML_IDENTIFIER = ".profile.daml";

  protected LoggingService log;

  protected RegistrationService registrationService = null;

  protected DomainService domainService = null;

  /**
   * Quiescence preparedness
   */
  //private QuiescenceReportService qrs;
  //private AgentIdentificationService ais;

  protected IncrementalSubscription supportLineageSubscription;
  protected IncrementalSubscription availabilityChangeSubscription;
  protected IncrementalSubscription registerTaskSubscription;

  protected static int WARNING_SUPPRESSION_INTERVAL = 5;
  protected long warningCutoffTime = 0;
  protected static final String REGISTRATION_GRACE_PERIOD_PROPERTY = 
                "org.cougaar.servicediscovery.plugin.RegistrationGracePeriod";

  protected Alarm retryAlarm;
  protected int knownSCAs = 0;

  protected boolean rehydrated;

  protected ProviderDescription provD = null;
  protected boolean publishProviderCapabilities;


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

  public void setDomainService(DomainService ds) {
    this.domainService = ds;
  }


  public void setLoggingService(LoggingService log) {
    this.log = log;
  }

  public void setRegistrationService(RegistrationService rs) {
    registrationService = rs;
  }


  public void suspend() {
    if (log.isInfoEnabled()) {
      log.info(getAgentIdentifier() + " suspend.");
    }
    super.suspend();

    if (retryAlarm != null) {
      if (log.isInfoEnabled()) {
	log.info(getAgentIdentifier() + " cancelling retryAlarm.");
      }
      retryAlarm.cancel();
    }
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
      (IncrementalSubscription) getBlackboardService().subscribe(supportLineagePredicate);
    availabilityChangeSubscription =
      (IncrementalSubscription) getBlackboardService().subscribe(availabilityChangePredicate);
    registerTaskSubscription =
      (IncrementalSubscription) getBlackboardService().subscribe(registerTaskPredicate);

    Collection params = getParameters();

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

    rehydrated = getBlackboardService().didRehydrate();

    if (rehydrated) {
      Collection pcCollection = 
	getBlackboardService().query(providerCapabilitiesPredicate);
      
      publishProviderCapabilities = (pcCollection.isEmpty());
    } else {
      publishProviderCapabilities = true;
    }
  }

  protected void execute () {
    if (isProvider()) {

      if (publishProviderCapabilities) {
	ProviderCapabilities providerCapabilities = 
	  createProviderCapabilities();

	if (providerCapabilities != null) {
	  getBlackboardService().publishAdd(providerCapabilities);
	  publishProviderCapabilities = false;
	} else {
	  retryErrorLog("Problem getting ProviderDescription, try again later.");
	}
      }
      
      if (availabilityChangeSubscription.hasChanged()) {
	Collection adds = availabilityChangeSubscription.getAddedCollection();
	handleAvailabilityChange(adds);
      }
    }
  }
  
  /* Returns null if unable to parse the provider description */
  protected  ProviderDescription getPD() {
    if (provD == null) {
      ProviderDescription pd = new ProviderDescriptionImpl(log);
      try {
	boolean ok = pd.parseDAML(getAgentIdentifier() + DAML_IDENTIFIER);

	if (ok && (pd.getProviderName() != null)) {
	  provD = pd;
	}
      } catch (java.util.ConcurrentModificationException cme) {
	// Jena can do a concurrent mod exception. See bug 3052
 	// Leave provD uninitialized
      }
    }
    return provD;
  }
  
  protected long getWarningCutOffTime() {
    if (warningCutoffTime == 0) {
      WARNING_SUPPRESSION_INTERVAL = Integer.getInteger(REGISTRATION_GRACE_PERIOD_PROPERTY,
							WARNING_SUPPRESSION_INTERVAL).intValue();
      warningCutoffTime = System.currentTimeMillis() + WARNING_SUPPRESSION_INTERVAL*60000;
    }
    
    return warningCutoffTime;
  }
  
  protected void retryErrorLog(String message) {
    retryErrorLog(message, null);
  }
  
  // When an error occurs, but we'll be retrying later, treat it as a DEBUG
  // at first. After a while it becomes an error.
  protected void retryErrorLog(String message, Throwable e) {
    
    long absTime = getAlarmService().currentTimeMillis()+ 
      (int)(Math.random()*10000) + 1000;
    
    retryAlarm = new RetryAlarm(absTime);
    getAlarmService().addAlarm(retryAlarm);
    
    if(System.currentTimeMillis() > getWarningCutOffTime()) {
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
  
  protected Collection scaServiceClassifications(Collection supportLineageCollection) {
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
  
  protected abstract  boolean registrationComplete();

  /* Returns initial version of ProviderCapabilities created from the 
   * provider DAML file.
   */
  protected ProviderCapabilities createProviderCapabilities(){
    ProviderDescription pd = getPD();

    if (pd == null) {
      return null;
    }

    Collection serviceProfiles = pd.getServiceProfiles();

    PlanningFactory planningFactory = 
	(PlanningFactory) domainService.getFactory("planning");
    SDFactory sdFactory = 
      (SDFactory) domainService.getFactory(SDDomain.SD_NAME);
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


  protected void updateRegisterTaskDispositions() {
    PlanningFactory planningFactory = 
      (PlanningFactory) domainService.getFactory("planning");
    
    for (Iterator iterator = registerTaskSubscription.iterator();
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
						     planningFactory,
						     conf,
						     true);

      if (pe == null) {
	if (log.isInfoEnabled()) {
	  log.info(getAgentIdentifier() +
		   " adding a disposition to RegisterServices task, confidence rating is  " +
		   conf);
	}
	Disposition disposition =
	  planningFactory.createDisposition(task.getPlan(), task, estResult);
	getBlackboardService().publishAdd(disposition);
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
	getBlackboardService().publishChange(pe);
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
	  availabilityChange.setStatus(AvailabilityChangeMessage.COMPLETED);
	  getBlackboardService().publishChange(availabilityChange);
	  break;
      case AvailabilityChangeMessage.PENDING:
        // let it go.  might want to check to see if it takes a very long time...
        break;
      case AvailabilityChangeMessage.COMPLETED:
	if (!availabilityChange.isRegistryUpdated()) {
	  availabilityChange.setRegistryUpdated(true);
	  getBlackboardService().publishChange(availabilityChange);
	}
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

  protected void handleAvailabilityChange(Collection availabilityChanges) {
    for (Iterator iterator = availabilityChanges.iterator(); iterator.hasNext();) {
      AvailabilityChangeMessage availabilityChange = (AvailabilityChangeMessage) iterator.next();
      handleAvailabilityChange(availabilityChange);
    }
    
    return;
  }
  
  protected void updateProviderCapability(AvailabilityChangeMessage availabilityChange) {
    Collection pcCollection = 
      getBlackboardService().query(providerCapabilitiesPredicate);

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
	  (PlanningFactory) domainService.getFactory("planning");
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
	    // Construct ScheduleElement to fill the entire period.
	    ScheduleElementImpl newScheduleElement = 
	      new ScheduleElementImpl(timeSpan.getStartTime(),
				      timeSpan.getEndTime());
	    newAvailability.add(newScheduleElement);
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
	  getBlackboardService().publishChange(capabilities);

	  
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

  protected abstract void addRegisteredRole(final AvailabilityChangeMessage availabilityChange);

  protected abstract void removeRegisteredRole(final AvailabilityChangeMessage availabilityChange);

  // Is this Agent a service provider?
  protected boolean isProvider() {
    return getProviderFile().exists();
  }
  
  // Get the DAML service provider file
  protected File getProviderFile() {
    String damlFileName = getAgentIdentifier().toString() + DAML_IDENTIFIER;
    return new File(org.cougaar.servicediscovery.Constants.getServiceProfileURL().getFile() +
		    damlFileName);
  }

  public class RetryAlarm implements Alarm {
    private long expiresAt;
    private boolean expired = false;

    public RetryAlarm (long expirationTime) {
      expiresAt = expirationTime;
    }

    public long getExpirationTime() { return expiresAt; }
    public synchronized void expire() {
      if (!expired) {
        expired = true;
        getBlackboardService().signalClientActivity();
      }
    }
    public boolean hasExpired() { return expired; }
    public synchronized boolean cancel() {
      boolean was = expired;
      expired=true;
      return was;
    }
    public String toString() {
      return "<RetryAlarm "+expiresAt+
        (expired?"(Expired) ":" ")+
        "for SDCommunityBasedRegistrationPlugin at " + 
	getAgentIdentifier() + ">";
    }
  }
}



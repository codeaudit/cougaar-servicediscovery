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
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.AlarmService;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.community.Community;
import org.cougaar.core.service.community.CommunityChangeEvent;
import org.cougaar.core.service.community.CommunityChangeListener;
import org.cougaar.core.service.community.CommunityResponse;
import org.cougaar.core.service.community.CommunityResponseListener;
import org.cougaar.core.service.community.CommunityService;
import org.cougaar.core.service.community.Entity;

import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.Disposition;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.planning.plugin.util.PluginHelper;

import org.cougaar.servicediscovery.Constants;
import org.cougaar.servicediscovery.description.LineageList;
import org.cougaar.servicediscovery.description.LineageListWrapper;
import org.cougaar.servicediscovery.description.ProviderCapabilities;
import org.cougaar.servicediscovery.description.ProviderDescription;
import org.cougaar.servicediscovery.description.ProviderDescriptionImpl;
import org.cougaar.servicediscovery.description.ServiceCategory;
import org.cougaar.servicediscovery.description.ServiceClassification;
import org.cougaar.servicediscovery.description.ServiceClassificationImpl;
import org.cougaar.servicediscovery.description.ServiceProfile;
import org.cougaar.servicediscovery.description.StatusChangeMessage;
import org.cougaar.servicediscovery.description.SupportLineageList;
import org.cougaar.servicediscovery.service.RegistrationService;
import org.cougaar.servicediscovery.util.UDDIConstants;
import org.cougaar.servicediscovery.transaction.DAMLReadyRelay;
import org.cougaar.servicediscovery.SDFactory;
import org.cougaar.servicediscovery.SDDomain;

import org.cougaar.util.UnaryPredicate;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Read local agent DAML profile file. Use the listed roles and register this agent with those
 * roles in the YP.
 **/
public class SDCommunityBasedRegistrationPlugin extends ComponentPlugin implements PrivilegedClaimant {

  private static final String DAML_IDENTIFIER = ".profile.daml";

  private LoggingService log;

  private CommunityService communityService = null;
  private DomainService domainService = null;
  private RegistrationService registrationService = null;

  private IncrementalSubscription supportLineageSubscription;
  protected IncrementalSubscription statusChangeSubscription;
  private IncrementalSubscription registerTaskSubscription;

  private static int WARNING_SUPPRESSION_INTERVAL = 5;
  private long warningCutoffTime = 0;
  private static final String REGISTRATION_GRACE_PERIOD_PROPERTY = 
                "org.cougaar.servicediscovery.plugin.RegistrationGracePeriod";

  private Alarm retryAlarm;

  private boolean rehydrated;

  private int knownSCAs;
  private HashMap scaHash = null;

  private ProviderDescription provD = null;
  private boolean publishProviderCapabilities = true;

  private UnaryPredicate supportLineagePredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof LineageListWrapper) {
	LineageListWrapper wrapper = (LineageListWrapper) o;
	return (wrapper.getType() == LineageList.SUPPORT);
      } else {
	return false;
      }
    }
  };

  private UnaryPredicate statusChangePredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof StatusChangeMessage);
    }
  };

  private UnaryPredicate registerTaskPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return ((o instanceof Task) &&
	      (((Task) o).getVerb().equals(org.cougaar.glm.ldm.Constants.Verb.RegisterServices)));
    }
  };


  public void setCommunityService(CommunityService cs) { 
    this.communityService = cs; 
  }


  public void setDomainService(DomainService ds) {
    this.domainService = ds;
  }

  public void setLoggingService(LoggingService log) {
    this.log = log;
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

    // Remove all community change notifications
    Set scaSet = scaHash.entrySet();
    
    if (log.isInfoEnabled()) {
      log.info(getAgentIdentifier() + " removing community change listeners.");
    }

    for (Iterator iterator = scaSet.iterator();
	 iterator.hasNext();) {
      Map.Entry entry = (Map.Entry) iterator.next();
      
      ((SCAInfo) entry.getValue()).clearCommunity();
    }
  }

  protected void setupSubscriptions() {
    supportLineageSubscription =
      (IncrementalSubscription) getBlackboardService().subscribe(supportLineagePredicate);
    statusChangeSubscription =
      (IncrementalSubscription) getBlackboardService().subscribe(statusChangePredicate);
    registerTaskSubscription =
      (IncrementalSubscription) getBlackboardService().subscribe(registerTaskPredicate);

    registrationService = (RegistrationService) getBindingSite().
      getServiceBroker().getService(this, RegistrationService.class, null);

    Collection params = getParameters();

    if (params.size() > 0) {
	String numStr = (String) params.iterator().next();
      try {
	knownSCAs = Integer.parseInt(numStr);
      } catch (NumberFormatException nfe) {
        knownSCAs = 0;
	log.error(getAgentIdentifier() + " invalid SCA count parameter - " + 
		  numStr, nfe);
      }
    } else {
      knownSCAs = 0;
    }

    scaHash = new HashMap();

    rehydrated = getBlackboardService().didRehydrate();

    if (rehydrated) {
      // Currently losing all pre-existing status change messages.
      // rebuild scaHash & reregister
      for (Iterator iterator = supportLineageSubscription.iterator();
	   iterator.hasNext();) {
	SupportLineageList sca = 
	  (SupportLineageList) ((LineageListWrapper) iterator.next()).getLineageList();
	handleNewSCA(sca);
      }
    }
  }

  protected void execute () {
    if (isProvider()) {

      if (publishProviderCapabilities) {
	getBlackboardService().publishAdd(createProviderCapabilities());
	
	publishProviderCapabilities = false;
      }
	
      Set scaSet = scaHash.entrySet();
      
      for (Iterator iterator = scaSet.iterator();
	   iterator.hasNext();) {
	Map.Entry entry = (Map.Entry) iterator.next();
	
	SCAInfo scaInfo = (SCAInfo) entry.getValue();
	
	if (scaInfo.readyToRegister()) {
	  log.debug("Registering: " + getAgentIdentifier() + " with " +
		    scaInfo.getCommunity());
	  initialRegister(scaInfo);
	}
      }
	
      if (supportLineageSubscription.hasChanged()) {
	Collection adds = supportLineageSubscription.getAddedCollection();
	
	if (adds.size() > 0) {
	  for (Iterator iterator = adds.iterator();
	       iterator.hasNext();) {
	    SupportLineageList sca = 
	      (SupportLineageList) ((LineageListWrapper) iterator.next()).getLineageList();
	    handleNewSCA(sca);
	  }
	  
	  // Adds sca to all preexisting registrations.
	  //updateRegistration(adds);
	}
      }

      handleStatusChange();
    }


    updateRegisterTaskDispositions(registerTaskSubscription);
  }

  private void initialRegister(final SCAInfo scaInfo) {
    if (isProvider()) {
		  
      if (!scaInfo.readyToRegister()) {
	if (log.isDebugEnabled()) {
	  log.debug(getAgentIdentifier() + "Exiting initialRegister early - " +
		    " SCAInfo not ready - " + 
		    " community " + scaInfo.getCommunity() +
		    " isRegistered " + scaInfo.getIsRegistered() +
		    " pendingRegistration " + scaInfo.getPendingRegistration() +
		    " isDeleted " + scaInfo.getIsDeleted());
	}


	return;
      }


      try {
        final ProviderDescription pd = getPD();

	scaInfo.setPendingRegistration(true);

	RegistrationService.Callback cb =
	  new RegistrationService.Callback() {
	  public void invoke(Object o) {
	    boolean success = ((Boolean) o).booleanValue();
	    if (log.isInfoEnabled()) {
	      log.info(pd.getProviderName()+ " initialRegister success = " + 
		       success + " with " + scaInfo.getCommunity().getName());
	    }
	    
            scaInfo.setIsRegistered(true);
            scaInfo.setPendingRegistration(false);
	    scaInfo.clearCommunity();

	    retryAlarm = null;   // probably not safe
	    getBlackboardService().signalClientActivity();
	  }
	
          public void handle(Exception e) {
            scaInfo.setPendingRegistration(false); // okay to try again
            scaInfo.setIsRegistered(false);

	    retryErrorLog("Problem adding ProviderDescription to " + 
			  scaInfo.getCommunity().getName() + 
			  ", try again later: " +
			  getAgentIdentifier(), e);
          }
        };
	
	// actually submit the request.
        registrationService.addProviderDescription(scaInfo.getCommunity(),
						   pd,
						   scaServiceClassifications(supportLineageSubscription),
						   cb);
      } catch (RuntimeException e) {
	  scaInfo.setIsRegistered(false);	
          scaInfo.setPendingRegistration(false); // okay to try again
	  
	  retryErrorLog("Problem adding ProviderDescription to " + 
			scaInfo.getCommunity().getName() + 
			", try again later: " +
			getAgentIdentifier(), e);
      }
    }
  }
	
  private ProviderDescription getPD() {
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

  private void updateRegistration(Collection scas) {
    final Collection adds = scas;
    
    Set scaSet = scaHash.entrySet();
      
    for (Iterator iterator = scaSet.iterator();
	 iterator.hasNext();) {
      Map.Entry entry = (Map.Entry) iterator.next();
      
      final SCAInfo scaInfo = (SCAInfo) entry.getValue();
      
      if (scaInfo.getIsRegistered()) {
	log.debug("Added SCAs - " + scas + " - to " + 
		  getAgentIdentifier() + " with " +
		  scaInfo.getCommunity());
	RegistrationService.Callback cb = new RegistrationService.Callback() {
	  public void invoke(Object o) {
	    boolean success = ((Boolean)o).booleanValue();
	    if (!success) {
	      log.error(getAgentIdentifier() +
			" unable to update registry in " + scaInfo.getCommunity() + 
			" with new SCAs - " + adds);
	    } else {
	      if (log.isInfoEnabled()) {
		log.info(getAgentIdentifier() +
			 " updated registry in " + scaInfo.getCommunity() + 
			 " with new SCAs - " + adds);
	      }
	    }
	  }
	  public void handle(Exception e) {
	    retryErrorLog(getAgentIdentifier() +
			  " unable to update registry in " + 
			  scaInfo.getCommunity() + 
			  " with new SCAs - " + adds + 
			  ", try again later", e);
          }
	};

	registrationService.updateServiceDescription(scaInfo.getCommunity(),
						     getAgentIdentifier().toString(),
						     scaServiceClassifications(adds),
						     cb);
      }      
    }
  }

  private Collection scaServiceClassifications(Collection supportLineageCollection) {
    Collection serviceClassifications =
      new ArrayList(supportLineageCollection.size());
    for (Iterator iterator = supportLineageCollection.iterator();
	 iterator.hasNext();) {
      SupportLineageList supportList = 
	(SupportLineageList) ((LineageListWrapper) iterator.next()).getLineageList();
      ServiceClassification sca =
	new ServiceClassificationImpl(supportList.getRoot(),
				      supportList.getRoot(),
				      UDDIConstants.SUPPORT_COMMAND_ASSIGNMENT);
      serviceClassifications.add(sca);
    }
    return serviceClassifications;
  }

  private boolean isRegistered() {

    // Have all the known SCAs reported in?
    if (scaHash.size() < knownSCAs) {
      if (log.isDebugEnabled()) {
	log.debug(getAgentIdentifier() + " isRegistered(): scaHash.size() = " + 
		  scaHash.size() + " knownSCAs = " + knownSCAs);
      }
      return false;
    }

    Set scaSet = scaHash.entrySet();
    
    for (Iterator iterator = scaSet.iterator();
	 iterator.hasNext();) {
      Map.Entry entry = (Map.Entry) iterator.next();
      
      SCAInfo scaInfo = (SCAInfo) entry.getValue();
      
      if (!scaInfo.getIsRegistered()) {
	if (log.isDebugEnabled()) {
	  log.debug(getAgentIdentifier() + " isRegistered(): " + 
		    scaInfo.getCommunity() + " is not registered.");
	}
	return false;
      }
    } 

    return true;
  }

  private void updateRegisterTaskDispositions(Collection registerTasks) {
    for (Iterator iterator = registerTasks.iterator();
	 iterator.hasNext();) {
      Task task = (Task) iterator.next();
      PlanElement pe = task.getPlanElement();
      double conf = 1.0;

      if (isProvider() && !isRegistered()) {
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
		       " didRehydrate() == " + 
			getBlackboardService().didRehydrate() + 
		       " rehydrated == " + rehydrated);
	    }
	  }
	  conf = 0.0;
	}
      } else {
	conf = 1.0;
      } 

      PlanningFactory planningFactory = 
	(PlanningFactory) domainService.getFactory("planning");
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
      }else if (pe.getEstimatedResult().getConfidenceRating() != conf) {
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

  protected void handleStatusChange() {
    for (Iterator iterator = statusChangeSubscription.iterator(); iterator.hasNext();) {
      final StatusChangeMessage statusChange = (StatusChangeMessage) iterator.next();

      synchronized (statusChange) {
        switch (statusChange.getStatus()) {

        case StatusChangeMessage.REQUESTED:
	  if (!isProvider()) {
	    statusChange.setStatus(StatusChangeMessage.DONE);
	    statusChange.setRegistryUpdated(true);
	    getBlackboardService().publishChange(statusChange);
	    break;
	  } else {
	    synchronized(statusChange) {
	      statusChange.setStatus(StatusChangeMessage.PENDING);
	    }
	    initiateDelete(statusChange);
	  }	
	break;

        case StatusChangeMessage.PENDING:
          // let it go.  might want to check to see if it takes a very long time...
          break;

        case StatusChangeMessage.COMPLETED:
          statusChange.setRegistryUpdated(true);
	  statusChange.setStatus(StatusChangeMessage.DONE);
          getBlackboardService().publishChange(statusChange);
          break;

        case StatusChangeMessage.DONE:
          // should drop it from the list;
          break;

        case StatusChangeMessage.ERROR:
          // retry, perhaps?
          break;
        }
      }
    }

    // Handle statusChangeMessage as trumping the presence of DAML file.
    // Don't change isRegistered flag because that would assume that we
    // should reregister.
    //isRegistered = false;
    return;
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

  private long getWarningCutOffTime() {
    if (warningCutoffTime == 0) {
      WARNING_SUPPRESSION_INTERVAL = Integer.getInteger(REGISTRATION_GRACE_PERIOD_PROPERTY,
							WARNING_SUPPRESSION_INTERVAL).intValue();
            warningCutoffTime = System.currentTimeMillis() + WARNING_SUPPRESSION_INTERVAL*60000;
    }

    return warningCutoffTime;
  }

  private void retryErrorLog(String message) {
    retryErrorLog(message, null);
  }

  // When an error occurs, but we'll be retrying later, treat it as a DEBUG
  // at first. After a while it becomes an error.
  private void retryErrorLog(String message, Throwable e) {
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

  private void handleNewSCA(SupportLineageList sca) {
    if (log.isDebugEnabled()) {
      log.debug(getAgentIdentifier() + " adding sca " + 
		sca.getRoot());
    }
    SCAInfo scaInfo = (SCAInfo) scaHash.get(sca.getRoot());
    
    if (scaInfo == null) {
      scaInfo = new SCAInfo(null, false, false, false);
      scaHash.put(sca.getRoot(), scaInfo);
    }
    
    Community scaCommunity = 
      communityService.getCommunity(getYPCommunityName(sca),
				    new SCACommunityResponseListener(scaInfo));
    
    if (scaCommunity != null) {
      scaInfo.setCommunity(scaCommunity);
      log.debug("Registering: " + getAgentIdentifier() + " with " +
		scaInfo.getCommunity());
      initialRegister(scaInfo);
    }
  }

  private void initiateDelete(final StatusChangeMessage statusChange) {
    Set scaSet = scaHash.entrySet();

    for (Iterator scaIterator = scaSet.iterator();
	 scaIterator.hasNext();) {
      Map.Entry entry = (Map.Entry) scaIterator.next();
      
      final SCAInfo scaInfo = (SCAInfo) entry.getValue();
      
      if (scaInfo.getIsRegistered() ||
	  scaInfo.getPendingRegistration()) {
	// Delete entry
	Collection serviceClassifications = new ArrayList(1);
	ServiceClassification sca =
	  new ServiceClassificationImpl(statusChange.getRole(),
					statusChange.getRole(),
					UDDIConstants.MILITARY_SERVICE_SCHEME);
	serviceClassifications.add(sca);
	
	RegistrationService.Callback cb =
	  new RegistrationService.Callback() {
	  public void invoke(Object o) {
	    boolean success = ((Boolean) o).booleanValue();
	    synchronized(statusChange) {
	      scaInfo.setIsDeleted(true);
	      updateStatusChange(statusChange);
	    }
	    getBlackboardService().signalClientActivity();
	  }

	  public void handle(Exception e) {
	    log.error("handleStatusChange", e);
	    synchronized(statusChange) {
	      statusChange.setStatus(StatusChangeMessage.ERROR);
	    }
	    getBlackboardService().signalClientActivity();
	  }
	};

	registrationService.deleteServiceDescription(scaInfo.getCommunity(),
						     getAgentIdentifier().toString(),
						     serviceClassifications,
						     cb);
      }
    }
  }

  private void updateStatusChange(final StatusChangeMessage statusChange) {
    Set scaSet = scaHash.entrySet();
    boolean complete = true;
    for (Iterator scaIterator = scaSet.iterator();
	 scaIterator.hasNext();) {
      Map.Entry entry = (Map.Entry) scaIterator.next();
      
      SCAInfo scaInfo = (SCAInfo) entry.getValue();
      if (!scaInfo.getIsDeleted()) {
	break;
      }
    }
    
    if (complete) {
      statusChange.setStatus(StatusChangeMessage.COMPLETED);
    }
  }

  private String getYPCommunityName(SupportLineageList sca) {
    // For now assume every SCA represented by a YPCommunity called
    // <sca>-YPCOMMUNITY
    return sca.getRoot() + "-YPCOMMUNITY";
  }

  private ProviderCapabilities createProviderCapabilities() {
    ProviderDescription pd = getPD();
    Collection serviceProfiles = pd.getServiceProfiles();

    SDFactory sdFactory = (SDFactory) domainService.getFactory(SDDomain.SD_NAME);
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
	  providerCapabilities.addCapability(role, echelon);
	  break;
	}
      }
    }

    return providerCapabilities;
  }
    
  private class SCAInfo {
    private Community mySCACommunity;
    private SCACommunityChangeListener myCommunityListener;
    private boolean myIsRegistered;
    private boolean myIsDeleted;
    private boolean myPendingRegistration;
    
    public SCAInfo(Community scaCommunity, boolean isRegistered, 
		   boolean pendingRegistration, boolean isDeleted) {
      mySCACommunity = scaCommunity;
      myIsRegistered = isRegistered;
      myPendingRegistration = pendingRegistration;
      myIsDeleted = isDeleted;
    }

    public Community getCommunity(){
      return mySCACommunity;
    }

    public void setCommunity(Community scaCommunity){
      if (scaCommunity == null) {
	clearCommunity();
      } else {
	if (mySCACommunity == null) {
	  if (log.isDebugEnabled()) {
	    log.debug(getAgentIdentifier() + " adding listener for " + 
		      scaCommunity);
	  }
	  mySCACommunity = scaCommunity;
	
	  // First time so set up change listener
	  myCommunityListener = new SCACommunityChangeListener(this);
	  communityService.addListener(myCommunityListener);
	} else {
	  mySCACommunity = scaCommunity;
	}
      }
    }


    public void clearCommunity() {
      if (log.isDebugEnabled()) {
	log.debug(getAgentIdentifier() + " removing listener for " + mySCACommunity);
      }
      mySCACommunity = null;
      if (myCommunityListener != null) {
	communityService.removeListener(myCommunityListener);
      }
    }

    public boolean getIsRegistered() {
      return myIsRegistered;
    }

    public void setIsRegistered(boolean isRegistered){ 
      if ((myIsRegistered) && (!isRegistered) &&
	  (log.isDebugEnabled())) {
	RuntimeException re  = new RuntimeException();
	log.debug(getAgentIdentifier() + " setIsRegistered() going from true to false.", re);
      }
      myIsRegistered = isRegistered;
    }

    public boolean getPendingRegistration() {
      return myPendingRegistration;
    }

    public void setPendingRegistration(boolean pendingRegistration){ 
      myPendingRegistration = pendingRegistration;
    }

    public boolean getIsDeleted() {
      return myIsDeleted;
    }

    public void setIsDeleted(boolean isDeleted){ 
      myIsDeleted = isDeleted;
    }

    public boolean readyToRegister() {
      return ((getCommunity() != null) &&
	      (!getIsRegistered()) &&
	      (!getPendingRegistration()) &&
              (!getIsDeleted()));
    }
  }

  private class SCACommunityResponseListener 
  implements CommunityResponseListener {
    private SCAInfo scaInfo;

    public SCACommunityResponseListener(SCAInfo info) {
      scaInfo = info;
    }

    public void getResponse(CommunityResponse resp){
      if (log.isDebugEnabled()) {
	log.debug(getAgentIdentifier() + " got Community info for " +
		  (Community) resp.getContent());
      }

      Community scaCommunity = (Community) resp.getContent();

      scaInfo.setCommunity(scaCommunity);
      getBlackboardService().signalClientActivity();
    }
  }

  private class SCACommunityChangeListener 
  implements CommunityChangeListener {
    private SCAInfo scaInfo;
    String communityName;

    public SCACommunityChangeListener(SCAInfo info) {
      scaInfo = info;
      communityName = scaInfo.getCommunity().getName();
    }

    public void communityChanged(CommunityChangeEvent event){
      Community scaCommunity = event.getCommunity();

      // Paranoia code - bug in community code seems to lead to
      // notifications with null communities.
      if (scaCommunity == null) {
	log.debug(getAgentIdentifier() + 
		  " received Community change info for a null community");
	return;
      }

      if (log.isDebugEnabled()) {
	log.debug(getAgentIdentifier() + " got Community change info for " +
		  scaCommunity);
      }

      if (scaCommunity == null) {
	log.debug(getAgentIdentifier() + 
		  " received Community change info for a null community");
	return;
      }

      if (scaCommunity.getName().equals(getCommunityName())) {
	scaInfo.setCommunity(scaCommunity);

	if (scaInfo.readyToRegister()) {
	  if (log.isDebugEnabled()) {
	    log.debug(getAgentIdentifier() + " signalClientActivity for " + 
		      scaCommunity);
	  }
	  
	  if (getBlackboardService() == null) {
	    log.warn(getAgentIdentifier() + " ignoring change notification " +
		     " - getBlackboardService() returned null");
	    scaInfo.clearCommunity();
	  } else {
	    getBlackboardService().signalClientActivity();
	  }
	}
      } else if (log.isDebugEnabled()) {
	log.debug(getAgentIdentifier() + 
		  " ignoring CommunityChangeEvent  for " + 
		  scaCommunity.getName() + 
		  " - listening for - " + getCommunityName());
      }

    }

    public String getCommunityName() {
      return communityName;
    }
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


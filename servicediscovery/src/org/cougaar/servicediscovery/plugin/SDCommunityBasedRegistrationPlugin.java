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

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.blackboard.PrivilegedClaimant;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AlarmService;
import org.cougaar.core.service.community.Community;
import org.cougaar.core.service.community.CommunityChangeEvent;
import org.cougaar.core.service.community.CommunityChangeListener;
import org.cougaar.core.service.community.CommunityResponse;
import org.cougaar.core.service.community.CommunityResponseListener;
import org.cougaar.core.service.community.CommunityService;

import org.cougaar.planning.ldm.plan.Task;

import org.cougaar.servicediscovery.Constants;
import org.cougaar.servicediscovery.description.AvailabilityChangeMessage;
import org.cougaar.servicediscovery.description.LineageList;
import org.cougaar.servicediscovery.description.LineageListWrapper;
import org.cougaar.servicediscovery.description.ProviderDescription;
import org.cougaar.servicediscovery.description.ServiceCategory;
import org.cougaar.servicediscovery.description.ServiceClassification;
import org.cougaar.servicediscovery.description.ServiceClassificationImpl;
import org.cougaar.servicediscovery.description.SupportLineageList;
import org.cougaar.servicediscovery.service.RegistrationService;
import org.cougaar.servicediscovery.util.UDDIConstants;
import org.cougaar.servicediscovery.SDFactory;
import org.cougaar.servicediscovery.SDDomain;

import org.cougaar.util.TimeSpan;
import org.cougaar.util.UnaryPredicate;



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
public class SDCommunityBasedRegistrationPlugin extends SDRegistrationPluginBase {
  private CommunityService communityService = null;

  private HashMap scaHash = null;

  public void setCommunityService(CommunityService cs) { 
    this.communityService = cs; 
  }

  public void suspend() {
    super.suspend();

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
    super.setupSubscriptions();

    scaHash = new HashMap();

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
    super.execute();

    if (isProvider()) {
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
    }

    // No harm since publish change only occurs if the conf rating has changed.
    updateRegisterTaskDispositions();
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

  protected boolean registrationComplete() {

    // Have all the known SCAs reported in?
    if (scaHash.size() < knownSCAs) {
      if (log.isDebugEnabled()) {
	log.debug(getAgentIdentifier() + " registrationComplete(): scaHash.size() = " + 
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
	  log.debug(getAgentIdentifier() + " registrationComplete(): " + 
		    scaInfo.getCommunity() + " is not registered.");
	}
	return false;
      }
    } 

    return true;
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

  protected void addRegisteredRole(final AvailabilityChangeMessage availabilityChange) {
    Set scaSet = scaHash.entrySet();

    for (Iterator scaIterator = scaSet.iterator();
	 scaIterator.hasNext();) {
      Map.Entry entry = (Map.Entry) scaIterator.next();
      
      final SCAInfo scaInfo = (SCAInfo) entry.getValue();
      
      if (scaInfo.getIsRegistered() ||
	  scaInfo.getPendingRegistration()) {
	// Add the role
	Collection serviceClassifications = new ArrayList(1);
	ServiceClassification sca =
	  new ServiceClassificationImpl(availabilityChange.getRole().toString(),
					availabilityChange.getRole().toString(),
					UDDIConstants.MILITARY_SERVICE_SCHEME);
	serviceClassifications.add(sca);
	
	RegistrationService.Callback cb =
	  new RegistrationService.Callback() {
	  public void invoke(Object o) {
	    if (log.isDebugEnabled()) {
	      log.debug(getAgentIdentifier() + 
			" added yp registration for role " +
			availabilityChange.getRole() + 
			"with " + scaInfo.getCommunity());
	    }
	    getBlackboardService().signalClientActivity();
	  }

	  public void handle(Exception e) {
	    log.error(getAgentIdentifier() + ":addRegisteredRole()", e);
	    synchronized(availabilityChange) {
	      availabilityChange.setStatus(AvailabilityChangeMessage.ERROR);
	    }
	    getBlackboardService().signalClientActivity();
	  }
	};
	registrationService.updateServiceDescription(scaInfo.getCommunity(),
						     getAgentIdentifier().toString(),
						     serviceClassifications,
						     cb);
      } else {
	if (log.isInfoEnabled()) {
	  log.info(getAgentIdentifier() + 
		   " unable to add yp registration for role " +
		   availabilityChange.getRole() + 
		   "with " + scaInfo.getCommunity());
	}
      }
    }
  }

  protected void removeRegisteredRole(final AvailabilityChangeMessage availabilityChange) {
    Set scaSet = scaHash.entrySet();

    for (Iterator scaIterator = scaSet.iterator();
	 scaIterator.hasNext();) {
      Map.Entry entry = (Map.Entry) scaIterator.next();
      
      final SCAInfo scaInfo = (SCAInfo) entry.getValue();
      
      if (scaInfo.getIsRegistered() ||
	  scaInfo.getPendingRegistration()) {
	// Delete role
	Collection serviceClassifications = new ArrayList(1);
	ServiceClassification sca =
	  new ServiceClassificationImpl(availabilityChange.getRole().toString(),
					availabilityChange.getRole().toString(),
					UDDIConstants.MILITARY_SERVICE_SCHEME);
	serviceClassifications.add(sca);
	
	RegistrationService.Callback cb =
	  new RegistrationService.Callback() {
	  public void invoke(Object o) {
	    if (log.isDebugEnabled()) {
	      log.debug(getAgentIdentifier() + 
			" removed yp registration for role " +
			availabilityChange.getRole() + 
			"with " + scaInfo.getCommunity());
	    }
	    getBlackboardService().signalClientActivity();
	  }

	  public void handle(Exception e) {
	    log.error(getAgentIdentifier() + ":removeRegisteredRole()", e);
	    synchronized(availabilityChange) {
	      availabilityChange.setStatus(AvailabilityChangeMessage.ERROR);
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

  private String getYPCommunityName(SupportLineageList sca) {
    // For now assume every SCA represented by a YPCommunity called
    // <sca>-YPCOMMUNITY
    return sca.getRoot() + "-YPCOMMUNITY";
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
}

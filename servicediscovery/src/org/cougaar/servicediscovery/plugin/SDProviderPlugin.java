/*
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.servicediscovery.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.service.LoggingService;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.planning.ldm.plan.ScheduleElement;
import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.servicediscovery.SDDomain;
import org.cougaar.servicediscovery.SDFactory;
import org.cougaar.servicediscovery.description.ProviderCapabilities;
import org.cougaar.servicediscovery.description.ProviderCapability;
import org.cougaar.servicediscovery.description.ServiceContract;
import org.cougaar.servicediscovery.description.ServiceContractImpl;
import org.cougaar.servicediscovery.description.ServiceRequest;
import org.cougaar.servicediscovery.transaction.ProviderServiceContractRelay;
import org.cougaar.servicediscovery.transaction.ServiceContractRelay;
import org.cougaar.util.MutableTimeSpan;
import org.cougaar.util.TimeSpan;
import org.cougaar.util.UnaryPredicate;

/**
 * SDProviderPlugin generates the LineageLists for the Agent.
 */
public class SDProviderPlugin extends SimplePlugin
{
  private static Integer START_TIME_KEY = new Integer(AspectType.START_TIME);
  private static Integer END_TIME_KEY = new Integer(AspectType.END_TIME);


  private IncrementalSubscription mySelfOrgSubscription;
  private IncrementalSubscription myServiceContractRelaySubscription;
  private IncrementalSubscription myProviderCapabilitiesSubscription;
  

  private String myAgentName;

  protected LoggingService myLoggingService;

  protected SDFactory mySDFactory;

  private UnaryPredicate myServiceContractRelayPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof ProviderServiceContractRelay) {
	ServiceContractRelay relay = (ServiceContractRelay) o;
	return (relay.getProviderName().equals(myAgentName));
      } else {
	return false;
      }
    }
  };

  private UnaryPredicate mySelfOrgPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Organization) {
	Organization org = (Organization) o;
	if (org.isLocal()) {
	  return true;
	}
      }
      return false;
    }
  };

  private UnaryPredicate myProviderCapabilitiesPred = 
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

  protected void setupSubscriptions() {
    myLoggingService =
      (LoggingService) getBindingSite().getServiceBroker().getService(this, LoggingService.class, null);

    myAgentName = getBindingSite().getAgentIdentifier().toString();

    myServiceContractRelaySubscription = (IncrementalSubscription) subscribe(myServiceContractRelayPred);
    mySelfOrgSubscription = (IncrementalSubscription) subscribe(mySelfOrgPred);
    myProviderCapabilitiesSubscription = 
      (IncrementalSubscription)subscribe(myProviderCapabilitiesPred);

    mySDFactory = (SDFactory) getFactory(SDDomain.SD_NAME);
  }


  public void execute() {
    if (myServiceContractRelaySubscription.hasChanged()) {

      Collection addedRelays =
	myServiceContractRelaySubscription.getAddedCollection();
      for (Iterator adds = addedRelays.iterator(); adds.hasNext();) {
	ProviderServiceContractRelay relay = 
	  (ProviderServiceContractRelay) adds.next();

	if (myLoggingService.isDebugEnabled()) {
	  myLoggingService.debug(getAgentIdentifier() +
				 ": execute() new ServiceContractRelay - " +
				 relay);
	}
        handleServiceContractRelay(relay);
      }

      Collection changedRelays = 
	myServiceContractRelaySubscription.getChangedCollection();
      for (Iterator changes = changedRelays.iterator(); changes.hasNext();) {
	ProviderServiceContractRelay relay = 
	  (ProviderServiceContractRelay) changes.next();

	if (myLoggingService.isDebugEnabled()) {
	  myLoggingService.debug(getAgentIdentifier() +
				 ": execute() modified ServiceContractRelay - " +
				 relay);
	}
	handleServiceContractRelay(relay);
      }

      // Not currently handling removes
    }

    if(myProviderCapabilitiesSubscription.hasChanged()) {
      Collection changedCapabilities = 
	myProviderCapabilitiesSubscription.getChangedCollection();
      
      for (Iterator changes = changedCapabilities.iterator();
	   changes.hasNext();) {
	ProviderCapabilities capabilities = 
	  (ProviderCapabilities) changes.next();
	handleChangedProviderCapabilities(capabilities);
      }
    }
  }

  protected void handleServiceContractRelay(ProviderServiceContractRelay relay){
    ServiceRequest serviceRequest = relay.getServiceRequest();
    ServiceContract serviceContract = relay.getServiceContract();

    HashMap contractPreferences = 
      copyPreferences(serviceRequest.getServicePreferences());

    boolean contractChanged = false;
    boolean contractExists = (serviceContract != null);

    // Replace start/end time preferences
    // Construct start/end pref by making request agree with capability avail
    // schedule
    TimeSpan requestedTimeSpan = 
      mySDFactory.getTimeSpanFromPreferences(serviceRequest.getServicePreferences());

    TimeSpan contractTimeSpan = (serviceContract != null) ?
      mySDFactory.getTimeSpanFromPreferences(serviceContract.getServicePreferences()) :
      null;

    if (myLoggingService.isDebugEnabled()) {
      String contractTimeSpanString = (contractTimeSpan == null) ?
	"" : 
	new Date(contractTimeSpan.getStartTime()) + 
	" " + 
	new Date(contractTimeSpan.getEndTime());

      myLoggingService.debug(getAgentIdentifier() +
			     ": handleServiceContractRelay() relay = " + relay + 
			     " " + relay.getUID() +
			     " requestedTimeSpan = " + 
			     new Date(requestedTimeSpan.getStartTime()) + " " +
			     new Date(requestedTimeSpan.getEndTime()) + 
			     contractTimeSpanString);
    }

    Collection modifiedContractPreferences = null;
 
    if (requestedTimeSpan == null) {
      if (myLoggingService.isInfoEnabled()) {
	myLoggingService.info(getAgentIdentifier() + 
			      ": handleServiceContractRelay() unable to handle service request - " + 
			      relay.getServiceRequest() +
			      " - does not have start and/or end time preferences.");
      }   

      // Remove start/end preferences since provider can't meet request.
      contractPreferences.remove(START_TIME_KEY);
      contractPreferences.remove(END_TIME_KEY);
      contractChanged = true;
    } else {
      TimeSpan verifiedContractTimeSpan = 
	checkProviderCapability(getProviderCapability(serviceRequest.getServiceRole()),
				requestedTimeSpan);
      
      if (verifiedContractTimeSpan == null) {
	if (myLoggingService.isInfoEnabled()) {
	  myLoggingService.info(getAgentIdentifier() + 
				": handleServiceContractRelay() unable to handle service request - " + 
				relay.getServiceRequest() + 
				" - does not match provider capabilities.");
	}

	// Remove start/end preferences since provider can't meet request.
	contractPreferences.remove(START_TIME_KEY);
	contractPreferences.remove(END_TIME_KEY);
	contractChanged = true;
      } else if (contractExists) {
	//compare with existing contract
	if (myLoggingService.isDebugEnabled()) {
	  myLoggingService.debug(getAgentIdentifier() + 
				 ": handleServiceContractRelay() - " +
				 " current time span = " +
				 new Date(contractTimeSpan.getStartTime()) +
				 " to " + 
				 new Date(contractTimeSpan.getEndTime()) +
				 " new time span = " +
				 new Date(verifiedContractTimeSpan.getStartTime()) +
				 " to " + 
				 new Date(verifiedContractTimeSpan.getEndTime()) + 
				 " (current == new) = " +
				 contractTimeSpan.equals(verifiedContractTimeSpan));
	}

	if (!contractTimeSpan.equals(verifiedContractTimeSpan)) {
	  // Replace contract start end with requested start end
	  modifiedContractPreferences = 
	    mySDFactory.createTimeSpanPreferences(verifiedContractTimeSpan);

	  contractChanged = true;
	}
      } else if (!verifiedContractTimeSpan.equals(requestedTimeSpan)) {
	// Replace start/end with what provider can handle.
	modifiedContractPreferences = 
	  mySDFactory.createTimeSpanPreferences(verifiedContractTimeSpan);

	contractChanged = true;
      }
    }

	
    if (modifiedContractPreferences != null) {
      for (Iterator iterator = modifiedContractPreferences.iterator();
	   iterator.hasNext(); ) {
	Preference preference = (Preference) iterator.next();
	contractPreferences.put(new Integer(preference.getAspectType()),
				preference);
      }
    }

    if (!contractExists) {
      if (myLoggingService.isInfoEnabled()) {
	myLoggingService.info(getAgentIdentifier() + 
			      ": added new ServiceContract on a relay from " + 
			      relay.getUID() + " for the role " + 
			      serviceRequest.getServiceRole());
      }
      serviceContract =
	mySDFactory.newServiceContract(getSelfOrg(),
				       serviceRequest.getServiceRole(),
				       contractPreferences.values());
      relay.setServiceContract(serviceContract);
    } else if (contractChanged) {
      if (myLoggingService.isInfoEnabled()) {
	myLoggingService.info(getAgentIdentifier() + 
			      ": changed ServiceContract on a relay from " + 
			      relay.getUID() +
			      " for the role " + serviceRequest.getServiceRole());
      }
      ((ServiceContractImpl) serviceContract).setServicePreferences(contractPreferences.values());
    }

    if ((!contractExists) || (contractChanged)) {
      publishChange(relay);
    }
    
  }
  void handleChangedProviderCapabilities(ProviderCapabilities capabilities) {
    if (myLoggingService.isInfoEnabled()) {
        myLoggingService.info(getAgentIdentifier() + 
			      " changed ProviderCapabilities " + 
			      capabilities);
    }

    for (Iterator iterator = capabilities.getCapabilities().iterator();
	 iterator.hasNext();) {
      ProviderCapability capability = (ProviderCapability) iterator.next();
      Collection contracts = getMatchingContracts(capability);
      
      for (Iterator relayIterator = contracts.iterator(); 
	   relayIterator.hasNext();) {
	ProviderServiceContractRelay relay = 
	  (ProviderServiceContractRelay) relayIterator.next();
	ServiceContract contract = relay.getServiceContract();

	if (!contract.isRevoked()) {
	  boolean changeRequired = false;

	  TimeSpan currentContractTimeSpan = 
	    mySDFactory.getTimeSpanFromPreferences(contract.getServicePreferences());
	  
	  if (currentContractTimeSpan != null) {
	    TimeSpan contractTimeSpan = 
	      checkProviderCapability(capability, currentContractTimeSpan);
	    if (contractTimeSpan == null) {
	      if (myLoggingService.isInfoEnabled()) {
		myLoggingService.info(getAgentIdentifier() + 
				      " revoking contract " + 
				      relay.getServiceContract() + 
				      " due to provider capability change.");
	      }
	      mySDFactory.revokeServiceContract(contract);
	      changeRequired = true;
	    } else if (!(currentContractTimeSpan.equals(contractTimeSpan))) {
	      if (myLoggingService.isInfoEnabled()) {
		myLoggingService.info(getAgentIdentifier() +
				      " changing contract availability to match " +
				      " provider availability: current contract time span " + 
				      currentContractTimeSpan.getStartTime() +
				      " - " +
				      currentContractTimeSpan.getEndTime() +
				      " provider capability " +
				      contractTimeSpan.getStartTime() + " - " +
				      contractTimeSpan.getEndTime());
	      }
				      
	      HashMap copy = 
		copyPreferences(contract.getServicePreferences());
	      
	      Collection timespanPreferences = 
		mySDFactory.createTimeSpanPreferences(contractTimeSpan);

	      // Replace start/end with what provider can handle.
	      for (Iterator preferenceIterator = timespanPreferences.iterator();
		   preferenceIterator.hasNext(); ) {
		Preference preference = (Preference) preferenceIterator.next();
		copy.put(new Integer(preference.getAspectType()),
			 preference);
	      }

	      ServiceContract newContract = 
		mySDFactory.newServiceContract(getSelfOrg(),
					       contract.getServiceRole(),
					       copy.values());
	      relay.setServiceContract(newContract);
	      changeRequired = true;
	    }
	  }
	  
	  if (changeRequired) {
	    if (myLoggingService.isInfoEnabled()) {
	      myLoggingService.info(getAgentIdentifier() +
				    " handleChangeProviderCap " +" publish change relay for "
+ contract.getServiceRole() + "  " + contract);
	    }
	    publishChange(relay);
          }
        }
      }
    }
  }

  protected Collection getMatchingContracts(ProviderCapability capability) {
    ArrayList matchingRelays = new ArrayList();
    
    for (Iterator contracts = myServiceContractRelaySubscription.getCollection().iterator();
	 contracts.hasNext();) {
      ServiceContractRelay relay = (ServiceContractRelay) contracts.next();
      ServiceContract contract = relay.getServiceContract();
      //find the service contract relay with matching role to the service disrupted
      if (contract.getServiceRole().equals(capability.getRole())) {
        matchingRelays.add(relay);
      }
    }
    return matchingRelays;
  }

  protected Organization getSelfOrg() {
    for (Iterator iterator = mySelfOrgSubscription.iterator();
	 iterator.hasNext();) {
      return (Organization) iterator.next();
    }

    return null;
  }

  protected ProviderCapability getProviderCapability(Role role) { 
    for (Iterator iterator = myProviderCapabilitiesSubscription.iterator();
	 iterator.hasNext();) {
      ProviderCapabilities capabilities = (ProviderCapabilities) iterator.next();
      ProviderCapability capability = 
	capabilities.getCapability(role);

      if (myLoggingService.isInfoEnabled()) {
	myLoggingService.info(getAgentIdentifier() +
			      " getCapability returned " + capability + 
			      " for "  + role);
      }
      
      if (capability != null) {
	return capability;
      }
      
    }
    
    return null;
  }
  
  protected TimeSpan checkProviderCapability(ProviderCapability capability, 
					     TimeSpan requestedTimeSpan) {
    // Deliberately set to invalid values so can catch case where provider
    // doesn't handle thee specified role
    long earliest = TimeSpan.MAX_VALUE;
    long latest = TimeSpan.MIN_VALUE;
    
    if (capability != null) {
      Schedule currentAvailability = 
	capability.getAvailableSchedule();
      
      Collection overlaps = 
	currentAvailability.getOverlappingScheduleElements(requestedTimeSpan.getStartTime(),
							   requestedTimeSpan.getEndTime());
      
      if (overlaps.size() != 0) {
	for (Iterator overlap = overlaps.iterator();
	     overlap.hasNext();) {
	  // Take info from first overlan. We don't yet have the ability to
	  // include a schedule of timespans.
	  ScheduleElement scheduleElement = (ScheduleElement) overlap.next();
	  earliest = 
	    Math.max(scheduleElement.getStartTime(),
		     requestedTimeSpan.getStartTime());
	  latest = 
	    Math.min(scheduleElement.getEndTime(),
		     requestedTimeSpan.getEndTime());
	  break;
	}
      } else {
	if (myLoggingService.isInfoEnabled()) {
	  myLoggingService.info(getAgentIdentifier() + 
				" no overlaps, requestedTimeSpan = " +
				requestedTimeSpan +
				" available schedule " + currentAvailability);
	}
      }
    }
  
    if ((earliest == TimeSpan.MAX_VALUE) || 
	(latest == TimeSpan.MIN_VALUE)) {
      return null;
    } else {
      MutableTimeSpan returnTimeSpan = new MutableTimeSpan();
      returnTimeSpan.setTimeSpan(earliest, latest);
      return returnTimeSpan;
    }
  }
 
  private HashMap copyPreferences(Collection preferences) {
    HashMap preferenceMap = new HashMap(preferences.size());

    for (Iterator iterator = preferences.iterator();
	 iterator.hasNext();) {
      Preference original = (Preference) iterator.next();
      
      Preference copy =
	getFactory().newPreference(original.getAspectType(),
				   original.getScoringFunction(),
				   original.getWeight());
      preferenceMap.put(new Integer(copy.getAspectType()), copy);
    }

    return preferenceMap;
  }
}

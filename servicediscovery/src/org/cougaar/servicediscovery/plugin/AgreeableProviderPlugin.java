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

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.LoggingService;

import org.cougaar.planning.ldm.PlanningDomain;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.HasRelationships;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.Preference;

import org.cougaar.servicediscovery.SDDomain;
import org.cougaar.servicediscovery.SDFactory;
import org.cougaar.servicediscovery.description.ServiceContract;
import org.cougaar.servicediscovery.description.ServiceRequest;
import org.cougaar.servicediscovery.transaction.ProviderServiceContractRelay;
import org.cougaar.servicediscovery.transaction.ServiceContractRelay;

import org.cougaar.util.TimeSpan;
import org.cougaar.util.UnaryPredicate;

/**
 * ServiceDiscovery Plugin at Provider that agrees to new incoming requests as is.
 * It receives ServiceContractRelays, and creates new ServiceContracts that 
 * exactly match the request, sending that as the response back on the relay. 
 * The SD Relay will see the Contract, and fill in the necessary Relationship
 * on the Entities, so that the relationship can be used, and Tasks allocated. 
 *<p>
 * Extenders of this plugin might not agree to all requests, but for example might
 * check a local capacity, or put time constraints on the contracts. More complex
 * providers support contracts being revoked, for example.
 *<p>
 * This plugin is limited by: not looking at ProviderCapabilities or handling
 * changes to them, and not handling changes to service contract requests.
 */
public class AgreeableProviderPlugin extends ComponentPlugin {

  // Constants used to address Time preferences
  private static final Integer START_TIME_KEY = new Integer(AspectType.START_TIME);
  private static final Integer END_TIME_KEY = new Integer(AspectType.END_TIME);

  /** Subscription to the self Entity */
  private IncrementalSubscription myLocalEntitySubscription;

  /** Subscription to service contract requests */
  private IncrementalSubscription myServiceContractRelaySubscription;
  
  /** Name of the local agent */
  private String myAgentName;

  // Services and factories
  protected LoggingService myLoggingService;
  protected DomainService myDomainService;
  protected SDFactory mySDFactory;
  protected PlanningFactory planningFactory;

  /**
   * Subscription for ServiceContractRelays where the desired provider is me.
   */
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

  /** Subscription for the self Entity or Organization */
  private UnaryPredicate myLocalEntityPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Asset && o instanceof HasRelationships) {
	HasRelationships entity = (HasRelationships) o;
	if (entity.isLocal()) {
	  return true;
	}
      }
      return false;
    }
  };

  /**
   * Used by the binding utility through reflection to set my DomainService
   */
  public void setDomainService(DomainService domainService) {
    myDomainService = domainService;
  }

  /**
   * Get a pointer to the DomainService, for creating factories.
   */
  public DomainService getDomainService() {
    return myDomainService;
  }

  /**
   * Load less critical services, and get pointers to factoreis.
   */
  public void load() {
    super.load();

    mySDFactory = (SDFactory) getDomainService().getFactory(SDDomain.SD_NAME);
    planningFactory = (PlanningFactory) getDomainService().getFactory(PlanningDomain.PLANNING_NAME);

    // Done with Domain service, so release it
    getServiceBroker().releaseService(this, DomainService.class, getDomainService());

    myLoggingService =
      (LoggingService)getServiceBroker().getService(this, LoggingService.class, null);

    if (myLoggingService == null)
      myLoggingService = LoggingService.NULL;
    
    myAgentName = getAgentIdentifier().toString();
  }

  /**
   * Every load method should have an unload; Here we unload the LoggingService.
   */
  public void unload() {
    if (myLoggingService != LoggingService.NULL)
      getServiceBroker().releaseService(this, LoggingService.class, myLoggingService);

    super.unload();
  }

  /**
   * Create subscriptions to ServiceContractRelays and the self Asset.
   */
  protected void setupSubscriptions() {
    myServiceContractRelaySubscription = 
      (IncrementalSubscription) getBlackboardService().subscribe(myServiceContractRelayPred);
    myLocalEntitySubscription = 
      (IncrementalSubscription) getBlackboardService().subscribe(myLocalEntityPred);
  }

  /** 
   * For each new ServiceContractRelay, reply with a Contract exactly matching what they requested. 
   */
  public void execute() {
    if (myServiceContractRelaySubscription.hasChanged()) {

      // For each new service contract request, handle it
      Collection addedRelays =
	myServiceContractRelaySubscription.getAddedCollection();
      for (Iterator adds = addedRelays.iterator(); adds.hasNext();) {
	ProviderServiceContractRelay relay = 
	  (ProviderServiceContractRelay) adds.next();

	if (myLoggingService.isDebugEnabled()) {
	  myLoggingService.debug("execute() new ServiceContractRelay - " +
				 relay);
	}
        handleServiceContractRelay(relay);
      }

      // We do not handle changed or removed service contract requests....
      if (myLoggingService.isDebugEnabled()) {

	Collection changedRelays = 
	  myServiceContractRelaySubscription.getChangedCollection();
	for (Iterator changes = changedRelays.iterator(); changes.hasNext();) {
	  ProviderServiceContractRelay relay = 
	    (ProviderServiceContractRelay) changes.next();
	  
	  myLoggingService.debug("execute() ignoring modifed ServiceContractRelay - " +
				 relay);
	}
	
	
	Collection removedRelays = 
	  myServiceContractRelaySubscription.getRemovedCollection();
	for (Iterator removes = removedRelays.iterator(); removes.hasNext();) {
	  ProviderServiceContractRelay relay = 
	    (ProviderServiceContractRelay) removes.next();
	  
	  myLoggingService.debug("execute: ignoring removed ServiceContractRelay - " +
				 relay);
	}
      } // end of ifDebug
    } // end of block for changed ServiceContractRelays
  } // end of execute()

  /** 
   * Create a ServiceContract that matches the request, add it to the Relay, and publishChange the Relay.
   * This is how the plugin responds to the customer. In this case, the response is always YES to whatever
   * they asked. Other users might, for example, check the capacity of this provider in some way. Or
   * confirm that the local self Entity has the requested Role.
   *
   * @param relay the service request relay to which we will respond
   */
  protected void handleServiceContractRelay(ProviderServiceContractRelay relay){
    // Pull out the request
    ServiceRequest serviceRequest = relay.getServiceRequest();
    ServiceContract serviceContract = relay.getServiceContract();

    // Once a relay has a contract, it is done.
    if (serviceContract != null) { 
      if (myLoggingService.isDebugEnabled())
	myLoggingService.debug("handleServiceContractRelay() relay = " + relay + 
			     " " + relay.getUID() +
			     " already has a contract. Not handling changed/removed requests.");

      return;
    }

    // Now handle the preferences on the Contract request we just received......
     
    // Make a copy of the preferences, to avoid modifying them in place.
    // Note that in our example, these are basically none.
    Map contractPreferences = 
      copyPreferences(serviceRequest.getServicePreferences());

    // Get a TimeSpan to represent when they wanted the service. In the pizza app, this is FOREVER.
    TimeSpan requestedTimeSpan = 
      mySDFactory.getTimeSpanFromPreferences(serviceRequest.getServicePreferences());

    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug("handleServiceContractRelay() relay = " + relay + 
			     " " + relay.getUID() +
			     " requestedTimeSpan = " + 
			     new Date(requestedTimeSpan.getStartTime()) + " " +
			     new Date(requestedTimeSpan.getEndTime()));
    }

    // If the request was poorly formed, the timespan might be null....
    if (requestedTimeSpan == null) {
      myLoggingService.error("handleServiceContractRelay() unable to satisfy service request - " + 
			     relay.getServiceRequest() +
			     " - does not have start and/or end time preferences.");
      
      // Remove start/end preferences since provider can't meet request.
      contractPreferences.remove(START_TIME_KEY);
      contractPreferences.remove(END_TIME_KEY);
    }

    // OK, we are ready to respond now...

    // Create a new service contract between the local Entity, to provide the requested role,
    // with the preferences as requested on the Relay.
    serviceContract =
      mySDFactory.newServiceContract(getLocalEntity(),
				     serviceRequest.getServiceRole(),
				     contractPreferences.values());
    relay.setServiceContract(serviceContract);

    if (myLoggingService.isInfoEnabled()) {
      myLoggingService.info("added new ServiceContract on a relay from " + 
			    relay.getUID() + " for the role " + 
			    serviceRequest.getServiceRole());
    }

    // This is where the Provider responds to the client. The SD LP will see the Relay,
    // and construct the relationships at both agents -- we have a new relationship!
    getBlackboardService().publishChange(relay);
    
  } // end of handleServiceContractRelay

  /**
   * Get the local (self) Entity; the first Asset on the Entity subscription.
   * @return the local (self) Asset, if any on our subscription
   */
  protected Asset getLocalEntity() {
    for (Iterator iterator = myLocalEntitySubscription.iterator();
	 iterator.hasNext();) {
      return (Asset) iterator.next();
    }

    return null;
  }

  /**
   * Deep copy a set of preferences, to avoid any modification errors.
   * @param preferences to copy
   * @return a Map of the exact same preferences.
   */
  private Map copyPreferences(Collection preferences) {
    Map preferenceMap = new HashMap(preferences.size());

    for (Iterator iterator = preferences.iterator();
	 iterator.hasNext();) {
      Preference original = (Preference) iterator.next();
      
      Preference copy =
	planningFactory.newPreference(original.getAspectType(),
					original.getScoringFunction(),
					original.getWeight());
      preferenceMap.put(new Integer(copy.getAspectType()), copy);
    }

    return preferenceMap;
  }
} // end of AgreeableProviderPlugin

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
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.mts.MessageAddress;

import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.Disposition;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.ScoringFunction;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.TimeAspectValue;
import org.cougaar.planning.ldm.plan.Verb;
import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.util.UnaryPredicate;


import org.cougaar.glm.ldm.Constants;
import org.cougaar.glm.ldm.oplan.Oplan;
import org.cougaar.glm.ldm.asset.Organization;

import org.cougaar.servicediscovery.SDDomain;
import org.cougaar.servicediscovery.SDFactory;
import org.cougaar.servicediscovery.description.MMRoleQuery;
import org.cougaar.servicediscovery.description.ScoredServiceDescription;
import org.cougaar.servicediscovery.description.ServiceClassification;
import org.cougaar.servicediscovery.description.ServiceDescription;
import org.cougaar.servicediscovery.description.ServiceRequest;
import org.cougaar.servicediscovery.util.UDDIConstants;
import org.cougaar.servicediscovery.transaction.MMQueryRequest;
import org.cougaar.servicediscovery.transaction.ServiceContractRelay;



import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;

/**
 * Test implementation -  generates a MMQueryRequest for a spare part service
 *
 */
public class SDClientPlugin extends SimplePlugin {
  private IncrementalSubscription myOplanSubscription;
  private IncrementalSubscription mySelfOrgSubscription;
  private IncrementalSubscription myMMRequestSubscription;
  private IncrementalSubscription myServiceContractRelaySubscription;
  private IncrementalSubscription myFindProvidersTaskSubscription;

  private LoggingService myLoggingService;

  private SDFactory mySDFactory;

  private boolean myFindingProviders = false;

  private UnaryPredicate myOplanPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Oplan) {
        return true;
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

  private UnaryPredicate myServiceContractRelayPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof ServiceContractRelay);
    }
  };


  private UnaryPredicate myMMRequestPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof MMQueryRequest);
    }
  };

  private UnaryPredicate myFindProvidersTaskPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      return ((o instanceof Task) &&
	      (((Task) o).getVerb().equals(Constants.Verb.FindProviders)));
    }
  };

  private static Calendar myCalendar = Calendar.getInstance();

  private static long DEFAULT_START_TIME = -1;
  private static long DEFAULT_END_TIME = -1;


  static {
    myCalendar.set(1990, 0, 1, 0, 0, 0);
    DEFAULT_START_TIME = myCalendar.getTime().getTime();

    myCalendar.set(2010, 0, 1, 0, 0, 0);
    DEFAULT_END_TIME = myCalendar.getTime().getTime();
  }


  protected void setupSubscriptions() {
    myOplanSubscription = (IncrementalSubscription)subscribe(myOplanPred);
    mySelfOrgSubscription = (IncrementalSubscription)subscribe(mySelfOrgPred);
    myMMRequestSubscription = (IncrementalSubscription)subscribe(myMMRequestPred);
    myServiceContractRelaySubscription = (IncrementalSubscription)subscribe(myServiceContractRelayPred);
    myFindProvidersTaskSubscription = (IncrementalSubscription)subscribe(myFindProvidersTaskPred);

    myLoggingService =
      (LoggingService) getBindingSite().getServiceBroker().getService(this, LoggingService.class, null);

    mySDFactory = (SDFactory) getFactory(SDDomain.SD_NAME);

    if (didRehydrate()) {
      myFindingProviders = false;

      if (needToFindProviders()) {
	myFindingProviders = true;
	Collection params = getDelegate().getParameters();    
	
	for (Iterator iterator = params.iterator();
	     iterator.hasNext();) {
	  Role role = Role.getRole((String) iterator.next());
	  boolean foundContract = checkProvider(role);

	  if (!foundContract) {
	    queryServices(role);
	  }
	}
      }
    }
  }

  public void execute() {
    if ((myOplanSubscription.hasChanged() &&
	(needToFindProviders()))) {
      findProviders();
    }

    if (myMMRequestSubscription.hasChanged()) {
      generateServiceRequests(myMMRequestSubscription.getChangedCollection());
    }
    
    //if your relays have changed, check for revokes
    if(myServiceContractRelaySubscription.hasChanged()) {
      Collection changedRelays = 
	myServiceContractRelaySubscription.getChangedCollection();
      for (Iterator iterator = changedRelays.iterator();
	   iterator.hasNext();) {
        ServiceContractRelay relay = (ServiceContractRelay)iterator.next();

        //actually, you only want to take action if you are the client agent
	// (not if you are the provider)

        if ((relay.getServiceContract().isRevoked()) &&
	    (relay.getClient().equals(getSelfOrg()))) {
	  //if your service contract got revoked, do a new service query
	  queryServices(relay.getServiceContract().getServiceRole());
	  //publishRemove(relay);
	}
      }
    }

    if (myFindProvidersTaskSubscription.hasChanged()) {
      Collection adds = myFindProvidersTaskSubscription.getAddedCollection();
      updateFindProvidersTaskDispositions(adds);
    }
  }

  protected void requestServiceContract(ServiceDescription serviceDescription) {
    String providerName = serviceDescription.getProviderName();

    for (Iterator iterator = serviceDescription.getServiceClassifications().iterator();
         iterator.hasNext();) {
      ServiceClassification serviceClassification =
        (ServiceClassification) iterator.next();
      if (serviceClassification.getClassificationSchemeName().equals(UDDIConstants.MILITARY_SERVICE_SCHEME)) {
        Role role =
          Role.getRole(serviceClassification.getClassificationName());
        ServiceRequest request =
          mySDFactory.newServiceRequest(getSelfOrg(),
                                        role,
                                        getDefaultPreferences(DEFAULT_START_TIME,
                                                              DEFAULT_END_TIME));

        ServiceContractRelay relay =
          mySDFactory.newServiceContractRelay(MessageAddress.getMessageAddress(providerName),
                                              request);
        publishAdd(relay);
      }
    }
  }

  protected Organization getSelfOrg() {
    for (Iterator iterator = mySelfOrgSubscription.iterator();
         iterator.hasNext();) {
      return (Organization) iterator.next();
    }

    return null;
  }

  protected void queryServices(Role role) {
    MMQueryRequest mmRequest;

    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(getAgentIdentifier() + 
			     " asking MatchMaker for : " + role);
    }
      mmRequest =
        mySDFactory.newMMQueryRequest(new MMRoleQuery(role, null));
    publishAdd(mmRequest);
  }

  protected Collection getDefaultPreferences(long startTime, long endTime) {
    ArrayList preferences = new ArrayList(2);

    AspectValue startTAV = TimeAspectValue.create(AspectType.START_TIME, startTime);
    ScoringFunction startScoreFunc =
      ScoringFunction.createStrictlyAtValue(startTAV);
    Preference startPreference =
      getFactory().newPreference(AspectType.START_TIME, startScoreFunc);

    AspectValue endTAV = TimeAspectValue.create(AspectType.END_TIME, endTime);
    ScoringFunction endScoreFunc =
      ScoringFunction.createStrictlyAtValue(endTAV);
    Preference endPreference =
      getFactory().newPreference(AspectType.END_TIME, endScoreFunc );

    preferences.add(startPreference);
    preferences.add(endPreference);

    return preferences;
  }

  private boolean needToFindProviders() {
    if (myFindingProviders == true) {
      return false;
    }

    Oplan oplan = null;
    for (Iterator iterator = myOplanSubscription.iterator();
	 iterator.hasNext();) {
      oplan = (Oplan) iterator.next();
      break;
    }
    
    return ((oplan != null) &&
	    (oplan.isActive()));
  }

  protected void findProviders() {
    myFindingProviders = true;
    Collection params = getDelegate().getParameters();
    
    for (Iterator iterator = params.iterator();
	 iterator.hasNext();) {
      Role role = Role.getRole((String) iterator.next());
      queryServices(role);
    }
  }
    
  protected void generateServiceRequests(Collection mmRequests) {
    for (Iterator iterator = mmRequests.iterator();
	 iterator.hasNext();) {
      MMQueryRequest mmRequest = (MMQueryRequest) iterator.next();
      
      if (myLoggingService.isDebugEnabled()) {
	myLoggingService.debug("execute: MMQueryRequest has changed." +
			       mmRequest);
      }
      if (mmRequest.getResult() != null) {
	Collection services = mmRequest.getResult();
	
	if (myLoggingService.isDebugEnabled()) {
	  
	  myLoggingService.debug("Results for query " + 
				 mmRequest.getQuery().toString());
	  myLoggingService.debug("number of providers: " + 
				 services.size());
	}
	
	for (Iterator serviceIterator = services.iterator();
	     serviceIterator.hasNext();) {
	  ScoredServiceDescription serviceDescription =
	    (ScoredServiceDescription) serviceIterator.next();
	  if (myLoggingService.isDebugEnabled()) {
	    myLoggingService.debug(getAgentIdentifier() +
				   " execute: - provider: " +
				   serviceDescription.getProviderName() +
				   " score: " + serviceDescription.getScore());
	  }
	  
	  requestServiceContract(serviceDescription);
	  
	  // only want one contract
	  break;
	}
	
	// Done with the query so clean up
	// publishRemove(mmRequest);
      }
    }
  }

  protected boolean checkProvider(Role role) {
    boolean foundContract = false;
    
    for (Iterator relayIterator = myServiceContractRelaySubscription.iterator();
	 relayIterator.hasNext();) {
      ServiceContractRelay relay = 
	(ServiceContractRelay) relayIterator.next();
      if (!relay.getServiceContract().isRevoked() &&
	  relay.getServiceContract().getServiceRole().equals(role)) {
	foundContract = true;
	break;
      }
    }

    return foundContract;
  }

  private void updateFindProvidersTaskDispositions(Collection findProvidersTasks) {
    Oplan oplan = null;
    for (Iterator iterator = myOplanSubscription.iterator();
	 iterator.hasNext();) {
      oplan = (Oplan) iterator.next();
      break;
    }


    double conf = 1.0;

    if ((oplan != null) &&
	(oplan.isActive())) {
      // Look to see if we've got all our contracts
      
      Collection params = getDelegate().getParameters();    
      for (Iterator iterator = params.iterator();
	   iterator.hasNext();) {
	Role role = Role.getRole((String) iterator.next());
	boolean foundContract = checkProvider(role);
	
	if (!foundContract) {
	  conf = 0.0;
	  break;
	}
      }
    }

    for (Iterator iterator = findProvidersTasks.iterator();
	 iterator.hasNext();) {
      Task task = (Task) iterator.next();
      PlanElement pe = task.getPlanElement();

      AllocationResult estResult = 
	PluginHelper.createEstimatedAllocationResult(task, 
						     theLDMF, 
						     conf, 
						     true);
      if (pe == null) {
	Disposition disposition = 
	  theLDMF.createDisposition(task.getPlan(), task, estResult);
	publishAdd(disposition);
      } else {
	pe.setEstimatedResult(estResult);
	publishChange(pe);
      }
    }
  }
}








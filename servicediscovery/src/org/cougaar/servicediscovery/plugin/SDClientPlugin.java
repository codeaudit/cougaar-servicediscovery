/*
 * <copyright>
 *  Copyright 2002 BBNT Solutions, LLC
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

import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.ScoringFunction;
import org.cougaar.planning.ldm.plan.TimeAspectValue;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.glm.ldm.oplan.Oplan;
import org.cougaar.glm.ldm.asset.MilitaryOrgPG;
import org.cougaar.glm.ldm.asset.Organization;

import org.cougaar.servicediscovery.Constants;
import org.cougaar.servicediscovery.SDDomain;
import org.cougaar.servicediscovery.SDFactory;

import org.cougaar.servicediscovery.description.MMRoleQuery;
import org.cougaar.servicediscovery.description.LineageList;
import org.cougaar.servicediscovery.description.ScoredServiceDescription;
import org.cougaar.servicediscovery.description.ServiceClassification;
import org.cougaar.servicediscovery.description.ServiceDescription;
import org.cougaar.servicediscovery.description.ServiceRequest;

import org.cougaar.servicediscovery.transaction.MMQueryRequest;
import org.cougaar.servicediscovery.transaction.ServiceContractRelay;
import org.cougaar.servicediscovery.util.UDDIConstants;


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
  private IncrementalSubscription lineage;
  private IncrementalSubscription myServiceContractRelaySubscription;

  private LoggingService myLoggingService;

  private SDFactory mySDFactory;

  private UnaryPredicate lineageListPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      if(o instanceof LineageList){
        if(((LineageList)o).getType() == LineageList.COMMAND){
          return true;
        }
      }
      return false;
    }};

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

    myLoggingService =
      (LoggingService) getBindingSite().getServiceBroker().getService(this, LoggingService.class, null);

    mySDFactory = (SDFactory) getFactory(SDDomain.SD_NAME);

    // subscribe to lineage
    lineage = (IncrementalSubscription)getBlackboardService().subscribe(lineageListPredicate);

  }

  public void execute() {
    if (myOplanSubscription.getAddedCollection().size() > 0) {
      Collection params = getDelegate().getParameters();

      for (Iterator iterator = params.iterator();
           iterator.hasNext();) {
        Role role = Role.getRole((String) iterator.next());
        queryServices(role);
      }
    }

    if (myMMRequestSubscription.hasChanged()) {
      for (Iterator iterator =
        myMMRequestSubscription.getChangedCollection().iterator();
           iterator.hasNext();) {
        MMQueryRequest mmRequest = (MMQueryRequest) iterator.next();

        if (myLoggingService.isDebugEnabled()) {
          myLoggingService.debug("execute: MMQueryRequest has changed." +
                                 mmRequest);
        }
        if (mmRequest.getResult() != null) {
          Collection services = mmRequest.getResult();
          myLoggingService.debug("execute: services size: " + services.size());

          // print all results
	  if (myLoggingService.isDebugEnabled()) {

	    myLoggingService.debug("Results for query "+mmRequest.getQuery().toString());
	    for (Iterator serviceIterator = services.iterator();
		 serviceIterator.hasNext();) {
	      ScoredServiceDescription serviceDescription =
		(ScoredServiceDescription) serviceIterator.next();
	      myLoggingService.debug("Score " + serviceDescription.getScore());
	      myLoggingService.debug("Provider name " + serviceDescription.getProviderName());
	      myLoggingService.debug("*********");
	    }
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

    //if your relays have changed, check for revokes
    if(myServiceContractRelaySubscription.hasChanged()) {
      Iterator relays = myServiceContractRelaySubscription.getChangedCollection().iterator();
      while(relays.hasNext()) {
        ServiceContractRelay relay = (ServiceContractRelay)relays.next();

        //actually, you only want to take action if you are the client agent
	// (not if you are the provider)

        if ((relay.getServiceContract().isRevoked()) &&
	    (relay.getClient().equals(getSelfOrg()))) {
	  //if your service contract got revoked, do a new service query
	  queryServices(relay.getServiceRequest().getServiceRole());
	}
      }
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

  protected String getRequestedSupportEchelon() {
    Organization selfOrg = getSelfOrg();

    if (selfOrg == null) {
      myLoggingService.error("getRequestedSupportEchelon(): Agent does not " +
			     " have a local Organization Asset.");
      return "";
    } else {
      MilitaryOrgPG militaryOrgPG = selfOrg.getMilitaryOrgPG();
      if (militaryOrgPG != null) {
	String echelon = militaryOrgPG.getRequestedEchelonOfSupport();

	if (echelon != null) {
	  echelon = Constants.MilitaryEchelon.mapToMilitaryEchelon(echelon);
	  if (echelon != Constants.MilitaryEchelon.UNDEFINED) {
	    return echelon;
	  }
	}
      }
      return Constants.MilitaryEchelon.BRIGADE;
    }
  }

  protected void queryServices(Role role) {

    String clientName=null;
    Iterator lineageIterator = lineage.iterator();
    if(lineageIterator.hasNext()){
      LineageList lineageCollection = (LineageList)lineageIterator.next();
      clientName = lineageCollection.getLeaf();
    }

    MMQueryRequest mmRequest;
    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(clientName + " asking MatchMaker for : " + role + " at " + getRequestedSupportEchelon());
    }
      mmRequest =
        mySDFactory.newMMQueryRequest(new MMRoleQuery(role, getRequestedSupportEchelon()));
    publishAdd(mmRequest);
  }

  Collection getDefaultPreferences(long startTime, long endTime) {
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

}








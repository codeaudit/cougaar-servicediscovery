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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.persist.PersistenceNotEnabledException;
import org.cougaar.core.service.EventService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.glm.ldm.Constants;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.mlm.plugin.organization.GLSConstants;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.Disposition;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.ScoringFunction;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.TimeAspectValue;
import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.planning.plugin.util.PluginHelper;

import org.cougaar.servicediscovery.SDDomain;
import org.cougaar.servicediscovery.SDFactory;
import org.cougaar.servicediscovery.description.LineageEchelonScorer;
import org.cougaar.servicediscovery.description.LineageList;
import org.cougaar.servicediscovery.description.LineageListWrapper;
import org.cougaar.servicediscovery.description.MMRoleQuery;
import org.cougaar.servicediscovery.description.ProviderCapabilities;
import org.cougaar.servicediscovery.description.ProviderCapability;
import org.cougaar.servicediscovery.description.ScoredServiceDescription;
import org.cougaar.servicediscovery.description.ServiceClassification;
import org.cougaar.servicediscovery.description.ServiceDescription;
import org.cougaar.servicediscovery.description.ServiceInfoScorer;
import org.cougaar.servicediscovery.description.ServiceRequest;
import org.cougaar.servicediscovery.description.TimeInterval;
import org.cougaar.servicediscovery.transaction.MMQueryRequest;
import org.cougaar.servicediscovery.transaction.ServiceContractRelay;
import org.cougaar.servicediscovery.util.UDDIConstants;
import org.cougaar.util.PropertyParser;
import org.cougaar.util.UnaryPredicate;

/**
 * Look up in YP agents providing roles given as parameters,
 * by creating MMRequests for these. When it gets answers,
 * create relays with those agents asking for the service.
 * Keep asking until have contracts in the relays for all roles
 * and all time intervals.
 *
 */
public class SDClientPlugin extends SimplePlugin implements GLSConstants {

  private IncrementalSubscription mySelfOrgSubscription;
  private IncrementalSubscription myMMRequestSubscription;
  protected IncrementalSubscription myServiceContractRelaySubscription;
  private IncrementalSubscription myFindProvidersTaskSubscription;
  private IncrementalSubscription myLineageListSubscription;

  /** for knowing when we get our self org asset **/
  private Organization mySelfOrg = null;

  protected LoggingService myLoggingService;
  protected EventService eventService;

  protected SDFactory mySDFactory;

  private boolean myFindingProviders = false;

  /**
   * RFE 3162: Set to true to force a persistence as soon as the agent
   * has finished finding providers. Send a CougaarEvent announcing completion.
   * This allows controllers to kill the agent as early as possible, without
   * potentially causing logistics plugin problems on rehydration.
   * Defaults to false - ie, do not force an early persist.
   **/
  private static final boolean persistEarly;
  static {
    persistEarly = PropertyParser.getBoolean("org.cougaar.servicediscovery.plugin.SDClientPlugin.persistEarly", false);
  }

  private static UnaryPredicate mySelfOrgPred = new UnaryPredicate() {
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

  // Subscription to ServiceContractRelays for which I am the client!
  private UnaryPredicate myServiceContractRelayPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof ServiceContractRelay) {
	ServiceContractRelay relay = (ServiceContractRelay) o;
        return relay.getClient().equals(getSelfOrg());
      } else {
	return false;
      }
    }
  };


  private static UnaryPredicate myMMRequestPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof MMQueryRequest);
    }
  };

  private static UnaryPredicate myFindProvidersTaskPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      return ((o instanceof Task) &&
	      (((Task) o).getVerb().equals(Constants.Verb.FindProviders)));
    }
  };

  private static UnaryPredicate myLineageListPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      return ((o instanceof LineageListWrapper) &&
	      (((LineageListWrapper) o).getType() == LineageList.COMMAND));
    }
  };

  private UnaryPredicate myProviderCapabilitiesPred = 
  new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof ProviderCapabilities);
    }
  };


  private static Calendar myCalendar = Calendar.getInstance();

  protected static long DEFAULT_START_TIME = -1;
  protected static long DEFAULT_END_TIME = -1;


  static {
    myCalendar.set(1990, 0, 1, 0, 0, 0);
    myCalendar.set(Calendar.MILLISECOND, 0);
    DEFAULT_START_TIME = myCalendar.getTime().getTime();

    myCalendar.set(2010, 0, 1, 0, 0, 0);
    myCalendar.set(Calendar.MILLISECOND, 0);
    DEFAULT_END_TIME = myCalendar.getTime().getTime();
  }


  protected void setupSubscriptions() {
    mySelfOrgSubscription = (IncrementalSubscription) subscribe(mySelfOrgPred);
    myMMRequestSubscription = (IncrementalSubscription) subscribe(myMMRequestPred);
    myServiceContractRelaySubscription = (IncrementalSubscription) subscribe(myServiceContractRelayPred);
    myFindProvidersTaskSubscription = (IncrementalSubscription) subscribe(myFindProvidersTaskPred);

    myLoggingService =
      (LoggingService) getBindingSite().getServiceBroker().getService(this, LoggingService.class, null);

    // get event service
    eventService = (EventService)
      getBindingSite().getServiceBroker().getService(
          this, EventService.class, null);

    mySDFactory = (SDFactory) getFactory(SDDomain.SD_NAME);

    if (didRehydrate()) {
      myFindingProviders = false;

      if ((!myFindProvidersTaskSubscription.isEmpty()) &&
	   (needToFindProviders())) {
	myFindingProviders = true;
	Collection params = getDelegate().getParameters();

	for (Iterator iterator = params.iterator();
	     iterator.hasNext();) {
	  Role role = Role.getRole((String) iterator.next());
	  boolean foundContract = checkProviderCompletelyCoveredOrRequested(role);

	  if (!foundContract) {
	    queryServices(role);
	  }
	}
      }
    }
  }

  public void execute() {
    if (myFindProvidersTaskSubscription.hasChanged()) {
      Collection adds = myFindProvidersTaskSubscription.getAddedCollection();

      if ((adds != null) && (!adds.isEmpty()) &&
	  (needToFindProviders())) {
	findProviders();
      }

      updateFindProvidersTaskDispositions(myFindProvidersTaskSubscription);
    }

    // If matchmaker has new possible providers, look at options,
    // and generate the relays possibly
    if (myMMRequestSubscription.hasChanged()) {
      generateServiceRequests(myMMRequestSubscription.getChangedCollection());
    }

    //if your relays have changed, check for revokes
    if(myServiceContractRelaySubscription.hasChanged()) {
      Collection changedRelays =
	myServiceContractRelaySubscription.getChangedCollection();

      handleChangedServiceContractRelays(changedRelays);

      // Update disposition on FindProviders task
      if (changedRelays.size() > 0) {
        if (myLoggingService.isDebugEnabled()) {
          myLoggingService.debug(getAgentIdentifier() +
                                 " changedRelays.size = " + 
				 changedRelays.size() + 
				 ", updateFindProvidersTaskDispositions");
        }
	updateFindProvidersTaskDispositions(myFindProvidersTaskSubscription);
      }
    }
  } // end of execute method

  /**
   * If a changed relay is revoked and you are the client, do a new
   * service query for this role
   */
  protected void handleChangedServiceContractRelays(Collection changedRelays) {
    for (Iterator iterator = changedRelays.iterator();
         iterator.hasNext();) {
      ServiceContractRelay relay = (ServiceContractRelay)iterator.next();

      //you want to take action if you are the client agent
      //and the service contract was revoked
      if (relay.getServiceContract().isRevoked()  &&
          relay.getClient().equals(getSelfOrg())) {
        //do a new service query
        queryServices(relay.getServiceContract().getServiceRole());
        //publishRemove(relay);
      }
    }

  }

  /**
   * create & publish a relay with service request to the provider specified in the
   * serviceDescription for the specified time interval.
   */
  protected void requestServiceContract(ServiceDescription serviceDescription,
                                        TimeInterval interval) {
    Role role = getRole(serviceDescription);
    if (role==null) {
      if (myLoggingService.isWarnEnabled()) {
        myLoggingService.warn(getAgentIdentifier() +
                               " error requesting service contract: a null role");
      }
    } else {
      String providerName = serviceDescription.getProviderName();
      long startTime = interval.getStartTime();
      long endTime = interval.getEndTime();
      ServiceRequest request =
          mySDFactory.newServiceRequest(getSelfOrg(),
					role,
					getDefaultPreferences(startTime,
							      endTime));

      ServiceContractRelay relay =
          mySDFactory.newServiceContractRelay(MessageAddress.getMessageAddress(providerName),
					      request);
      if (myLoggingService.isInfoEnabled()) {
        myLoggingService.info(getAgentIdentifier() + " SDClient.requestServiceContract: publish relay to " + providerName +
                               " asking for role: " + role);
      }
      publishAdd(relay);
    }
  }

  // Get the self org out of the subscription
  protected Organization getSelfOrg() {
    if (mySelfOrg == null) {
      for (Iterator iterator = mySelfOrgSubscription.iterator();
	   iterator.hasNext();) {
	mySelfOrg = (Organization) iterator.next();
      }
    }

    return mySelfOrg;
  }

  protected String getMinimumEchelon(Role role) {
    Collection collection = 
      getBlackboardService().query(myProviderCapabilitiesPred);
    
    ProviderCapabilities capabilities = null;
    if (collection.size() > 0) {
      Iterator iterator = collection.iterator();
      capabilities = (ProviderCapabilities) iterator.next();
      if (iterator.hasNext()) {
	myLoggingService.warn(getAgentIdentifier() + 
			      " getMinimumEchelon: multiple ProviderCapabilities found." +
			      " Using - " + capabilities);
      }
    }

    return LineageEchelonScorer.getMinimumEchelonOfSupport(capabilities, role);
  }

  protected LineageListWrapper getCommandLineageList() {
    Collection collection = 
      getBlackboardService().query(myLineageListPred);
    
    if (collection.size() > 0) {
      Iterator iterator = collection.iterator();
      LineageListWrapper commandListWrapper = 
	(LineageListWrapper) iterator.next();

      if (iterator.hasNext()) {
	myLoggingService.warn(getAgentIdentifier() + 
			      " getCommandLineageList: multiple COMMAND LineageLists found." +
			      " Using - " + commandListWrapper);
      }
      
      return commandListWrapper;
    }

    return null;
  }

  /**
   * create and publish a MMQueryRequest for this role
   */
  protected void queryServices(Role role) {
    String minimumEchelon = getMinimumEchelon(role);

    LineageListWrapper commandListWrapper = getCommandLineageList();

    if (commandListWrapper != null) {
      LineageEchelonScorer scorer = 
	new LineageEchelonScorer(commandListWrapper,
				 minimumEchelon,
				 role);
      queryServices(role, scorer);
    } else {
      myLoggingService.error(getAgentIdentifier() + 
			     " queryServices: no COMMAND LineageList on blackboard." + 
			     " Unable to generate MMRoleQuery for " + role);
    }
  }

  /**
   * create and publish a MMQueryRequest for this role
   */
  protected void queryServices(Role role, ServiceInfoScorer scorer) {
    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(getAgentIdentifier() +
			     " asking MatchMaker for role : " + role +
			     " - serviceInfoScorer : " + scorer);
    }
    MMQueryRequest mmRequest =
        mySDFactory.newMMQueryRequest(new MMRoleQuery(role, scorer));
    publishAdd(mmRequest);
  }

  /**
   * Return a Collection of Preferences containing a corresponding
   * start time and end timed preference.
   */
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

  // If we have not yet created the service requests, and we're
  // in an OPLAN stage where we should do work, return true
  private boolean needToFindProviders() {
    return (!(myFindingProviders) && (isActive()));
  }

  // For each role in parameters, generate a service request
  protected void findProviders() {
    myFindingProviders = true;
    Collection params = getDelegate().getParameters();

    for (Iterator iterator = params.iterator();
	 iterator.hasNext();) {
      Role role = Role.getRole((String) iterator.next());
      queryServices(role);
    }
  }

  /**
   * For each answered request, pick an appropriate provider
   * and send them a service request.
   * Note that input is the changed service requests only.
   */
  protected void generateServiceRequests(Collection mmRequests) {
    for (Iterator iterator = mmRequests.iterator();
         iterator.hasNext();) {
      MMQueryRequest mmRequest = (MMQueryRequest) iterator.next();

      if (myLoggingService.isDebugEnabled()) {
        myLoggingService.debug(getAgentIdentifier() + ": execute: MMQueryRequest has changed: " + mmRequest);
      }

      // Only do anything if the query has a result
      Collection services = mmRequest.getResult();
      if (services != null) {
	if (services.size() == 0) {
	  // MMPlugin said no one matched?
	  if (myLoggingService.isWarnEnabled())
	    myLoggingService.warn(getAgentIdentifier() + 
				  " got 0 results for query " + mmRequest.getQuery());
	  //return;
	  continue; // on to next changed mmRequest
	}

        Role role = null;
        Collection intervals = null;

        if (myLoggingService.isDebugEnabled()) {
          myLoggingService.debug(getAgentIdentifier() + ": Results for query " +
                                 mmRequest.getQuery().toString());
          myLoggingService.debug("    number of avail providers: " +
                                 services.size());
        }

        //make sure any ties are resolved in a reliable & consistent order
        ArrayList servicesList = new ArrayList();
        servicesList.addAll(services);
        reorderAnyTiedServiceDescriptions(servicesList);

        //Look through the service descriptions to
        //just figure out the role & time intervals you need to ask for
        for (Iterator serviceIterator = servicesList.iterator();
             serviceIterator.hasNext();) {
          ScoredServiceDescription serviceDescription =
            (ScoredServiceDescription) serviceIterator.next();
          if (myLoggingService.isDebugEnabled()) {
            myLoggingService.debug(getAgentIdentifier() +
                                   " execute: - provider: " +
                                   serviceDescription.getProviderName() +
                                   " score: " + serviceDescription.getScore());
          }

          if (role == null) {
	    // Find the role out of the service classifications in the desc
            role = getRole(serviceDescription);
	    // Then use that to get the outstanding intervals
	    // If we have a service request for this role, expect
	    // intervals to be null
            intervals = getCurrentlyUncoveredIntervalsWithoutOutstandingRequests(
                DEFAULT_START_TIME, DEFAULT_END_TIME, role);
          }
        } //end loop finding role & intervals needed

        if (role == null || intervals == null) {
          //error state, log a message
          if (myLoggingService.isWarnEnabled()) {
            myLoggingService.warn(getAgentIdentifier() +
                                   " error generating service requests: a null role or time interval: " +
                                   "role is " + role +
                                   " and time intervals are " + intervals + ", and services.size was " + services.size());
          }
	  continue; // on to the next changed MMRequest
	  //    return;
        }

        if (myLoggingService.isDebugEnabled()) {
          myLoggingService.debug(getAgentIdentifier() +
                                 " execute: want a provider for role: " + role);
        }

        //now, for each interval, pick a provider (service description) and
        //request a contract
        Iterator neededIntervals = intervals.iterator();
        if(!neededIntervals.hasNext()) {
          if (myLoggingService.isDebugEnabled()) {
            myLoggingService.debug(getAgentIdentifier() +
                                   " empty needed intervals for role: " + role);
          }
        }

        while(neededIntervals.hasNext()) {
          TimeInterval currentInterval = (TimeInterval) neededIntervals.next();
          Iterator serviceIterator = servicesList.iterator();
          boolean madeNewRequest = false;

	  // Loop over providers who might cover the interval
          while(serviceIterator.hasNext()) {
            ScoredServiceDescription sd = (ScoredServiceDescription) serviceIterator.next();
            //if you have already asked this provider, skip it
            if(alreadyAskedForContractWithProvider(role, sd.getProviderName(), currentInterval)) {
              if (myLoggingService.isDebugEnabled()) {
                myLoggingService.debug(getAgentIdentifier() +
                                       " skipping " + sd.getProviderName() + " for role: " + role);
              }
              continue;
            } else {
              //remember that you found a provider to request from
              madeNewRequest = true;
              //do the request
              if (myLoggingService.isDebugEnabled()) {
                myLoggingService.debug(getAgentIdentifier() +
                                       " requesting contract with " + sd.getProviderName() + " for role " + role);
              }
              requestServiceContract(sd, currentInterval);

              //stop looking for providers for this interval, since we found one
              break;
            }
          }

          //if you were not able to find a provider to request from
          //for this interval, take appropriate action
          if(!madeNewRequest) {
            handleRequestWithNoRemainingProviderOption(role, currentInterval);
          }
        } // end of loop over intervals
      } // end of check that the request had results
      // Done with the query so clean up
      // publishRemove(mmRequest);
    }//end looping through changed mm requests
  }

  /**
   * Modify the order of scoredServiceDescriptions so that ties are in the order
   * you want them to be
   */
  protected void reorderAnyTiedServiceDescriptions(ArrayList scoredServiceDescriptions) {
    //do nothing, trust the matchmaker ordering
  }

  /**
   * return true if you already have a service contract relay with this provider
   */
  protected boolean alreadyAskedForContractWithProvider(Role role, String providerName,
      TimeInterval timeInterval) {

    boolean foundContract = false;
    for (Iterator relayIterator = myServiceContractRelaySubscription.iterator();
         relayIterator.hasNext();) {
      ServiceContractRelay relay =
        (ServiceContractRelay) relayIterator.next();
      if (relay.getProviderName().equals(providerName) &&
          relay.getClient().equals(getSelfOrg()) &&
          relay.getServiceRequest().getServiceRole().equals(role)) {
        foundContract = true;
        break;
      }
    }
    return foundContract;
  }

  /**
   * Log a warning that you couldn't find a provider for this option
   */
  protected void handleRequestWithNoRemainingProviderOption(Role role, TimeInterval currentInterval) {
    //this means you have a time interval where you have exhausted all possible
    //providers. Log a warning.
    if (myLoggingService.isWarnEnabled()) {
      myLoggingService.warn(getAgentIdentifier() +
                            " failed to get contract for " + role.toString() +
                            " for time period from " + new java.util.Date(currentInterval.getStartTime()) +
                            " to " + new java.util.Date(currentInterval.getEndTime()));
    }
  }

  /**
   * For this role, check if there is a non-revoked service contract or an
   * unanswered request. If so, return an empty collection. If not, return
   * a collection containing the time interval between desiredStart and
   * desiredEnd.
   */
  protected Collection getCurrentlyUncoveredIntervalsWithoutOutstandingRequests(
      long desiredStart, long desiredEnd, Role role) {

    ArrayList ret = new ArrayList();
    if(!checkProviderCompletelyCoveredOrRequested(role)) {
      ret.add(new TimeInterval(desiredStart, desiredEnd));
    }
    return ret;
  }

  /**
   * return true if you have a non-revoked service contract for
   * this role or if you have an unanswered request
   */
  protected boolean checkProviderCompletelyCoveredOrRequested(Role role) {
    boolean foundContract = false;
    for (Iterator relayIterator = myServiceContractRelaySubscription.iterator();
         relayIterator.hasNext();) {
      ServiceContractRelay relay =
        (ServiceContractRelay) relayIterator.next();

      // We're good if we have a valid contract,
      // or at least a valid servicerequest
      if (relay.getServiceContract() != null &&
          !relay.getServiceContract().isRevoked() &&
          relay.getClient().equals(getSelfOrg()) &&
          relay.getServiceContract().getServiceRole().toString().equals(role.toString())) {
        foundContract = true;
        break;
      } else if(relay.getServiceContract() == null &&
              relay.getClient().equals(getSelfOrg()) &&
              relay.getServiceRequest().getServiceRole().toString().equals(role.toString())) {
        foundContract = true;
        break;
      }
    }
    return foundContract;
  }

  /**
   * return true if you have a non-revoked service contract for this role
   */
  protected boolean checkProviderCompletelyCovered(Role role) {
    boolean foundContract = false;
    for (Iterator relayIterator = myServiceContractRelaySubscription.iterator();
         relayIterator.hasNext();) {
      ServiceContractRelay relay =
        (ServiceContractRelay) relayIterator.next();

      // We're good only if we have a valid contract
      if (relay.getServiceContract() != null &&
          !relay.getServiceContract().isRevoked() &&
          relay.getClient().equals(getSelfOrg()) &&
          relay.getServiceContract().getServiceRole().toString().equals(role.toString())) {
        foundContract = true;
        break;
      }
    }
    return foundContract;
  }

  // Log helper: Where have we gotten to?
  private String stateMessage() {
    String ret = getAgentIdentifier() + " SDClient.updateFindProvidersTaskDispositions: State of plugin ";

    //what is the status of the find providers task
    for (Iterator iterator = myFindProvidersTaskSubscription.iterator();
         iterator.hasNext();) {
      Task task = (Task) iterator.next();
      PlanElement pe = task.getPlanElement();
      if(pe != null && pe.getEstimatedResult()!=null) {
        ret = ret.concat("\n     FindProviders conf: " + pe.getEstimatedResult().getConfidenceRating());
      }
      else {
        ret = ret.concat("\n     FindProviders task has no result yet");
      }
    }
    //which roles do we need?
    ret = ret.concat("\n     The roles needed are: ");
    Collection params = getDelegate().getParameters();
    for (Iterator iterator = params.iterator();
         iterator.hasNext();) {
      Role role = Role.getRole((String) iterator.next());
      ret = ret.concat(role + " ");
    }
    //what service contracts relays do we have, and which have contracts (replies)?
    for (Iterator relayIterator = myServiceContractRelaySubscription.iterator();
         relayIterator.hasNext();) {
      ServiceContractRelay relay =
        (ServiceContractRelay) relayIterator.next();

      if(relay.getClient().equals(getSelfOrg())) {
        ret = ret.concat("\n     Sent a service contract relay to " + relay.getProviderName() + " for the role " +
              relay.getServiceRequest().getServiceRole().toString());
      }
      if (relay.getServiceContract() != null &&
          relay.getClient().equals(getSelfOrg())) {
        ret = ret.concat(", and the provider has answered.");
      }
      else if(relay.getServiceContract() == null &&
              relay.getClient().equals(getSelfOrg())) {
        ret = ret.concat(", but no answer yet from the provider.");
      }
    }

    // Now look to see what roles we have asked the MM for
    for(Iterator queryIterator = myMMRequestSubscription.iterator(); queryIterator.hasNext();) {
      MMQueryRequest request = (MMQueryRequest) queryIterator.next();
      ret = ret.concat("\n     MMQueryRequest exists for " + ((MMRoleQuery)request.getQuery()).getRole());
      if(request.getResult() == null) {
        ret = ret.concat(", but no reply.");
      }
      else {
        ret = ret.concat(", and reply exists with " + request.getResult().size() + " possible providers.");
      }
    }
    return ret;
  }

  // See if we have all contracts for all the roles we need. If so, set
  // FindProviders conf to 1. Otherwise, to 0.
  private void updateFindProvidersTaskDispositions(Collection findProvidersTasks) {
    // Print verbose status of all requests
    if (myLoggingService.isInfoEnabled()) {
      myLoggingService.info(stateMessage());
    }

    double conf = 1.0;

    // First determine whether we have found all our providers
    if (isActive()) {
      if (myLoggingService.isDebugEnabled()) {
        myLoggingService.debug(getAgentIdentifier() +
                               " isActiveTrue ");
      }

      // Look to see if we've got all our contracts for roles
      // specified as plugin parameters. If any one is missing,
      // then overall confidence stays 0
      Collection params = getDelegate().getParameters();

      if (myLoggingService.isInfoEnabled()) {
	myLoggingService.info(getAgentIdentifier() +
			      " SDClient.updateFindProvidersTaskDispositions: contracts found - ");
      }

      for (Iterator iterator = params.iterator();
	   iterator.hasNext();) {
	Role role = Role.getRole((String) iterator.next());
	boolean foundContract = checkProviderCompletelyCovered(role);

        if (myLoggingService.isInfoEnabled()) {
          myLoggingService.info("     " + foundContract + " for role " + role);
        }

	if (!foundContract) {
	  conf = 0.0;
	  break;
	} // end of block where found no contract for a Role
      } // end of loop over Roles passed in as parameters
    } // end of isActive() check block

    // Did we just find our providers? This will signal
    // whether the agent should persiste
    boolean justFoundProviders = false;

    //Only request persist if in initial FindProviders stage
    boolean doingInitialFindProviders = true;
    
    // Now update the confidence on the FindProviders task
    for (Iterator iterator = findProvidersTasks.iterator();
	 iterator.hasNext();) {
      Task task = (Task) iterator.next();
      Set oStages = getOplanStages(task);
      if (oStages == null || oStages.size() != 1)
	doingInitialFindProviders = false;
	
      PlanElement pe = task.getPlanElement();

      AllocationResult estResult =
	PluginHelper.createEstimatedAllocationResult(task,
						     theLDMF,
						     conf,
						     true);
      if (pe == null) {
	Disposition disposition =
	  theLDMF.createDisposition(task.getPlan(), task, estResult);
        if (myLoggingService.isInfoEnabled()) {
          myLoggingService.info(getAgentIdentifier() + " updateFindProvidersTaskDispositions: Create disposition with conf " +
                                conf);
        }
	publishAdd(disposition);

	// Are we in the first findProviders stage? Then if conf=1, justFoundProviders
	// isActive says this is at least the findProviders.
	// but I don't want to persist with every oplan stage change
	if (persistEarly && doingInitialFindProviders && conf == 1.0) {
	  justFoundProviders = true;
	  if (myLoggingService.isInfoEnabled())
	    myLoggingService.info(getAgentIdentifier() + " just added a conf 1.0 dispo to findProviders while doingInitialFindProviders - going to request persistence.");
	}

      } else {
	if (conf != pe.getEstimatedResult().getConfidenceRating()) {
	  if (myLoggingService.isInfoEnabled()) {
            myLoggingService.info(getAgentIdentifier() + 
				  " updateFindProvidersTaskDispositions: Changed conf from " +
				  pe.getEstimatedResult().getConfidenceRating() +
				  " to " + conf);
	  } // end if logging block

	  pe.setEstimatedResult(estResult);
	  publishChange(pe);
	  
	  // if conf = 1.0 and we're in the first findProviders stage
	  // then justFoundProviders
	  // isActive says this is at least the findProviders,
	  // but I don't want to persist with every oplan stage change
	  if (persistEarly && doingInitialFindProviders && conf == 1.0) {
	    justFoundProviders = true;
	    if (myLoggingService.isInfoEnabled())
	      myLoggingService.info(getAgentIdentifier() + " just changed findProviders dispo to 1.0 while doingInitialFindProviders - going to request persistence.");
	  }

	}
      } // end of block to change PE conf
    } // end of loop over FindProviders tasks

    // If the agent just finished finding providers, force a persistence now
    // that way, if the agent dies anytime after this, it will come
    // back with its providers intact
    if (persistEarly && justFoundProviders) {
      try {
	// Bug 3282: Persistence doesn't put in the correct reasons for blocking in SchedulableStatus
	getBlackboardService().persistNow();
	
	// Now send a Cougaar event indicating the agent has its providers
	// and has persisted them.
	if (eventService != null &&
	    eventService.isEventEnabled()) {
	  eventService.event(getAgentIdentifier() + " persisted after finding providers.");
	} else {
	  myLoggingService.info(getAgentIdentifier() + " (no event service): persisted after finding providers.");
	}
      } catch (PersistenceNotEnabledException nope) {
	if (eventService != null &&
	    eventService.isEventEnabled()) {
	  eventService.event(getAgentIdentifier() + " finished finding providers (persistence not enabled).");
	} else {
	  myLoggingService.info(getAgentIdentifier() + " (no event service): finished finding providers (persistence not enabled).");
	}
      } // try/catch block
    } // if we just found our providers

  } // end of method

  // Is this agent active currently in the OPLAN (based on stages)
  private boolean isActive() {
    /*
    Oplan oplan = null;
    for (Iterator iterator = myOplanSubscription.iterator();
	 iterator.hasNext();) {
      oplan = (Oplan) iterator.next();
      break;
    }

    return ((oplan != null) &&
	    (oplan.isActive()));
	    */

    // For now always true if we have a FindProviders task
    return (!myFindProvidersTaskSubscription.isEmpty());
  }

  protected Role getRole(ServiceDescription serviceDescription) {

    for (Iterator iterator = serviceDescription.getServiceClassifications().iterator();
         iterator.hasNext();) {
      ServiceClassification serviceClassification =
        (ServiceClassification) iterator.next();
      if (serviceClassification.getClassificationSchemeName().equals(UDDIConstants.MILITARY_SERVICE_SCHEME)) {
        Role role =
          Role.getRole(serviceClassification.getClassificationName());
        return role;
      }
    }
    return null;

  }

  protected Set getOplanStages(Task task) {
    Enumeration origpp = task.getPrepositionalPhrases();

    while (origpp.hasMoreElements()) {
      PrepositionalPhrase app = (PrepositionalPhrase) origpp.nextElement();
      if (app.getPreposition().equals(FOR_OPLAN_STAGES)) {
	return (Set) app.getIndirectObject();
      }
    }

    return Collections.EMPTY_SET;
  }
}








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
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.persist.PersistenceNotEnabledException;
import org.cougaar.core.service.EventService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.glm.ldm.Constants;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.mlm.plugin.organization.GLSConstants;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.Disposition;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.planning.ldm.plan.ScheduleElement;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.planning.plugin.util.PluginHelper;

import org.cougaar.servicediscovery.SDDomain;
import org.cougaar.servicediscovery.SDFactory;
import org.cougaar.servicediscovery.description.LineageEchelonScorer;
import org.cougaar.servicediscovery.description.Lineage;
import org.cougaar.servicediscovery.description.MMRoleQuery;
import org.cougaar.servicediscovery.description.ProviderCapabilities;
import org.cougaar.servicediscovery.description.ProviderCapability;
import org.cougaar.servicediscovery.description.ScoredServiceDescription;
import org.cougaar.servicediscovery.description.ServiceClassification;
import org.cougaar.servicediscovery.description.ServiceContract;
import org.cougaar.servicediscovery.description.ServiceDescription;
import org.cougaar.servicediscovery.description.ServiceInfoScorer;
import org.cougaar.servicediscovery.description.ServiceRequest;
import org.cougaar.servicediscovery.description.ServiceRequestImpl;
import org.cougaar.servicediscovery.transaction.MMQueryRequest;
import org.cougaar.servicediscovery.transaction.ServiceContractRelay;
import org.cougaar.servicediscovery.util.UDDIConstants;

import org.cougaar.util.MutableTimeSpan;
import org.cougaar.util.NonOverlappingTimeSpanSet;
import org.cougaar.util.PropertyParser;
import org.cougaar.util.TimeSpan;
import org.cougaar.util.TimeSpans;
import org.cougaar.util.TimeSpanSet;
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
  private static int WARNING_SUPPRESSION_INTERVAL = 4;
  private static final String CLIENT_GRACE_PERIOD = 
    "org.cougaar.servicediscovery.plugin.ClientGracePeriod"; 
  private long myWarningCutoffTime = -1;

  private static TimeSpan DEFAULT_TIME_SPAN;
  static {
    DEFAULT_TIME_SPAN = TimeSpans.getSpan(SDFactory.DEFAULT_START_TIME,
					  SDFactory.DEFAULT_END_TIME);
  }


  private IncrementalSubscription mySelfOrgSubscription;
  private IncrementalSubscription myMMRequestSubscription;
  protected IncrementalSubscription myServiceContractRelaySubscription;
  private IncrementalSubscription myFindProvidersTaskSubscription;
  private IncrementalSubscription myLineageSubscription;

  /** for knowing when we get our self org asset **/
  private Organization mySelfOrg = null;

  protected LoggingService myLoggingService;
  protected EventService myEventService;

  protected SDFactory mySDFactory;

  private NonOverlappingTimeSpanSet myOPCONSchedule = null;

  private Map myRoles = new HashMap();
  /**
   * RFE 3162: Set to true to force a persistence as soon as the agent
   * has finished finding providers. Send a CougaarEvent announcing completion.
   * This allows controllers to kill the agent as early as possible, without
   * potentially causing logistics plugin problems on rehydration.
   * Defaults to false - ie, do not force an early persist.
   **/
  private static final boolean PERSIST_EARLY;

  static {
    PERSIST_EARLY = PropertyParser.getBoolean("org.cougaar.servicediscovery.plugin.SDClientPlugin.persistEarly", false);
  }

  private boolean myNeedToFindProviders = true;

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

  private static UnaryPredicate myLineagePred = new UnaryPredicate() {
    public boolean execute(Object o) {
      return ((o instanceof Lineage) &&
	      (((Lineage) o).getType() == Lineage.OPCON));
    }
  };

  private UnaryPredicate myProviderCapabilitiesPred = 
  new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof ProviderCapabilities);
    }
  };

  protected void setupSubscriptions() {
    mySelfOrgSubscription = 
      (IncrementalSubscription) subscribe(mySelfOrgPred);
    myMMRequestSubscription = 
      (IncrementalSubscription) subscribe(myMMRequestPred);
    myServiceContractRelaySubscription = 
      (IncrementalSubscription) subscribe(myServiceContractRelayPred);
    myFindProvidersTaskSubscription = 
      (IncrementalSubscription) subscribe(myFindProvidersTaskPred);
    myLineageSubscription = 
      (IncrementalSubscription) subscribe(myLineagePred);

    myLoggingService =
      (LoggingService) getBindingSite().getServiceBroker().getService(this, LoggingService.class, null);

    // get event service
    myEventService = 
      (EventService) getBindingSite().getServiceBroker().getService(this, EventService.class, null);

    mySDFactory = (SDFactory) getFactory(SDDomain.SD_NAME);

    setOPCONSchedule(buildOPCONSchedule());

    setNeedToFindProviders(true);

    initializeNumberOfProvidersPerRole();

    if (didRehydrate()) {
      if (needToFindProviders()) {
	findProviders();
      }
    }
  }

  public void execute() {
    if (needToFindProviders()) {
      if (myLoggingService.isDebugEnabled()) {
	myLoggingService.debug(getAgentIdentifier() +
			       ": execute - needToFindProviders.");
      }
      
      findProviders();
    }


    if (myFindProvidersTaskSubscription.hasChanged()) {
      updateFindProvidersTaskDispositions();
    }

    // If matchmaker has new possible providers, look at options,
    // and generate the relays possibly
    if (myMMRequestSubscription.hasChanged()) {
      generateServiceRequests(myMMRequestSubscription.getChangedCollection());
    }

    //if your relays have changed, check for revokes
    if (myServiceContractRelaySubscription.hasChanged()) {
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
	updateFindProvidersTaskDispositions();
      }


      Collection removed = myServiceContractRelaySubscription.getRemovedCollection();
      
      for (Iterator removedIterator = removed.iterator();
	   removedIterator.hasNext();) {
	ServiceContract removedContract = 
	  ((ServiceContractRelay) removedIterator.next()).getServiceContract();

	if (removedContract != null) {
	  TimeSpan timeSpan = 
	    mySDFactory.getTimeSpanFromPreferences(removedContract.getServicePreferences());

	  Role role = removedContract.getServiceRole();
	  if (!checkProviderCompletelyRequested(role, timeSpan)) {
	    queryServices(role, timeSpan);
	  }
	}
      }
    }

    if (myLineageSubscription.hasChanged()) {
      handleChangedLineage();
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
	//What had the contract covered?
	TimeSpan timeSpan = 
	  mySDFactory.getTimeSpanFromPreferences(relay.getServiceRequest().getServicePreferences());
	queryServices(relay.getServiceContract().getServiceRole(), 
		      timeSpan);
        //publishRemove(relay);
      }
    }

  }

  /**
   * create & publish a relay with service request to the provider specified in the
   * serviceDescription for the specified time interval.
   */
  protected void requestServiceContract(ServiceDescription serviceDescription,
                                        TimeSpan interval) {
    Role role = getRole(serviceDescription);
    if (role == null) {
      if (myLoggingService.isWarnEnabled()) {
        myLoggingService.warn(getAgentIdentifier() +
                               ": error requesting service contract: a null role");
      }
    } else {
      String providerName = serviceDescription.getProviderName();
      ServiceRequest request =
          mySDFactory.newServiceRequest(getSelfOrg(),
					role,
					mySDFactory.createTimeSpanPreferences(interval));

      ServiceContractRelay relay =
          mySDFactory.newServiceContractRelay(MessageAddress.getMessageAddress(providerName),
					      request);
      if (myLoggingService.isDebugEnabled()) {
        myLoggingService.debug(getAgentIdentifier() + 
			      ": requestServiceContract() publish relay to " + providerName +
			      " asking for role: " + role + 
			      " from " + new Date(interval.getStartTime()) +
			      " to " + new Date(interval.getEndTime()));
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

  protected Lineage getCommandLineage(TimeSpan timeSpan) {
    Collection matches = getOPCONSchedule().intersectingSet(timeSpan);
    
    Lineage commandLineage = null;

    switch (matches.size()) {
      
      case 0:
	break;

      case 1:
	LineageTimeSpan lineageTimeSpan = 
	  (LineageTimeSpan)(matches.iterator().next());
	commandLineage = lineageTimeSpan.getLineage();
	break;
	
      default:
	if (myLoggingService.isDebugEnabled()) {
	  myLoggingService.debug(getAgentIdentifier() + 
				 " getCommandLineage: OPCON schedule has " +
				 matches.size() + " " + matches + 
				 " elements overlapping " + timeSpan);
	}
	break;
    }

    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(getAgentIdentifier() + 
			     ": getCommandLineage: returning - " + commandLineage + 
			     " for " + timeSpan);
    }

    return commandLineage;
  }

  protected TimeSpan getOPCONTimeSpan() {
    if ((getOPCONSchedule().first() != null) &&
	(getOPCONSchedule().last() != null)) {
      TimeSpan opconTimeSpan = 
	TimeSpans.getSpan(((TimeSpan) getOPCONSchedule().first()).getStartTime(),
			  ((TimeSpan) getOPCONSchedule().last()).getEndTime());
      return opconTimeSpan;
    } else {
      return DEFAULT_TIME_SPAN;
    }
  }

  protected void queryServices(Role role) {
    queryServices(role, getOPCONTimeSpan());
  }

  /**
   * create and publish a MMQueryRequest for this role
   */
  protected void queryServices(Role role, TimeSpan timeSpan) {
    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(getAgentIdentifier() +
			     ": queryServices() role = " + role +
			     " timeSpan = " + new Date(timeSpan.getStartTime()) + 
			     " to " + new Date(timeSpan.getEndTime()));
    }

    String minimumEchelon = getMinimumEchelon(role);

    Collection opconLineages = getOPCONSchedule().intersectingSet(timeSpan);
    
    
    if (opconLineages.size() == 0) {
      myLoggingService.debug(getAgentIdentifier() + 
			     ": queryServices: no OPCON Lineage on blackboard" +
			     "for requested time span - " + 
			     timeSpan.getStartTime() + " to " +
			     timeSpan.getEndTime() +
			     ". Making single element OPCON lineage.");

      // Build a 1 node opcon list
      ArrayList list = new ArrayList();
      list.add(getAgentIdentifier().toString());
      MutableTimeSpan defaultTimeSpan = new MutableTimeSpan();
      defaultTimeSpan.setTimeSpan(SDFactory.DEFAULT_START_TIME,
				  SDFactory.DEFAULT_END_TIME);

      LineageTimeSpan lineageTimeSpan = 
	new LineageTimeSpan(mySDFactory.newLineage(Lineage.OPCON,
						   list,
						   defaultTimeSpan),
			    defaultTimeSpan);
      opconLineages = new ArrayList();
      opconLineages.add(lineageTimeSpan);
    }

    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(getAgentIdentifier() +
			     ": queryServices() OPCON schedule = " + getOPCONSchedule() +
			     " OPCON schedule intersectingSet = " + opconLineages);
    }
    
    for (Iterator iterator = opconLineages.iterator();
	 iterator.hasNext();) {
      LineageTimeSpan opconTimeSpan = (LineageTimeSpan) iterator.next();

      LineageEchelonScorer scorer = 
	new LineageEchelonScorer(opconTimeSpan.getLineage(),
				 minimumEchelon,
				 role);
      queryServices(role, scorer, opconTimeSpan);
    }
  }

  /**
   * create and publish a MMQueryRequest for this role
   */
  protected void queryServices(Role role, ServiceInfoScorer scorer, 
			       TimeSpan timeSpan) {
    boolean outstandingQuery = false;
    
    // Check to make sure we don't already have an outstanding request for this time span
    for (Iterator queryIterator = myMMRequestSubscription.iterator(); 
	 queryIterator.hasNext();) {
      MMQueryRequest request = (MMQueryRequest) queryIterator.next();
      
      if (matchingRequest(request, role, timeSpan, scorer)) {
	outstandingQuery = true;
	
	if (myLoggingService.isDebugEnabled()) {
	  myLoggingService.debug(getAgentIdentifier() +
				 " ignoring call to ask for MatchMaker for role : " + role +
				 " - serviceInfoScorer : " + scorer + 
				 " - timeSpan : " + timeSpan +
				 ". Outstanding MMQuery  - " + request.getQuery() + 
				 " - already exists.");
	}
	break;
      }
    }
    
    if (!outstandingQuery) {
      if (myLoggingService.isDebugEnabled()) {
	myLoggingService.debug(getAgentIdentifier() +
			       " asking MatchMaker for role : " + role +
			       " - serviceInfoScorer : " + scorer + 
			       " - timeSpan : " + timeSpan);
      }
      MMQueryRequest mmRequest =
        mySDFactory.newMMQueryRequest(new MMRoleQuery(role, scorer, timeSpan));
      publishAdd(mmRequest);
    }
  }

  protected void setNeedToFindProviders(boolean flag) {
    myNeedToFindProviders = flag;
  }

  // If we have not yet created the service requests, and we're
  // in an OPLAN stage where we should do work, return true
  protected boolean needToFindProviders() {
    return ((myNeedToFindProviders) && (!myFindProvidersTaskSubscription.isEmpty()));
  }

  // For each role in parameters, generate a MMQueryRequest
  protected void findProviders() {
    Collection roleParams = parseRoleParams();
    
    for (Iterator iterator = roleParams.iterator();
	 iterator.hasNext();) {
      Role role = Role.getRole((String)iterator.next());
      
      if (!checkProviderCompletelyRequested(role, getOPCONTimeSpan())) {
	queryServices(role);

      }
    }
    setNeedToFindProviders(false);
  }

  protected Collection parseRoleParams() {
    Collection params = getDelegate().getParameters();
    ArrayList roleparams =  new ArrayList(1);

    for (Iterator iterator = params.iterator();
	 iterator.hasNext();) {
      String fullParam = (String)iterator.next();
      if (fullParam.indexOf(":") > 0) {
        roleparams.add(fullParam.substring(0,fullParam.indexOf(":")));
      }
      else {
        roleparams.add(fullParam);
      }
   }
    return roleparams;
  }

  protected void initializeNumberOfProvidersPerRole() {
    Collection params = getDelegate().getParameters();

    for (Iterator iterator = params.iterator();
	 iterator.hasNext();) {
      String fullParam = (String)iterator.next();
      int endRoleIndex;
      if (fullParam.indexOf(":") > 0) {
        endRoleIndex = fullParam.indexOf(":");
        Role desiredRole = Role.getRole(fullParam.substring(0, endRoleIndex));
        String numProviders = 
	  fullParam.substring(endRoleIndex + 1, fullParam.length());
        if (myLoggingService.isInfoEnabled()) {
          myLoggingService.info("numProviders desired for role " +
				desiredRole + " is " + numProviders);
        }
        Integer i = new Integer(numProviders);
        if (i != null) {
          myRoles.put(desiredRole, i);
        }
      }
    }
  }

  /**
   * For each answered request, pick an appropriate provider
   * and send them a service request.
   * Note that input is the changed service requests only.
   * Also need HashMap of number of providers desired for each role.
   */
  protected void generateServiceRequests(Collection mmRequests) {
    for (Iterator iterator = mmRequests.iterator();
         iterator.hasNext();) {
      MMQueryRequest mmRequest = (MMQueryRequest) iterator.next();
      MMRoleQuery query = (MMRoleQuery) mmRequest.getQuery();


      if (myLoggingService.isDebugEnabled()) {
        myLoggingService.debug(getAgentIdentifier() + 
			       ": generateServiceRequests() MMQueryRequest " +
			       " has changed: " + mmRequest.getUID() + 
			       " query = " + query);
      }

      if (query.getObsolete()) {
	if (myLoggingService.isDebugEnabled()) {
	  myLoggingService.debug(getAgentIdentifier() + 
				 ": generateServiceRequests() ignoring obsolete request - " +
				 mmRequest.getUID());
	}
	continue;
      }
      

      // Only do anything if the query has a result
      Collection services = mmRequest.getResult();
      
      if ((services == null) ||
	  (services.size() == 0)) {
	// MMPlugin said no one matched?
	if (System.currentTimeMillis() > getWarningCutoffTime()) {
	  myLoggingService.error(getAgentIdentifier() + 
				 ": generateServiceRequests() got 0 results" +
				 " for query - " + 
				 query);
	} else if (myLoggingService.isDebugEnabled()) {
	  myLoggingService.debug(getAgentIdentifier() + 
				 ": generateServiceRequests() got 0 results" +
				 " for query - " + 
				 query);
	}
      } else {
        Role role = query.getRole();
	TimeSpan timeSpan = query.getTimeSpan();

        if (myLoggingService.isDebugEnabled()) {
          myLoggingService.debug(getAgentIdentifier() + 
				 ": generateServiceRequests() results for " +
				 "query - " +
                                 query + " for role = " + role +
				 ", time span = " + query.getTimeSpan() + 
				 " - number of avail providers - " +
                                 services.size());
        }

        Collection intervals = 
	      getCurrentlyUncoveredIntervalsWithoutOutstandingRequests(
                timeSpan.getStartTime(),
		timeSpan.getEndTime(), 
		role);
	if (intervals.size() == 0) {
	  if (myLoggingService.isDebugEnabled()) {
	    myLoggingService.debug(getAgentIdentifier() +
				   ": generateServiceRequests() no " +
				   " uncovered time periods for - " + role +
				   ". Will not generate a service request.");
	  }
	  continue; // on to the next changed MMRequest
	}
    
	int desiredNumberOfProviders = getDesiredNumberOfProviders(role);
	
        //make sure any ties are resolved in a reliable & consistent order
        Collection servicesList = 
	  reorderAnyTiedServiceDescriptions(new ArrayList(services));
	
	//now, for each interval, pick a provider (service description) and
	//request a contract
	for (Iterator neededIntervals = intervals.iterator();
	     neededIntervals.hasNext();) {
	  TimeSpan currentInterval = (TimeSpan) neededIntervals.next();
	  
	  boolean madeNewRequest = false;
	  int numProviderFound = 0;
	  
	  for (Iterator serviceIterator = servicesList.iterator();         
	       serviceIterator.hasNext() &&
		 numProviderFound < desiredNumberOfProviders;) {
	    ScoredServiceDescription sd = 
	      (ScoredServiceDescription) serviceIterator.next();
	    //if you have already asked this provider, skip it
	    if (alreadyAskedForContractWithProvider(role, 
						    sd.getProviderName(), 
						    currentInterval)) {
	      if (myLoggingService.isDebugEnabled()) {
		myLoggingService.debug(getAgentIdentifier() +
				       " skipping " + 
				       sd.getProviderName() + 
				       " for role: " + role);
	      }
	    } else {
	      //remember that you found a provider to request from
	      madeNewRequest = true;
	      numProviderFound++;
	      //do the request
	      if (myLoggingService.isDebugEnabled()) {
		myLoggingService.debug(getAgentIdentifier() +
				       " requesting contract with " + 
				       sd.getProviderName() + 
				       " for role - " + 
				       role + ", time - " + 
				       currentInterval);
	      }
	      requestServiceContract(sd, currentInterval);
	    }
	  }  // end of for loop for number of desired providers

	  //if you were not able to find a provider to request from
	  //for this interval, take appropriate action
	  if (!madeNewRequest) {
	    handleRequestWithNoRemainingProviderOption(role, currentInterval);
	  } 
	}// end of for loop over uncovered intervals
      } // end of loop over MMRequests 
    }
  }

  /**
   * Modify the order of scoredServiceDescriptions so that ties are in the 
   * order you want them to be
   */
  protected Collection reorderAnyTiedServiceDescriptions(ArrayList scoredServiceDescriptions) {
    return scoredServiceDescriptions;
    //do nothing, trust the matchmaker ordering
  }

  /**
   * return true if you already have a service contract relay with this 
   * provider
   */
  protected boolean alreadyAskedForContractWithProvider(Role role, 
                                                        String providerName,
                                                        TimeSpan timeSpan) {
    for (Iterator relayIterator = myServiceContractRelaySubscription.iterator();
         relayIterator.hasNext();) {
      ServiceContractRelay relay =
        (ServiceContractRelay) relayIterator.next();
      if (relay.getProviderName().equals(providerName) &&
          relay.getClient().equals(getSelfOrg()) &&
          relay.getServiceRequest().getServiceRole().equals(role)) {
	// Did we ask for the same time period?
	TimeSpan requestedTimeSpan = 
	  mySDFactory.getTimeSpanFromPreferences(relay.getServiceRequest().getServicePreferences());
	return (requestedTimeSpan.equals(timeSpan));
      }
    }
    return false;
  }

  /**
   * Log a warning that you couldn't find a provider for this option
   */
  protected void handleRequestWithNoRemainingProviderOption(Role role, 
							    TimeSpan currentInterval) {
    //this means you have a time interval where you have exhausted all possible
    //providers. Log a warning.
    if (myLoggingService.isWarnEnabled()) {
      myLoggingService.warn(getAgentIdentifier() +
                            " failed to get contract for " + role +
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
    TimeSpan timeSpan = TimeSpans.getSpan(desiredStart, desiredEnd);
    if (!checkProviderCompletelyRequested(role, timeSpan)) {
      ret.add(timeSpan);
    }
    return ret;
  }

  /**
   * return true if you have a non-revoked service contract for
   * this role or if you have an unanswered request
   */
  protected boolean checkProviderCompletelyRequested(Role role,
						     TimeSpan timeSpan) {
    if (timeSpan == null) {
      return false;
    }

    return requested(role, timeSpan);
  }


  /**
   * return true if you have a non-revoked service contract for
   * this role or if you have an unanswered request
   */
  protected boolean checkProviderCompletelyCovered(Role role,
						   TimeSpan timeSpan) {
    if (timeSpan == null) {
      return false;
    }

    return covered(role, timeSpan);
  }

  protected boolean covered(Role role,
			    TimeSpan timeSpan) {

    TimeSpanSet contractTimeSpanSet = new TimeSpanSet();

    for (Iterator relayIterator = myServiceContractRelaySubscription.iterator();
         relayIterator.hasNext();) {
      ServiceContractRelay relay =
        (ServiceContractRelay) relayIterator.next();

      ServiceContract contract = relay.getServiceContract();
      if ((contract != null) &&
	  (!contract.isRevoked()) &&
	  (relay.getClient().equals(getSelfOrg())) &&
	  (contract.getServiceRole().equals(role))) {
	TimeSpan contractTimeSpan = 
	  mySDFactory.getTimeSpanFromPreferences(contract.getServicePreferences());
	contractTimeSpanSet.add(contractTimeSpan);
      }
    }

    return continuousCoverage(timeSpan, contractTimeSpanSet);

  }

  protected boolean requested(Role role,
			      TimeSpan timeSpan) {
    
    TimeSpanSet requestTimeSpanSet = new TimeSpanSet();

    for (Iterator relayIterator = myServiceContractRelaySubscription.iterator();
         relayIterator.hasNext();) {
      ServiceContractRelay relay =
        (ServiceContractRelay) relayIterator.next();

      ServiceRequest request = relay.getServiceRequest();
      
      if (relay.getClient().equals(getSelfOrg()) &&
	  request.getServiceRole().equals(role)) {
	TimeSpan requestTimeSpan = 
	  mySDFactory.getTimeSpanFromPreferences(request.getServicePreferences());
	requestTimeSpanSet.add(requestTimeSpan);
      }
    }

    return continuousCoverage(timeSpan, requestTimeSpanSet);
  }

  protected boolean continuousCoverage(TimeSpan targetTimeSpan, TimeSpanSet timeSpanSet) {
    if (timeSpanSet.isEmpty()) {
      return false;
    }

    long currentEarliest = -1;
    long currentLatest = -1;
    for (Iterator timeSpanIterator = timeSpanSet.iterator();
	 timeSpanIterator.hasNext();) {
      TimeSpan timeSpan = (TimeSpan) timeSpanIterator.next();
      if (currentEarliest == -1) {
	currentEarliest = timeSpan.getStartTime();
      } else {
	currentEarliest = Math.min(currentEarliest, timeSpan.getStartTime());
      }

      if (currentLatest == -1) {
	currentLatest = timeSpan.getEndTime();
      } else {
	if (currentLatest < timeSpan.getStartTime()) {
	  // Missing coverage
	  if (myLoggingService.isDebugEnabled()) {
	    myLoggingService.debug(getAgentIdentifier() + 
				   ":  continuousCoverage() returning false for timeSpan = " + 
				   new Date(timeSpan.getStartTime()) + 
				   " to " + new Date(timeSpan.getEndTime()) + 
				   ". Gap in coverage detected at " + new Date(currentLatest));
	  }
	  return false;
	} else {
	  currentLatest = Math.max(currentLatest, timeSpan.getEndTime());
	}
      }
    }

    return (currentEarliest <= targetTimeSpan.getStartTime() &&
	    currentLatest >= targetTimeSpan.getEndTime());
  }

  protected void handleChangedLineage() {
    
    NonOverlappingTimeSpanSet currentOPCONSchedule = buildOPCONSchedule();
    
    
    if (currentOPCONSchedule.equals(getOPCONSchedule())) {
      if (myLoggingService.isDebugEnabled()) {
	myLoggingService.debug(getAgentIdentifier() + ": handleChangedLineage() " +
			       " change in lineage subscription - no change in OPCON schedule. " +
			       " currentOPCONSchedule = " + currentOPCONSchedule + 
			       " previous OPCON schedule = " + getOPCONSchedule());
      }
    } else {
      if (myLoggingService.isDebugEnabled()) {
	myLoggingService.debug(getAgentIdentifier() + ": handleChangedLineage() " + 
			       " change in OPCON schedule - " +
			       " currentOPCONSchedule = " + currentOPCONSchedule + 
			       " previous OPCON schedule = " + getOPCONSchedule());
      }
     
      verifyOutstandingRequests(currentOPCONSchedule);
      verifyServiceContracts(currentOPCONSchedule);
    
      setOPCONSchedule(currentOPCONSchedule);
      
      // Ping execute so that we query for missing services
      setNeedToFindProviders(true);
      wake();
    }
  }

  // Log helper: Where have we gotten to?
  private String stateMessage() {
    String message = "State of plugin - ";

    //what is the status of the find providers task
    for (Iterator iterator = myFindProvidersTaskSubscription.iterator();
         iterator.hasNext();) {
      Task task = (Task) iterator.next();
      PlanElement pe = task.getPlanElement();
      if ((pe != null) && 
	  (pe.getEstimatedResult() != null)) {
        message = message.concat("\n     FindProviders conf: " + 
			 pe.getEstimatedResult().getConfidenceRating());
      } else {
        message = message.concat("\n     FindProviders task has no result yet");
      }
    }

    //which roles do we need?
    message = message.concat("\n     The roles needed are: ");
    Collection roleparams = parseRoleParams();
    for (Iterator iterator = roleparams.iterator();
         iterator.hasNext();) {
      Role role = Role.getRole((String) iterator.next());
      message = message.concat(role + " ");
    }

    //what service contracts relays do we have, and which have contracts 
    // (replies)?
    for (Iterator relayIterator = myServiceContractRelaySubscription.iterator();
         relayIterator.hasNext();) {
      ServiceContractRelay relay =
        (ServiceContractRelay) relayIterator.next();

      if (relay.getClient().equals(getSelfOrg())) {
        message = message.concat("\n     Sent a service contract relay to " + 
				 relay.getProviderName() + " for the role " +
				 relay.getServiceRequest().getServiceRole());
      }
      if (relay.getServiceContract() != null &&
          relay.getClient().equals(getSelfOrg())) {
        message = message.concat(", and the provider has answered.");
      }
      else if (relay.getServiceContract() == null &&
              relay.getClient().equals(getSelfOrg())) {
        message = message.concat(", but no answer yet from the provider.");
      }
    }

    // Now look to see what roles we have asked the MM for
    for (Iterator queryIterator = myMMRequestSubscription.iterator(); 
	queryIterator.hasNext();) {
      MMQueryRequest request = (MMQueryRequest) queryIterator.next();
      message = message.concat("\n     MMQueryRequest exists for " + 
			       ((MMRoleQuery)request.getQuery()).getRole());
      if (request.getResult() == null) {
        message = message.concat(", but no reply.");
      }
      else {
        message = message.concat(", and reply exists with " + 
				 request.getResult().size() + 
				 " possible providers.");
      }
    }
    return message;
  }

  // See if we have all contracts for all the roles we need. If so, set
  // FindProviders conf to 1. Otherwise, to 0.
  private void updateFindProvidersTaskDispositions() {
    // Print verbose status of all requests
    if (myLoggingService.isInfoEnabled()) {
      myLoggingService.info(getAgentIdentifier() + 
      ": updateFindProvidersTaskDispositions() " + stateMessage());
    }

    if (myFindProvidersTaskSubscription.isEmpty()) {
      // Nothing to update
      return;
    }
    double conf = 1.0;

    // First determine whether we have found all our providers

    // Look to see if we've got all our contracts for roles
    // specified as plugin parameters. If any one is missing,
    // then overall confidence stays 0
    Collection roleParams = parseRoleParams();
    
    if (myLoggingService.isInfoEnabled()) {
      myLoggingService.info(getAgentIdentifier() +
			    ": updateFindProvidersTaskDispositions: contracts found - ");
    }
    
    for (Iterator iterator = roleParams.iterator();
	 iterator.hasNext();) {
      Role role = Role.getRole((String) iterator.next());
      boolean foundContract = 
	checkProviderCompletelyCovered(role,
				       getOPCONTimeSpan());
      
      if (myLoggingService.isInfoEnabled()) {
	myLoggingService.info("     " + foundContract + " for role " + role);
      }
      
      if (!foundContract) {
	conf = 0.0;
	break;
      } // end of block where found no contract for a Role
    } // end of loop over Roles passed in as parameters

    // Did we just find our providers? This will signal
    // whether the agent should persiste
    boolean justFoundProviders = false;
    
    //Only request persist if in initial FindProviders stage
    boolean doingInitialFindProviders = true;
    
    // Now update the confidence on the FindProviders task
    for (Iterator iterator = myFindProvidersTaskSubscription.iterator();
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
          myLoggingService.info(getAgentIdentifier() +
                                ": updateFindProvidersTaskDispositions: Create disposition with conf " 
                                + conf);
        }
	publishAdd(disposition);

	// Are we in the first findProviders stage? Then if conf = 1, 
	// justFoundProviders
	// needToFindProviders() says we've received the FindProvider task.
	// but I don't want to persist with every oplan stage change
	if ((PERSIST_EARLY) && 
	    (doingInitialFindProviders) && 
	    (conf == 1.0)) {
	  justFoundProviders = true;
	  if (myLoggingService.isInfoEnabled()) {
	    myLoggingService.info(getAgentIdentifier() 
                                  + ": updateFindProvidersTaskDispositions: " +
				  " Just added a conf 1.0 disposition to findProviders while doingInitialFindProviders - going to request persistence.");
	  }
	}
      } else {
	if (conf != pe.getEstimatedResult().getConfidenceRating()) {
	  if (myLoggingService.isInfoEnabled()) {
            myLoggingService.info(getAgentIdentifier() + 
				  ": updateFindProvidersTaskDispositions() Changed conf from " +
				  pe.getEstimatedResult().getConfidenceRating() +
				  " to " + conf);
	  } // end if logging block

	  pe.setEstimatedResult(estResult);
	  publishChange(pe);
	  
	  // if conf = 1.0 and we're in the first findProviders stage
	  // then justFoundProviders
	  // needToFindProviders says this is at least the findProviders,
	  // but I don't want to persist with every oplan stage change
	  if ((PERSIST_EARLY) && 
	      (doingInitialFindProviders) && 
	      (conf == 1.0)) {
	    justFoundProviders = true;
	    if (myLoggingService.isInfoEnabled())
	      myLoggingService.info(getAgentIdentifier() 
                                    + " just changed findProviders dispo to 1.0 while doingInitialFindProviders - going to request persistence.");
	  }

	}
      } // end of block to change PE conf
    } // end of loop over FindProviders tasks

    if (conf == 1.0) {
      resetWarningCutoffTime();
    }

    handlePersistEarly(justFoundProviders);

  } // end of method

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

  protected long getWarningCutoffTime() {
    if (myWarningCutoffTime == -1) {
      WARNING_SUPPRESSION_INTERVAL = 
	Integer.getInteger(CLIENT_GRACE_PERIOD, 
			   WARNING_SUPPRESSION_INTERVAL).intValue();
      myWarningCutoffTime = System.currentTimeMillis() + 
	(WARNING_SUPPRESSION_INTERVAL * 60000);
    }
    
    return myWarningCutoffTime;
  }

  protected void resetWarningCutoffTime() {
    myWarningCutoffTime = -1;
  }

  protected int getDesiredNumberOfProviders(Role role) {
    Integer desiredProviders = ((Integer) myRoles.get(role));
    if (desiredProviders != null) {
      return desiredProviders.intValue();
    } else {
      return 1;
    }
  }
 
  protected NonOverlappingTimeSpanSet getOPCONSchedule() {
    return myOPCONSchedule;
  }

  protected void setOPCONSchedule(NonOverlappingTimeSpanSet opconSchedule) {
    myOPCONSchedule = opconSchedule;
  }

  protected NonOverlappingTimeSpanSet buildOPCONSchedule() {
    NonOverlappingTimeSpanSet opconSchedule = new NonOverlappingTimeSpanSet();
    
    for (Iterator iterator = myLineageSubscription.iterator();
	 iterator.hasNext();) {
      Lineage lineage = (Lineage) iterator.next();
      
      if (lineage.getType() == Lineage.OPCON) {
	List lineageSchedule = new ArrayList(lineage.getSchedule());
	
	if (myLoggingService.isDebugEnabled()) {
	  myLoggingService.debug(getAgentIdentifier() + 
				 " buildOPCONSchedule() - " +
				 " found OPCON lineage " + lineage);
	}
	
	for (Iterator scheduleIterator = lineageSchedule.iterator();
	     scheduleIterator.hasNext();) {
	  ScheduleElement element = (ScheduleElement) scheduleIterator.next();
	    opconSchedule.add(new LineageTimeSpan(lineage, 
						  element.getStartTime(),
						  element.getEndTime()));
	}
      }
    }

    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(getAgentIdentifier() + " buildOPCONSchedule() " +
			     " current schedule - " + opconSchedule);
    }

    return opconSchedule;
  }

  protected boolean matchingRequest(MMQueryRequest request,
				    Role role,
				    TimeSpan timeSpan,
				    ServiceInfoScorer scorer) {
    if (!(request.getQuery() instanceof MMRoleQuery)) {
      return false;
    }
    
    MMRoleQuery query = (MMRoleQuery) request.getQuery();

    if (query.getObsolete()) {
      return false;
    }

    MMRoleQuery newQuery = new MMRoleQuery(role, scorer, timeSpan);

    if (request.getResult() == null) {
      return (query.equals(newQuery));
    } else {
      return false;
    }
  }

      
  private void handlePersistEarly(boolean justFoundProviders) {
    // If the agent just finished finding providers, force a persistence now
    // that way, if the agent dies anytime after this, it will come
    // back with its providers intact
    if (PERSIST_EARLY && justFoundProviders) {
      try {
	// Bug 3282: Persistence doesn't put in the correct reasons for 
	// blocking in SchedulableStatus
	getBlackboardService().persistNow();
	
	// Now send a Cougaar event indicating the agent has its providers
	// and has persisted them.
	if (myEventService != null &&
	    myEventService.isEventEnabled()) {
	  myEventService.event(getAgentIdentifier() + 
			     " persisted after finding providers.");
	} else {
	  myLoggingService.info(getAgentIdentifier() + 
				" (no event service): persisted after finding providers.");
	}
      } catch (PersistenceNotEnabledException nope) {
	if (myEventService != null &&
	    myEventService.isEventEnabled()) {
	  myEventService.event(getAgentIdentifier() + 
			     " finished finding providers (persistence not enabled).");
	} else {
	  myLoggingService.info(getAgentIdentifier() + 
				" (no event service): finished finding providers (persistence not enabled).");
	}
      } // try/catch block
    } // if we just found our providers
  }

  public void verifyOutstandingRequests(TimeSpanSet currentOPCONSchedule) {
    for (Iterator requestIterator = myMMRequestSubscription.iterator();
	 requestIterator.hasNext();) {
      MMQueryRequest request =
	(MMQueryRequest) requestIterator.next();
      MMRoleQuery query = (MMRoleQuery) request.getQuery();
      boolean obsolete = false;
      
      if (myLoggingService.isDebugEnabled()) {
	myLoggingService.debug(getAgentIdentifier() + 
			       ": verifyOutstandingRequests() " + 
			       " iterating over MMQueryRequests, request = " +
			       request.getUID() + 
			       " query = " + query);
      }
    
      if (query.getObsolete()) {
	if (myLoggingService.isDebugEnabled()) {
	  myLoggingService.debug(getAgentIdentifier() + 
				 ": verifyOutstandingRequests() " + 
				 " request = " + request.getUID() +
				 " already marked as obsolete");
	}
	continue;
      }

      TimeSpan requestTimeSpan = query.getTimeSpan();
      Lineage requestLineage = getCommandLineage(requestTimeSpan);

      if (requestLineage == null) {
	if (myLoggingService.isDebugEnabled()) {
	  myLoggingService.debug(getAgentIdentifier() + 
				 ": verifyOutstandingRequests() " + 
				 " request = " + request.getUID() +
				 " no longer matches the command lineage." +
				 "Marking as obsolete");
	}
	obsolete = true;
      } else {
	TimeSpanSet currentMatches = 
	  new TimeSpanSet(currentOPCONSchedule.intersectingSet(requestTimeSpan));
	
	
	if ((currentMatches.isEmpty()) || 
	    (((TimeSpan) currentMatches.first()).getStartTime() > 
	     requestTimeSpan.getStartTime())) {
	  if (myLoggingService.isDebugEnabled()) {
	    myLoggingService.debug(getAgentIdentifier() + 
				   ": verifyOutstandingRequest() " + 
				   " iterating over MMQueryRequest, " +
				   " request = " + request.getUID() +
				   " for " + 
				   new Date(requestTimeSpan.getStartTime()) + 
				   " - " +
				   new Date(requestTimeSpan.getEndTime()) + 
				   " no longer has an OPCON. Marking as obsolete");
	  }
	  obsolete = true;
	} else {
	  long currentLatest = TimeSpan.MAX_VALUE;
	  
	  // check for lineage change
	  for (Iterator currentIterator = currentMatches.iterator();
	       currentIterator.hasNext();) {
	    LineageTimeSpan currentTimeSpan = 
	      (LineageTimeSpan) currentIterator.next();
	    
	    // Check that OPCONs are continguous
	    if (currentTimeSpan.getStartTime() > currentLatest) {
	      //opcon gap 
	      if (myLoggingService.isDebugEnabled()) {
		myLoggingService.debug(getAgentIdentifier() + 
				       ": verifyOutstandingRequests() " + 
				       " OPCON gap starting at " + 
				       new Date(currentLatest) +
				       " - marking MMQueryRequest = " +
				       request.getUID() + " as obsolete.");
	      }
	      
	      obsolete = true;
	      break;
	    }
	    
	    if (!requestLineage.getList().equals(currentTimeSpan.getLineage().getList())) {
	      if (myLoggingService.isDebugEnabled()) {
		myLoggingService.debug(getAgentIdentifier() + 
				       ": verifyOutstandingRequest() " + 
				       " marking MMQueryRequest = " + 
				       request.getUID() +
				       " as obsolete." +
				       "Request lineage = " + 
				       requestLineage.getList() +
				       " does not equal current lineage = " +
				       currentTimeSpan.getLineage().getList());
	      }
	      
	      obsolete = true;
	      break;
	    }
	  }
	}
      }

      if (obsolete) {
	query.setObsolete(true);
	publishChange(request);
      }
    }
  }
	


  public void verifyServiceContracts(TimeSpanSet currentOPCONSchedule) {
    for (Iterator relayIterator = myServiceContractRelaySubscription.iterator();
	 relayIterator.hasNext();) {
      ServiceContractRelay relay =
	(ServiceContractRelay) relayIterator.next();
      
      if (myLoggingService.isDebugEnabled()) {
	myLoggingService.debug(getAgentIdentifier() + 
			       ": verifyServiceContracts() " + 
			       " iterating over service contract relays, relay = " +
			       relay);
      }
      
      ServiceContract contract = relay.getServiceContract();
      if ((contract != null) &&
	  (!contract.isRevoked()) &&
	  (relay.getClient().equals(getSelfOrg()))) {
	
	// Look at request for lineage because that shows what we asked for.
	ServiceRequest request = relay.getServiceRequest();
	TimeSpan requestTimeSpan = 
	  mySDFactory.getTimeSpanFromPreferences(request.getServicePreferences());
	
	Lineage requestLineage = getCommandLineage(requestTimeSpan);
	
	if (requestLineage == null) {
	  if (myLoggingService.isDebugEnabled()) {
	    myLoggingService.debug(getAgentIdentifier() + 
				   ": verifyServiceContracts() " +
				   " unable to find original lineage for request = " + 
				   request);
	  }
	  publishRemove(relay);
	  continue;
	} 
	
	TimeSpanSet currentMatches = 
	  new TimeSpanSet(currentOPCONSchedule.intersectingSet(requestTimeSpan));
	
	if ((currentMatches.isEmpty()) || 
	    (((TimeSpan) currentMatches.first()).getStartTime() > 
	     requestTimeSpan.getStartTime())) {
	  if (myLoggingService.isDebugEnabled()) {
	    myLoggingService.debug(getAgentIdentifier() + 
				   ": verifyServiceContracts() " + 
				   " iterating over service contract relays, removing relay = " +
				   relay + 
				   " requestTimeSpan = " + requestTimeSpan +
				   " no longer has an OPCON.");
	  }
	  publishRemove(relay);
	  continue;
	}
	
	
	Collection timeSpanPreferences = null;
	TimeSpan newRequestTimeSpan = null;
	
	long currentLatest = TimeSpan.MAX_VALUE;
	
	// check for lineage change
	for (Iterator currentIterator = currentMatches.iterator();
	     currentIterator.hasNext();) {
	  LineageTimeSpan currentTimeSpan = 
	    (LineageTimeSpan) currentIterator.next();
	  
	  // Check that OPCONs are continguous
	  if (currentTimeSpan.getStartTime() > currentLatest) {
	    //opcon gap 
	    if (myLoggingService.isDebugEnabled()) {
	      myLoggingService.debug(getAgentIdentifier() + 
				     ": verifyServiceContracts() " + 
				     " OPCON gap starting at " + 
				     new Date(currentLatest) +
				     " - resetting contract end to " + 
				     new Date(currentLatest));
	    }
	    
	    //change pref/, end time = currentTimeSpan.getStartTime();
	    newRequestTimeSpan = TimeSpans.getSpan(requestTimeSpan.getStartTime(),
						   currentLatest);
	    timeSpanPreferences = 
	      mySDFactory.createTimeSpanPreferences(newRequestTimeSpan);
	    break;
	  } else {
	    currentLatest = currentTimeSpan.getEndTime();
	  }
	  
	  if (!requestLineage.getList().equals(currentTimeSpan.getLineage().getList())) {
	    // adjust contract if possible
	    if (currentTimeSpan.getStartTime() > 
		requestTimeSpan.getStartTime()) {
	      if (myLoggingService.isDebugEnabled()) {
		myLoggingService.debug(getAgentIdentifier() + 
				       ": verifyServiceContracts() " + 
				       " resetting contract end to " + 
				       currentTimeSpan.getStartTime());
	      }
	      
	      //change pref/, end time = currentTimeSpan.getStartTime();
	      newRequestTimeSpan = TimeSpans.getSpan(requestTimeSpan.getStartTime(),
						     currentTimeSpan.getStartTime());

		timeSpanPreferences = 
		  mySDFactory.createTimeSpanPreferences(newRequestTimeSpan);
		break;
	      } else {
		if (myLoggingService.isDebugEnabled()) {
		  myLoggingService.debug(getAgentIdentifier() + 
					 ": verifyServiceContracts() " + 
					 " iterating over service contract relays, removing relay = " +
					 relay + 
					 " requestLineage - " + 
					 requestLineage.getList() + 
					 " - != currentLineage - " + 
					 currentTimeSpan.getLineage().getList());
		}
		publishRemove(relay);
	      }
	    break;
	  }
	}
	
	if (timeSpanPreferences != null) {
	  HashMap requestPreferences = 
	    copyPreferences(request.getServicePreferences());
	  
	  for (Iterator timeSpanIterator = timeSpanPreferences.iterator();
	       timeSpanIterator.hasNext();) {
	    
	    Preference timeSpanPreference = 
	      (Preference) timeSpanIterator.next();
	    requestPreferences.put(new Integer(timeSpanPreference.getAspectType()),
				   timeSpanPreference);
	  }
	  
	  if (myLoggingService.isDebugEnabled()) {
	    TimeSpan timeSpan = 
	      mySDFactory.getTimeSpanFromPreferences(requestPreferences.values());
	    myLoggingService.debug(getAgentIdentifier() + 
				   ": changing time span on service request - " +
				   relay + " to " + 
				   new Date(timeSpan.getStartTime()) + 
				   " - " + new Date(timeSpan.getEndTime()));
	  }
	  
	  ((ServiceRequestImpl) request).setServicePreferences(requestPreferences.values());
	  publishChange(relay);
	}
      }
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
    
  private static class LineageTimeSpan extends MutableTimeSpan {
    Lineage myLineage = null;

    public LineageTimeSpan(Lineage lineage, TimeSpan timeSpan) {
      this(lineage, timeSpan.getStartTime(), timeSpan.getEndTime());
    }

    public LineageTimeSpan(Lineage lineage, long startTime, long endTime) {
      super();

      setTimeSpan(startTime, endTime);
      myLineage = lineage;
    }

    public Lineage getLineage() {
      return myLineage;
    }

    public boolean equals(Object o) {
      if (o instanceof LineageTimeSpan) {
	LineageTimeSpan lineageTimeSpan = (LineageTimeSpan) o;
	return ((lineageTimeSpan.getStartTime() == getStartTime()) &&
		(lineageTimeSpan.getEndTime() == getEndTime()) &&
		(lineageTimeSpan.getLineage().equals(getLineage())));
      } else {
	return false;
      }
    }

    public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("start=" + new Date(getStartTime()) +
		 ", end=" + new Date(getEndTime()));
      
      buf.append(", lineage=" + myLineage);
      buf.append("]");
      
      return buf.toString();
    }
  }
}





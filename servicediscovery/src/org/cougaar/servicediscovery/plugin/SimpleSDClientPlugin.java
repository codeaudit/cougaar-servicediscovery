/* 
 * <copyright>
 * 
 *  Copyright 2004 BBNT Solutions, LLC
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

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.planning.ldm.PlanningDomain;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.Entity;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.Disposition;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.plugin.util.PluginHelper;

import org.cougaar.glm.ldm.Constants; // for FindProviders...

import org.cougaar.servicediscovery.SDDomain;
import org.cougaar.servicediscovery.SDFactory;
import org.cougaar.servicediscovery.description.MMRoleQuery;
import org.cougaar.servicediscovery.description.ScoredServiceDescription;
import org.cougaar.servicediscovery.description.ServiceClassification;
import org.cougaar.servicediscovery.description.ServiceDescription;
import org.cougaar.servicediscovery.description.ServiceInfoScorer;
import org.cougaar.servicediscovery.description.ServiceRequest;
import org.cougaar.servicediscovery.transaction.MMQueryRequest;
import org.cougaar.servicediscovery.transaction.ServiceContractRelay;
import org.cougaar.servicediscovery.util.RoleScorer;
import org.cougaar.servicediscovery.util.UDDIConstants;
import org.cougaar.util.TimeSpan;
import org.cougaar.util.UnaryPredicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

/**
 * Simple ServiceDiscovery ClientPlugin, responsible for initiating queries for services, 
 * taking the resulting service pointer, and sending a Relay to the Provider requesting service. 
 *<p>
 * Service Discovery is initiated by receiving a Find Providers task, and looking for the single Role
 * indicated on the AS Prepositional Phrase.
 * Sends a Disposition on the FindProviders Task when it gets a Relationship
 * with a provider for the service.
 *<p>
 * This Plugin is written to be extended (or copied). See the pizza module for an example of doing this.
 *<p>
 * Limitations: not quiescent aware, doesn't handle time-phased relationships, 
 * not guaranteed to work with persistence and restarts, and assumes that providers
 * will not revoke relationships or otherwise change their mind.
 */
public class SimpleSDClientPlugin extends ComponentPlugin {
  /** Our queries to the Matchmaker */
  private IncrementalSubscription myMMRequestSubscription;
  /** Our requests to Providers */
  private IncrementalSubscription myServiceContractRelaySubscription;
  /** FindProviders Tasks asking us to do work */
  private IncrementalSubscription myFindProvidersTaskSubscription;

  protected LoggingService myLoggingService;
  private DomainService myDomainService;

  // Used to create ServiceContractRelay, etc
  private SDFactory mySDFactory;

  // Used to create AllocationResults, Dispositions, etc
  private PlanningFactory planningFactory;

  // A pointer to the local (self) entity
  private Entity selfEntity;

  /**
   * Used by the binding utility through reflection to set my DomainService
   */
  public void setDomainService(DomainService domainService) {
    myDomainService = domainService;
  }

  /**
   * Get the DomainService set by reflection, for getting pointers to Factories.
   */
  protected DomainService getDomainService() {
    return myDomainService;
  }

  /**
   * Load other services, particularly those not essential to operations. 
   * In this case, use the DomainService to get Factories and release it. 
   * Also get the Logging Service.
   */
  public void load() {
    super.load();

    myLoggingService = (LoggingService)getServiceBroker().getService(this, LoggingService.class, null);
    if (myLoggingService == null)
      myLoggingService = LoggingService.NULL;

    mySDFactory = (SDFactory) getDomainService().getFactory(SDDomain.SD_NAME);
    planningFactory = (PlanningFactory) getDomainService().getFactory(PlanningDomain.PLANNING_NAME);

    // Once we've gotten the Factories, we don't need the Domain Service.
    getServiceBroker().releaseService(this, DomainService.class, getDomainService());
    setDomainService(null);
  }
  
  /** Release any services retrieved during load() -- in this case, the LoggingService. */
  public void unload() {
    if (myLoggingService != LoggingService.NULL) {
      getServiceBroker().releaseService(this, LoggingService.class, myLoggingService);
      myLoggingService = LoggingService.NULL;
    }
    super.unload();
  }

  /**
   * Subscribe to MMRequests (requests to the Matchmaker),
   * ServiceContractRelays (where we'll get responses from the Providers),
   * and FindProviders Tasks (requests to us to do ServiceDiscovery).
   */
  protected void setupSubscriptions() {
    myMMRequestSubscription = (IncrementalSubscription) getBlackboardService().subscribe(MYMMREQUESTPREDICATE);
    myServiceContractRelaySubscription = (IncrementalSubscription) getBlackboardService().subscribe(MYSERVICECONTRACTRELAYPREDICATE);
    myFindProvidersTaskSubscription = (IncrementalSubscription) getBlackboardService().subscribe(getFindProvidersPredicate());
  }

  /**
   * Issue MMQueries for FindProviders Tasks, ServiceContractRelays for responses to MMQueries, 
   * and Dispositions on the FindProviders Tasks when those Relays are filled in with a Contract. 
   *<p>
   * For each new FindProviders Task, issue an MMQuery to the Matchmaker, looking for the 
   * specified Role.
   * If an MMQuery changes, issue a new ServiceContractRelay to the first named Provider
   * that was found. 
   * If a ServiceContractRelay changes, update the Disposition on the original FindProviders
   * Task if we've now found a Provider for the needed Service.
   */
  protected void execute() {
    // One if block below per subscription

    // Look for new FindProviders Tasks -- requests for Services
    if (myFindProvidersTaskSubscription.hasChanged()) {
      // If we've been asked to do more ServiceDiscovery
      Collection adds = myFindProvidersTaskSubscription.getAddedCollection();

      for (Iterator addIterator = adds.iterator(); addIterator.hasNext();) {
	// Grab the task
        Task findProviderTask = (Task) addIterator.next();
	// Get the requested role from the Task.
        Role taskRole = getRole(findProviderTask);

	// Send the query for the given Role, because of the given Task.
        queryServices(taskRole, findProviderTask);
      }

      // We don't handle removes or changes to the FindProviders...
      if (myLoggingService.isDebugEnabled()) {
        Collection changes = myFindProvidersTaskSubscription.getChangedCollection();
        Collection removes = myFindProvidersTaskSubscription.getRemovedCollection();
        if (!changes.isEmpty() || !removes.isEmpty()) {
          myLoggingService.debug("execute: ignoring changed/deleted FindProvider tasks - " + 
				 " changes = " + changes +
                                 ", removes = " + removes);
        }
      }
    } // end of block to handle FindProviders Tasks

    // Look for responses on our queries to the Matchmaker for YP lookups
    if (myMMRequestSubscription.hasChanged()) {
      for (Iterator iterator = myMMRequestSubscription.getChangedCollection().iterator(); 
	   iterator.hasNext();) {
        MMQueryRequest mmRequest = (MMQueryRequest) iterator.next();

        if (myLoggingService.isDebugEnabled()) {
          myLoggingService.debug("execute: MMQueryRequest has changed." + mmRequest);
        }

	// Get the request
	Collection services = mmRequest.getResult();
        if (services != null) {
          // Debugging: print all results
          if (myLoggingService.isDebugEnabled()) {

            myLoggingService.debug("Results for query " + mmRequest.getQuery().toString());
            for (Iterator serviceIterator = services.iterator(); serviceIterator.hasNext();) {
              ScoredServiceDescription serviceDescription = 
		(ScoredServiceDescription) serviceIterator.next();
              myLoggingService.debug("Score " + serviceDescription.getScore());
              myLoggingService.debug("Provider name " + serviceDescription.getProviderName());
              myLoggingService.debug("*********");
            }
          } /// End of debugging....

	  // For each service
          for (Iterator serviceIterator = services.iterator(); serviceIterator.hasNext();) {
            ScoredServiceDescription serviceDescription = 
	      (ScoredServiceDescription) serviceIterator.next();

	    // Debugging...
            if (myLoggingService.isDebugEnabled()) {
              myLoggingService.debug("execute: - provider: " +
                                     serviceDescription.getProviderName() + 
				     " score: " + serviceDescription.getScore());
            } // end debug

	    // Request a Service Contract for it
            requestServiceContract(serviceDescription);
	    
            // only want one contract, no matter how many are in loop
	    if (myLoggingService.isDebugEnabled() && serviceIterator.hasNext())
	      myLoggingService.debug("Had more than one service: " + services.size());
            break;
          }

          // Done with the query so could clean up, but leave it for the MatchmakeryQueryServlet
          //getBlackboardService().publishRemove(mmRequest);
        }
      }
    } // end of block to handle response from the Matchmaker

    // Look for responses on our queries to potential providers
    if (myServiceContractRelaySubscription.hasChanged()) {
      Collection changedRelays = myServiceContractRelaySubscription.getChangedCollection();

      // Update disposition on FindProviders task
      if (changedRelays.size() > 0) {
        if (myLoggingService.isDebugEnabled()) {
          myLoggingService.debug("changedRelays.size = " + changedRelays.size() +
                                 ", updateFindProvidersTaskDispositions");
        }
        updateFindProvidersTaskDispositions(changedRelays);
      }
    } // end of block to handle changed ServiceContractRelays
  } // end of execute

  /////////
  // Methods to get the query to the Matchmaker...

  /**
   * Return the Role (indirect object) on the Constants.Prepositions.AS Prep Phrase in the given
   * FindProviders Task, if any -- this is the Provider Role we will look for.
   *
   * @param findProvidersTask Task which should contain an AS Prepositional Phrase
   * @return Role on AS Phrase, indicate Role desired from ServiceDiscovery -- may be null
   */
  protected Role getRole(Task findProvidersTask) {
    PrepositionalPhrase asPhrase = findProvidersTask.getPrepositionalPhrase(org.cougaar.planning.Constants.Preposition.AS);

    if (asPhrase == null) {
      return null;
    } else {
      return (Role) asPhrase.getIndirectObject();
    }
  }

  /**
   * Send an {@link MMQueryRequest} to the Matchmaker, requesting a provider
   * with the given {@link Role}, satisfying the given Task. 
   *<p>
   * Extenders of this plugin might choose to over-ride this method, though it usually will
   * not be necessary.
   *
   * @param role The {@link Role} to request
   * @param findProvidersTask The Task whose request this will satisfy
   */
  protected void queryServices(Role role, Task findProvidersTask) {
    MMQueryRequest mmRequest;

    if (myLoggingService.isInfoEnabled())
      myLoggingService.info("queryServices asking MatchMaker for: " + role + " because of task " + findProvidersTask);

    // Create a scoring function for the Matchmaker to use to weigh providers
    ServiceInfoScorer sis = getServiceInfoScorer(role, findProvidersTask);

    // Now create the set of YP / SD structures, publishing the request on the BlackBoard,
    // for the MatchmakerPlugin to pick up
    MMRoleQuery roleQuery = new MMRoleQuery(role, sis);
    mmRequest = mySDFactory.newMMQueryRequest(roleQuery);
    getBlackboardService().publishAdd(mmRequest);
  }// end of queryServices

  /**
   * Create a scoring function to weigh different services registered in the YP, for use
   * by the Matchmaker. 
   * In this case, create a function that requires the given (Commercial) role.
   *<p>
   * Extenders of this plugin may over-ride this method to use a different function.
   *
   * @param role the Role to look for
   * @param fpTask The FindProviders task this will satisfy
   * @return the scoring function to hand the Matchmaker
   */
  protected ServiceInfoScorer getServiceInfoScorer(Role role, Task fpTask) {
    // Create a Service Scorer that looks for a provider with the given role,
    return new RoleScorer(role);
  }

  ///////////
  // Support methods for tasking Matchmaker response and requesting the service from the Provider

  /**
   * Create and publish a relay with a service request for the specified Role, to the Provider 
   * specified in the serviceDescription.
   *
   * @param serviceDescription the Matchmaker tells us about the possible service provider in this Service Description
   */
  protected void requestServiceContract(ServiceDescription serviceDescription) {
    //  Pull the Role off the Commercial Service Scheme in this serviceDescription
    Role role = getRole(serviceDescription);
    if (role == null) {
      myLoggingService.error("Error requesting service contract: a null Role in serviceDescription: " + serviceDescription);
    } else {
      // Create a request good for all time
      TimeSpan timeSpan = TimeSpan.FOREVER;
      String providerName = serviceDescription.getProviderName();

      // Create a ServiceRequest from the self Entity, for the given Role,
      // for the specified time period (forever)
      ServiceRequest request = mySDFactory.newServiceRequest(getLocalEntity(), role,
                                                             mySDFactory.createTimeSpanPreferences(timeSpan));

      // Send that ServiceRequest in a Relay to the given Provider
      ServiceContractRelay relay = 
	mySDFactory.newServiceContractRelay(MessageAddress.getMessageAddress(providerName),
					    request);
      getBlackboardService().publishAdd(relay);
    }
  } // end of requestServiceContract

  /**
   * Retrieve the Role that the Provider is offering out of the ServiceDescription. 
   * Note that this assumes the role is under the CommercialServiceScheme, using the 
   * Domain's Constants file. Extenders will over-ride this method.
   *
   * @param serviceDescription The Description of the Service as advertised in the YP.
   * @return the Role advertised by the Provider, or null if none found.
   */
  protected Role getRole(ServiceDescription serviceDescription) {
    // A serviceDescription may have multiple Classifications
    for (Iterator iterator = serviceDescription.getServiceClassifications().iterator(); 
	 iterator.hasNext();) {
      ServiceClassification serviceClassification = (ServiceClassification) iterator.next();
      // We have placed the Roles providers are registering in the CommercialServiceScheme
      if (serviceClassification.getClassificationSchemeName().equals(UDDIConstants.COMMERCIAL_SERVICE_SCHEME)) {
        Role role = Role.getRole(serviceClassification.getClassificationName());
        return role;
      }
    }
    return null;
  }

  /**
   * Get the local (self) Entity - from a local cache, or by querying the Blackboard.
   * @return the local (self) Entity
   */
  protected Entity getLocalEntity() {
    if (selfEntity == null) {
      Collection entities = getBlackboardService().query(new UnaryPredicate() {
	  public boolean execute(Object o) {
	    if (o instanceof Entity) {
	      return ((Entity)o).isLocal();
	    }
	    return false;
	  }
	});
    selfEntity = (Entity)entities.iterator().next();
    }
    return selfEntity;
  }

  //////////
  // Methods for responding to the ServiceDiscovery requester when the ServiceContract is fulfilled

  /**
   * For each ServiceContractRelay that changes and has a ServiceContract, find the
   * un-disposed FindProviders Task that requested a Provider with the given Role, and
   * dispose it as succesful.
   *<p>
   * Assumes there will be one un-disposed FindProviders Task per Role.
   */
  protected void updateFindProvidersTaskDispositions(Collection changedServiceContractRelays) {
    if (myFindProvidersTaskSubscription.isEmpty()) {
      // Nothing to update
      return;
    }
    
    // For each answered Relay
    for (Iterator relayIterator = changedServiceContractRelays.iterator(); relayIterator.hasNext();) {
      ServiceContractRelay relay = (ServiceContractRelay) relayIterator.next();

      // If it includes a Contract
      if (relay.getServiceContract() != null) {
        Role relayRole = relay.getServiceContract().getServiceRole();

	// Look through my FindProviders Tasks
        for (Iterator taskIterator = myFindProvidersTaskSubscription.iterator(); taskIterator.hasNext();) {
          Task findProvidersTask = (Task) taskIterator.next();

          Disposition disposition = (Disposition) findProvidersTask.getPlanElement();

	  // I'm looking for an incomplete one
          if (disposition == null) {
	    // Get the Role that this was looking for
            Role taskRole = getRole(findProvidersTask);

            // Assuming only 1 open (un-disposed) task per Role.
            if (taskRole.equals(relayRole)) {
	      // So - they asked for a Role for which we now have a Service Contract.
	      // Create a Disposition for the Task, with an EstimatedResult saying success

	      // The estAR is for the findProvidersTask, created by the planningFactory,
	      // with a confidence of 1.0, success=true
              AllocationResult estResult = PluginHelper.createEstimatedAllocationResult(findProvidersTask,
                                                                                        planningFactory, 
											1.0, true);
	      // The disposition should be the same Plan as the Task, for the findProvidersTask,
	      // and include the new success estimatedResult
              disposition = planningFactory.createDisposition(findProvidersTask.getPlan(), 
								findProvidersTask,
                                                                estResult);

              getBlackboardService().publishAdd(disposition);
            }
          }
        } // end of loop over FindProviders tasks
      }
    } // end of loop over changed ServiceContractRelays
  } // end of updateFindProvidersTaskDispositions

  ////////////////
  // Now the Unary Predicates for use with subscriptions.
  // Static Final cause we only need the one, unchanging instance.
  // Names in all caps since they're constants.

  private static final UnaryPredicate MYSERVICECONTRACTRELAYPREDICATE = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof ServiceContractRelay);
    }
  };

  private static final UnaryPredicate MYMMREQUESTPREDICATE = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof MMQueryRequest);
    }
  };

  /**
   * Predicate to get the FindProviders Tasks. In the off-chance that your verb is 
   * slightly different, you can over-ride this method to point to your
   * domain-specific Verb Constant.
   *<b>
   * Note that it is not a constant, just so it can be over-ridden.
   */ 
  protected UnaryPredicate getFindProvidersPredicate() {
    return new UnaryPredicate() {
	public boolean execute(Object o) {
	  if (o instanceof Task) {
	    return ((Task) o).getVerb().equals(Constants.Verb.FindProviders);
	  } else {
	    return false;
	  }
	}
      };
  }
}

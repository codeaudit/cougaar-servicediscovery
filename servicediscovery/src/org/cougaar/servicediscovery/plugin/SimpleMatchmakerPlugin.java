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
import java.util.Iterator;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.servicediscovery.util.UDDIConstants;
import org.cougaar.servicediscovery.description.MMRoleQuery;
import org.cougaar.servicediscovery.description.ScoredServiceDescriptionImpl;
import org.cougaar.servicediscovery.description.ServiceClassification;
import org.cougaar.servicediscovery.description.ServiceClassificationImpl;
import org.cougaar.servicediscovery.description.ServiceInfo;
import org.cougaar.servicediscovery.service.RegistryQueryService;
import org.cougaar.servicediscovery.transaction.MMQueryRequest;
import org.cougaar.servicediscovery.transaction.MMQueryRequestImpl;
import org.cougaar.servicediscovery.transaction.RegistryQuery;
import org.cougaar.servicediscovery.transaction.RegistryQueryImpl;
import org.cougaar.util.UnaryPredicate;

/**
 * The Matchmaker is responsible for taking service discovery requests (MMQueryRequests)
 * from the SDClient, and issuing asynchronous queries to the YP
 * to find matching providers. When one (or more) is found, send the scored
 * results back the SDClient on the MMQueryRequest.
 *<p>
 * This version assumes the Role requested will be in the CommercialServiceScheme, and allows the YP
 * to handle walking up YP communities as necessary. It does not handle quiescence,
 * is not guaranteed to work with kills/restarts (persistence), only works
 * with a distributed YP (using communities, not a single static instance), etc.
 *
 * @property org.cougaar.servicediscovery.plugin.SimpleMatchmakerQueryGracePeriod (in minutes, default is 2) specifies 
 * how long to wait before YP query errors should be logged at ERROR instead of DEBUG.
 */
public class SimpleMatchmakerPlugin extends ComponentPlugin {
  private static final String QUERY_GRACE_PERIOD_PROPERTY = 
                "org.cougaar.servicediscovery.plugin.SimpleMatchmakerQueryGracePeriod";
  private static final int DEFAULT_WARNING_SUPPRESSION_INTERVAL = 2; // minutes
  private static final int WARNING_SUPPRESSION_INTERVAL; // minutes

  static {
      WARNING_SUPPRESSION_INTERVAL = 
	Integer.getInteger(QUERY_GRACE_PERIOD_PROPERTY,
			   DEFAULT_WARNING_SUPPRESSION_INTERVAL).intValue();
  }

  private long myWarningCutoffTime = 0;

  private LoggingService myLoggingService;
  private RegistryQueryService myRegistryQueryService;
  private IncrementalSubscription myClientRequestSubscription;

  /** outstanding RQ are those which have been issued but have not yet returned */
  private ArrayList myOutstandingRQs = new ArrayList();

  /** pending RQs are returned RQ which haven't been consumed by the plugin yet */
  private ArrayList myPendingRQs = new ArrayList();

  /** Subscribe to MMQueryRequests from the SDClient */
  private UnaryPredicate myQueryRequestPredicate =
    new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof MMQueryRequest) {
          MMQueryRequest qr = (MMQueryRequest) o;
          return (qr.getQuery() instanceof MMRoleQuery);
        }
        return false;
      }
    };

  /** Reflection sets the RegistryQueryService at startup - plugin will not load if not found. */
  public void setRegistryQueryService(RegistryQueryService rqs) {
    myRegistryQueryService = rqs;
  }

  /**
   * Over-ride parent load to get non-essential services (the log service here).
   */
  public void load() {
    super.load();

    myLoggingService = (LoggingService)
      getServiceBroker().getService(this, LoggingService.class, null);
    if (myLoggingService == null) {
      myLoggingService = LoggingService.NULL;
    }
  }

  /**
   * Every load() should have an unload(), to unload() the manually loaded service.
   */
  public void unload() {
    if (myRegistryQueryService != null) {
      getServiceBroker().releaseService(this,
					RegistryQueryService.class,
					myRegistryQueryService);
      myRegistryQueryService = null;
    }

    if ((myLoggingService != null) && 
	(myLoggingService != LoggingService.NULL)) {
      getServiceBroker().releaseService(this, 
					LoggingService.class,
					myLoggingService);
      myLoggingService = null;
    }

    super.unload();
  }

  /**
   * Subscribe to MMQueryRequests from the SDClientPlugin.
   */
  protected void setupSubscriptions() {
    myClientRequestSubscription = 
      (IncrementalSubscription) getBlackboardService().subscribe(myQueryRequestPredicate);
  }

  /**
   * Loop through new MMQueryRequests, posting a new asynchronous YP query for each. Also
   * runs when the YP query callbacks signal that the plugin should run. So loop through the pending
   * requests, posting alarms to retry those with errors later, and sending back the answer
   * to the SDClient for those that have a list of providers.
   *<p>
   * Note that you must specify the service scheme in which Roles will be found in the YP, 
   * through the helper getServiceSchemeForRoles() method.
   */
  protected void execute() {
    // Look at all the new MMQueryRequests from the SDClientPlugin
    if (myClientRequestSubscription.hasChanged()) {
      for (Iterator i = myClientRequestSubscription.getAddedCollection().iterator();
	   i.hasNext();) {
        MMQueryRequest queryRequest =  (MMQueryRequest) i.next();
        MMRoleQuery query = (MMRoleQuery) queryRequest.getQuery();
        RegistryQuery rq = new RegistryQueryImpl();
	RQ r;

        // Find all service providers for specifed Role (as code and name) under the given service scheme.
        ServiceClassification roleSC =
	  new ServiceClassificationImpl(query.getRole().toString(),
					query.getRole().toString(),
					getServiceSchemeForRoles());
        rq.addServiceClassification(roleSC);
	if (myLoggingService.isDebugEnabled())
	  myLoggingService.debug("RegistryQuery = " +rq + " " +
				 rq.getServiceClassifications());
	r = new RQ(queryRequest, query, rq);

	// Put the new query in a queue, issuing an asynchronous request with a callback
        postRQ(r);
      }
    } // end of block for new requests from the SDClientPlugin

    // Now handle any callbacks that have come in. When they come in, they are put
    // on the pending Queue, and then the Blackboard is signaled to run this plugin. That is 
    // when we get here.
    RQ r;
    while ((r = getPendingRQ()) != null) {
      if (r.exception == null) {
	// This means it succeeded -- send the found providers back to the SDClient
	handleResponse(r);
      } else {
	// There was an exception. Log and retry later (by setting an alarm)
	handleException(r);
      }
    } // end of loop over pending Q, which should now be empty

    // If at this point we have nothing on the post queue (new queries we just sent) 
    // nor outstanding alarms (queries that failed and we're retrying later), then
    // we are done (until the next SDClient query come in).
  } // end of execute()

  /**
   * Return the UDDI Service Scheme that contains the Roles we will look for. 
   * This is typically the COMMERCIAL_SERVICE_SCHEME or MILITARY_SERVICE_SCHEME. 
   * <p>
   * This method may be the only one an extender of this plugin needs to over-ride.
   * @return UDDI Service Scheme to find Roles in
   */
  protected String getServiceSchemeForRoles() {
    return UDDIConstants.COMMERCIAL_SERVICE_SCHEME;
  }

  /**
   * Handle an exception from a YP query callback by logging something, waiting a while, and trying again.
   * @param r YP query that had error.
   */
  protected void handleException(RQ r) {
    retryErrorLog(r, ": Exception querying YP registry for " +
      r.query.getRole().toString() +
      ", will try again later.", r.exception);
    r.exception = null;
  }

  /**
   * Handle an error with a YP lookup request by logging and retrying later.
   * @param r YP query that had error.
   * @param message Pretty message to print to explain the error
   */ 
  private void retryErrorLog(RQ r, String message) {
    retryErrorLog(r, message, null);
  }

  /**
   * When an error occurs, but we'll be retrying later, treat it as a DEBUG
   * at first. After a while it becomes an error.
   * @param r YP query that had error.
   * @param message Pretty message to print to explain the error
   * @param e The exception that caused the problem
   */
  private void retryErrorLog(RQ r, String message, Throwable e) {
    // This needs to be random to avoid them all firing at once... FIXME!!!
    int rand = (int)(Math.random()*10000) + 1000;
    QueryAlarm alarm =
      new QueryAlarm(r, getAlarmService().currentTimeMillis() + rand);
    getAlarmService().addAlarm(alarm);

    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug("retryErrorLog - adding a QueryAlarm for " + r.query.getRole() + 
		", alarm: " + alarm);
    }

    // If we've waited to allow startup errors and now still getting errors,
    // then log these as errors. Until then, log them as DEBUG.
    if(System.currentTimeMillis() > getWarningCutoffTime()) {
      if (e == null)
	myLoggingService.error(message);
      else
	myLoggingService.error(message, e);
    } else if (myLoggingService.isDebugEnabled()) {
      if (e == null)
	myLoggingService.debug(message);
      else
	myLoggingService.debug(message, e);
    }
  }

  /**
   * Process a real response from the YP for a lookup. Score it using the
   * scoring function on the original query. If it scores well,
   * then put this service provider on the response back to the SDClient. 
   * If we don't have a good provider to send the SDClient, then check to 
   * see if the YP said there was another higher (parent) YP server to check. 
   * If there is, then post the query to that server. Otherwise, we've failed.
   */
  protected void handleResponse(RQ r) {
    MMQueryRequest queryRequest = r.queryRequest;
    MMRoleQuery query = r.query;

    Collection services = r.services;
    
    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug("handleResponse - registry query result size is : " + 
			     services.size() + " for query: " + 
			     query.getRole().toString());
    }

    ArrayList scoredServiceDescriptions = new ArrayList();
    for (Iterator iter = services.iterator(); iter.hasNext(); ) {
      ServiceInfo serviceInfo = (ServiceInfo) iter.next();

      // This is where the custom ServiceInfoScorer gets used. We match the service
      // provider returned by the YP against our rules, to decide if it will do.
      // For the pizza app, that is the RoleWithBlacklistScorer.
      // We insist on a positive score.
      int score = query.getServiceInfoScorer().scoreServiceInfo(serviceInfo);

      if (score >= 0) {
	scoredServiceDescriptions.add(new ScoredServiceDescriptionImpl(score,
								       serviceInfo));
	if(myLoggingService.isDebugEnabled()) {
	  myLoggingService.debug(": execute: adding Provider name - " + 
				 serviceInfo.getProviderName() +
				 " Service name: " + 
				 serviceInfo.getServiceName() +
				 " Service score: " + score);
	}
      } else {
	// Negative score means provider didn't pass. We'll keep looking.
	if (myLoggingService.isDebugEnabled()) {
	  myLoggingService.debug(": execute: ignoring Provider name - " + 
				 serviceInfo.getProviderName() +
				 " Service name: " + 
				 serviceInfo.getServiceName() +
				 " Service score: " + score);
	}
      }
    } // end of loop over services found in the YP on last look-up

    // If we didn't find any services that scored well
    if (scoredServiceDescriptions.isEmpty()) {
      // Is there another higher-level YP community?
      if (!r.nextContextFailed) {
	// We just didn't find any yet! Re-post, so we recurse up to the next higher
	// "context", or YP Community. (in our example, from Cambridge to MA)
	  if (myLoggingService.isDebugEnabled()) {
	    myLoggingService.debug(":execute - no matching provider for " + 
				   query.getRole() +
				   " in " + r.currentYPContext +
				   " retrying in next context.");
	  }
	  // post the query to the next YP server
	  postRQ(r);
      } else {
	// Couldn't find another YPServer to search
	retryErrorLog(r, 
		      ": unable to find provider for " + 
		      query.getRole() +
		      ", publishing empty query result. " +
		      "Will try query again later.");

      }
    } else {
      // We have some results. Sort them by score, so the client gets the best one.
      Collections.sort(scoredServiceDescriptions);
    }

    // Set our results on the request from the client, and publish change it to send it back
    ((MMQueryRequestImpl) queryRequest).setResult(scoredServiceDescriptions);
    ((MMQueryRequestImpl) queryRequest).setQueryCount(queryRequest.getQueryCount() + 1);
    getBlackboardService().publishChange(queryRequest);

    if(myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(": publishChanged query");
    }
  }

  /**
   * Get the real time after which DEBUG level problems become ERROR: This is
   * the parametrized warning cut-off interval (in minutes, default of 2), plus
   * the time of the first error.
   * @return time in millis
   */
  protected long getWarningCutoffTime() {
    if (myWarningCutoffTime == 0) {
      myWarningCutoffTime = System.currentTimeMillis() + 
	WARNING_SUPPRESSION_INTERVAL*60000;
    }

    return myWarningCutoffTime;
  }
  
  /**
   * Inner class representing status of a query to send to the YP.
   */
  private class RQ {
    MMQueryRequest queryRequest; // object sent from SDClient
    MMRoleQuery query; // contained in above
    RegistryQuery rq; // The actual query to send to the YP

    Collection services; // services found in YP
    Exception exception; // exception, if any, from last YP query
    boolean complete = false;
    Object previousYPContext = null; // last YP server searched
    Object currentYPContext = null; // next YP server to search
    boolean nextContextFailed = false; // is there a next YP server/


    RQ(MMQueryRequest queryRequest, MMRoleQuery query, RegistryQuery rq) {
      this.queryRequest = queryRequest;
      this.query = query;
      this.rq = rq;
    }
  }

  /** Issue an asynchronous request to the YP, noting the outstanding query. */
  private void postRQ(final RQ r) {
    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(": postRQ " + r + 
			     " (" + r.rq + ")" );
    }

    // The list of outstanding requests gets touched by the callback (YP) thread
    // and the plugin thread, so synchronize to avoid concurrent-mod exceptions.
    synchronized (myOutstandingRQs) {
      myOutstandingRQs.add(r);
    }

    // MatchmakerStubPlugin has another alternative -- where there is a single
    // fixed YP server
    findServiceWithDistributedYP(r);
  }

  /** Note an asynchronous response from the YP, and wake the plugin to handle it in the plugin thread. */
  private void pendRQ(RQ r) {
    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(": pendRQ " + r + " (" + r.rq + ")");
    }

    // The current request finished (for good or ill)
    r.complete = true;

    // Again, since these get touched in 2 threads, syncrhonize
    synchronized (myOutstandingRQs) {
      myOutstandingRQs.remove(r);
    }
    synchronized (myPendingRQs) {
      myPendingRQs.add(r);
    }
    // tell the plugin to wake up, so the execute() method gets called
    getBlackboardService().signalClientActivity();
  }

  /** Pop a pending RQ of the list (or null) so that we can deal with it. */
  private RQ getPendingRQ() {
    RQ r = null;
    synchronized (myPendingRQs) {
      if (!myPendingRQs.isEmpty()) {
        r = (RQ) myPendingRQs.remove(0); // treat like a fifo
        if (myLoggingService.isDebugEnabled()) {
          myLoggingService.debug(": getPendingRQ " + r + " (" + r.rq + ")");
        }
      }
    }
    return r;
  }

  /** 
   * This is the workhourse: ask the YP to find a service that matches the given request,
   * and to tell us when it is done with a callback we supply. It asks the YP to 
   * walk up the hierarchy of YP communities to find ever broader YP servers if that is necessary.
   */
  private void findServiceWithDistributedYP(final RQ r) {
    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(": findServiceWithDistributedYP - " +
			     " using YPCommunity search.");
    }
    
    // This is the main method. It issues the query to the YP server, 
    // through the RegistryQueryService, which is provided by the 
    // o.c.sd.service.UDDI4JRegistrationQueryServiceComponent.
    // Here, we tell the YP to start looking in the currentYPContext (YP server),
    // satisfying the given query, and to let us know using the given callback.
    myRegistryQueryService.findServiceAndBinding(r.currentYPContext, 
						 r.rq,
						 new RegistryQueryService.CallbackWithContext() {
      public void invoke(Object result) {
	// Take the given services results for our query
	r.services = (Collection) result;
	if (myLoggingService.isDebugEnabled()) {
	  myLoggingService.debug(": results = " + result + 
				 " for " + r.currentYPContext);
	}
	// And update the queues
	flush();
      }
						     
      public void handle(Exception e) {
	// Got some sort of error trying to do the YP lookup
	r.exception = e;
	if (myLoggingService.isDebugEnabled()) {
	  myLoggingService.debug(": failed during query of " +
				 r.queryRequest + 
				 " context =  " + r.currentYPContext, e);
	}
	// And update the queues
	flush();
      }
      
      public void setNextContext(Object context){
	// If one YP community / context doesn't have a matching provider,
	// we move up to the next one. IE, when Cambridge only
	// contains a blacklisted provider (Joes), we move up to MA.
	if (myLoggingService.isDebugEnabled())
	  myLoggingService.debug(": previous YPContext " +
				 r.currentYPContext + 
				 " current YPContext " + context);

	r.previousYPContext = r.currentYPContext;
	r.currentYPContext = context;
	
	// If there is no next context, then we're out of YP servers!
	if (context == null) {
	  r.nextContextFailed = true;
	}
      }
      
      private void flush() {
	// Put the request on the pending list for handling the next time the plugin runs
	pendRQ(r);
      }
    });
  } // end of findServiceWithDistributedYP

  /** Alarm to post a query to the YP after some time. */
  public class QueryAlarm implements Alarm {
    private long expiresAt; // when the alarm will fire
    private boolean expired = false; // has the alarm fired
    private RQ rq = null;

    public QueryAlarm (RQ rq, long expirationTime) {
      expiresAt = expirationTime;
      this.rq = rq;
    }
    public long getExpirationTime() { return expiresAt; }
    
    /** When the alarm fires, post the query to the YP. */
    public synchronized void expire() {
      if (!expired) {
        expired = true;
	rq.complete = false; // mark the query as in-progress
	postRQ(rq);
      }
    }

    public boolean hasExpired() { return expired; }
    public synchronized boolean cancel() {
      boolean was = expired;
      expired = true;
      return was;
    }
    public String toString() {
      return "<QueryAlarm " + expiresAt +
        (expired ? "(Expired) " : " ") +
	rq.query.getRole() + " " +
        "for MatchmakerPlugin at " + getAgentIdentifier() + ">";
    }
  } // end of QueryAlarm definition
}

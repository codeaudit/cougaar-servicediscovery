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
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.QuiescenceReportService;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.servicediscovery.Constants;
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
import org.cougaar.servicediscovery.util.UDDIConstants;
import org.cougaar.util.UnaryPredicate;

/**
 *
 * Query the YellowPages for possible service providers
 *
 */
public class MatchmakerStubPlugin extends SimplePlugin {
  private static int WARNING_SUPPRESSION_INTERVAL = 2;
  private long warningCutoffTime = 0;
  private static final String QUERY_GRACE_PERIOD_PROPERTY = 
                "org.cougaar.servicediscovery.plugin.QueryGracePeriod";


  private boolean usingCommunities;
  private String agentName;
  private LoggingService log;
  private RegistryQueryService registryQueryService;
  private QuiescenceReportService qrs;
  private AgentIdentificationService ais;
  private IncrementalSubscription clientRequestSub;

  // outstanding RQ are those which have been issued but have not yet returned
  private ArrayList outstandingRQs = new ArrayList();
  // pending RQs are returned RQ which haven't been consumed by the plugin yet
  private ArrayList pendingRQs = new ArrayList();

  private int outstandingAlarms = 0; // Outstanding alarms (any means non-quiescent)

  private UnaryPredicate queryRequestPredicate =
    new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof MMQueryRequest) {
          MMQueryRequest qr = (MMQueryRequest) o;
          return (qr.getQuery() instanceof MMRoleQuery);
        }
        return false;
      }
    };

  public void load() {
    super.load();

    this.log = (LoggingService)
      getBindingSite().getServiceBroker().getService(this, LoggingService.class, null);
    if (log == null) {
      log = LoggingService.NULL;
    }

    this.registryQueryService = (RegistryQueryService)
      getBindingSite().getServiceBroker().getService(this,
                                                     RegistryQueryService.class,
                                                     null);

    // Set up the QuiescenceReportService so that while waiting for the YP and alarms we
    // dont go quiescent by mistake
    this.ais = (AgentIdentificationService) getBindingSite().getServiceBroker().getService(this, AgentIdentificationService.class, null);
    this.qrs = (QuiescenceReportService) getBindingSite().getServiceBroker().getService(this, QuiescenceReportService.class, null);

    if (qrs != null)
      qrs.setAgentIdentificationService(ais);

    if (registryQueryService == null)
      throw new RuntimeException("Unable to obtain RegistryQuery service");

  }

  public void unload() {
    if (registryQueryService != null) {
      getBindingSite().getServiceBroker().releaseService(this,
                                                         RegistryQueryService.class,
                                                         registryQueryService);
      registryQueryService = null;
    }

    if (qrs != null) {
      getBindingSite().getServiceBroker().releaseService(this,
                                                         QuiescenceReportService.class,
                                                         qrs);
      qrs = null;
    }

    if (ais != null) {
      getBindingSite().getServiceBroker().releaseService(this,
                                                         AgentIdentificationService.class,
                                                         ais);
      ais = null;
    }

    if ((log != null) && (log != LoggingService.NULL)) {
      getBindingSite().getServiceBroker().releaseService(this, LoggingService.class, log);
      log = null;
    }
    super.unload();
  }

  protected void setupSubscriptions() {
    agentName = getBindingSite().getAgentIdentifier().toString();

    clientRequestSub = (IncrementalSubscription) subscribe(queryRequestPredicate);
    Collection params = getDelegate().getParameters();
    if (params.size() > 0) {
      usingCommunities =
	Boolean.valueOf((String) params.iterator().next()).booleanValue();
    } else {
      usingCommunities = false;
    }
  }

 
  protected void execute() {
    if (clientRequestSub.hasChanged()) {
      Collection newRequest = clientRequestSub.getAddedCollection();
      for (Iterator i = newRequest.iterator(); i.hasNext();) {
        MMQueryRequest queryRequest =  (MMQueryRequest) i.next();
        MMRoleQuery query = (MMRoleQuery) queryRequest.getQuery();
        RegistryQuery rq = new RegistryQueryImpl();

        // Find all service providers for specifed Role
        ServiceClassification roleSC =
	  new ServiceClassificationImpl(query.getRole().toString(),
					query.getRole().toString(),
					UDDIConstants.MILITARY_SERVICE_SCHEME);
        rq.addServiceClassification(roleSC);

        RQ r = new RQ(queryRequest, query, rq);
        postRQ(r);
      }
    }

    RQ r;
    while ( (r = getPendingRQ()) != null) {
      if (r.exception != null) {
	handleException(r);
      } else {
	handleResponse(r);
      }
    }

    // Whenever we submit a query to the YP we go off into the ether
    // So if there are outstanding YP queries or alarms, then mark the fact that we are not done yet
    // so that Quiescence stuff doesnt decide we're done prematurely early
    // Note that pendingRQs should _always_ be empty at this point. And we'll only have oustandingAlarms
    // if there were exceptions talking to the YP.
    if (qrs != null) {
      if (outstandingRQs.isEmpty() && pendingRQs.isEmpty() && outstandingAlarms == 0) {
	// Nothing on the lists and no outstanding alarmas - so we're done
	qrs.setQuiescentState();
	if (log.isInfoEnabled())
	  log.info(agentName + " finished all YP queries. Now quiescent.");
      } else {
	// Some query waiting for an answer, or waiting for this Plugin to handle it
	// Or waiting to retry a query
	// We're not done
	qrs.clearQuiescentState();
	if (log.isInfoEnabled())
	  log.info(agentName + " has outstanding YP queries or answers. Not quiescent.");
	if (log.isDebugEnabled())
	  log.debug("            YP questions outstanding: " + outstandingRQs.size() + ". YP answers to process: " + pendingRQs.size() + ". Outstanding alarms: " + outstandingAlarms);
      }
    }
  }

  protected void handleException(RQ r) {
    retryErrorLog(r, getAgentIdentifier() +
      " Exception querying registry for " +
      r.query.getRole().toString() +
      ", try again later.", r.exception);
    r.exception = null;
  }

  private void retryErrorLog(RQ r, String message) {
    retryErrorLog(r, message, null);
  }

  // When an error occurs, but we'll be retrying later, treat it as a DEBUG
  // at first. After a while it becomes an error.
  private void retryErrorLog(RQ r, String message, Throwable e) {
    int rand = (int)(Math.random()*10000) + 1000;
    QueryAlarm alarm =
      new QueryAlarm(r, getAlarmService().currentTimeMillis() + rand);
    getAlarmService().addAlarm(alarm);
    // Alarms silently make us non-quiescent -- so keep track of when we have any
    outstandingAlarms++;

    if (log.isDebugEnabled()) {
      log.debug(getAgentIdentifier() + 
		" adding a QueryAlarm for r.query.getRole()" + 
		" alarm - " + alarm);
    }

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

  protected void handleResponse(RQ r) {
    MMQueryRequest queryRequest = r.queryRequest;
    MMRoleQuery query = r.query;
    Collection services = r.services;

    if (log.isDebugEnabled()) {
      log.debug(agentName + " registry query result size is : " + services.size() + " for query: " + query.getRole().toString());
    }

    /*
    String echelon = query.getEchelon();
    if ((query.getEchelon() == null) ||
	(query.getEchelon().equals(""))) {
      echelon = getRequestedEchelonOfSupport(query.getRole());
    }

    if (log.isDebugEnabled()) {
      log.debug(getAgentIdentifier() + " looking for " +
		query.getRole() + " at " + echelon + " level");
    }
    */

    ArrayList scoredServiceDescriptions = new ArrayList();
    for (Iterator iter = services.iterator(); iter.hasNext(); ) {
      ServiceInfo serviceInfo = (ServiceInfo) iter.next();
      int score = query.getServiceInfoScorer().scoreServiceInfo(serviceInfo);

      if (score >= 0) {
	scoredServiceDescriptions.add(new ScoredServiceDescriptionImpl(score,
								       serviceInfo));
	if(log.isDebugEnabled()) {
	  log.debug(agentName + ":execute: adding Provider name: " + serviceInfo.getProviderName() +
		    " Service name: " + serviceInfo.getServiceName() +
		    " Service score: " + score);
	}
      } else {
	if (log.isDebugEnabled()) {
	  log.debug(agentName + ":execute: ignoring Provider name: " + serviceInfo.getProviderName() +
		      " Service name: " + serviceInfo.getServiceName() +
		      " Service score: " + score);
	}
      }
    }

    if (scoredServiceDescriptions.isEmpty()) {
      if ((usingCommunities) &&
	  (!r.getNextContextFailed)) {
	  if (log.isDebugEnabled()) {
	    log.debug(agentName + " no matching provider for " + query.getRole() +
		      " in " + r.currentYPContext +
		      " retrying in next context.");
	  }
	  postRQ(r);
      } else {
	// Couldn't find another YPServer to search
	retryErrorLog(r, 
		      agentName + " unable to find provider for " + 
		      query.getRole() +
		      ", publishing empty query result. " +
		      "Will try query again later.");

      }
    } else {
      Collections.sort(scoredServiceDescriptions);
    }


    ((MMQueryRequestImpl) queryRequest).setResult(scoredServiceDescriptions);
    ((MMQueryRequestImpl) queryRequest).setQueryCount(queryRequest.getQueryCount() + 1);
    getBlackboardService().publishChange(queryRequest);

    if(log.isDebugEnabled()) {
      log.debug(agentName + ": publishChanged query");
    }
  }

  private long getWarningCutOffTime() {
    if (warningCutoffTime == 0) {
      WARNING_SUPPRESSION_INTERVAL = Integer.getInteger(QUERY_GRACE_PERIOD_PROPERTY,
							WARNING_SUPPRESSION_INTERVAL).intValue();
      warningCutoffTime = System.currentTimeMillis() + WARNING_SUPPRESSION_INTERVAL*60000;
    }

    return warningCutoffTime;
  }

  private class RQ {
    MMQueryRequest queryRequest;
    MMRoleQuery query;
    RegistryQuery rq;
    Collection services;
    Exception exception;
    boolean complete = false;
    Object previousYPContext = null;
    Object currentYPContext = null;
    boolean getNextContextFailed = false;

    RQ(MMQueryRequest queryRequest, MMRoleQuery query, RegistryQuery rq) {
      this.queryRequest = queryRequest;
      this.query = query;
      this.rq = rq;
    }
  }

  // issue a async request
  private void postRQ(final RQ r) {
    if (log.isDebugEnabled()) {
      log.debug(getAgentIdentifier() + ": posting "+r+" ("+r.rq+")");
    }
    synchronized (outstandingRQs) {
      outstandingRQs.add(r);
    }

    if (usingCommunities) {
      registryQueryService.findServiceAndBinding(r.currentYPContext, r.rq,
						 new RegistryQueryService.CallbackWithContext() {
	public void invoke(Object result) {
	  r.services = (Collection) result;
	  if (log.isDebugEnabled()) {
	    log.debug(getAgentIdentifier() + " results = " + result + 
		      " for " + r.currentYPContext);
	  }
	  flush();
	}
	public void handle(Exception e) {
	  r.exception = e;
	  if (log.isDebugEnabled()) {
	    log.debug(getAgentIdentifier() + " failed during query of " +
		    r.queryRequest + " context =  " + r.currentYPContext, e);
	  }
	  flush();
	}

	public void setNextContext(Object context){
	  if (log.isDebugEnabled()) {
	    log.debug(getAgentIdentifier() + " previous YPContext " +
		      r.currentYPContext + " current YPContext " + context);
	  }
	  r.previousYPContext = r.currentYPContext;
	  r.currentYPContext = context;
	  
	  if (context == null) {
	    r.getNextContextFailed = true;
	  }
	}

	private void flush() {
	  pendRQ(r);
	}
      });
    } else {
      registryQueryService.findServiceAndBinding(r.rq,
						 new RegistryQueryService.Callback() {
	public void invoke(Object result) {
	  r.services = (Collection) result;
	  flush();
	}
	public void handle(Exception e) {
	  r.exception = e;
	  //log.error("Failed during query of "+r.queryRequest, e);
	  flush();
	}

	private void flush() {
          pendRQ(r);
	}
      });
    }
  }

  // note an async response and wake the plugin
  private void pendRQ(RQ r) {
    if (log.isDebugEnabled()) {
      log.debug(getAgentIdentifier() + " pending "+r+" ("+r.rq+")");
    }
    r.complete = true;
    synchronized (outstandingRQs) {
      outstandingRQs.remove(r);
    }
    synchronized (pendingRQs) {
      pendingRQs.add(r);
    }
    wake();                     // tell the plugin to wake up
  }

  // get a pending RQ (or null) so that we can deal with it
  private RQ getPendingRQ() {
    RQ r = null;
    synchronized (pendingRQs) {
      if (!pendingRQs.isEmpty()) {
        r = (RQ) pendingRQs.remove(0); // treat like a fifo
        if (log.isDebugEnabled()) {
          log.debug(getAgentIdentifier() + " retrieving "+r+" ("+r.rq+")");
        }
      }
    }
    return r;
  }

  public class QueryAlarm implements Alarm {
    private long expiresAt;
    private boolean expired = false;
    private RQ rq = null;

    public QueryAlarm (RQ rq, long expirationTime) {
      expiresAt = expirationTime;
      this.rq = rq;
    }
    public long getExpirationTime() { return expiresAt; }
    public synchronized void expire() {
      if (!expired) {
        expired = true;
	rq.complete = false;
	postRQ(rq);
	--outstandingAlarms;
      }
    }

    public boolean hasExpired() { return expired; }
    public synchronized boolean cancel() {
      boolean was = expired;
      expired = true;
      --outstandingAlarms;
      return was;
    }
    public String toString() {
      return "<QueryAlarm " + expiresAt +
        (expired ? "(Expired) " : " ") +
	rq.query.getRole() + " " +
        "for MatchmakerStubPlugin at " + getAgentIdentifier() + ">";
    }
  }
}














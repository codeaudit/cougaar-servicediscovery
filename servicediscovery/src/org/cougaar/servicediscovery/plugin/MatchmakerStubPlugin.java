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
import org.cougaar.servicediscovery.description.LineageList;
import org.cougaar.servicediscovery.description.LineageListWrapper;
import org.cougaar.servicediscovery.description.MMRoleQuery;
import org.cougaar.servicediscovery.description.ProviderCapabilities;
import org.cougaar.servicediscovery.description.ProviderCapability;
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
public final class MatchmakerStubPlugin extends SimplePlugin {
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
  private IncrementalSubscription lineageListSub;

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

  private UnaryPredicate lineageListPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return ((o instanceof LineageListWrapper) &&
	      (((LineageListWrapper) o).getType() == LineageList.COMMAND));
    }
  };


  private UnaryPredicate providerCapabilitiesPredicate = 
  new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof ProviderCapabilities) {
	ProviderCapabilities providerCapabilities = (ProviderCapabilities) o;
	return (providerCapabilities.getProviderName().equals(agentName));
      } else {
	return false;
      }
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
    lineageListSub = (IncrementalSubscription) subscribe(lineageListPredicate);

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

  protected float scoreServiceProvider(ServiceInfo serviceInfo,
				       String requestedEchelonOfSupport) {
    int echelonScore = getEchelonScore(serviceInfo, requestedEchelonOfSupport);

    if (log.isDebugEnabled()) {
      log.debug("scoreServiceProvider: echelon score " + echelonScore);
    }
    if (echelonScore < 0) {
      return -1;
    }

    int lineageScore = getLineageScore(serviceInfo);
    if (log.isDebugEnabled()) {
      log.debug("scoreServiceProvider: lineage score " + lineageScore);
    }
    if (lineageScore < 0) {
      return -1;
    } else {
      lineageScore = 100 * lineageScore;
    }

    return echelonScore + lineageScore;
  }

  protected int getEchelonScore(ServiceInfo serviceInfo,
				String requestedEchelonOfSupport) {
    int requestedEchelonOrder =
      Constants.MilitaryEchelon.echelonOrder(requestedEchelonOfSupport);

    if (requestedEchelonOrder == -1) {
      if (log.isWarnEnabled())
	log.warn(getAgentIdentifier() + " getEchelonScore() - invalid echelon " + requestedEchelonOfSupport);
      return 0;
    }

    int serviceEchelonOrder = -1;

    for (Iterator iterator = serviceInfo.getServiceClassifications().iterator();
	 iterator.hasNext();) {
      ServiceClassification classification =
	(ServiceClassification) iterator.next();
      if (classification.getClassificationSchemeName().equals(UDDIConstants.MILITARY_ECHELON_SCHEME)) {

	String serviceEchelon = classification.getClassificationCode();
	serviceEchelonOrder =
	  Constants.MilitaryEchelon.echelonOrder(serviceEchelon);
	break;
      }
    }

    if (serviceEchelonOrder == -1) {
      if (log.isInfoEnabled()) {
	log.info(agentName + ": Ignoring service with a bad echelon of support: " +
		  serviceEchelonOrder + " for provider: " + serviceInfo.getProviderName());
      }
      return -1;
    } if (serviceEchelonOrder < requestedEchelonOrder) {
      if (log.isInfoEnabled()) {
	log.info(agentName + ": Ignoring service with a lower echelon of support: " +
		  serviceEchelonOrder + " for provider: " + serviceInfo.getProviderName());
      }
      return -1;
    } else {
      return (serviceEchelonOrder - requestedEchelonOrder);
    }
  }

  protected int getLineageScore(ServiceInfo serviceInfo) {
    LineageListWrapper commandLineageWrapper = null;

    for (Iterator iterator = lineageListSub.iterator();
	 iterator.hasNext();) {
      commandLineageWrapper = (LineageListWrapper) iterator.next();
    }

    if (commandLineageWrapper == null) {
      if (log.isWarnEnabled()) {
        log.warn(agentName + ": in getLineageScore, has no command lineage");
      }
      return -1;
    }

    //if there are multiple SCAs, return the minimum distance
    //among them
    int minHops = Integer.MAX_VALUE;

    for (Iterator iterator = serviceInfo.getServiceClassifications().iterator();
	 iterator.hasNext();) {
      ServiceClassification classification =
	(ServiceClassification) iterator.next();
      if (classification.getClassificationSchemeName().equals(UDDIConstants.SUPPORT_COMMAND_ASSIGNMENT)) {
	int hops = 
	  commandLineageWrapper.countHops(agentName,
					  classification.getClassificationName());
	if (hops != -1) {
	  minHops = Math.min(minHops, hops);
	}
      }
    }

    if(minHops == Integer.MAX_VALUE) {
      if (log.isInfoEnabled()) {
        log.info(agentName + ": in getLineageScore, does not intersect with provider's lineage "+
                 " for provider " + serviceInfo.getProviderName());
      }
      return -1;
    }
    else
      return minHops;
  }

  protected String getRequestedEchelonOfSupport(Role role) {
    Collection pcCollection = query(providerCapabilitiesPredicate);
    int providedEchelonIndex = -1;

    for (Iterator iterator = pcCollection.iterator();
	 iterator.hasNext();) {
      ProviderCapabilities capabilities = 
	(ProviderCapabilities) iterator.next();

      ProviderCapability providerCapability = 
	  capabilities.getCapability(role);
      
      if (providerCapability != null) {
	providedEchelonIndex = 
	  Constants.MilitaryEchelon.echelonOrder(providerCapability.getEchelon());
	break;
      }
    }
    
    return Constants.MilitaryEchelon.ECHELON_ORDER[providedEchelonIndex + 1];
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

    String echelon = query.getEchelon();
    if ((query.getEchelon() == null) ||
	(query.getEchelon().equals(""))) {
      echelon = getRequestedEchelonOfSupport(query.getRole());
    }

    if (log.isDebugEnabled()) {
      log.debug(getAgentIdentifier() + " looking for " +
		query.getRole() + " at " + echelon + " level");
    }

    ArrayList scoredServiceDescriptions = new ArrayList();
    for (Iterator iter = services.iterator(); iter.hasNext(); ) {
      ServiceInfo serviceInfo = (ServiceInfo) iter.next();
      
      if (!query.getPredicate().execute(serviceInfo)) {
	if (log.isDebugEnabled()) {
	  log.debug(agentName + ":execute: query predicate rejected " +
		    " Provider name:" + serviceInfo.getProviderName() +
		    " for Service name: " + serviceInfo.getServiceName());
	}
	continue;
      }
	
      float score = scoreServiceProvider(serviceInfo, echelon);

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
	((MMQueryRequestImpl) queryRequest).setResult(scoredServiceDescriptions);
	getBlackboardService().publishChange(queryRequest);
	if(log.isDebugEnabled()) {
	  log.debug(agentName + ": publishChanged query");
	}
      }
    } else {
      Collections.sort(scoredServiceDescriptions);
      ((MMQueryRequestImpl) queryRequest).setResult(scoredServiceDescriptions);
      getBlackboardService().publishChange(queryRequest);
      if(log.isDebugEnabled()) {
	log.debug(agentName + ": publishChanged query");
      }
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














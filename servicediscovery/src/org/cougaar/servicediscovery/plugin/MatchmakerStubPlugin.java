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
import java.util.Iterator;
import java.util.List;


import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.QuiescenceReportService;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.servicediscovery.Constants;
import org.cougaar.servicediscovery.description.Lineage;
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
import org.cougaar.util.TimeSpan;
import org.cougaar.util.UnaryPredicate;

/**
 *
 * Query the YellowPages for possible service providers
 *
 */
public class MatchmakerStubPlugin extends SimplePlugin {
  private static int WARNING_SUPPRESSION_INTERVAL = 2;
  private long myWarningCutoffTime = 0;
  private static final String QUERY_GRACE_PERIOD_PROPERTY = 
                "org.cougaar.servicediscovery.plugin.QueryGracePeriod";

  private boolean myDistributedYPServers;
  private String myAgentName;
  private LoggingService myLoggingService;
  private RegistryQueryService myRegistryQueryService;
  private QuiescenceReportService myQuiescenceReportService;
  private AgentIdentificationService myAgentIdentificationService;
  private IncrementalSubscription myClientRequestSubscription;
  private IncrementalSubscription myLineageSubscription;

  // outstanding RQ are those which have been issued but have not yet returned
  private ArrayList myOutstandingRQs = new ArrayList();
  // pending RQs are returned RQ which haven't been consumed by the plugin yet
  private ArrayList myPendingRQs = new ArrayList();

  // Outstanding alarms (any means non-quiescent)
  private int myOutstandingAlarms = 0; 


  private UnaryPredicate myLineagePredicate =
    new UnaryPredicate() {
      public boolean execute(Object o) {
        return (o instanceof Lineage);
      }
  };

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

  public void load() {
    super.load();

    myLoggingService = (LoggingService)
      getBindingSite().getServiceBroker().getService(this, 
						     LoggingService.class, 
						     null);
    if (myLoggingService == null) {
      myLoggingService = LoggingService.NULL;
    }

    myRegistryQueryService = (RegistryQueryService)
      getBindingSite().getServiceBroker().getService(this,
                                                     RegistryQueryService.class,
                                                     null);
    myAgentIdentificationService = 
      (AgentIdentificationService) getBindingSite().getServiceBroker().getService(this, AgentIdentificationService.class, null);

    // Set up the QuiescenceReportService so that while waiting for the YP and
    // alarms we don't go quiescent by mistake
    myQuiescenceReportService = 
      (QuiescenceReportService) getBindingSite().getServiceBroker().getService(this, QuiescenceReportService.class, null);

    if (myQuiescenceReportService != null)
      myQuiescenceReportService.setAgentIdentificationService(myAgentIdentificationService);

    if (myRegistryQueryService == null)
      throw new RuntimeException("Unable to obtain RegistryQuery service");
  }

  public void unload() {
    if (myRegistryQueryService != null) {
      getBindingSite().getServiceBroker().releaseService(this,
                                                         RegistryQueryService.class,
                                                         myRegistryQueryService);
      myRegistryQueryService = null;
    }

    if (myQuiescenceReportService != null) {
      getBindingSite().getServiceBroker().releaseService(this,
                                                         QuiescenceReportService.class,
                                                         myQuiescenceReportService);
      myQuiescenceReportService = null;
    }

    if (myAgentIdentificationService != null) {
      getBindingSite().getServiceBroker().releaseService(this,
                                                         AgentIdentificationService.class,
                                                         myAgentIdentificationService);
      myAgentIdentificationService = null;
    }

    if ((myLoggingService != null) && 
	(myLoggingService != LoggingService.NULL)) {
      getBindingSite().getServiceBroker().releaseService(this, 
							 LoggingService.class,
							 myLoggingService);
      myLoggingService = null;
    }
    super.unload();
  }

  protected void setupSubscriptions() {
    myAgentName = getBindingSite().getAgentIdentifier().toString();

    myClientRequestSubscription = 
      (IncrementalSubscription) subscribe(myQueryRequestPredicate);
    myLineageSubscription = 
      (IncrementalSubscription) subscribe(myLineagePredicate);

    Collection params = getDelegate().getParameters();
    if (params.size() > 0) {
      myDistributedYPServers =
	Boolean.valueOf((String) params.iterator().next()).booleanValue();
    } else {
      myDistributedYPServers = false;
    }
  }

 
  protected void execute() {
    if (myClientRequestSubscription.hasChanged()) {

      for (Iterator i = myClientRequestSubscription.getAddedCollection().iterator();
	   i.hasNext();) {
        MMQueryRequest queryRequest =  (MMQueryRequest) i.next();
        MMRoleQuery query = (MMRoleQuery) queryRequest.getQuery();
        RegistryQuery rq = new RegistryQueryImpl();
	RQ r;

        // Find all service providers for specifed Role
        ServiceClassification roleSC =
	  new ServiceClassificationImpl(query.getRole().toString(),
					query.getRole().toString(),
					UDDIConstants.MILITARY_SERVICE_SCHEME);
        rq.addServiceClassification(roleSC);

	if (myDistributedYPServers) {
	  r = new RQ(queryRequest, query, rq);
	} else {
	  r = new RQ(queryRequest, query, rq);
	}

        postRQ(r);
      }
    }


    RQ r;
    while ((r = getPendingRQ()) != null) {
      if (r.exception != null) {
	handleException(r);
      } else {
	handleResponse(r);
      }
    }

    handleQuiescenceReport();
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
    myOutstandingAlarms++;

    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(getAgentIdentifier() + 
		" adding a QueryAlarm for r.query.getRole()" + 
		" alarm - " + alarm);
    }

    if(System.currentTimeMillis() > getWarningCutoffTime()) {
      if (e == null)
	myLoggingService.error(getAgentIdentifier() + message);
      else
	myLoggingService.error(getAgentIdentifier() + message, e);
    } else if (myLoggingService.isDebugEnabled()) {
      if (e == null)
	myLoggingService.debug(getAgentIdentifier() + message);
      else
	myLoggingService.debug(getAgentIdentifier() + message, e);
    }
  }

  protected void handleResponse(RQ r) {
    MMQueryRequest queryRequest = r.queryRequest;
    MMRoleQuery query = r.query;

    if (query.getObsolete()) {
      if (myLoggingService.isDebugEnabled()) {
	myLoggingService.debug(myAgentName + 
			       " ignoring registry query result for obsolete request - " +
			       r.query);
      }
      return;
    }

    Collection services = r.services;

    
    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(myAgentName + 
			     " registry query result size is : " + 
			     services.size() + " for query: " + 
			     query.getRole().toString() + " " +
			     new Date(query.getTimeSpan().getStartTime()) +
			     " to " +
			     new Date(query.getTimeSpan().getEndTime()));
    }

    ArrayList scoredServiceDescriptions = new ArrayList();
    for (Iterator iter = services.iterator(); iter.hasNext(); ) {
      ServiceInfo serviceInfo = (ServiceInfo) iter.next();
      int score = query.getServiceInfoScorer().scoreServiceInfo(serviceInfo);

      if (score >= 0) {
	scoredServiceDescriptions.add(new ScoredServiceDescriptionImpl(score,
								       serviceInfo));
	if(myLoggingService.isDebugEnabled()) {
	  myLoggingService.debug(myAgentName + 
				 ":execute: adding Provider name: " + 
				 serviceInfo.getProviderName() +
				 " Service name: " + 
				 serviceInfo.getServiceName() +
				 " Service score: " + score);
	}
      } else {
	if (myLoggingService.isDebugEnabled()) {
	  myLoggingService.debug(myAgentName + 
				 ":execute: ignoring Provider name: " + 
				 serviceInfo.getProviderName() +
				 " Service name: " + 
				 serviceInfo.getServiceName() +
				 " Service score: " + score);
	}
      }
    }

    if (scoredServiceDescriptions.isEmpty()) {
      if ((myDistributedYPServers) &&
	  (!r.getNextContextFailed)) {
	  if (myLoggingService.isDebugEnabled()) {
	    myLoggingService.debug(myAgentName + 
				   " no matching provider for " + 
				   query.getRole() +
				   " in " + r.currentYPContext +
				   " retrying in next context.");
	  }
	  postRQ(r);
      } else {
	// Couldn't find another YPServer to search
	retryErrorLog(r, 
		      myAgentName + " unable to find provider for " + 
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

    if(myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(myAgentName + ": publishChanged query");
    }
  }

  protected void handleQuiescenceReport() {
    // Whenever we submit a query to the YP we go off into the ether
    // So if there are outstanding YP queries or alarms, then mark the fact that we are not done yet
    // so that Quiescence stuff doesnt decide we're done prematurely early
    // Note that myPendingRQs should _always_ be empty at this point. And we'll only have oustandingAlarms
    // if there were exceptions talking to the YP.

    if (myQuiescenceReportService != null) {
      // Check if done with YP queries in synch blocks
      // since callbacks may be running
      boolean noOutRQs = false;
      synchronized (myOutstandingRQs) {
	noOutRQs = myOutstandingRQs.isEmpty();
      }
      boolean noPendRQs = false;
      synchronized (myPendingRQs) {
	noPendRQs = myPendingRQs.isEmpty();
      }

      if (noOutRQs && 
	  noPendRQs && 
	  myOutstandingAlarms == 0) {
	// Nothing on the lists and no outstanding alarms - so we're done
	myQuiescenceReportService.setQuiescentState();
	resetWarningCutoffTime();
	if (myLoggingService.isInfoEnabled())
	  myLoggingService.info(myAgentName + 
				" finished all YP queries. Now quiescent.");

      } else {
	// Some query waiting for an answer, or waiting for this Plugin to 
	// handle it, or waiting to retry a query
	// We're not done

	myQuiescenceReportService.clearQuiescentState();
	if (myLoggingService.isInfoEnabled())
	  myLoggingService.info(myAgentName + 
				" has outstanding YP queries or answers. " +
				"Not quiescent.");
	if (myLoggingService.isDebugEnabled()) {
	  // Get the toStrings in synch blocks since callbacks
	  // may currently be executing
	  String outRQs = "";
	  String pendRQs = "";
	  synchronized (myOutstandingRQs) {
	    outRQs = myOutstandingRQs.toString();
	  }
	  synchronized (myPendingRQs) {
	    pendRQs = myPendingRQs.toString();
	  }

	  myLoggingService.debug("\tYP questions outstanding: " + 
				 // AMH: Actually print the outstanding RQs
				 //				 myOutstandingRQs.size() + 
				 outRQs + 
				 ". YP answers to process: " + 
				 pendRQs + 
				 //				 myPendingRQs.size() + 
				 ". Outstanding alarms: " + myOutstandingAlarms);
	}
      }
    }
  }

  protected long getWarningCutoffTime() {
    if (myWarningCutoffTime == 0) {
      WARNING_SUPPRESSION_INTERVAL = 
	Integer.getInteger(QUERY_GRACE_PERIOD_PROPERTY,
			   WARNING_SUPPRESSION_INTERVAL).intValue();
      myWarningCutoffTime = System.currentTimeMillis() + 
	WARNING_SUPPRESSION_INTERVAL*60000;
    }

    return myWarningCutoffTime;
  }
  
  protected void resetWarningCutoffTime() {
    myWarningCutoffTime = -1;
  }

  protected Lineage getLineage(int lineageType, TimeSpan timeSpan) {
    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(myAgentName + 
			     ": getLineage() requested lineage of type " +
			     Lineage.typeToRole(lineageType) + 
			     new Date(timeSpan.getStartTime()) + 
			     " - " +
			     new Date(timeSpan.getEndTime()));
    }

    for (Iterator iterator = myLineageSubscription.iterator();
	 iterator.hasNext();) {
      Lineage lineage = (Lineage) iterator.next();
      if (lineage.getType() == lineageType) {
	Collection lineageTimeSpans = 
	  lineage.getSchedule().getOverlappingScheduleElements(timeSpan.getStartTime(),
							       timeSpan.getEndTime());

	if (!lineageTimeSpans.isEmpty()) {
	  TimeSpan lineageTimeSpan = 
	    (TimeSpan) lineageTimeSpans.iterator().next();
	  if ((lineageTimeSpan.getStartTime() <= timeSpan.getStartTime()) &&
	      (lineageTimeSpan.getEndTime() >= timeSpan.getEndTime())) {

	    if (myLoggingService.isDebugEnabled()) {
	      myLoggingService.debug(myAgentName + 
				     ": getLineage() returning " + 
				     lineage);
	    }
	    return lineage;
	  } else {
	    myLoggingService.warn(myAgentName + 
				  ": getLineage() requested timeSpan " +
				  new Date(timeSpan.getStartTime()) + 
				  " - " +
				  new Date(timeSpan.getEndTime()) +
				  " spans more than one lineage.");
	    return null;
	  }
	}
      }
    }
    
    myLoggingService.error(myAgentName + 
			   ": getLineage() requested lineage of type " +
			   Lineage.typeToRole(lineageType) + 
			   new Date(timeSpan.getStartTime()) + 
			   " - " +
			   new Date(timeSpan.getEndTime()) +
			   " does not match any lineage.\n" +
			   myLineageSubscription);
    return null;
  }

  private class RQ {
    MMQueryRequest queryRequest;
    MMRoleQuery query;
    RegistryQuery rq;
    Lineage ypLineage;

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
      this.ypLineage = null;
    }

    // Verbose toString used when printing myOutstandingRQs in handleQuiescenceReport
    public String toString() {
      return "RQ for query <" + queryRequest.getUID() + ": ResultCode " + queryRequest.getResultCode() + ", query: " + query + "> using lineage " + ypLineage + ". Has exception? " + (exception == null ? "No" : "Yes") + ". Complete? " + complete + " getNextContextFailed? " + getNextContextFailed;
    }
  }

  // issue a async request
  private void postRQ(final RQ r) {
    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(getAgentIdentifier() + ": posting " + r + 
			     " (" + r.rq + ")" );
    }
    synchronized (myOutstandingRQs) {
      myOutstandingRQs.add(r);
    }

    if (myDistributedYPServers) {
      findServiceWithDistributedYP(r);
    } else {
      findServiceWithCentralizedYP(r);
    }
  }

  // note an async response and wake the plugin
  private void pendRQ(RQ r) {
    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(getAgentIdentifier() + 
			     " pending " + r + " (" + r.rq + ")");
    }
    r.complete = true;
    synchronized (myOutstandingRQs) {
      myOutstandingRQs.remove(r);
    }
    synchronized (myPendingRQs) {
      myPendingRQs.add(r);
    }
    wake();                     // tell the plugin to wake up
  }

  // get a pending RQ (or null) so that we can deal with it
  private RQ getPendingRQ() {
    RQ r = null;
    synchronized (myPendingRQs) {
      if (!myPendingRQs.isEmpty()) {
        r = (RQ) myPendingRQs.remove(0); // treat like a fifo
        if (myLoggingService.isDebugEnabled()) {
          myLoggingService.debug(getAgentIdentifier() + 
				 " retrieving " + r + " (" + r.rq + ")");
        }
      }
    }
    return r;
  }

  private boolean useYPCommunitySearchPath(final RQ r) {
    Lineage opconLineage = getLineage(Lineage.OPCON, r.query.getTimeSpan());
    Lineage adconLineage = getLineage(Lineage.ADCON, r.query.getTimeSpan());
    
    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(getAgentIdentifier() + ": useYPCommunitySearchPath() " +
			     " timeSpan = (" +
			     new Date(r.query.getTimeSpan().getStartTime()) +
			     ", " + 
			     new Date(r.query.getTimeSpan().getEndTime()) +
			     ")  adconLineage = " + adconLineage +
			     " opconLineage = " + opconLineage);
      if ((opconLineage != null) && 
	  (adconLineage != null)) {
	myLoggingService.debug(getAgentIdentifier() + ": useYPCommunitySearchPath()() " +
			       " adconLineage.getList() = " + 
			       adconLineage.getList() +
			       " opconLineage.getList() = " + 
			       opconLineage.getList() + 
			       " opconLineage.getList().equals(adconLineage.getList()) == " + 
			       opconLineage.getList().equals(adconLineage.getList()));
      }
    }
    
    if ((adconLineage == null) || (opconLineage == null)) {
      if (myLoggingService.isDebugEnabled()) {
	myLoggingService.debug(getAgentIdentifier() + 
			       ": useYPCommunitySearchPath() " + 
			       " returning false - " + 
			       "no Administrative or Operational lineage." + 
			       " ADCON lineage = " + adconLineage +
			       " OPCON lineage = " + opconLineage + 
			       " for " 	+	
				new Date(r.query.getTimeSpan().getStartTime()) +
			       " to " +
			       new Date(r.query.getTimeSpan().getEndTime()));
      }

      return false;
    } else {
      return (opconLineage.getList().equals(adconLineage.getList()));
    }
  }

  private void findServiceWithCentralizedYP(final RQ r) {
    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(getAgentIdentifier() +
			     " findServiceWithCentralizedYP: r = " + r);
    }
  
    myRegistryQueryService.findServiceAndBinding(r.rq,
						 new RegistryQueryService.Callback() {
      public void invoke(Object result) {
	r.services = (Collection) result;
	if (myLoggingService.isDebugEnabled()) {
	  myLoggingService.debug(getAgentIdentifier() + 
				 " results = " + result + 
				 " for " + r.currentYPContext);
	}
	flush();
      }
      public void handle(Exception e) {
	r.exception = e;
	if (myLoggingService.isDebugEnabled()) {
	  myLoggingService.debug(getAgentIdentifier() + 
				 " failed during query of " +
				 r.queryRequest, e);
	}
	flush();
      }
      
      private void flush() {
	pendRQ(r);
      }
    });
  }

  private void findServiceWithDistributedYP(final RQ r) {
    if (useYPCommunitySearchPath(r)) {
      if (myLoggingService.isDebugEnabled()) {
	myLoggingService.debug(getAgentIdentifier() +
			       " findServiceWithDistributedYP: " +
			       " using YPCommunity search.");
      }

      r.ypLineage = null;
      myRegistryQueryService.findServiceAndBinding(r.currentYPContext, r.rq,
						   new RegistryQueryService.CallbackWithContext() {
	public void invoke(Object result) {
	  r.services = (Collection) result;
	  if (myLoggingService.isDebugEnabled()) {
	    myLoggingService.debug(getAgentIdentifier() + 
				   " results = " + result + 
				   " for " + r.currentYPContext);
	  }
	  flush();
	}
	
	public void handle(Exception e) {
	  r.exception = e;
	  if (myLoggingService.isDebugEnabled()) {
	    myLoggingService.debug(getAgentIdentifier() + 
				   " failed during query of " +
				   r.queryRequest + 
				   " context =  " + r.currentYPContext, e);
	  }
	  flush();
	}
	
	public void setNextContext(Object context){
	  if (myLoggingService.isDebugEnabled()) {
	    myLoggingService.debug(getAgentIdentifier() + 
				   " previous YPContext " +
				   r.currentYPContext + 
				   " current YPContext " + context);
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
      r.ypLineage = getLineage(Lineage.OPCON, r.query.getTimeSpan());

      if (r.ypLineage == null) {
	String errorMessage = getAgentIdentifier() + 
	  " no Operation lineage for " +
	  new Date(r.query.getTimeSpan().getStartTime()) +
	  " to " +
	  new Date(r.query.getTimeSpan().getEndTime());
	IllegalStateException ise = new IllegalStateException();
	// AMH: Don't retry here. Instead, let the while in execute()
	// call handleException to do this.
	r.exception = ise;
	if (myLoggingService.isDebugEnabled())
	  myLoggingService.debug(getAgentIdentifier() + "AMH: findServiceWithDistributedYP had no Operation Lineage, doing pendRQ with an IllegalStateException for RQ " + r);
	pendRQ(r);
	//	retryErrorLog(r,errorMessage, ise);
	return;
      }

      if (myLoggingService.isDebugEnabled()) {
	myLoggingService.debug(getAgentIdentifier() +
			       " findServiceWithDistributedYP: " +
			       " using lineage based search - " +
			       " r.ypLineage = " + r.ypLineage);
      }

      List lineageList = r.ypLineage.getList();
      int listSize = lineageList.size();
      
      if (r.currentYPContext == null) {
	r.currentYPContext = (listSize > 1) ?
	  (String) lineageList.get(listSize - 2) : 
	  (String) r.ypLineage.getLeaf();
      } else {
	int index = lineageList.indexOf(r.currentYPContext);
	if (index > 0) {
	  r.previousYPContext = r.currentYPContext;
	  r.currentYPContext = (String) lineageList.get(index - 1);
	} else {
	  // Reached the top of the lineage. Restart the search.
	  r.currentYPContext = (listSize > 1) ?
	    (String) lineageList.get(listSize - 2) : 
	    (String) r.ypLineage.getLeaf();
	}
      }
      
      myRegistryQueryService.findServiceAndBinding((String) r.currentYPContext,
						   r.rq,
						   new RegistryQueryService.CallbackWithContext() {
	public void invoke(Object result) {
	  r.services = (Collection) result;
	  if (myLoggingService.isDebugEnabled()) {
	    myLoggingService.debug(getAgentIdentifier() + 
				   " results = " + result + 
				   " for " + r.currentYPContext);
	  }
	  flush();
	}
	
	public void handle(Exception e) {
	  r.exception = e;
	  if (myLoggingService.isDebugEnabled()) {
	    myLoggingService.debug(getAgentIdentifier() + 
				   " failed during query of " +
				   r.queryRequest + " context =  " + 
				   r.currentYPContext, e);
	  }
	  flush();
	}
	
	public void setNextContext(Object context){
	  if (myLoggingService.isDebugEnabled()) {
	    myLoggingService.debug(getAgentIdentifier() + 
				   " getNextContext() for " +
				   r.currentYPContext + " returned " + 
				   context);
	  }
	  
	  
	  if (context == null) {
	    // Restart search
	    r.currentYPContext = null;
	    r.getNextContextFailed = true;
	  }
	}
	
	private void flush() {
	  pendRQ(r);
	}
      });
    }
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
	--myOutstandingAlarms;
      }
    }

    public boolean hasExpired() { return expired; }
    public synchronized boolean cancel() {
      boolean was = expired;
      expired = true;
      --myOutstandingAlarms;
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














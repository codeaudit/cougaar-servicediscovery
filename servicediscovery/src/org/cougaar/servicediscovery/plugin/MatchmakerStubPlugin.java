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

import org.cougaar.servicediscovery.Constants;
import org.cougaar.servicediscovery.description.*;
import org.cougaar.servicediscovery.service.RegistryQueryService;
import org.cougaar.servicediscovery.transaction.MMQueryRequest;
import org.cougaar.servicediscovery.transaction.MMQueryRequestImpl;
import org.cougaar.servicediscovery.transaction.RegistryQuery;
import org.cougaar.servicediscovery.transaction.RegistryQueryImpl;
import org.cougaar.servicediscovery.util.UDDIConstants;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.planning.plugin.legacy.SimplePlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 *
 *
 *
 */
public final class MatchmakerStubPlugin extends SimplePlugin {

  private String agentName;
  private LoggingService log;
  private RegistryQueryService registryQueryService;
  private IncrementalSubscription clientRequestSub;
  private IncrementalSubscription lineageListSub;

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
      return ((o instanceof LineageList) &&
	      (((LineageList) o).getType() == LineageList.COMMAND));
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
  }

  protected void execute() {
    if (clientRequestSub.hasChanged()) {
      Collection newRequest = clientRequestSub.getAddedCollection();
      for (Iterator i = newRequest.iterator(); i.hasNext();) {
        MMQueryRequest queryRequest =  (MMQueryRequest) i.next();
        MMRoleQuery query = (MMRoleQuery) queryRequest.getQuery();
        RegistryQuery rq = new RegistryQueryImpl();

        // Find all service providers for specifed Role
        ServiceClassification sc = new ServiceClassificationImpl(query.getRole().toString(),
                                                                 query.getRole().toString(),
                                                                 UDDIConstants.MILITARY_SERVICE_SCHEME);
        rq.addServiceClassification(sc);

        Collection services = registryQueryService.findServiceAndBinding(rq);
        if (log.isDebugEnabled()) {
          log.debug("Registry query result size is : " + services.size() + " for query: " + query.getRole().toString());
        }

        ArrayList scoredServiceDescriptions = new ArrayList();
        for (Iterator iter = services.iterator(); iter.hasNext(); ) {
          ServiceInfo serviceInfo = (ServiceInfo) iter.next();
	  float score = scoreServiceProvider(serviceInfo, query.getEchelon());
	  
	  if (score >= 0) {
	    scoredServiceDescriptions.add(new ScoredServiceDescriptionImpl(score,
									   serviceInfo));
	    if(log.isDebugEnabled()) {
	      log.debug(agentName + ":execute: adding Provider name: " + serviceInfo.getProviderName() +
			" Service name: " + serviceInfo.getServiceName() +
			" Service score: " + score);
	    }
	  } else {
	    // Negative score means the provider failed one of the screens

	    if(log.isDebugEnabled()) {
	      log.debug(agentName + ":execute: ignoring Provider name: " + serviceInfo.getProviderName() +
			" Service name: " + serviceInfo.getServiceName() + 
			" Service score: " + score);
	    }
          }
        }

        Collections.sort(scoredServiceDescriptions);
        ((MMQueryRequestImpl) queryRequest).setResult(scoredServiceDescriptions);
        getBlackboardService().publishChange(queryRequest);
        if(log.isDebugEnabled()) {
          log.debug(agentName + ": publishChanged query");
        }
      }
    }
  }

  protected float scoreServiceProvider(ServiceInfo serviceInfo, 
				       String requestedEchelonOfSupport) {
    int echelonScore = getEchelonScore(serviceInfo, requestedEchelonOfSupport);
    if (echelonScore < 0) {
      return -1;
    } 

    int lineageScore = getLineageScore(serviceInfo);
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
      return 0;
    }

    int serviceEchelonOrder = -1;

    for (Iterator iterator = serviceInfo.getServiceClassifications().iterator();
	 iterator.hasNext();) {
      ServiceClassification classification = 
	(ServiceClassification) iterator.next();
      if (classification.getClassificationSchemeName().equals(UDDIConstants.MILITARY_ECHELON_SCHEME)) {
  // this should be getClassificationValue and not name.
  // It works now since they are the same string in the registry.
	String serviceEchelon = classification.getClassificationName();
	
	serviceEchelonOrder = 
	  Constants.MilitaryEchelon.echelonOrder(serviceEchelon);
	break;
      }
    }
    
    if (serviceEchelonOrder == -1) {
      if (log.isDebugEnabled()) {
	log.debug(agentName + ": Ignoring service with a bad echelon of support: " + 
		  serviceEchelonOrder);
      }
      return -1;
    } if (serviceEchelonOrder < requestedEchelonOrder) {
      return -1;
    } else {
      return (serviceEchelonOrder - requestedEchelonOrder);
    }
  }

  protected int getLineageScore(ServiceInfo serviceInfo) {
    LineageList commandLineage = null;

    for (Iterator iterator = lineageListSub.iterator();
	 iterator.hasNext();) {
      commandLineage = (LineageList) iterator.next();
    }

    if (commandLineage == null) {
      return -1;
    }

    for (Iterator iterator = serviceInfo.getServiceClassifications().iterator();
	 iterator.hasNext();) {
      ServiceClassification classification = 
	(ServiceClassification) iterator.next();
      if (classification.getClassificationSchemeName().equals(UDDIConstants.SUPPORT_COMMAND_ASSIGNMENT)) {
	return commandLineage.countHops(agentName, 
					classification.getClassificationName());
      }
    }

    return -1;
  }
}



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

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.blackboard.PrivilegedClaimant;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.mts.MessageAddress;

import org.cougaar.glm.ldm.Constants;

import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.Disposition;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.planning.plugin.util.PluginHelper;

import org.cougaar.servicediscovery.description.ProviderDescription;
import org.cougaar.servicediscovery.description.ProviderDescriptionImpl;
import org.cougaar.servicediscovery.description.ServiceClassification;
import org.cougaar.servicediscovery.description.ServiceClassificationImpl;
import org.cougaar.servicediscovery.description.SupportLineageList;
import org.cougaar.servicediscovery.service.RegistrationService;
import org.cougaar.servicediscovery.util.UDDIConstants;
import org.cougaar.servicediscovery.util.StatusChangeMessage;
import org.cougaar.servicediscovery.transaction.DAMLReadyRelay;
import org.cougaar.servicediscovery.SDFactory;
import org.cougaar.servicediscovery.SDDomain;

import org.cougaar.util.ConfigFinder;
import org.cougaar.util.UnaryPredicate;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;



public class SDRegistrationPlugin extends SimplePlugin implements PrivilegedClaimant {

  private static final String DAML_IDENTIFIER = ".profile.daml";

  private LoggingService log;

  //private RegistrationService
  private RegistrationService registrationService = null;

  private String myAgent;

  private IncrementalSubscription supportLineageSubscription;
  private IncrementalSubscription statusChangeSubscription;
  private IncrementalSubscription registerTaskSubscription;

  private boolean isRegistered = false;

  private Alarm myAlarm;

  private UnaryPredicate supportLineagePredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof SupportLineageList);
    }
  };

  private UnaryPredicate statusChangePredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof StatusChangeMessage);
    }
  };

  private UnaryPredicate registerTaskPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return ((o instanceof Task) &&
	      (((Task) o).getVerb().equals(Constants.Verb.RegisterServices)));
    }
  };

  protected void setupSubscriptions() {

    log = (LoggingService) getBindingSite().getServiceBroker().
      getService(this, LoggingService.class, null);

    registrationService = (RegistrationService) getBindingSite().
      getServiceBroker().getService(this, RegistrationService.class, null);

    supportLineageSubscription = 
      (IncrementalSubscription) subscribe(supportLineagePredicate);
    statusChangeSubscription = 
      (IncrementalSubscription) subscribe(statusChangePredicate);
    registerTaskSubscription = 
      (IncrementalSubscription) subscribe(registerTaskPredicate);
  }

  protected void execute () {
    if (!isRegistered) {
      initialRegister();
      if (isRegistered) {
	if (log.isDebugEnabled()) {
	  log.debug("Registering: " + myAgent);
	}

	updateRegisterTaskDispositions(registerTaskSubscription);
	return;
      }
      
    } else if (supportLineageSubscription.hasChanged()) {
      Collection adds = supportLineageSubscription.getAddedCollection();
      Collection serviceClassifications = scaServiceClassifications(adds);
      registrationService.updateServiceDescription(getAgentIdentifier().toString(), 
						   serviceClassifications);
      // Not handling changes/removes
    }


    if (statusChangeSubscription.hasChanged()) {
      Collection adds = statusChangeSubscription.getAddedCollection();
      for (Iterator iterator = adds.iterator(); iterator.hasNext();) {
	StatusChangeMessage statusChange = 
	  (StatusChangeMessage) iterator.next();
	Collection serviceClassifications = new ArrayList(1);
	ServiceClassification sca = 
	  new ServiceClassificationImpl(statusChange.getRole(), 
					statusChange.getRole(),
					UDDIConstants.MILITARY_SERVICE_SCHEME);
	serviceClassifications.add(sca);
	boolean registryDeleteStatus = 
	  registrationService.deleteServiceDescription(myAgent,
						       serviceClassifications);
	statusChange.setRegistryUpdated(registryDeleteStatus);
	publishChange(statusChange);
      }
      // Handle statusChangeMessage as trumping the presence of DAML file.
      // Don't change isRegistered flag because that would assume that we
      // should reregister.
      //isRegistered = false;
      
      // No point in updating a non-existant registration
      return;
    }    

    if (registerTaskSubscription.hasChanged()) {
      Collection adds = registerTaskSubscription.getAddedCollection();
      updateRegisterTaskDispositions(adds);
    }
  }

  private void initialRegister() {
    myAgent = getAgentIdentifier().toString();
    String damlfilename = myAgent + DAML_IDENTIFIER;
    try {
      InputStream in = ConfigFinder.getInstance().open(damlfilename);
      if (in != null) {
        ProviderDescription pd = new ProviderDescriptionImpl(log);
        boolean result = pd.parseDAML(damlfilename);
        // TODO:  need to respond in some way to registration failures
        if(result && pd.getProviderName() != null) {
	  if (registrationService.addProviderDescription(pd, scaServiceClassifications(supportLineageSubscription)))  {
	    isRegistered = true;
	    myAlarm = null;
	  }
	  else {
	    int rand = (int)(Math.random()*10000) + 1000;
	    myAlarm = this.wakeAfter(rand);
	    if (log.isErrorEnabled()) {
	      log.error("Error:  problem adding ProviderDescription now, try again later: " + myAgent);
	    }
	  }
        } else {
          //if the parsing failed, it may be because one of the url's was down, so try again later
          int rand = (int)(Math.random()*10000) + 1000;
          myAlarm = this.wakeAfter(rand);
          if (log.isErrorEnabled()) {
            log.error("Error:  ProviderDescription is null, not registering agent now, try again later: " + myAgent);
          }
        }
      } else {
	isRegistered = true;
        if (log.isDebugEnabled()) {
          log.debug("Agent " + myAgent + " Not Registering, no daml file.");
        }
      }
    } catch( IOException ioe) {
      isRegistered = true;
      if (log.isDebugEnabled()) {
        log.debug("Agent " + myAgent + " Not Registering, no daml file.");
      }
    }
  }
    

  private Collection scaServiceClassifications(Collection supportLineageCollection) {
    Collection serviceClassifications = 
      new ArrayList(supportLineageCollection.size());
    for (Iterator iterator = supportLineageCollection.iterator(); 
	 iterator.hasNext();) {
      SupportLineageList supportList = (SupportLineageList) iterator.next();
      ServiceClassification sca = 
	new ServiceClassificationImpl(supportList.getRoot(), 
				      supportList.getRoot(),
				      UDDIConstants.SUPPORT_COMMAND_ASSIGNMENT);
      serviceClassifications.add(sca);
    }
    return serviceClassifications;
  }
  
  private void updateRegisterTaskDispositions(Collection registerTasks) {
    for (Iterator iterator = registerTasks.iterator();
	 iterator.hasNext();) {
      Task task = (Task) iterator.next();
      PlanElement pe = task.getPlanElement();
      double conf;
      if (isRegistered) {
	conf = 1.0;
      } else {
	conf = 0.0;
      }

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





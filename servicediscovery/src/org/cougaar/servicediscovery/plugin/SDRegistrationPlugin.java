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
import org.cougaar.planning.plugin.legacy.SimplePlugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

//import org.cougaar.util.UnaryPredicate;


public class SDRegistrationPlugin extends SimplePlugin implements PrivilegedClaimant {

  private static final String DAML_IDENTIFIER = ".profile.daml";

  //private static Logger log;
  private LoggingService log;

  //private RegistrationService
  private RegistrationService registrationService = null;

  private String myAgent;

  private IncrementalSubscription supportLineageSubscription;
  private IncrementalSubscription damlReadySubscription;
  private IncrementalSubscription statusChangeSubscription;

  private SDFactory mySDFactory;

  private boolean isRegistered = false;

  private Alarm myAlarm;

  private UnaryPredicate supportLineagePredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof SupportLineageList);
    }
  };

  private UnaryPredicate damlReadyPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof DAMLReadyRelay);
    }
  };

  private UnaryPredicate statusChangePredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof StatusChangeMessage);
    }
  };

  protected void setupSubscriptions() {

    log = (LoggingService) getBindingSite().getServiceBroker().
      getService(this, LoggingService.class, null);

    registrationService = (RegistrationService) getBindingSite().
      getServiceBroker().getService(this, RegistrationService.class, null);

    supportLineageSubscription = (IncrementalSubscription)getBlackboardService().subscribe(supportLineagePredicate);
    damlReadySubscription = (IncrementalSubscription)getBlackboardService().subscribe(damlReadyPredicate);
    statusChangeSubscription = (IncrementalSubscription)getBlackboardService().subscribe(statusChangePredicate);
    mySDFactory = (SDFactory) getFactory(SDDomain.SD_NAME);
    DAMLReadyRelay relay = mySDFactory.newDAMLReadyRelay(MessageAddress.getMessageAddress("DAML"));
    publishAdd(relay);

  }

  protected void execute () {
    if(damlReadySubscription.getChangedCollection().size()>0 || myAlarm!=null) {
      if (!isRegistered) {
        Iterator relayIt = damlReadySubscription.iterator();
        while(relayIt.hasNext()) {
          DAMLReadyRelay relay = (DAMLReadyRelay)relayIt.next();
          if(relay.isReady()) {
            initialRegister();
            if(isRegistered) {
              if (log.isDebugEnabled()) {
                log.debug("Registering: " + myAgent);
              }

              if (!supportLineageSubscription.getCollection().isEmpty()) {
                Collection serviceCategories = null;
                serviceCategories = new ArrayList();
                for (Iterator i = supportLineageSubscription.getCollection().iterator(); i.hasNext();) {
                  SupportLineageList supportList = (SupportLineageList) i.next();
                  ServiceClassification eos = new ServiceClassificationImpl(supportList.getEchelonOfSupport(),
                                                                            supportList.getEchelonOfSupport(),
                                                                            UDDIConstants.MILITARY_ECHELON_SCHEME);
                  serviceCategories.add(eos);

                  ServiceClassification sca = new ServiceClassificationImpl(supportList.getRoot(), supportList.getRoot(),
                                                                            UDDIConstants.SUPPORT_COMMAND_ASSIGNMENT);
                  serviceCategories.add(sca);
                }
                registrationService.updateServiceDescription(getAgentIdentifier().toString(), serviceCategories);

                return;
              }
            }
          }
        }
      }
    }
    // If I am a provider, update my service descriptions in the registry
    if (isRegistered) {

      Collection serviceCategories = null;
      if (supportLineageSubscription.hasChanged()) {
        serviceCategories = new ArrayList();
        if (! supportLineageSubscription.getAddedCollection().isEmpty()) {
          for (Iterator i = supportLineageSubscription.getAddedCollection().iterator(); i.hasNext();) {
            SupportLineageList supportList = (SupportLineageList) i.next();
            ServiceClassification eos = new ServiceClassificationImpl(supportList.getEchelonOfSupport(),
                                                                      supportList.getEchelonOfSupport(),
                                                                      UDDIConstants.MILITARY_ECHELON_SCHEME);
            serviceCategories.add(eos);

            ServiceClassification sca = new ServiceClassificationImpl(supportList.getRoot(), supportList.getRoot(),
                                                                      UDDIConstants.SUPPORT_COMMAND_ASSIGNMENT);
            serviceCategories.add(sca);
          }
          registrationService.updateServiceDescription(getAgentIdentifier().toString(), serviceCategories);
        }
      }
      // Update if the list changed
      if (! supportLineageSubscription.getChangedCollection().isEmpty()) {
        for (Iterator i = supportLineageSubscription.getChangedCollection().iterator(); i.hasNext();) {
          SupportLineageList supportList = (SupportLineageList) i.next();
          ServiceClassification eos = new ServiceClassificationImpl(supportList.getEchelonOfSupport(),
                                                                    supportList.getEchelonOfSupport(),
                                                                    UDDIConstants.MILITARY_ECHELON_SCHEME);
          serviceCategories.add(eos);

          ServiceClassification sca = new ServiceClassificationImpl(supportList.getRoot(), supportList.getRoot(),
                                                                    UDDIConstants.SUPPORT_COMMAND_ASSIGNMENT);
          serviceCategories.add(sca);
        }
        registrationService.updateServiceDescription(getAgentIdentifier().toString(), serviceCategories);
      }
      Collection categories = new ArrayList();
      StatusChangeMessage statusChange = null;
      if (statusChangeSubscription.hasChanged()) {
        for (Iterator iterator = statusChangeSubscription.getAddedCollection().iterator(); iterator.hasNext();) {
          statusChange = (StatusChangeMessage) iterator.next();
          ServiceClassification sca = new ServiceClassificationImpl(statusChange.getRole(), statusChange.getRole(),
                                                                    UDDIConstants.MILITARY_SERVICE_SCHEME);
          categories.add(sca);
          statusChange.setRegistryUpdated(registrationService.deleteServiceDescription(myAgent, categories));
          publishChange(statusChange);
        }
      }
    }
    // TODO: are removes expected?
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
            if(registrationService.addProviderDescription(pd)) {
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
        if (log.isDebugEnabled()) {
          log.debug("Agent " + myAgent + " Not Registering, no daml file.");
        }
      }
    } catch( IOException ioe) {
      if (log.isDebugEnabled()) {
        log.debug("Agent " + myAgent + " Not Registering, no daml file.");
      }
    }
  }
}

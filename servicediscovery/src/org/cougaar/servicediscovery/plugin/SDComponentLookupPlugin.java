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
import org.cougaar.servicediscovery.description.ProviderDescription;
import org.cougaar.servicediscovery.service.RegistrationService;
import org.cougaar.servicediscovery.transaction.ProviderDescriptionQuery;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.planning.plugin.legacy.SimplePlugin;

import java.util.Iterator;

public class SDComponentLookupPlugin extends SimplePlugin {

  //private static Logger log;
  private LoggingService log;

  private RegistrationService myRegistrationService = null;

  private IncrementalSubscription myServiceRequestSubscription;

  private UnaryPredicate myRequestForServiceDescription = new UnaryPredicate() {
      public boolean execute(Object o) {
        if( o instanceof ProviderDescriptionQuery ) {
          return true;
        }
        return false;
      }
    };

  protected void setupSubscriptions() {

    log = (LoggingService) getBindingSite().getServiceBroker().
        getService(this, LoggingService.class, null);

    myRegistrationService = (RegistrationService) getBindingSite().
      getServiceBroker().getService(this, RegistrationService.class, null);

    myServiceRequestSubscription = (IncrementalSubscription)subscribe(myRequestForServiceDescription);
  }

  protected void execute () {
    if( myServiceRequestSubscription.hasChanged()) {
      for (Iterator iter = myServiceRequestSubscription.getAddedCollection().iterator(); iter.hasNext();) {
//         ServiceDescriptionRequest sdr = (ServiceDescriptionRequest) iter.next();
//         ServiceDescription sd = myRegistrationService.getServiceDescription(sdr.getKey());
//         sdr.setServiceDescription(sd);
        ProviderDescriptionQuery pdq = (ProviderDescriptionQuery) iter.next();
        ProviderDescription pd = myRegistrationService.getProviderDescription(pdq.getKey());
        pdq.setProviderDescription(pd);
        if(log.isDebugEnabled()) {
          log.debug("Replying to relay request -SD is: "+pdq.getProviderDescription());
        }
        publishChange(pdq);
      }
    }
  }

}

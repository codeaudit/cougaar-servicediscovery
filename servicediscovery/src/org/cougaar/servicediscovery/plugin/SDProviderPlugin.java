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
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.servicediscovery.SDDomain;
import org.cougaar.servicediscovery.SDFactory;
import org.cougaar.servicediscovery.description.ServiceContract;
import org.cougaar.servicediscovery.description.ServiceRequest;
import org.cougaar.servicediscovery.transaction.ServiceContractRelay;
import org.cougaar.servicediscovery.util.StatusChangeMessage;
import org.cougaar.util.UnaryPredicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * SDProviderPlugin generates the LineageLists for the Agent.
 */
public class SDProviderPlugin extends SimplePlugin
{
  private IncrementalSubscription mySelfOrgSubscription;
  private IncrementalSubscription myServiceContractRelaySubscription;
  private IncrementalSubscription myMessageSubscription;

  private String myAgentName;

  private SDFactory mySDFactory;

  private UnaryPredicate myServiceContractRelayPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof ServiceContractRelay) {
	ServiceContractRelay relay = (ServiceContractRelay) o;
	return (relay.getProviderName().equals(myAgentName));
      } else {
	return false;
      }
    }
  };

  private UnaryPredicate mySelfOrgPred = new UnaryPredicate() {
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

  private UnaryPredicate myMessagePred = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof StatusChangeMessage);
    }
  };

  protected void setupSubscriptions() {
    myServiceContractRelaySubscription = (IncrementalSubscription) subscribe(myServiceContractRelayPred);
    mySelfOrgSubscription = (IncrementalSubscription) subscribe(mySelfOrgPred);
    myMessageSubscription = (IncrementalSubscription)subscribe(myMessagePred);

    mySDFactory = (SDFactory) getFactory(SDDomain.SD_NAME);

    myAgentName = getBindingSite().getAgentIdentifier().toString();
  }

  public void execute() {
    if (myServiceContractRelaySubscription.hasChanged()) {

      Collection addedRelays =
	myServiceContractRelaySubscription.getAddedCollection();
      for (Iterator adds = addedRelays.iterator(); adds.hasNext();) {
	ServiceContractRelay relay = (ServiceContractRelay) adds.next();

	ServiceRequest serviceRequest = relay.getServiceRequest();

	// Just say yes
	ArrayList contractPreferences =
	  new ArrayList(serviceRequest.getServicePreferences().size());
	for (Iterator iterator = serviceRequest.getServicePreferences().iterator();
	     iterator.hasNext();) {
	  Preference requestPreference = (Preference) iterator.next();
	  Preference contractPreference =
	    getFactory().newPreference(requestPreference.getAspectType(),
				       requestPreference.getScoringFunction(),
				       requestPreference.getWeight());
	  contractPreferences.add(contractPreference);
	}

	ServiceContract serviceContract =
	  mySDFactory.newServiceContract(getSelfOrg(),
					 serviceRequest.getServiceRole(),
					 contractPreferences);
	relay.setServiceContract(serviceContract);
	publishChange(relay);
      }

      // Not currently handling modifications or removes
      // On remove -
      //   should publish remove lineage relay to superior
      //   remove published lineage list associated with the superior
    }

    //see if you got a message to disrupt service
    if(myMessageSubscription.hasChanged()) {
      Iterator it = myMessageSubscription.getChangedCollection().iterator();
      while(it.hasNext()) {
        StatusChangeMessage m = (StatusChangeMessage)it.next();
        //only proceed if the registry has already been updated to reflect service disruption
        if(m.registryUpdated()) {
          Iterator contracts = myServiceContractRelaySubscription.getCollection().iterator();
          while(contracts.hasNext()) {
            ServiceContractRelay contractRelay = (ServiceContractRelay)contracts.next();
            //find the service contract relay with matching role to the service disrupted
            if(contractRelay.getServiceRequest().getServiceRole().toString().equals(m.getRole())) {
              //revoke the service contract
              contractRelay.setServiceContract(getRevokedServiceContract(contractRelay));
              publishChange(contractRelay);
            }
          }
        }
      }
    }
  }

  private ServiceContract getRevokedServiceContract(ServiceContractRelay relay) {
    ServiceContract sc = relay.getServiceContract();
    mySDFactory.revokeServiceContract(sc);
    return sc;
  }

  protected Organization getSelfOrg() {
    for (Iterator iterator = mySelfOrgSubscription.iterator();
	 iterator.hasNext();) {
      return (Organization) iterator.next();
    }

    return null;
  }

}








/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

package org.cougaar.servicediscovery.lp;

import org.cougaar.core.blackboard.*;
import org.cougaar.core.domain.EnvelopeLogicProvider;
import org.cougaar.core.domain.LogicProvider;
import org.cougaar.core.domain.RootPlan;
import org.cougaar.core.mts.MessageAddress;

import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.LocalPG;
import org.cougaar.planning.ldm.asset.PropertyGroup;
import org.cougaar.planning.ldm.asset.PropertyGroupSchedule;

import org.cougaar.planning.ldm.plan.HasRelationships;
import org.cougaar.planning.ldm.plan.RelationshipSchedule;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.LogPlan;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

import org.cougaar.servicediscovery.SDFactory;
import org.cougaar.servicediscovery.description.ServiceContractRelationship;
import org.cougaar.servicediscovery.transaction.ServiceContractRelay;

import java.util.*;

/** ServiceContractLP is a "LogPlan Logic Provider":
 *
 * it provides the logic to keep Org Asset RelationshipSchedules
 * in synch with their ServiceContracts.
 * Relies on RelayLP to handle restart reconciliation.
 **/

public class ServiceContractLP implements LogicProvider, EnvelopeLogicProvider {
  private static Logger logger = Logging.getLogger(ServiceContractLP.class);
  private final LogPlan logplan;
  private final RootPlan rootplan;
  private final MessageAddress self;
  private final PlanningFactory ldmf;

  public ServiceContractLP(LogPlan logplan, RootPlan rootplan, MessageAddress self,
                           PlanningFactory ldmf) {
    this.logplan = logplan;
    this.rootplan = rootplan;
    this.self = self;
    this.ldmf = ldmf;
  }

  public void init() {
  }

  /**
   * @param o the Envelopetuple,
   *          where tuple.object
   *             == PlanElement with an Allocation to an cluster ADDED to LogPlan
   *
   * If the test returned true i.e. it was an ServiceContractRelay,
   * update local org assets and relationship schedules
   **/
  public void execute(EnvelopeTuple o, Collection changes) {
    Object obj = o.getObject();
    if (obj instanceof ServiceContractRelay) {
      ServiceContractRelay relay = (ServiceContractRelay) obj;

      // Is this the providing agent?
      Asset provider = relay.getProvider();
      if ((provider != null) &&
        (provider.getClusterPG().getMessageAddress().equals(self))){
        localProviderUpdate(o, (ServiceContractRelay) obj);
      }

      // If this the client agent
      Asset client = relay.getClient();
      if ((client != null) &&
        (client.getClusterPG().getMessageAddress().equals(self)) &&
        (relay.getServiceContract() != null)) {
        if (logger.isDebugEnabled()) logger.debug("Received " + relay);
        localClientUpdate(relay);
      }
    }
  }

  // Existing Relay logic should suffice.

  private void localProviderUpdate(EnvelopeTuple tuple, ServiceContractRelay relay) {
   // ServiceContract contract = relay.getServiceContract();
   // ServiceRequest request = relay.getServiceRequest();

    Asset provider = logplan.findAsset(relay.getProvider());
    if (provider == null) {
      logger.error(self + ": unable to process ServiceContractRelay - " +
		   relay.getUID() + " provider - " + relay.getProvider() +
		   " - is not local to this agent.");
      return;
    } else if (provider == relay.getProvider()) {
      logger.error(self + ": Assets in ServiceContractRelay must be " +
		   " clones. ServiceContractRelay - " + relay.getUID() +
		   " - references assets in the log plan.");
      return;
    }

    Asset client = relay.getClient();
    Asset localClient = logplan.findAsset(client);

    if (localClient == null) {
      client = ldmf.cloneInstance(client);
      if (related(client)){
        ((HasRelationships)client).setRelationshipSchedule(ldmf.newRelationshipSchedule((HasRelationships)client));
      }
    } else {
        client = localClient;

      if (localClient == relay.getClient()) {
        logger.error(self + ": Assets in ServiceContractRelay must be " +
		     " clones. ServiceContractRelay - " + relay.getUID() +
		     " - references assets in the log plan.");
      }
    }

    if (related(provider) && related(client)) {
      if (tuple.isChange() || tuple.isRemove()) {
	removeExistingRelationships(relay,
                                    (HasRelationships) provider,
                                    (HasRelationships) client);
      }

      if (tuple.isAdd() || tuple.isChange()) {
        addRelationships(relay,
			 (HasRelationships) provider,
			 (HasRelationships) client);
      }
    }

    Collection changes = new ArrayList();
    changes.add(new RelationshipSchedule.RelationshipScheduleChangeReport());
    rootplan.change(provider, changes); // change this to root plan

    if (localClient == null) {
      rootplan.add(client);
    } else {
      changes.clear();
      changes.add(new RelationshipSchedule.RelationshipScheduleChangeReport());
      rootplan.change(client, changes);
    }

    // Clear client and provider relationship, role, and available schedules to ensure
    // that there are no references to other organizations.
    clearSchedule(relay.getClient());
    clearSchedule(relay.getProvider());
  }

  private void localClientUpdate(ServiceContractRelay relay) {
    // figure out the assignee
   // ServiceContract contract = relay.getServiceContract();
   // ServiceRequest request = relay.getServiceRequest();

    Asset client = logplan.findAsset(relay.getClient()); // local client instance

    if (client == null) {
      logger.error(self + ": Unable to find client asset " +
                   relay.getClient() + " in " + self);
      return;
    }



    // figure out the asset being transferred
    Asset provider = logplan.findAsset(relay.getProvider());
    boolean newProvider;

    if (provider == null) {
      // Clone to ensure that we don't end up with cross cluster asset
      // references
      newProvider = true;
      provider = ldmf.cloneInstance(relay.getProvider());
      if (related(provider)) {
	HasRelationships hasRelationships = (HasRelationships) provider;
        hasRelationships.setLocal(false);
        hasRelationships.setRelationshipSchedule(ldmf.newRelationshipSchedule(hasRelationships));
      }
    } else {
      newProvider = false;
    }

    boolean updateRelationships = related(provider) && related(client);

    //Only munge relationships pertinent to the transfer - requires that
    //both receiving and transferring assets have relationship schedules
    if (updateRelationships) {
      removeExistingRelationships(relay,
				  (HasRelationships) provider,
				  (HasRelationships) client);

      addRelationships(relay,
		       (HasRelationships) provider,
		       (HasRelationships) client);

      Collection changeReports = new ArrayList();
      changeReports.add(new RelationshipSchedule.RelationshipScheduleChangeReport());
      rootplan.change(client, changeReports);
    }


    if (!newProvider) {
      // If we already had a matching asset - update with property groups
      // from the asset transfer.
      Vector transferredPGs = relay.getProvider().fetchAllProperties();

      for (Iterator pgIterator = transferredPGs.iterator();
           pgIterator.hasNext();) {
        Object next = pgIterator.next();

        //Don't propagate LocalPGs
        if (!(next instanceof LocalPG)) {
          if (next instanceof PropertyGroup) {
            provider.addOtherPropertyGroup((PropertyGroup) next);
          } else if (next instanceof PropertyGroupSchedule) {
            provider.addOtherPropertyGroupSchedule((PropertyGroupSchedule) next);
          } else {
            logger.error(self + ": unrecognized property type - " +
                         next + " - on provider " + provider);
          }
        }
      }
    }


    // publish the added or changed provider
    if (newProvider) {            // add it if it wasn't already there
      rootplan.add(provider);
    } else {
      if (updateRelationships) {
        Collection changeReports = new ArrayList();
        changeReports.add(new RelationshipSchedule.RelationshipScheduleChangeReport());
        rootplan.change(provider, changeReports);
      } else {
        rootplan.change(provider, null);
      }
    }
  }

  private final static boolean related(Asset a) {
    return (a instanceof HasRelationships);
  }

  private void addRelationships(ServiceContractRelay relay,
				HasRelationships provider,
				HasRelationships client) {

    if(relay.getServiceContract().isRevoked()) {
      return;
    }

    // Add ServiceContract relationships to local assets
    Collection localRelationships =
      convertToLocalRelationships(relay,
                                  provider,
                                  client);

    RelationshipSchedule providerSchedule =
      ((HasRelationships)provider).getRelationshipSchedule();
    providerSchedule.addAll(localRelationships);

    RelationshipSchedule clientSchedule =
      ((HasRelationships)client).getRelationshipSchedule();
    // TODO: IDEA says this type cast is redundant, need to test this out
    clientSchedule.addAll(localRelationships);
  }

  private void removeExistingRelationships(final ServiceContractRelay relay,
                                           HasRelationships provider,
                                           HasRelationships client) {

    RelationshipSchedule clientSchedule =
      client.getRelationshipSchedule();
    RelationshipSchedule providerSchedule =
      provider.getRelationshipSchedule();

    Collection remove =
      providerSchedule.getMatchingRelationships(new UnaryPredicate() {
	public boolean execute(Object obj) {
	  if (obj instanceof ServiceContractRelationship) {
	    return ((ServiceContractRelationship) obj).getServiceContractUID().equals(relay.getUID());
	  } else {
	    return false;
	  }
	}
      });

    providerSchedule.removeAll(remove);

    remove =
      clientSchedule.getMatchingRelationships(new UnaryPredicate() {
	public boolean execute(Object obj) {
	  if (obj instanceof ServiceContractRelationship) {
	    return ((ServiceContractRelationship) obj).getServiceContractUID().equals(relay.getUID());
	  } else {
	    return false;
	  }
	}
      });

    clientSchedule.removeAll(remove);
  }

  protected Collection convertToLocalRelationships(ServiceContractRelay relay,
						   HasRelationships localProvider,
						   HasRelationships client) {
    Collection relationships = new ArrayList();

    relationships.add(SDFactory.newServiceContractRelationship(relay, localProvider, client));

    return relationships;
  }

  // Clear relationship, role and availble schedules to ensure that there
  // are no dangling references to other organizations.
  private void clearSchedule(Asset asset) {
    if (related(asset)) {
      ((HasRelationships ) asset).setRelationshipSchedule(null);
    }

    if (asset.getRoleSchedule() != null) {
      asset.getRoleSchedule().clear();

      if (asset.getRoleSchedule().getAvailableSchedule() != null) {
        asset.getRoleSchedule().getAvailableSchedule().clear();
      }
    }
  }

}










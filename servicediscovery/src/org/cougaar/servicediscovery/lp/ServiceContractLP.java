/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
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

package org.cougaar.servicediscovery.lp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

import org.cougaar.core.blackboard.EnvelopeTuple;
import org.cougaar.core.domain.EnvelopeLogicProvider;
import org.cougaar.core.domain.LogicProvider;
import org.cougaar.core.domain.RootPlan;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.planning.ldm.LogPlan;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.LocalPG;
import org.cougaar.planning.ldm.asset.PropertyGroup;
import org.cougaar.planning.ldm.asset.PropertyGroupSchedule;
import org.cougaar.planning.ldm.plan.HasRelationships;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.RelationshipSchedule;
import org.cougaar.servicediscovery.SDFactory;
import org.cougaar.servicediscovery.description.ServiceContractRelationship;
import org.cougaar.servicediscovery.transaction.ServiceContractRelay;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

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
        if (logger.isDebugEnabled()) {
	  logger.debug(self + ": Received " + relay);
	}
        localClientUpdate(relay);
      }
    }
  }

  // Existing Relay logic should suffice.

  private void localProviderUpdate(EnvelopeTuple tuple, ServiceContractRelay relay) {
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
	return;
      }
    }

    boolean updateRelationships = false;

    if (related(provider) && related(client)) {
      if (logger.isDebugEnabled()) {
	logger.debug(self + ": localProviderUpdate() relay = " + relay +
		     " tuple.isAdd() = " + tuple.isAdd() +
		     " tuple.isChange() = " + tuple.isChange() +
		     " tuple.isRemove() = " + tuple.isRemove());
      }
      updateRelationships = tuple.isAdd() || tuple.isRemove();

      Collection localRelationships =
	convertToLocalRelationships(relay,
				    (HasRelationships) provider,
				    (HasRelationships) client);
      
      if (tuple.isChange()) {
	updateRelationships = 
	  (localScheduleUpdateRequired(localRelationships, (HasRelationships) provider) || 
	   localScheduleUpdateRequired(localRelationships, (HasRelationships) client)); 
      }

      if (logger.isDebugEnabled()) {
	logger.debug(self + ": localProviderUpdate() updateRelationships = " + 
		     updateRelationships);
      }

      if (updateRelationships) {
	removeExistingRelationships(relay,
                                    (HasRelationships) provider,
                                    (HasRelationships) client);

        addRelationships(localRelationships,
			 (HasRelationships) provider,
			 (HasRelationships) client);
      }
    }

    
    if (updateRelationships) {
      Collection changes = new ArrayList();
      changes.add(new RelationshipSchedule.RelationshipScheduleChangeReport());
      if (logger.isInfoEnabled()) {
	logger.info(self + ": localProviderUpdate() changed provider :" + 
		    provider);
      }
      rootplan.change(provider, changes); // change this to root plan
    }

    if (localClient == null) {
      rootplan.add(client);
    } else if (updateRelationships) {
      Collection changes = new ArrayList();
      changes.add(new RelationshipSchedule.RelationshipScheduleChangeReport());
      if (logger.isInfoEnabled()) {
	logger.info(self + ": localProviderUpdate() changed client :" + 
		    client);
      }
      rootplan.change(client, changes);
    }

    // Clear client and provider relationship, role and available schedules to
    // ensure that there are no references to other organizations.
    clearSchedule(relay.getClient());
    clearSchedule(relay.getProvider());
  }

  private void localClientUpdate(ServiceContractRelay relay) {
    Asset client = logplan.findAsset(relay.getClient()); // local client instance

    if (client == null) {
      logger.error(self + ": unable to process ServiceContractRelay - " +
		   relay.getUID() + " client - " + relay.getClient() +
		   " - is not local to this agent.");
      return;
    } else if (client == relay.getClient()) {
      logger.error(self + ": Assets in ServiceContractRelay must be " +
		   " clones. ServiceContractRelay - " + relay.getUID() +
		   " - references assets in the log plan.");
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
      if (provider == relay.getProvider()) {
	logger.error(self + ": Assets in ServiceContractRelay must be " +
		     " clones. ServiceContractRelay - " + relay.getUID() +
		     " - references assets in the log plan.");
	return;
      }
      newProvider = false;
    }

    if (logger.isDebugEnabled()) {
      logger.debug(self + ": localClientUpdate() relay = " + relay);
    }

    boolean updateRelationships = false;

    //Only munge relationships pertinent to the transfer - requires that
    //both receiving and transferring assets have relationship schedules
    if (related(provider) && related(client)) {
      Collection localRelationships =
	convertToLocalRelationships(relay,
				    (HasRelationships) provider,
				    (HasRelationships) client);
      updateRelationships = 
	(localScheduleUpdateRequired(localRelationships, 
				     (HasRelationships) provider) || 
	 localScheduleUpdateRequired(localRelationships, 
				     (HasRelationships) client)); 
      

      if (updateRelationships) {
	removeExistingRelationships(relay,
				  (HasRelationships) provider,
				  (HasRelationships) client);

	addRelationships(localRelationships,
		       (HasRelationships) provider,
		       (HasRelationships) client);
	
	if (logger.isInfoEnabled()) {
	  logger.info(self + ": localClientUpdate() changed client: " + 
		      client);
	}

	Collection changeReports = new ArrayList();
	changeReports.add(new RelationshipSchedule.RelationshipScheduleChangeReport());
	rootplan.change(client, changeReports);
      }
    }


    boolean updatePGs = false;

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
	    PropertyGroup transferredPG = (PropertyGroup) next;
	    PropertyGroup localPG = 
	      provider.searchForPropertyGroup(transferredPG.getPrimaryClass());
	    if ((localPG == null) ||
		(!localPG.equals(transferredPG))) {
	      if (logger.isDebugEnabled()) {
		logger.debug(self + ": localProviderUpdate() pgs not equal " +
			     " localPG = " + localPG +
			     " transferredPG = " + transferredPG);
	      }
	      provider.addOtherPropertyGroup((PropertyGroup) next);
	      updatePGs = true;
	    } else if (next instanceof PropertyGroupSchedule) {
	      PropertyGroupSchedule transferredPGSchedule = 
		(PropertyGroupSchedule) next;
	      PropertyGroupSchedule localPGSchedule = 
		provider.searchForPropertyGroupSchedule(transferredPGSchedule.getClass());
	      if ((localPGSchedule == null) ||
		  (!localPGSchedule.equals(transferredPGSchedule))) {
		if (logger.isDebugEnabled()) {
		  logger.debug(self + ": localProviderUpdate() pgschedules not equal " +
			       " localPGSchedule = " + localPGSchedule +
			       " transferredPG = " + transferredPGSchedule);
		}
		provider.addOtherPropertyGroupSchedule((PropertyGroupSchedule) next);
		updatePGs = true;
	      } else {
		logger.error(self + ": unrecognized property type - " +
			     next + " - on provider " + provider);
	      }
	    }
	  }
	}
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug(self + ": localClientUpdate() updateRelationships = " +
		   updateRelationships + "updatePGs = " + updatePGs);
    }


    // publish the added or changed provider
    if (newProvider) {            // add it if it wasn't already there
      rootplan.add(provider);
    } else {
      if (updateRelationships) {
        Collection changeReports = new ArrayList();
        changeReports.add(new RelationshipSchedule.RelationshipScheduleChangeReport());
	if (logger.isInfoEnabled()) {
	  logger.info(self + ": localClientUpdate() changed provider: " + provider);
	}
        rootplan.change(provider, changeReports);
      } else if (updatePGs) {
	if (logger.isInfoEnabled())
	  logger.info(self + ": localClientUpdate() changed provider: " + provider);
        rootplan.change(provider, null);
      }
    }
  }

  private final static boolean related(Asset a) {
    return (a instanceof HasRelationships);
  }

  private boolean localScheduleUpdateRequired(Collection localRelationships,
					      HasRelationships localAsset) {

    Collection localMatches = new ArrayList();
    RelationshipSchedule localRelationshipSchedule = 
      localAsset.getRelationshipSchedule();

    // Find all existing elationships for the same service contract
    for (Iterator iterator = localRelationships.iterator();
         iterator.hasNext();) {
      final ServiceContractRelationship relationship = 
	(ServiceContractRelationship) iterator.next();
      
      Collection matching = 
	localRelationshipSchedule.getMatchingRelationships(new UnaryPredicate() {
	  public boolean execute(Object obj) {
	    if (obj instanceof ServiceContractRelationship) {
	      return ((ServiceContractRelationship) obj).getServiceContractUID().equals(relationship.getServiceContractUID());
	    } else {
	      return false;
	    }
	  }
	});
	  
      localMatches.addAll(matching);
    }

    if (localMatches.size() != localRelationships.size()) {
      return true;
    }

    // Compare entries
    for (Iterator iterator = localRelationships.iterator();
         iterator.hasNext();) {
      ServiceContractRelationship relationship = 
	(ServiceContractRelationship) iterator.next();
      boolean found = false;

      for (Iterator existingIterator = localMatches.iterator();
	   existingIterator.hasNext();) {
	ServiceContractRelationship existing = 
	  (ServiceContractRelationship) existingIterator.next();
	
	if (relationship.equals(existing)) {
	  found = true;
	  localMatches.remove(existing);
	  break;
	}
      }

      if (!found) {
	return true;
      }
    }
    
    return false;
  }

  private void addRelationships(Collection localRelationships,
				HasRelationships provider,
				HasRelationships client) {

    RelationshipSchedule providerSchedule = provider.getRelationshipSchedule();
    providerSchedule.addAll(localRelationships);

    RelationshipSchedule clientSchedule = client.getRelationshipSchedule();
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


    if(relay.getServiceContract().isRevoked()) {
      return relationships;
    }

    try {
    relationships.add(SDFactory.newServiceContractRelationship(relay, 
							       localProvider, 
							       client));
    } catch (IllegalArgumentException iae) {
      logger.error("Unable to create relationship provider " + localProvider + 
		   " and client " + client,
		   iae);
    }
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










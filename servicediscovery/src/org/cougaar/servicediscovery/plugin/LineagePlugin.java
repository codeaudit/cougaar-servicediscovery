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
import java.util.Iterator;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.service.LoggingService;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.servicediscovery.Constants;
import org.cougaar.servicediscovery.SDDomain;
import org.cougaar.servicediscovery.SDFactory;
import org.cougaar.servicediscovery.description.Lineage;
import org.cougaar.servicediscovery.description.LineageImpl;
import org.cougaar.servicediscovery.transaction.LineageRelay;
import org.cougaar.util.UnaryPredicate;

/**
 * LineagePlugin generates the Lineages for the Agent.
 */
public class LineagePlugin extends SimplePlugin
{
  private IncrementalSubscription myReportForDutySubscription;
  private IncrementalSubscription mySuperiorLineageRelaySubscription;
  private IncrementalSubscription mySubordinateLineageRelaySubscription;
  private IncrementalSubscription myLineageSubscription;

  private String myAgentName;

  private LoggingService myLoggingService;
  private SDFactory mySDFactory;

  private UnaryPredicate myLineagePred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Lineage) {
	return true;
      } else {
	return false;
      }
    }
  };

  private UnaryPredicate myReportForDutyPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Task) {
	Task task = (Task) o;
	return task.getVerb().equals(org.cougaar.glm.ldm.Constants.Verb.ReportForDuty);
      } else {
        return false;
      }
    }
  };

  private UnaryPredicate mySubordinateLineageRelayPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof LineageRelay) {
	LineageRelay relay = (LineageRelay) o;
	return (relay.getAgentName().equals(myAgentName));
      } else {
	return false;
      }
    }
  };

  private UnaryPredicate mySuperiorLineageRelayPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof LineageRelay) {
	LineageRelay relay = (LineageRelay) o;
	return (!relay.getAgentName().equals(myAgentName));
      } else {
	return false;
      }
    }
  };

  public void setLoggingService(LoggingService loggingService) {
    myLoggingService = loggingService;
  }
      
  protected void setupSubscriptions() {
    myAgentName = getBindingSite().getAgentIdentifier().toString();

    myReportForDutySubscription = 
      (IncrementalSubscription) subscribe(myReportForDutyPred);
    mySubordinateLineageRelaySubscription = 
      (IncrementalSubscription) subscribe(mySubordinateLineageRelayPred);
    mySuperiorLineageRelaySubscription = 
      (IncrementalSubscription) subscribe(mySuperiorLineageRelayPred);
    myLineageSubscription = 
      (IncrementalSubscription) subscribe(myLineagePred);

    mySDFactory = (SDFactory) getFactory(SDDomain.SD_NAME);

    // Add initial seed for command lineage?
    boolean addSeed = true;
    for (Iterator iterator = myLineageSubscription.iterator();
	 iterator.hasNext();) {
      Lineage lineage = (Lineage) iterator.next();
      if (lineage.getType() == Lineage.ADCON) {
	addSeed = false;
	break;
      }
    }

    if (addSeed) {
      if (myLoggingService.isDebugEnabled()) {
	myLoggingService.debug(getAgentIdentifier() + 
			       " publishing initial administrative command lineage.");
      }

      ArrayList list = new ArrayList();
      list.add(myAgentName);
      Lineage lineage =
	mySDFactory.newLineage(Lineage.ADCON, list);
      publishAdd(lineage);
      lineage =
	mySDFactory.newLineage(Lineage.OPCON, list);
      publishAdd(lineage);
    }
  }

  public void execute() {
    if (myReportForDutySubscription.hasChanged()) {

      Collection addedRFDs = myReportForDutySubscription.getAddedCollection();
      for (Iterator adds = addedRFDs.iterator(); adds.hasNext();) {
	Task task = (Task) adds.next();
	Collection roles =
	  (Collection) findIndirectObject(task, Constants.Preposition.AS);

	for (Iterator iterator = roles.iterator(); iterator.hasNext();) {
	  Role role = (Role) iterator.next();

	  if (role.equals(Constants.Role.SUPPORTSUBORDINATE)) {
	    addSupportLineage(task);
	  } else {
	    querySuperior(task);
	  }
	}
      }

      // Not currently handling modifications or removes
      // On remove -
      //   should publish remove lineage relay to superior
      //   remove published lineage list associated with the superior
    }

    if (mySubordinateLineageRelaySubscription.hasChanged()) {
      Collection addedSubordinateRelays =
	mySubordinateLineageRelaySubscription.getAddedCollection();

      for (Iterator adds = addedSubordinateRelays.iterator();
	   adds.hasNext();) {
	updateSubordinate((LineageRelay) adds.next());
      }

      // Not interested in modifications or removes
    }

    if (mySuperiorLineageRelaySubscription.hasChanged()) {

      Collection changedSuperiorRelays =
	mySuperiorLineageRelaySubscription.getChangedCollection();
      for (Iterator changes = changedSuperiorRelays.iterator();
	   changes.hasNext();) {
	updateLineage((LineageRelay) changes.next());
      }

      // Not handling adds/removes at this point
      // I believe both add/removes should only be initiated by this
      // plugin.
    }

    if (myLineageSubscription.hasChanged()) {
      if (myLoggingService.isDebugEnabled()) {
	myLoggingService.debug(myAgentName +
			       " lineage list subscription has changed.");
      }
      updateSubordinates();
    }
  }

  protected void addSupportLineage(Task rfdTask) {

    ArrayList list = new ArrayList();
    Asset superior =
      (Asset) findIndirectObject(rfdTask, Constants.Preposition.FOR);
    String scaName = superior.getClusterPG().getMessageAddress().toString();

    list.add(scaName);
    // Okay to support oneself but don't add duplicate entries to the
    // lineage
    if (!myAgentName.equals(scaName)) {
      list.add(myAgentName);
    }


    Lineage lineage = mySDFactory.newLineage(Lineage.SUPPORT, list);
    publishAdd(lineage);

    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(getAgentIdentifier() + 
			     "addSupportLineage: publishAdd of " +
			     lineage.getUID());
    }

  }

  protected void querySuperior(Task rfdTask) {
    // Missing command support logic - look at roles specified in the AS
    // prep.
    Asset superior = (Asset) findIndirectObject(rfdTask, Constants.Preposition.FOR);
    LineageRelay relay =
      mySDFactory.newLineageRelay(superior.getClusterPG().getMessageAddress());
    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(getAgentIdentifier() + 
			     " querySuperior: publishAdd of " +
			     relay.getUID());
    }

    publishAdd(relay);
  }

  protected void updateLineage(LineageRelay relay) {
    for (Iterator relayLineageIterator = relay.getLineages().iterator();
	 relayLineageIterator.hasNext();) {
      Lineage relayLineage = (Lineage) relayLineageIterator.next();
      Lineage localLineage = null;

      for (Iterator localLineageIterator = myLineageSubscription.iterator();
	   localLineageIterator.hasNext();) {
        Lineage lineage = 
	  (Lineage) localLineageIterator.next();
	if (relayLineage.getType() == lineage.getType()) {
          if(relayLineage.getType() == Lineage.SUPPORT) {
            if(lineage.getRoot().equals(relayLineage.getRoot())) {
              localLineage = lineage;
              break;
            }
          } else {
            localLineage = lineage;
            break;
          }
	}
      }


      ArrayList updatedList = new ArrayList(); 
      updatedList.addAll(relayLineage.getList());
      updatedList.add(myAgentName);


      if (localLineage != null) {
	// Compare lists before publish changing. Required because restart
	// processing for Relays resends all relays.
	if (!updatedList.equals(localLineage.getList())) {
	  ((LineageImpl) localLineage).setList(updatedList);
	  
	  if (myLoggingService.isDebugEnabled()) {
	    myLoggingService.debug(getAgentIdentifier() + 
				   " updateLineage: publishChange of " +
				   localLineage);
	  }
	  
	  publishChange(localLineage);
	}
      } else {
	localLineage = mySDFactory.newLineage(relayLineage.getType(),
					      updatedList);

	if (myLoggingService.isDebugEnabled()) {
	  myLoggingService.debug(getAgentIdentifier() + 
				 " updateLineage: publishAdd of " +
				 localLineage);
	}

	publishAdd(localLineage);
      }
    }
  }

  protected void updateSubordinate(LineageRelay relay) {
    ArrayList lineages = new ArrayList();

    for (Iterator iterator = myLineageSubscription.iterator();
	 iterator.hasNext();) {
      Lineage localLineage = (Lineage) iterator.next();
      lineages.add(mySDFactory.copyLineage(localLineage));
      if (myLoggingService.isDebugEnabled()) {
	myLoggingService.debug(myAgentName +
			       ":updateSubordinate localLineage " +
			       localLineage +
			       " type: " + localLineage.getType());
     }
    }

    relay.setLineages(lineages);


    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(getAgentIdentifier() + 
			     " updateSubordinate: publishChange of " +
			     relay.getUID());
    }

    publishChange(relay);
  }

  protected void updateSubordinates() {
    for (Iterator iterator = mySubordinateLineageRelaySubscription.iterator();
	 iterator.hasNext();) {
      updateSubordinate((LineageRelay) iterator.next());
    }
  }

  protected Object findIndirectObject(Task task, String prep) {
    PrepositionalPhrase pp = task.getPrepositionalPhrase(prep);
    if (pp == null) {
      myLoggingService.error("Didn't find a single \"" + prep +
			     "\" Prepositional Phrase in " + task);
      return null;
    } else {
      return pp.getIndirectObject();
    }
  }
}



/*
 * <copyright>
 *  Copyright 2002 BBNT Solutions, LLC
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
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.servicediscovery.Constants;
import org.cougaar.servicediscovery.SDDomain;
import org.cougaar.servicediscovery.SDFactory;
import org.cougaar.servicediscovery.description.LineageList;
import org.cougaar.servicediscovery.description.SupportLineageList;
import org.cougaar.servicediscovery.transaction.LineageListRelay;
import org.cougaar.util.UnaryPredicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * LineagePlugin generates the LineageLists for the Agent.
 */
public class LineagePlugin extends SimplePlugin
{
  private IncrementalSubscription myReportForDutySubscription;
  private IncrementalSubscription mySuperiorLineageListRelaySubscription;
  private IncrementalSubscription mySubordinateLineageListRelaySubscription;
  private IncrementalSubscription myLineageListSubscription;

  private String myAgentName;

  private LoggingService myLoggingService;
  private SDFactory mySDFactory;

  private UnaryPredicate myLineageListPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof LineageList);
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

  private UnaryPredicate mySubordinateLineageListRelayPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof LineageListRelay) {
	LineageListRelay relay = (LineageListRelay) o;
	return (relay.getAgentName().equals(myAgentName));
      } else {
	return false;
      }
    }
  };

  private UnaryPredicate mySuperiorLineageListRelayPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof LineageListRelay) {
	LineageListRelay relay = (LineageListRelay) o;
	return (!relay.getAgentName().equals(myAgentName));
      } else {
	return false;
      }
    }
  };

  protected void setupSubscriptions() {
    myReportForDutySubscription = (IncrementalSubscription) subscribe(myReportForDutyPred);
    mySubordinateLineageListRelaySubscription = (IncrementalSubscription) subscribe(mySubordinateLineageListRelayPred);
    mySuperiorLineageListRelaySubscription = (IncrementalSubscription) subscribe(mySuperiorLineageListRelayPred);
    myLineageListSubscription = (IncrementalSubscription) subscribe(myLineageListPred);
    myLoggingService =
      (LoggingService) getBindingSite().getServiceBroker().getService(this,
								      LoggingService.class,
								      null);
    mySDFactory = (SDFactory) getFactory(SDDomain.SD_NAME);

    myAgentName = getBindingSite().getAgentIdentifier().toString();

    // Add initial seed for command lineage
    LineageList commandLineage =
      mySDFactory.newLineageList(LineageList.COMMAND);
    commandLineage.add(myAgentName);
    publishAdd(commandLineage);
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

	  String echelon = Constants.getMilitaryEchelon(role);

	  if (!echelon.equals(Constants.MilitaryEchelon.UNDEFINED)) {
	    addSupportLineage(echelon, task);
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

    if (mySubordinateLineageListRelaySubscription.hasChanged()) {
      Collection addedSubordinateRelays =
	mySubordinateLineageListRelaySubscription.getAddedCollection();

      for (Iterator adds = addedSubordinateRelays.iterator();
	   adds.hasNext();) {
	updateSubordinate((LineageListRelay) adds.next());
      }

      // Not interested in modifications or removes
    }

    if (mySuperiorLineageListRelaySubscription.hasChanged()) {

      Collection changedSuperiorRelays =
	mySuperiorLineageListRelaySubscription.getChangedCollection();
      for (Iterator changes = changedSuperiorRelays.iterator();
	   changes.hasNext();) {
	updateLineage((LineageListRelay) changes.next());
      }

      // Not handling adds/removes at this point
      // I believe both add/removes should only be initiated by this
      // plugin.

      updateSubordinates();
    }
  }

  protected void addSupportLineage(String echelon, Task rfdTask) {
    // Remove any existing support lineages
    for (Iterator localListIterator = myLineageListSubscription.iterator();
	 localListIterator.hasNext();) {
      LineageList localLineage = (LineageList) localListIterator.next();
      if (localLineage instanceof SupportLineageList) {
	SupportLineageList localSupportLineage =
	  (SupportLineageList) localLineage;
	//Only room for 1
	if (localSupportLineage.countHops(localSupportLineage.getLeaf(),
					  localSupportLineage.getRoot()) == 1) {
	  // Multiple direct command superiors
	  myLoggingService.error(myAgentName +
				 " has multiple support command relationships." +
				 "  Replacing previous lineage - " +
				 localSupportLineage);
	} else {
	  myLoggingService.warn(myAgentName +
				"Replacing previous support lineage - " +
				localSupportLineage);
	}

	publishRemove(localSupportLineage);
      }

    SupportLineageList supportLineage =
      mySDFactory.newSupportLineageList(echelon);
    Asset superior =
      (Asset) findIndirectObject(rfdTask, Constants.Preposition.FOR);
    supportLineage.add(myAgentName);
    supportLineage.add(superior.getClusterPG().getMessageAddress().toString());

      publishAdd(supportLineage);
    }


    // Let all our subs know.
    updateSubordinates();
  }

  protected void querySuperior(Task rfdTask) {
    // Missing command support logic - look at roles specified in the AS
    // prep.
    Asset superior = (Asset) findIndirectObject(rfdTask, Constants.Preposition.FOR);
    LineageListRelay relay =
      mySDFactory.newLineageListRelay(superior.getClusterPG().getMessageAddress());
    publishAdd(relay);
  }

  protected void updateLineage(LineageListRelay relay) {
    for (Iterator relayListIterator = relay.getLineageLists().iterator();
	 relayListIterator.hasNext();) {
      LineageList relayLineage = (LineageList) relayListIterator.next();
      LineageList localLineage = null;


      for (Iterator localListIterator = myLineageListSubscription.iterator();
	   localListIterator.hasNext();) {
	LineageList lineage = (LineageList) localListIterator.next();
	if (relayLineage.getType() == lineage.getType()) {
	  localLineage = lineage;
	  break;
	}
      }

      boolean existingLineage;

      // Need to create a local lineage of the correct type
      if (localLineage == null) {
	localLineage =
	  mySDFactory.newLineageList(relayLineage.getType());
	existingLineage = false;
      } else {
	existingLineage = true;
      }

      if (localLineage instanceof SupportLineageList) {
	// Ignore if agent has a direct support command relationship
	if ((existingLineage) &&
	    (localLineage.countHops(localLineage.getLeaf(),
				    localLineage.getRoot()) == 1)) {
	  myLoggingService.warn(myAgentName +
				"  has a direct support command relationship." +
				" Ignoring support lineage from superior: " +
				relayLineage);
	  return;
	}


	((SupportLineageList) localLineage).setEchelonOfSupport(((SupportLineageList) relayLineage).getEchelonOfSupport());
      }

      // update localLineage
      localLineage.clear();
      localLineage.add(myAgentName);
      localLineage.addAll(relayLineage);


      if (existingLineage) {
	publishChange(localLineage);
      } else {
	publishAdd(localLineage);
      }
    }
  }

  protected void updateSubordinate(LineageListRelay relay) {
    ArrayList lineageLists = new ArrayList();

    for (Iterator iterator = myLineageListSubscription.iterator();
	 iterator.hasNext();) {
      LineageList localLineage = (LineageList) iterator.next();
      lineageLists.add(mySDFactory.copyLineageList(localLineage));
    }

    relay.setLineageLists(lineageLists);

    publishChange(relay);
  }

  protected void updateSubordinates() {
    for (Iterator iterator = mySubordinateLineageListRelaySubscription.iterator();
	 iterator.hasNext();) {
      updateSubordinate((LineageListRelay) iterator.next());
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








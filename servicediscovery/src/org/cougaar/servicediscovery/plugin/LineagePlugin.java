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
import java.util.Vector;

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
      if (o instanceof LineageList) {
	if (myLoggingService.isDebugEnabled()) {
	myLoggingService.debug(myAgentName + " lineage list: " + o +
			       " type: " + o.getClass());
	}
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

    if (myLineageListSubscription.hasChanged()) {
      if (myLoggingService.isDebugEnabled()) {
	myLoggingService.debug(myAgentName +
			       " lineage list subscription has changed.");
      }
      updateSubordinates();
    }
  }

  protected void addSupportLineage(Task rfdTask) {

    SupportLineageList supportLineage =
      mySDFactory.newSupportLineageList();
    supportLineage.add(myAgentName);

    Asset superior =
      (Asset) findIndirectObject(rfdTask, Constants.Preposition.FOR);
    String scaName = superior.getClusterPG().getMessageAddress().toString();
    // Okay to support oneself but don't add duplicate entries to the
    // lineage
    if (!myAgentName.equals(scaName)) {
      supportLineage.add(superior.getClusterPG().getMessageAddress().toString());
    }

    publishAdd(supportLineage);

    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug("addSupportLineage: publishAdd of " +
			     supportLineage.getUID());
    }
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
          if(relayLineage.getType() == LineageList.SUPPORT) {
            if(lineage.getRoot().equals(relayLineage.getRoot())) {
              localLineage = lineage;
              break;
            }
          }
          else {
            localLineage = lineage;
            break;
          }
	}
      }

      boolean existingLineage;

      if (localLineage == null) {
	localLineage =
	  mySDFactory.newLineageList(relayLineage.getType());
	existingLineage = false;
      } else {
	existingLineage = true;
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
      if (myLoggingService.isDebugEnabled()) {
	myLoggingService.debug(myAgentName +
			       ":updateSubordinates localLineage " +
			       localLineage +
			       " type: " + localLineage.getClass());
     }
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



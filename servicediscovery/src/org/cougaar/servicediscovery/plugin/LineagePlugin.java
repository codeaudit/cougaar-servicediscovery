/*
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
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

package org.cougaar.servicediscovery.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.service.LoggingService;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.servicediscovery.Constants;
import org.cougaar.servicediscovery.SDDomain;
import org.cougaar.servicediscovery.SDFactory;
import org.cougaar.servicediscovery.description.Lineage;
import org.cougaar.servicediscovery.description.LineageImpl;
import org.cougaar.servicediscovery.transaction.LineageRelay;
import org.cougaar.servicediscovery.description.LineageScheduleElement;
import org.cougaar.util.MutableTimeSpan;
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

  private ArrayList myExecuteAdds;
  private ArrayList myExecuteRemoves;
  private ArrayList myExecuteChanges;

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

    myExecuteAdds = new ArrayList();
    myExecuteRemoves = new ArrayList();
    myExecuteChanges = new ArrayList();

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
	    addCommandLineage(task); 
	  }
	}
      }

      Collection changedRFDs = 
	myReportForDutySubscription.getChangedCollection();
      for (Iterator changes = changedRFDs.iterator(); changes.hasNext();) {
	Task task = (Task) changes.next();
	modifyLineage(task);
      }

      Collection removedRFDs = 
	myReportForDutySubscription.getRemovedCollection();
      for (Iterator removes = removedRFDs.iterator(); removes.hasNext();) {
	Task task = (Task) removes.next();
	modifyLineage(task);
      }
 
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

      //Nothing required for modifications or removes
    }

    if (mySuperiorLineageRelaySubscription.hasChanged()) {

      Collection changedSuperiorRelays =
	mySuperiorLineageRelaySubscription.getChangedCollection();
      for (Iterator changes = changedSuperiorRelays.iterator();
	   changes.hasNext();) {
	updateLineage((LineageRelay) changes.next());
      }
      
      // I believe both relay add/removes should only be initiated by this
      // plugin.
    }

    if (myLineageSubscription.hasChanged()) {
      if (myLoggingService.isDebugEnabled()) {
	myLoggingService.debug(myAgentName +
			       ":execute lineage list subscription has changed.");
      }
      updateSubordinates();
    }

    // Done with all execute related publishing.
    myExecuteAdds.clear();
    myExecuteRemoves.clear();
    myExecuteChanges.clear();
  }

  protected void localPublishAdd(Object o) {
    myExecuteAdds.add(o);
    publishAdd(o);
  }

  protected void localPublishRemove(Object o) {
    myExecuteRemoves.add(o);
    publishRemove(o);
  }

  protected void localPublishChange(Object o) {
    myExecuteChanges.add(o);
    publishChange(o);
  }

  protected void localPublishChange(Object o, Collection changes) {
    myExecuteChanges.add(o);
    publishChange(o, changes);
  }
    


  protected void addCommandLineage(Task rfdTask) {
    LineageRelay relay = findRelay(rfdTask);
    Lineage localLineage = findLocalLineage(rfdTask);
    boolean addLocalLineage = (localLineage == null);

    if (addLocalLineage) {
      localLineage = createLocalLineage(rfdTask);
    }

    if (relay == null) {
      Asset superior = (Asset) findIndirectObject(rfdTask, Constants.Preposition.FOR);
      relay =
	mySDFactory.newLineageRelay(superior.getClusterPG().getMessageAddress());
      if (myLoggingService.isDebugEnabled()) {
	myLoggingService.debug(getAgentIdentifier() + 
			       ": addCommandLineage() publishAdd of " +
			       relay.getUID() + " to " + relay.getAgentName());
      }

      if (addLocalLineage) {
	localPublishAdd(localLineage);
      }
      localPublishAdd(relay);
    } else {
      Lineage relayLineage = findRelayLineage(rfdTask);
      
      Schedule updatedSchedule = null;
      if (relayLineage == null) {
	  updatedSchedule = constructLineageSchedule(localLineage, true);
      } else {
	updatedSchedule = updateLineageSchedule(localLineage,
						relayLineage,
						true);
      }
      
      if (!localLineage.getSchedule().equals(updatedSchedule)) {
	((LineageImpl) localLineage).setSchedule(updatedSchedule);
	if (addLocalLineage) {
	  localPublishAdd(localLineage);
	} else {
	  localPublishChange(localLineage);
	}
      }
    }
  }

  protected void addSupportLineage(Task rfdTask) {
    LineageImpl lineage = (LineageImpl) findLocalLineage(rfdTask);

    if (lineage != null) {
      lineage.setSchedule(constructLineageSchedule(lineage, true));
      localPublishChange(lineage);

      if (myLoggingService.isDebugEnabled()) {
	myLoggingService.debug(getAgentIdentifier() + 
			       "addSupportLineage: publishChange of " +
			       lineage.getUID());
      }
    } else {
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
      
      lineage = (LineageImpl) mySDFactory.newLineage(Lineage.SUPPORT, list);
      lineage.setSchedule(constructLineageSchedule(lineage, true));

      localPublishAdd(lineage);
      
      if (myLoggingService.isDebugEnabled()) {
	myLoggingService.debug(getAgentIdentifier() + 
			       ": addSupportLineage: publishAdd of " +
			       lineage.getUID() + " " + lineage);
      }
    }

  }

  protected Schedule constructLineageSchedule(Lineage lineage, boolean local) {
    Role lineageRole = ((lineage.getType() == Lineage.SUPPORT) && (!local)) ?
      Lineage.typeToRole(Lineage.OPCON) :
      Lineage.typeToRole(lineage.getType());					       

    /*
    Role lineageRole = ((lineage.getType() == Lineage.SUPPORT) && (local)) ?
      Lineage.typeToRole(lineage.getType())  :
      org.cougaar.glm.ldm.Constants.Role.SUPERIOR;
      */

    // Use converse for comparison to RFD tasks
    lineageRole = lineageRole.getConverse();

    List lineageList = lineage.getList();
    Schedule lineageSchedule = SDFactory.newLineageSchedule();

    
    int listLength = lineageList.size();
    String superiorName = (listLength >=2) ?
      (String) lineageList.get(listLength - 2) : null;

    if (superiorName == null) {
      if ((lineage.getType() == Lineage.SUPPORT) &&
	  (lineageList.size() == 1)) {
	// Okay to support yourself.
	superiorName = (String) lineageList.get(0);
      } else {
	myLoggingService.error(getAgentIdentifier() + 
			       ": constructLineageSchedule() lineage - " + 
			       lineage + 
			       " does not include a superior." + 
			       " Unable to construct schedule from RFD tasks.");
	return lineageSchedule;
      }
    }

    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(getAgentIdentifier() + ": constructLineageSchedule() " +
			     "looking for " + lineageRole + " RFDs to " + 
			     superiorName + " list = " + lineageList);
    }


    for (Iterator rfdIterator = myReportForDutySubscription.iterator();
	 rfdIterator.hasNext();) {
      Task task = (Task) rfdIterator.next();
      
      Asset taskSuperior =
	(Asset) findIndirectObject(task, Constants.Preposition.FOR);
      String taskSuperiorName = taskSuperior.getClusterPG().getMessageAddress().toString();
      if (taskSuperiorName.equals(superiorName)) {
	
	Collection roles =
	  (Collection) findIndirectObject(task, Constants.Preposition.AS);
	
	for (Iterator roleIterator = roles.iterator(); 
	     roleIterator.hasNext();) {
	  Role role = (Role) roleIterator.next();
	  
	  if (myLoggingService.isDebugEnabled()) {
	    myLoggingService.debug(getAgentIdentifier() + 
				   ": constructLineageSchedule() comparing " +
				   role + " to " + lineageRole);
	  }
	  if (role.equals(lineageRole)) {
	    long startTime =
	      (long) task.getPreferredValue(AspectType.START_TIME);
	    long endTime =
	      (long) task.getPreferredValue(AspectType.END_TIME);
	    
	    if (myLoggingService.isDebugEnabled()) {
	      myLoggingService.debug(getAgentIdentifier() + 
				     ": constructLineageSchedule() found a match");
	    }   
	    lineageSchedule.add(SDFactory.newLineageScheduleElement(startTime,
								    endTime));
	  }
	}
      }
    }

    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(getAgentIdentifier() + 
			     ": constructLineageSchedule() returning " +
			     lineageSchedule);
    }   
    return lineageSchedule;
  }

  protected void modifyLineage(Task rfdTask) {
    Lineage localLineage = findLocalLineage(rfdTask);
    if (localLineage == null) {
      myLoggingService.error(getAgentIdentifier() + " modifyLineage():" +
			     " unable to find matching local lineage for RFD task " + rfdTask);
      return;
    } 

    Lineage relayLineage = findRelayLineage(rfdTask);

    Schedule updatedSchedule = null;
    if (relayLineage == null) {
      updatedSchedule = constructLineageSchedule(localLineage, true);
    } else {
      updatedSchedule = updateLineageSchedule(localLineage,
					      relayLineage,
					      true);
    }
      
    if (!localLineage.getSchedule().equals(updatedSchedule)) {
      ((LineageImpl) localLineage).setSchedule(updatedSchedule);
      localPublishChange(localLineage);
    }
  }

  protected void updateLineage(LineageRelay relay) {
    Collection localLineages = getMatchingLineages(relay);
    Collection relayLineages = new ArrayList(relay.getLineages());

    // Find and update all exact lineage matches
    for (Iterator relayLineageIterator = relayLineages.iterator();
	 relayLineageIterator.hasNext();) {
      Lineage relayLineage = (Lineage) relayLineageIterator.next();

      if (myLoggingService.isDebugEnabled()) {
	myLoggingService.debug(getAgentIdentifier() + ": updateLineage() - " +
			       " looking for an exact match for " + 
			       relayLineage);
      }
      
      Lineage localLineage = findExactMatch(relayLineage, localLineages);

      if (localLineage != null) {
	localLineages.remove(localLineage);
	relayLineageIterator.remove();

	// Modify local lineage schedule if necessary
	Schedule relaySchedule = relayLineage.getSchedule();
	Schedule localSchedule = localLineage.getSchedule();
	
	if (!relaySchedule.equals(localSchedule)) {
	  Schedule modifiedSchedule = updateLineageSchedule(localLineage, 
							    relayLineage,
							    false);
	  
	  if (!modifiedSchedule.equals(localSchedule)) {
	    if (!modifiedSchedule.isEmpty()) {
	      ((LineageImpl) localLineage).setSchedule(modifiedSchedule);
	      localPublishChange(localLineage);
	    } else {
	      if (myLoggingService.isDebugEnabled()) {
		myLoggingService.debug(getAgentIdentifier() + " removing Lineage: " +
				       localLineage + " which is never active.");
	      }
	      localPublishRemove(localLineage);
	    }
	  }
	}
      }
    }

    // Find and update all the other lineages 
    for (Iterator relayLineageIterator = relayLineages.iterator();
	 relayLineageIterator.hasNext();) {
      Lineage relayLineage = (Lineage) relayLineageIterator.next();

      if (myLoggingService.isDebugEnabled()) {
	myLoggingService.debug(getAgentIdentifier() + ": updateLineage() - " +
			       " looking for a match for " + 
			       relayLineage.getType() + " " +
			       relayLineage.getList());
      }
      
      LineageImpl localLineage = 
	(LineageImpl) findMatch(relayLineage, localLineages);
      boolean newLineage = (localLineage == null);

      if (!newLineage) {
	localLineages.remove(localLineage);
      } else {
	localLineage = 
	  (LineageImpl) mySDFactory.newLineage(relayLineage.getType());
      }
      
      ArrayList list = new ArrayList(relayLineage.getList());
      list.add(myAgentName);
      localLineage.setList(list);
	
      if (newLineage) {
	localLineage.setSchedule(constructLineageSchedule(localLineage, false));
      }

      // Modify local lineage schedule if necessary
      Schedule relaySchedule = relayLineage.getSchedule();
      Schedule localSchedule = localLineage.getSchedule();
	
      if (!relaySchedule.equals(localSchedule)) {
	Schedule modifiedSchedule = updateLineageSchedule(localLineage, 
							  relayLineage,
							  false);
	((LineageImpl) localLineage).setSchedule(modifiedSchedule);
      }

      // Don't publish lineage which is never valid
      if (localLineage.getSchedule().isEmpty()) {
	if (newLineage) {
	  if (myLoggingService.isDebugEnabled()) {
	    myLoggingService.debug(getAgentIdentifier() + " ignoring Lineage: " +
				   localLineage + " which is never active.");
	  }
	} else {
	  if (myLoggingService.isDebugEnabled()) {
	    myLoggingService.debug(getAgentIdentifier() + " removing Lineage: " +
				   localLineage + " which is never active.");
	  }
	  localPublishRemove(localLineage);
	}
      } else {
	if (newLineage) {
	  if (myLoggingService.isDebugEnabled()) {
	    myLoggingService.debug(getAgentIdentifier() + ": updateLineage() " +
				   "adding lineage - " + localLineage);
	  }
	  localPublishAdd(localLineage);
	} else {
	  if (myLoggingService.isDebugEnabled()) {
	    myLoggingService.debug(getAgentIdentifier() + ": updateLineage() " +
				   "modifying lineage - " + localLineage);
	  }
	  localPublishChange(localLineage);
	}
      }
    }

    // Remove any left over local lineages - they are no longer valid.
    for (Iterator iterator = localLineages.iterator();
	 iterator.hasNext();) {
      Lineage lineage = (Lineage) iterator.next();

      // Make an exception for SCA lineages initiated by the local agent
      if ((lineage.getType() != Lineage.SUPPORT) || 
	  (!lineage.getRoot().equals(relay.getAgentName()))) {
	localPublishRemove(lineage);

	if (myLoggingService.isDebugEnabled()) {
	  myLoggingService.debug(getAgentIdentifier() + ": updateLineage() " +
			 " removing local lineage " + 
				 lineage.getType() + " " + 
				 lineage.getList() + 
				 ". Not found in relay lineages from " +
				 relay.getAgentName());
	}
      }
    }
  }

  protected void updateSubordinate(LineageRelay relay) {
    ArrayList lineages = new ArrayList();
    boolean adconFound = false;
    boolean opconFound = false;

    for (Iterator iterator = myLineageSubscription.iterator();
	 iterator.hasNext();) {
      Lineage localLineage = (Lineage) iterator.next();
      lineages.add(mySDFactory.copyLineage(localLineage));
      if (myLoggingService.isDebugEnabled()) {
	myLoggingService.debug(myAgentName +
			       ":updateSubordinate localLineage " +
			       localLineage);
      }
      if (localLineage.getType() == Lineage.ADCON) {
	adconFound = true;
      }

      if (localLineage.getType() == Lineage.OPCON) {
	opconFound = true;
      }
    }

    if (!adconFound) {
      myLoggingService.warn(myAgentName + 
                            ":updateSubordinate local ADCON Lineage not found");
      lineages.add(addLineageSeed(Lineage.ADCON));
    }

    if (!opconFound) {
      myLoggingService.warn(myAgentName + 
                            ":updateSubordinate local OPCON Lineage not found");
      lineages.add(addLineageSeed(Lineage.OPCON));
    }

    relay.setLineages(lineages);

    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(getAgentIdentifier() + 
			     " updateSubordinate: publishChange of " +
			     relay.getUID());
    }

    localPublishChange(relay);
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


  protected Schedule updateLineageSchedule(Lineage localLineage,
					   Lineage relayLineage,
					   boolean local) {
    Schedule relaySchedule = relayLineage.getSchedule();
    Schedule constructedSchedule = 
      constructLineageSchedule(localLineage, local);
    Schedule modifiedSchedule = SDFactory.newLineageSchedule();

    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(getAgentIdentifier() + ": updateLineageSchedule() " +
			     " relaySchedule = " + relayLineage + 
			     " constructedSchedule = " + constructedSchedule);
    }

    for (Iterator iterator = new ArrayList(constructedSchedule).iterator();
	 iterator.hasNext();) {
      LineageScheduleElement constructedScheduleElement = 
	(LineageScheduleElement) iterator.next();
      Collection relayScheduleElements = 
	relaySchedule.getOverlappingScheduleElements(constructedScheduleElement.getStartTime(),
						     constructedScheduleElement.getEndTime());
      
      if (relayScheduleElements.size() > 0) {
	for (Iterator intersectingIterator = relayScheduleElements.iterator();
	     intersectingIterator.hasNext();) {
	  LineageScheduleElement relayScheduleElement = 
	    (LineageScheduleElement) intersectingIterator.next();
	  if (relayScheduleElement.equals(constructedScheduleElement)) {
	    modifiedSchedule.add(constructedScheduleElement);
	  } else {
	    long start = Math.max(relayScheduleElement.getStartTime(),
				  constructedScheduleElement.getStartTime());
	    long end = Math.min(relayScheduleElement.getEndTime(),
				constructedScheduleElement.getEndTime());
	    modifiedSchedule.add(SDFactory.newLineageScheduleElement(start,
								     end));
	  }
	}
      }
    }
    
    if ((modifiedSchedule.size() == 0) &&
	(myLoggingService.isDebugEnabled())) {
      myLoggingService.debug(getAgentIdentifier() + " updateLineageSchedule() " + 
			     " no overlap between relay Schedule and constructed schedule.");
    }

    return modifiedSchedule;
  }

  protected Role getRFDRole(Task rfdTask) {
    Collection roles =
      (Collection) findIndirectObject(rfdTask, Constants.Preposition.AS);
    
    if ((roles == null) ||
	(roles.size() == 0)){
      myLoggingService.error(getAgentIdentifier() + 
			     ": getRFDRole() RFD task - " + rfdTask +
			     " does not specify a role.");
      return null;
    } else if (roles.size() > 1) {
      myLoggingService.error(getAgentIdentifier() + 
			     ": getRFDRole() expected 1 role in RFD task - " +
			     rfdTask + ". Found " + roles.size() + 
			     ": " + roles + 
			     ". Will ignore all but the first role.");
      
    }
    
    Role taskRole = null;
    for (Iterator iterator = roles.iterator(); iterator.hasNext();) {
      taskRole = (Role) iterator.next();
      break;
    }

    return taskRole;
  }

  protected String getSuperiorName(Task rfdTask) {
    Asset superior =
      (Asset) findIndirectObject(rfdTask, Constants.Preposition.FOR);

    if (superior != null) {
      return  superior.getClusterPG().getMessageAddress().toString();
    } else {
      myLoggingService.error(getAgentIdentifier() + 
			     ": getSuperiorName() RFD task - " + rfdTask +
			     " does not specify a superior.");
      return null;
    }
  }

  protected Lineage findLocalLineage(Task rfdTask) {
    Lineage lineage = null;

    for (Iterator iterator = myLineageSubscription.iterator();
	 iterator.hasNext();) {
      Lineage nextLineage = (Lineage) iterator.next();
      
      if (matchRFDTask(rfdTask, nextLineage)) {
	lineage = nextLineage;
	break;
      }
    }
    
    if (lineage == null) {
      // look in recent adds
      for (Iterator iterator = myExecuteAdds.iterator();
	   iterator.hasNext();) {
	Object next = iterator.next();

	if (next instanceof Lineage) {
	  if (matchRFDTask(rfdTask, (Lineage) next)) {
	    lineage = (Lineage) next;
	    break;
	  }
	}
      }
    }

    if ((lineage == null) &&
	(myLoggingService.isDebugEnabled())) {
      myLoggingService.debug(getAgentIdentifier() + 
			     ": findLocalLineage() no " + 
			     getRFDRole(rfdTask) + 
			     " lineage to " + getSuperiorName(rfdTask));
    }

    return lineage;
  } 

  protected Lineage findRelayLineage(Task rfdTask) {
    LineageRelay relay = findRelay(rfdTask);

    if (relay == null) {
      return null;
    }

    Role rfdRole = getRFDRole(rfdTask);
    if (rfdRole == null) {
      return null;
    }

    int lineageType = Lineage.roleToType(rfdRole.getConverse());

    if (lineageType == Lineage.SUPPORT) {
      // No pertinent info from Superior
      return null;
    }

    Lineage lineage = null;

    for (Iterator relayLineageIterator = relay.getLineages().iterator();
	 relayLineageIterator.hasNext();) {
      Lineage relayLineage = (Lineage) relayLineageIterator.next();
      if (relayLineage.getType() == lineageType) {
	lineage = relayLineage;
	break;
      }
    }
    
    if ((lineage == null) &&
	(myLoggingService.isDebugEnabled())) {
      myLoggingService.debug(getAgentIdentifier() + 
			     ": findRelayLineage() no " + rfdRole + 
			     " lineage to " + relay.getAgentName());
    }

    return lineage;
  }

  protected Lineage addLineageSeed(int lineageType) {
    ArrayList list = new ArrayList();
    list.add(myAgentName);
    MutableTimeSpan defaultTimeSpan = new MutableTimeSpan();
    defaultTimeSpan.setTimeSpan(SDFactory.DEFAULT_START_TIME,
				SDFactory.DEFAULT_END_TIME);
    Lineage lineage =
      mySDFactory.newLineage(lineageType, list, defaultTimeSpan);
    
    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(getAgentIdentifier() + 
			     " adding initial lineage - " + lineage);
    }
    
    return lineage;
  }

  protected Lineage createLocalLineage(Task rfdTask) {
    Role rfdRole = getRFDRole(rfdTask);
    if (rfdRole == null) {
      return null;
    }

    int lineageType = Lineage.roleToType(rfdRole.getConverse());
    ArrayList list = new ArrayList();
    Asset superior =
      (Asset) findIndirectObject(rfdTask, Constants.Preposition.FOR);
    String superiorName = 
      superior.getClusterPG().getMessageAddress().toString();
    
    list.add(superiorName);
    list.add(myAgentName);

    long startTime =
      (long) rfdTask.getPreferredValue(AspectType.START_TIME);
    long endTime =
      (long) rfdTask.getPreferredValue(AspectType.END_TIME);

    
    Lineage localLineage = 
      mySDFactory.newLineage(lineageType, list, 
			     SDFactory.newLineageScheduleElement(startTime, endTime));


    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(getAgentIdentifier() + 
			     ": createLocalLineage() localLineage " + localLineage);
    }   

    return localLineage;
  }

  protected boolean match(Lineage localLineage, Lineage relayLineage) {
    if (localLineage.getType() == relayLineage.getType()) {
      if (localLineage.getType() == Lineage.SUPPORT) {
	return localLineage.getRoot().equals(relayLineage.getRoot());
      } else {
	String lineageSuperior = (localLineage.getList().size() > 1) ?
	  (String) localLineage.getList().get(localLineage.getList().size() - 2) :
	  "";
	return lineageSuperior.equals((String) relayLineage.getLeaf());
      }
    } else {
      return false;
    }
  }

  protected Collection getMatchingLineages(LineageRelay relay) {
    ArrayList localLineages = new ArrayList();

    for (Iterator iterator = myLineageSubscription.iterator();
	 iterator.hasNext();) {
      Lineage lineage = (Lineage) iterator.next();
      String lineageSuperior = (lineage.getList().size() > 1) ?
	(String) lineage.getList().get(lineage.getList().size() - 2) :
	  "";

      if (lineageSuperior.equals(relay.getAgentName())) {
	localLineages.add(lineage);
      }
    }

    return localLineages;
  }

  protected Lineage findExactMatch(Lineage relayLineage, 
				   Collection localLineages) {
    ArrayList updatedList = new ArrayList(); 
    updatedList.addAll(relayLineage.getList());
    updatedList.add(myAgentName);
    
    for (Iterator iterator = localLineages.iterator();
	 iterator.hasNext();) {
      Lineage localLineage = (Lineage) iterator.next();
      if ((localLineage.getType() == relayLineage.getType()) &&
	  (localLineage.getList().equals(updatedList))) {
	return localLineage;
      }
    }
    return null;
  }

  protected Lineage findMatch(Lineage relayLineage, 
			      Collection localLineages) {
    // Must be exact for SCA lineages
    if (relayLineage.getType() == Lineage.SUPPORT) {
      return findExactMatch(relayLineage, localLineages);
    }

    for (Iterator iterator = localLineages.iterator();
	 iterator.hasNext();) {
      Lineage localLineage = (Lineage) iterator.next();
      if (localLineage.getType() == relayLineage.getType()) {
	return localLineage;
      }
    }
    return null;
  }

  protected LineageRelay findRelay(Task rfdTask) {
     String superiorName = getSuperiorName(rfdTask);

    if (superiorName == null) {
      return null;
    }

    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(getAgentIdentifier() + ": findRelay() - " +
			     "reporting to " + superiorName);
    }

    LineageRelay relay = null;
    for (Iterator relayIterator = mySuperiorLineageRelaySubscription.iterator();
	 relayIterator.hasNext();) {
      
      LineageRelay next = (LineageRelay) relayIterator.next();
      
      if (next.getAgentName().equals(superiorName)) {
	relay = next;
	break;
      } else {
	if (myLoggingService.isDebugEnabled()) {
	  myLoggingService.debug(getAgentIdentifier() + ": findRelay() - " +
				 " relay to " + next.getAgentName() +
				 " does not match.");
	}
      }
    }
    
    if (relay == null) {
      // look in recent adds
      for (Iterator relayIterator = myExecuteAdds.iterator();
	 relayIterator.hasNext();) {
      
	Object next = relayIterator.next();
	if (next instanceof LineageRelay) {
	  LineageRelay nextRelay = (LineageRelay) next;
	  if (nextRelay.getAgentName().equals(superiorName)) {
	    relay = nextRelay;
	    break;
	  } else {
	    if (myLoggingService.isDebugEnabled()) {
	      myLoggingService.debug(getAgentIdentifier() + ": findRelay() - " +
				     " recently added relay to " + nextRelay.getAgentName() +
				     " does not match.");
	    }
	  }
	}
      }
    }

    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(getAgentIdentifier() + ": findRelay() - " +
			     "returning " + relay);
    }

    return relay;
  }

  protected boolean matchRFDTask(Task rfdTask, Lineage localLineage) {
    String superiorName = getSuperiorName(rfdTask);

    if (superiorName == null) {
      return false;
    }

    Role rfdRole = getRFDRole(rfdTask);
    if (rfdRole == null) {
      return false;
    }

    int taskType = Lineage.roleToType(rfdRole.getConverse());

    if (localLineage.getType() == taskType) {
      int length = localLineage.getList().size();
      String lineageSuperior = (length >= 2) ?
	(String) localLineage.getList().get(length - 2) :
	((localLineage.getType() == Lineage.SUPPORT) &&
	 (length == 1)) ?
	// Okay to support yourself.
	(String) localLineage.getList().get(0) : null;
      
      
      return (superiorName.equals(lineageSuperior));
    } else {
      return false;
    }
  }
    
}

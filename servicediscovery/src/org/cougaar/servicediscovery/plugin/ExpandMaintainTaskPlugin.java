
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

/**
 * This plugin expands Maintain tasks into a workflow of 3 child Supply tasks.
 * It expects Maintain tasks to have WUC/1 through 3. Each Maintain task is
 * expanded to 3 different, hardcoded NSN Supply tasks based on this plugin's
 * WUCtoNSN table. These Supply tasks are set up to have all available
 * auxiliary query type slots.
 */
package org.cougaar.servicediscovery.plugin;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.glm.ldm.asset.ClassIXRepairPart;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.servicediscovery.Constants;
import org.cougaar.util.UnaryPredicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;


/**
 * Subscribe to Maintain tasks. Based on the WUC on the direct object,
 * expand into Supply subtasks.
 *
 */
public class ExpandMaintainTaskPlugin extends org.cougaar.planning.plugin.legacy.SimplePlugin {
  private IncrementalSubscription myTaskSubscription;
  private IncrementalSubscription myExpansionSubscription;
  private Hashtable WUCtoNSN=new Hashtable();

  private UnaryPredicate myTaskPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Task) {
        Task t = (Task)o;
        if(t.getVerb().equals(Constants.Verb.Maintain))
          return true;
      }
      return false;
    }
  };

  // Filters for Expansions
  private UnaryPredicate myExpansionPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Expansion) {
        Task t = ((PlanElement)o).getTask();
        if (t.getVerb().equals(Constants.Verb.SUPPLY))
          return true;
      }
      return false;
    }
  };


  protected void setupSubscriptions() {
    myTaskSubscription = (IncrementalSubscription)subscribe(myTaskPred);
    myExpansionSubscription = (IncrementalSubscription)subscribe(myExpansionPred);
    setupWUCtoNSNtable();
  }

  public void execute() {
    if (myTaskSubscription.getAddedCollection().size() > 0) {
      Collection tasks = myTaskSubscription.getAddedCollection();
      Iterator it = tasks.iterator();
      while(it.hasNext()) {
        Task t = (Task)it.next();
        expandMaintenanceTask(t);
      }
    }
    if (myExpansionSubscription.hasChanged()) {
      PluginHelper.updateAllocationResult(myExpansionSubscription);
    }
  }

  private void expandMaintenanceTask(Task t) {
    String wuc = t.getDirectObject().getTypeIdentificationPG().getTypeIdentification();
    if(WUCtoNSN.containsKey(wuc)) {
      Collection nsns = (Collection)WUCtoNSN.get(wuc);
      Iterator it = nsns.iterator();
      Vector subs = new Vector();
      while(it.hasNext()) {
        NewTask child = createTask((String)it.next());
        subs.add(child);
      }
      AllocationResult ar =
        PluginHelper.createEstimatedAllocationResult(t, theLDMF, 0.5, true);
//      ar.addAuxiliaryQueryInfo(AuxiliaryQueryType.PORT_NAME, "18-MAINTBN expands");
      Expansion ex = PluginHelper.wireExpansion(t, subs, theLDMF, ar);
      PluginHelper.publishAddExpansion(this.getSubscriber(), ex);
    }
    else {
      System.out.println(wuc + " not found");
    }
  }

  private NewTask createTask(String itemID) {
    NewTask newTask = theLDMF.newTask();
    newTask.setVerb(new Verb(Constants.Verb.SUPPLY));
    Asset a ;
      a = makePrototype(itemID);
    newTask.setPreferences(makePreferences());
    newTask.setPrepositionalPhrases(makePrepositionalPhrases());
    newTask.setPlan(theLDMF.getRealityPlan());
    newTask.setDirectObject(a);
    //I think that AQTYPE_COUNT = 8, and that 0-7 are defined types
    int[] types = new int[AuxiliaryQueryType.AQTYPE_COUNT];
    for(int i=0; i<types.length; i++) {
      types[i] = i;
    }
    newTask.setAuxiliaryQueryTypes(types);

    return newTask;
  }

  private Enumeration makePrepositionalPhrases() {
    Vector myPrepPhrases = new Vector();
    //none for now
    return myPrepPhrases.elements();
  }

  private Asset makePrototype(String itemID){
    Asset cix = theLDMF.getPrototype(itemID);
    if (cix == null) {
      cix = theLDMF.createPrototype(ClassIXRepairPart.class, itemID);
      theLDM.cachePrototype(itemID, cix);
    }
    else {
      System.out.println("prototype found " + itemID);
    }

    return cix;
  }

  private Enumeration makePreferences() {
    Vector myPreferences = new Vector();
    AspectValue stav = TimeAspectValue.create(AspectType.START_TIME, this.getDate());
    ScoringFunction sfst = ScoringFunction.createStrictlyAtValue(stav);
    Preference myStartTimePreference = theLDMF.newPreference(AspectType.START_TIME, sfst);

    AspectValue tav = TimeAspectValue.create(AspectType.END_TIME, this.getDate());
    ScoringFunction sft = ScoringFunction.createStrictlyAtValue(tav);
    Preference myTimePreference = theLDMF.newPreference(AspectType.END_TIME, sft);

    AspectValue qav = AspectValue.newAspectValue(AspectType.QUANTITY, 3);
    ScoringFunction sfq = ScoringFunction.createStrictlyAtValue(qav);
    Preference myQuantityPreference = theLDMF.newPreference(AspectType.QUANTITY, sfq);

    AspectValue cav = AspectValue.newAspectValue(AspectType.COST, 0.0);
    ScoringFunction sfc = ScoringFunction.createNearOrAbove(cav, 1.0);
    Preference myCostPreference = theLDMF.newPreference(AspectType.COST, sfc);

    myPreferences.addElement(myStartTimePreference);
    myPreferences.addElement(myTimePreference);
    myPreferences.addElement(myQuantityPreference);
    myPreferences.addElement(myCostPreference);

    return myPreferences.elements();
  }

  private void setupWUCtoNSNtable() {
    ArrayList arr = new ArrayList();
    arr.add("NSN/4710007606205");
    arr.add("NSN/4320012017527");
    arr.add("NSN/5930008432366");
    WUCtoNSN.put("WUC/1", arr);

    arr = new ArrayList();
    arr.add("NSN/1730007603370");
    arr.add("NSN/5945002010273");
    arr.add("NSN/5930011951836");
    WUCtoNSN.put("WUC/2", arr);

    arr = new ArrayList();
    arr.add("NSN/4310004145989");
    arr.add("NSN/6105007262754");
    arr.add("NSN/3110005656233");
    WUCtoNSN.put("WUC/3", arr);
  }

}










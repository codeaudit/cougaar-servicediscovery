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
import org.cougaar.glm.ldm.asset.ClassIXRepairPart;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.AuxiliaryQueryType;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.ScoringFunction;
import org.cougaar.planning.ldm.plan.TimeAspectValue;
import org.cougaar.planning.ldm.plan.Verb;
import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.servicediscovery.Constants;
import org.cougaar.servicediscovery.util.Switch;
import org.cougaar.util.UnaryPredicate;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

/**
 * This plugin is intended to be used in an agent along with the
 * TaskInitiationServlet so that the user can generate Maintain tasks
 * by pushing the servlet button. It assumes that no one in the agent
 * other than the TaskInitiationServlet is generating or changing
 * Switch objects.
 *
 * This plugin generates a set of 3 Maintain tasks (1 of each WUC)
 * upon receipt of changes to the Switch object, if the Switch state
 * is false.
 */

public class GenerateMaintainTaskPlugin extends SimplePlugin {
  private IncrementalSubscription mySwitchSubscription;


  private UnaryPredicate mySwitchPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Switch) {
        return true;
      } else {
        return false;
      }
    }
  };



  protected void setupSubscriptions() {
    mySwitchSubscription = (IncrementalSubscription)subscribe(mySwitchPred);
  }

  public void execute() {
    Collection changes = mySwitchSubscription.getChangedCollection();
    Iterator it = changes.iterator();
    while(it.hasNext()) {
      Switch s = (Switch) it.next();
      if(s.getState() == false) {
        Vector tasks = fabricateTasks();
        publishTasks(tasks);
      }
    }
  }

  private void publishTasks(Vector tasks) {
    for(int i = 0; i<tasks.size(); i++) {
      publishAdd(tasks.elementAt(i));
    }
  }

  private Vector fabricateTasks() {
    Vector ret = new Vector();

    String itemIDa = "WUC/1";
    String itemIDb = "WUC/2";
    String itemIDc = "WUC/3";
    //I think that AQTYPE_COUNT = 8, and that 0-7 are defined types
    int[] types = new int[AuxiliaryQueryType.AQTYPE_COUNT];
    for(int i=0; i<types.length; i++) {
      types[i] = i;
    }
    NewTask nt = createTask(itemIDa);
    nt.setAuxiliaryQueryTypes(types);
    ret.add(nt);
    nt = createTask(itemIDb);
    nt.setAuxiliaryQueryTypes(types);
    ret.add(nt);
    nt = createTask(itemIDc);
    nt.setAuxiliaryQueryTypes(types);
    ret.add(nt);

    return ret;
  }
  private NewTask createTask(String itemID) {
    NewTask newTask = theLDMF.newTask();
    newTask.setVerb(new Verb(Constants.Verb.MAINTAIN));
    Asset a ;
      a = makePrototype(itemID);
    newTask.setPreferences(makePreferences());
    newTask.setPrepositionalPhrases(makePrepositionalPhrases());
    newTask.setPlan(theLDMF.getRealityPlan());
    newTask.setDirectObject(a);

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


}










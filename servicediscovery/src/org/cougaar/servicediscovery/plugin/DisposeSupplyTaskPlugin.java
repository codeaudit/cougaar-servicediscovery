/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
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
 * This plugin is for testing purposes. It disposes of all Supply tasks, putting
 * on an allocation result with the first 8 auxiliary query slots filled in.
 */

package org.cougaar.servicediscovery.plugin;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.servicediscovery.Constants;
import org.cougaar.util.UnaryPredicate;

import java.util.Collection;
import java.util.Iterator;


/**
 * Dispose of supply tasks
 */
public class DisposeSupplyTaskPlugin extends SimplePlugin {
  private IncrementalSubscription myTaskSubscription;

  private UnaryPredicate myTaskPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Task) {
        Task t = (Task)o;
        if(t.getVerb().equals(Constants.Verb.Supply))
          return true;
      }
      return false;
    }
  };

  protected void setupSubscriptions() {
    myTaskSubscription = (IncrementalSubscription)subscribe(myTaskPred);
  }

  public void execute() {
    if (myTaskSubscription.getAddedCollection().size() > 0) {
      Collection tasks = myTaskSubscription.getAddedCollection();
      Iterator it = tasks.iterator();
      while(it.hasNext()) {
        Task t = (Task)it.next();
        disposeSupplyTask(t);
      }
    }
  }

  private void disposeSupplyTask(Task t) {
    AllocationResult ar =
          PluginHelper.createEstimatedAllocationResult(t, theLDMF, 1.0, true);
    ar.addAuxiliaryQueryInfo(0, "Example of line 0 in allocation result");
    ar.addAuxiliaryQueryInfo(1, "Example of line 1 in allocation result");
    ar.addAuxiliaryQueryInfo(2, "Example of line 2 in allocation result");
    ar.addAuxiliaryQueryInfo(3, "Example of line 3 in allocation result");
    ar.addAuxiliaryQueryInfo(4, "Example of line 4 in allocation result");
    ar.addAuxiliaryQueryInfo(5, "Example of line 5 in allocation result");
    ar.addAuxiliaryQueryInfo(6, "Example of line 6 in allocation result");
    ar.addAuxiliaryQueryInfo(7, "Example of line 7 in allocation result");
    Disposition disp = theLDMF.createDisposition(t.getPlan(), t, ar);
    publishAdd(disp);
  }

}










/*
 *  <copyright>
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
 *  </copyright>
 */
package org.cougaar.servicediscovery.plugin;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.cougaar.core.blackboard.IncrementalSubscription;

import org.cougaar.core.service.LoggingService;

import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.AuxiliaryQueryType;
import org.cougaar.planning.ldm.plan.Disposition;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.planning.plugin.util.PluginHelper;

import org.cougaar.servicediscovery.util.Switch;

import org.cougaar.util.UnaryPredicate;

/**
 * Plugin listens for supply tasks and switch object. If the the switch is off
 * the plugin will publish an unsuccessful disposition for the supply task. If
 * the switch is on and the odm can produce the requested nsn publishes a
 * successful disposition. Switch object is manipulated using the switch servlet
 * associated with the agent. http://&lt;hostname&gt;:&lt;port&gt;/$Agent/switchServlet
 *
 *@author    HSingh
 *@version   $Id: ODMPlugin.java,v 1.3 2003-01-22 21:03:56 lgoldsto Exp $
 */
public class ODMPlugin extends SimplePlugin {
	private IncrementalSubscription mySupplyTaskSubscription;
	private IncrementalSubscription mySwitchSubscription;

	private boolean switchState;
	private Hashtable odm_db;

	private LoggingService log;

	// Listens for supply tasks.
	private UnaryPredicate mySupplyTaskPred =
		new UnaryPredicate() {
			public boolean execute(Object o) {
				if(o instanceof Task) {
					Task task = (Task) o;
					return task.getVerb().equals(org.cougaar.glm.ldm.Constants.Verb.SUPPLY);
				}
				return false;
			}
		};

	// Listens for org.cougaar.servicediscovery.util.Switch objects.
	private UnaryPredicate mySwitchPred =
		new UnaryPredicate() {
			public boolean execute(Object o) {
				return o instanceof Switch;
			}
		};

	/**
	 * Set up blackboard subscriptions. Listens for:
	 * <ul>
	 *   <li> Tasks: Supply tasks allocated to this ODM.</li>
	 *   <li> Switch: Switch objects added to the ODM's blackboard.</li>
	 * </ul>
	 *
	 */
	protected void setupSubscriptions() {
		init_odm_db();
		switchState = true;
		mySupplyTaskSubscription =
			(IncrementalSubscription) subscribe(mySupplyTaskPred);
		mySwitchSubscription =
			(IncrementalSubscription) subscribe(mySwitchPred);
		log =
			(LoggingService) getBindingSite().getServiceBroker().getService(this, LoggingService.class, null);
	}

	/**
	 * If the state of the Switch object on the blackboard is false publishes
	 * unsuccessful disposition. If the state is true and the manufacturer is able
	 * to produce the nsn it publishes a successful disposition.
	 */
	public void execute() {
		Enumeration task_enum = mySupplyTaskSubscription.elements();
		while(task_enum.hasMoreElements()) {
			Task task = (Task) task_enum.nextElement();
			if(task.getPlanElement() != null) {
				continue;
			}

			String nsn =
				task.getDirectObject().getTypeIdentificationPG().getTypeIdentification();
			boolean tempState = switchState;

			if(tempState && !canProduce(nsn)) {
				tempState = false;
			}

			boolean isSuccess = tempState;
      AllocationResult estAR = PluginHelper.createEstimatedAllocationResult(task, theLDMF, 1.0, isSuccess);
			estAR.addAuxiliaryQueryInfo(AuxiliaryQueryType.PORT_NAME,
				getAgentIdentifier().toString()
				 + ": published disposition with result: " + tempState);

			Disposition disposition =
				getFactory().createDisposition(task.getPlan(), task, estAR);

			try {
				publishAdd(disposition);
				if(log.isDebugEnabled()) {
					log.debug("execute: published disposition for:" + nsn
						 + ":with result:" + tempState);
				}
			} catch(Exception e) {
				if(log.isDebugEnabled()) {
					log.debug("execute:unable to publish:" + tempState
						 + ":disposition");
					e.printStackTrace();
				}
			}
		}

		if(mySwitchSubscription.hasChanged()) {
			for(Iterator i = mySwitchSubscription.getChangedCollection().iterator(); i.hasNext(); ) {
				Switch sw = (Switch) i.next();
				switchState = sw.getState();
				break;
			}
		}
	}

	/**
	 * Looks up in odm_db to see whether the manufacturer can produce the nsn.
	 *
	 *@param nsn  NSN
	 *@return  true if the manufacturer can produce the nsn, else false
	 */
	protected boolean canProduce(String nsn) {
		String me = getAgentIdentifier().toString();
		if(odm_db.containsKey(me)){
			Vector v = (Vector)odm_db.get(me);
			return v.contains(nsn);
		}
		return false;
	}

	/**
	 * Hashmap containing which nsn's an odm can produce.
	 */
	protected void init_odm_db() {
		odm_db = new Hashtable();

		Vector v = new Vector();
		v.add("NSN/4710007606205");
		v.add("NSN/1730007603370");
		odm_db.put("PARTSALACARTE", v);
		odm_db.put("WARNERROBBINS", v);
	}

}


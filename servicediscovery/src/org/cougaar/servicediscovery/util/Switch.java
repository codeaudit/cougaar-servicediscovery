/*
 *  <copyright>
 *   Copyright 2002-2003 BBNT Solutions, LLC
 *   under sponsorship of the Defense Advanced Research Projects Agency (DARPA)
 *   and the Defense Logistics Agency (DLA).
 *  
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the Cougaar Open Source License as published by
 *   DARPA on the Cougaar Open Source Website (www.cougaar.org).
 *  
 *   THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *   PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *   IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *   MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *   ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *   HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *   DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *   TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *   PERFORMANCE OF THE COUGAAR SOFTWARE.
 *  </copyright>
 */
package org.cougaar.servicediscovery.util;

/**
 * ON/OFF Switch. Default start state is true/ON.
 *
 *@author    HSingh
 *@version   $Id: Switch.java,v 1.3 2003-01-23 20:01:18 mthome Exp $
 */
public class Switch {
	boolean state;

	/**
	 * Default state is true/ON
	 */
	public Switch() {
		this.state = true;
	}

	/**
	 * Toggles the state of the switch object.
	 */
	public void toggle() {
		this.state = !this.state;
	}

	/**
	 * Returns the current state of the object.
	 *
	 *@return   The current state
	 */
	public boolean getState() {
		return this.state;
	}

	public String toString() {
		if(this.state) {
			return "ON";
		} else {
			return "OFF";
		}
	}

}


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
package org.cougaar.servicediscovery.description;

import java.util.Collections;

import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * A Collection which maintains an ordered list of agent names - reflecting
 * the superior/subordinate chain from a root agent. 
 * Iteration starts with the current agent and goes to the root.
 */

public class LineageListWrapper implements java.io.Serializable, UniqueObject {
  private static Logger myLogger = Logging.getLogger(LineageListWrapper.class);
  private UID myUID = null;
  private LineageList myLineageList = null;

  public LineageListWrapper() {
  }

  public LineageListWrapper(int type) {
    myLineageList = new LineageListImpl(type);
  }


  public void setUID(UID newUID) {
    if (myUID != null) {
      myLogger.error("Attempt to reset UID.");
      return;
    } 
      
    myUID = newUID;
  }

  public UID getUID() {
    return myUID;
  }

  /*
   * @return a LineageList type
   */
  public int getType() {
    return myLineageList.getType();
  }

  /**
   * @return the name of the agent at the end of the lineage.
   */ 
  public String getLeaf() {
    return myLineageList.getLeaf();
  }

  /**
   * @return the name of the agent at the root of the lineage.
   */ 
  public String getRoot() {
    return myLineageList.getRoot();
  }

  /**
   * @return the number of links in the lineage between the two agents. 
   * Returns -1 if the agents are not linked.
   */ 
  public int countHops(String startingAgent, String endingAgent) {
    return myLineageList.countHops(startingAgent, endingAgent);
  }

  /*
   * @return a copy of the associated LineageList
   */
  public LineageList getLineageList() {
    switch (myLineageList.getType()) {
    case LineageList.COMMAND:
      return new LineageListImpl(myLineageList);
    case LineageList.SUPPORT:
      return new SupportLineageListImpl(myLineageList);
    default:
      myLogger.error("getLineageList(): unrecognized LineageListType - " + 
		   myLineageList.getType());
      return null;
    }

  }

  /*
   * Sets the associated LineageList
   */
  public synchronized void setLineageList(LineageList lineageList) {
    if ((myLineageList != null) &&
	(myLineageList.getType() != lineageList.getType())) {
      myLogger.error("setLineageList: Attempt to change LineageListType.");
    } else {
      myLineageList = lineageList;
    }
  }
 
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("UID=" + getUID());
    buf.append(myLineageList.toString());
    buf.append("]");
    return buf.toString();
  }

  public boolean equals(Object o) {
    if (o instanceof LineageListWrapper) {
      return ((LineageListWrapper) o).getUID().equals(getUID());
    } else {
      return false;
    }
  }
  
  public int hashCode()
  {
    return getUID().hashCode();
  }
 
}
















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
package org.cougaar.servicediscovery.description;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;
import org.cougaar.util.TimeSpan;

import org.cougaar.planning.ldm.plan.ScheduleElement;
import org.cougaar.planning.ldm.plan.ScheduleElementImpl;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.planning.ldm.plan.ScheduleImpl;

/**
 * A Collection which maintains an ordered list of agent names - reflecting
 * the superior/subordinate chain from a root agent. 
 * Iteration starts with the current agent and goes to the root.
 */

public class LineageImpl extends Lineage {
  private static Logger myLogger = Logging.getLogger(LineageImpl.class);

  private UID myUID = null;
  private ArrayList myList = new ArrayList();
  private int myType = -1;
  private Schedule mySchedule = new ScheduleImpl();

  public LineageImpl() {
  }

  public LineageImpl(int type) {
    super();
    
    if (!validType(type)) {
      throw new IllegalArgumentException("Unrecognized lineage type: " + type);
    }
    
    myType = type;

    // Default to always valid
    mySchedule.add(new ScheduleElementImpl(TimeSpan.MIN_VALUE, 
					   TimeSpan.MAX_VALUE));
  }
  
  public LineageImpl(int type, List list) {
    this(type);
    
    myList.addAll(list);
  }

  public LineageImpl(int type, List list, Schedule schedule) {
    this(type);
    
    myList.addAll(list);

    mySchedule = new ScheduleImpl(schedule);
  }


  public LineageImpl(Lineage lineage) {
    this(lineage.getType(), lineage.getList(), lineage.getSchedule());
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

  /**
   * @return the type of the lineage.
   * Should be one of the defined lineage types.
   **/
  public int getType() {
    return myType;
  }

  /**
   * @param type of the lineage.
   * Should be one of the defined lineage types.
   **/
  public void setType(int type) {
    // Not allowed to set type more than once
    if (myType != -1) {
      myLogger.error("Attempt to reset lineage type.");
      return;
    } 

    if (!validType(type)) {
      throw new IllegalArgumentException("Unrecognized lineage type: " + type);
    }
      
    myType = type;
  }


  /**
   * @return the name of the agent at the end of the lineage
   * Null if lineage has no members
   */ 
  public String getLeaf() {
    return (String) ((myList.size() > 0) ? 
		     (myList.get(myList.size() - 1)) : null);
  }


  /**
   * @return the name of the agent at the root of the lineage.
   */ 
  public String getRoot() {
    return (String) ((myList.size() > 0) ? myList.get(0) : null);
  }


  /**
   * @return the number of links in the lineage between the two agents. 
   * Returns -1 if the agents are not linked.
   */ 
  public int countHops(String startingAgent, String endingAgent) {
    if (startingAgent.equals(getLeaf()) &&
	endingAgent.equals(getRoot())) {
      return myList.size() - 1;
    }

    int start = myList.indexOf(startingAgent);
    int end = myList.indexOf(endingAgent);

    if ((start == -1) || (end == -1) || (end > start)) {
      return -1;
    } else {
      return start - end;
    }
  }


  /*
   * @return an unmodifiable list of all agents in the lineage
   */
  public List getList() {
    return Collections.unmodifiableList(myList);
  }

  /*
   * Sets the associated lineage list
   */
  public synchronized void setList(List list) {
    myList = new ArrayList(list);
  }

  /**
   * @return the time periods for which the lineage is valid
   */
  public Schedule getSchedule() {
    return mySchedule;
  }

  /*
   * Sets the Schedule 
   */
  public void setSchedule(Schedule schedule) {
    mySchedule = new ScheduleImpl(schedule);
  }
 

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("UID=" + getUID());

    String typeStr = "";
    switch (myType) {
    case (ADCON):
      typeStr = ADCON_STRING;
      break;
    case (OPCON):
      typeStr = OPCON_STRING;
      break;
    case (SUPPORT):
      typeStr = SUPPORT_STRING;
      break;
    default:
      typeStr = UNDEFINED_STRING;
    }
    buf.append(", type=" + typeStr);

    buf.append(", list=[");
    for (int i = 0; i < myList.size(); i++) {
      buf.append(String.valueOf(myList.get(i)));
      if (i < (myList.size() - 1)) 
        buf.append(", ");
    }
    buf.append("]");

    buf.append(", schedule=" + getSchedule().toString());
    return buf.toString();
  }

  public boolean equals(Object o) {
    if (o instanceof LineageImpl) {
      return ((LineageImpl) o).getUID().equals(getUID());
    } else {
      return false;
    }
  }
  
  public int hashCode() {
    return getUID().hashCode();
  }
}

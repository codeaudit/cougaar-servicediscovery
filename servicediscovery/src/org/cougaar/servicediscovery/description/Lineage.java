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


import org.cougaar.core.util.UniqueObject;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.servicediscovery.Constants;

import java.util.List;

/**
 * A Collection which maintains an ordered list of agent names - reflecting
 * the superior/subordinate chain from a root agent. 
 * Iteration starts with the current agent and goes to the root.
 */

public abstract class Lineage implements java.io.Serializable, UniqueObject {

  public static final int UNDEFINED = -1;
  public static final int ADCON = 1;
  public static final int OPCON = 2;
  public static final int SUPPORT = 3;

  public static final String UNDEFINED_STRING = "Undefined";
  public static final String ADCON_STRING = "ADCON";
  public static final String OPCON_STRING = "OPCON";
  public static final String SUPPORT_STRING = "SUPPORT";

  /* returns null if type is not recognized */
  public static Role typeToRole(int type) {
    switch (type) {
      case ADCON:
        return Constants.Roles.ADMINISTRATIVESUPERIOR;
      case OPCON:
        return Constants.Roles.OPERATIONALSUPERIOR;
      case SUPPORT:
        return Constants.Roles.SUPPORTSUPERIOR;
      default:
        return null;
    }
  }

  public static int roleToType(Role role) {
    if ((role.equals(Constants.Roles.ADMINISTRATIVESUPERIOR)) ||
      (role.equals(Constants.Roles.SUPERIOR))) {
      return ADCON;
    } else if (role.equals(Constants.Roles.OPERATIONALSUPERIOR)) {
      return OPCON;
    } else if (role.equals(Constants.Roles.SUPPORTSUPERIOR)) {
      return SUPPORT;
    } else {
      return UNDEFINED;
    }
  }

  public static boolean validType(int lineageType) {
    switch (lineageType) {
      case (ADCON):
      case (OPCON):
      case (SUPPORT):
        return true;
      default:
        return false;
    }
  }

  public static int countHops(List lineageList, String startingAgent, String endingAgent) {
    int listSize = lineageList.size();
    String leaf = (String) ((listSize > 0) ?
      (lineageList.get(listSize - 1)) : null);
    String root = (String) ((listSize > 0) ? lineageList.get(0) : null);

    if (startingAgent.equals(leaf) &&
      endingAgent.equals(root)) {
      return listSize - 1;
    }

    int start = lineageList.indexOf(startingAgent);
    int end = lineageList.indexOf(endingAgent);

    if ((start == -1) || (end == -1) || (end > start)) {
      return -1;
    } else {
      return start - end;
    }
  }


  /**
   * @return the type of the lineage.
   * Should be one of the defined lineage types.
   **/
  abstract public int getType();


  /**
   * @return the name of the agent at the end of the lineage.
   */
  abstract public String getLeaf();

  /**
   * @return the name of the agent at the root of the lineage.
   */
  abstract public String getRoot();

  /**
   * @return the number of links in the lineage between the two agents. 
   * Returns -1 if the agents are not linked.
   */
  abstract public int countHops(String startingAgent, String endingAgent);

  /**
   * @return an unmodifiable list of agents in the lineage
   */
  abstract public List getList();

  /**
   * @return the time periods for which the lineage is valid
   */
  abstract public Schedule getSchedule();
}
















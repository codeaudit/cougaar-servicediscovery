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

import org.cougaar.core.util.UniqueObject;

import java.util.List;

/**
 * A Collection which maintains an ordered list of agent names - reflecting
 * the superior/subordinate chain from a root agent. 
 * Iteration starts with the current agent and goes to the root.
 */

public interface LineageList extends List, java.io.Serializable, UniqueObject {

  public static final int COMMAND = 1;
  public static final int SUPPORT = 2;

  /**
   * @return the type of the lineage.
   * Should be one of the defined lineage types.
   **/
  public int getType();

  /**
   * @return the name of the agent at the end of the lineage.
   */ 
  public String getLeaf();

  /**
   * @return the name of the agent at the root of the lineage.
   */ 
  public String getRoot();

  /**
   * @return the number of links in the lineage between the two agents. 
   * Returns -1 if the agents are not linked.
   */ 
  public int countHops(String startingAgent, String endingAgent);

}
















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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.cougaar.util.ArrayListFoundation;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;


/**
 * A Collection which maintains an ordered list of agent names - reflecting
 * the superior/subordinate chain from a root agent. 
 * Iteration starts with the current agent and goes to the root.
 */


public class LineageListImpl extends ArrayListFoundation 
implements LineageList {
  private static Logger logger = Logging.getLogger(LineageListImpl.class);
  private int myLineageType = -1;

  public static boolean validType(int lineageType) {
    switch (lineageType) {
    case (COMMAND):
    case (SUPPORT):
      return true;
    default:
      return false;
    }
  }

  public LineageListImpl(int lineageType) {
    super();
    
    if (!validType(lineageType)) {
      throw new IllegalArgumentException("Unrecognized lineageType: " + lineageType);
    }
    
    myLineageType = lineageType;
  }
  
  public LineageListImpl(int lineageType, Collection c) {
    this(lineageType);
    
    addAll(c);
  }
  
  public LineageListImpl(LineageList lineage) {
    this(lineage.getType());
    
    unsafeUpdate(lineage);
  }

  /**
   * @return the lineageType of the lineage.
   * Should be one of the defined lineage types.
   **/
  public int getType() {
    return myLineageType;
  }

  /**
   * @param lineageType of the lineage.
   * Should be one of the defined lineage types.
   **/
  public void setType(int lineageType) {
    // Not allowed to set type more than once
    if (myLineageType != -1) {
      logger.error("Attempt to reset lineage type.");
      return;
    } 

    if (!validType(lineageType)) {
      throw new IllegalArgumentException("Unrecognized lineageType: " + lineageType);
    }
      
    myLineageType = lineageType;
  }

  

  /**
   * @return the name of the agent at the end of the lineage.
   * Null if lineage has no members
   */ 
  public String getLeaf() {
    return (String) ((size > 0) ? (elementData[0]) : null);
  }


  /**
   * @return the name of the agent at the root of the lineage.
   */ 
  public String getRoot() {
    return (String) ((size > 0) ? elementData[size - 1] : null);
  }


  /**
   * @return the number of links in the lineage between the two agents. 
   * Returns -1 if the agents are not linked.
   */ 
  public int countHops(String startingAgent, String endingAgent) {
    if (startingAgent.equals(getLeaf()) &&
	endingAgent.equals(getRoot())) {
      return size() - 1;
    }

    int start = indexOf(startingAgent);
    int end = indexOf(endingAgent);

    if ((start == -1) || (end == -1) || (start > end)) {
      return -1;
    } else {
      return end - start;
    }
  }

  public boolean add(Object o) {
    if (! (o instanceof String)) 
      throw new IllegalArgumentException();
    String agentName = (String) o;
    if (!contains(agentName)) {
      super.add(agentName);
      return true;
    } else {
      logger.error("add: agent names must be unique. Unable to add " + o + 
		   " to " + this);
      return false;
    }
  }

  public void add(int i, Object o) {
    throw new UnsupportedOperationException("LineageList.add(int index, Object o) is not supported."); 
  }

  public boolean addAll(Collection c) {
    boolean hasChanged = false;

    if (c instanceof List) {
      List list = (List)c;
      int numToAdd = list.size();
      
      for (int index = 0; index < numToAdd; index++) {
        if (add(list.get(index))) {
          hasChanged = true;
        }
      }
    } else {
      for (Iterator i = c.iterator(); i.hasNext(); ) {
        if (add(i.next()))
          hasChanged = true;
      }
    }

    return hasChanged;
  }

  public boolean addAll(int index, Collection c) {
    throw new UnsupportedOperationException("LineageList.addAll(int index, Collection c) is not supported."); 
  }

  public boolean contains(Object o) {
    if (o instanceof String) {
      return find((String) o) != -1;
    }
    return false;
  }

  public boolean remove(Object o) {
    if (o instanceof String) {
      int i = find((String)o);
      if (i == -1) return false;
      super.remove(i);
      return true;
    } else {
      return false;
    }
  }

  public Object set(int index, Object element) {
    throw new UnsupportedOperationException("LineageListImpl.set(int index, Object element) is not supported."); 
  }


  /**
   * Returns a string representation of this collection.  The string
   * representation consists of a list of the collection's elements, 
   * enclosed in square brackets (<tt>"[]"</tt>).  Adjacent elements are 
   * separated by the characters <tt>", "</tt> (comma and space).  Elements 
   * are converted to strings as by <tt>String.valueOf(Object)</tt>.<p>
   *
   * This implementation creates an empty string buffer, appends a left
   * square bracket, and walks the elements of the collection appending the string
   * representation of each element in turn.  After appending each element
   * except the last, the string <tt>", "</tt> is appended.  Finally a right
   * bracket is appended.  A string is obtained from the string buffer, and
   * returned.
   * 
   * @return a string representation of this collection.
   */
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("[");
    for (int i = 0; i < size; i++) {
      buf.append(String.valueOf(elementData[i]));
      if (i < (size - 1)) 
        buf.append(", ");
    }
    buf.append("]");
    return buf.toString();
  }

  /** @return the index of the object in the list or -1 **/
  protected final int find(String o) {
    int l = size;
    for (int i = 0; i<l; i++) {
      if (o.equals(elementData[i])) return i;
    }
    return -1;
  }

  /** 
   * unsafeUpdate - replaces all elements with specified Collection
   * Should only be used if c has already been validated.
   * @return boolean - true if any elements added else false.
   */
  protected boolean unsafeUpdate(Collection c) {
    clear();
    return super.addAll(c);
  }

  public boolean equals(Object o) {
    if (o instanceof LineageListImpl) {
      return ((myLineageType == ((LineageListImpl) o).getType()) &&
	      (super.equals(o)));
    } else {
      return false;
    }
  }

  
  public int hashCode() { 
    return super.hashCode() + myLineageType;
  }
  
 }




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
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.util.UnaryPredicate;

public class MMRoleQuery implements MMQuery, java.io.Serializable {
  private Role myRole = null;
  private String myEchelon = null;
  private UnaryPredicate myPredicate = DEFAULT_PREDICATE;

  private static UnaryPredicate DEFAULT_PREDICATE = new UnaryPredicate() {
    public boolean execute(Object o) {
      return true;
    }
  };

  public MMRoleQuery(Role role) {
    myRole = role;
  }

  public MMRoleQuery(Role role, String eos) {
    myRole = role;
    myEchelon = eos;
  }

  public MMRoleQuery(Role role, String eos, UnaryPredicate predicate) {
    myRole = role;
    myEchelon = eos;
    setPredicate(predicate);
  }

  public MMRoleQuery() {
    super();
  }

  public void setRole(Role role) {
    myRole = role;
  }

  public Role getRole() {
    return myRole;
  }

  public void setEchelon(String eos) {
    myEchelon = eos;
  }

  public String getEchelon() {
    return myEchelon;
  }

  public UnaryPredicate getPredicate() {
    return myPredicate;
  }

  public void setPredicate(UnaryPredicate predicate) {
    if (predicate == null) {
      myPredicate = DEFAULT_PREDICATE;
    } else {
      myPredicate = predicate;
    }
  }

  public String toString() {
    return "Role: " + myRole + ", Echelon: " + myEchelon;
  }
}







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

import org.cougaar.core.util.UID;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Maintains a Role and echelon
 */


public class ProviderCapabilityImpl implements ProviderCapability {
  private Role myRole = null;
  private String myEchelon = null;


  public ProviderCapabilityImpl() {
  }

  public ProviderCapabilityImpl(Role role, String echelon) {
    setRole(role);
    setEchelon(echelon);
  }

  /**
   * @return the echelon name
   **/
  public String getEchelon() {
    return myEchelon;
  }

  /**
   * @param echelon
   **/
  public void setEchelon(String echelon) {
    myEchelon = echelon;
  }


  /**
   * @return the echelon name
   **/
  public Role getRole() {
    return myRole;
  }

  /**
   * @param echelon
   **/
  public void setRole(Role role) {
    myRole = role;
  }


 }




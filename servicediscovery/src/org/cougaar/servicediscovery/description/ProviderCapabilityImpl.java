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
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.servicediscovery.Constants;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * Maintains a Role and echelon
 */


public class ProviderCapabilityImpl implements ProviderCapability {
  private static Logger logger = Logging.getLogger(ProviderCapability.class);

  private Role myRole = null;
  private String myEchelon = null;
  private Schedule myAvailableSchedule = null;

  public ProviderCapabilityImpl() {
  }

  public ProviderCapabilityImpl(Role role, String echelon, 
				Schedule availableSchedule) {
    setRole(role);
    setEchelon(echelon);
    setAvailableSchedule(availableSchedule);
  }

  /**
   * @return the echelon of support
   **/
  public String getEchelon() {
    return myEchelon;
  }

  /**
   * @param echelon
   **/
  public void setEchelon(String echelon) {
    String mappedEchelon = 
      Constants.MilitaryEchelon.mapToMilitaryEchelon(echelon);

    if (mappedEchelon.equals(Constants.MilitaryEchelon.UNDEFINED)) {
      logger.warn("setEchelon: changing echelon from " + getEchelon() +
		  " to " + mappedEchelon + " for " + this);
    }
    myEchelon = mappedEchelon;
  }


  /**
   * @return the provided role
   **/
  public Role getRole() {
    return myRole;
  }

  /**
   * @param Role
   **/
  public void setRole(Role role) {
    if (myRole != null) {
      logger.error("setRole: attempt to change role from " +  myRole + 
		   " to " + role + " ignored.");
    } else {
      myRole = role;
    }
  }

  /**
   * @return the availability schedule
   **/
  public Schedule getAvailableSchedule() {
    return myAvailableSchedule;
  }

  /**
   * @param availableSchedule
   **/
  public void setAvailableSchedule(Schedule availableSchedule) {
    myAvailableSchedule = availableSchedule;
  }
 }





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

import java.util.Collection;

/**
 * Describes a provider capability for a specific role - including support 
 * role, echelon of support, and availability.
 */

public interface ProviderCapability extends java.io.Serializable {

  /**
   * @return the provider's echelon of support
   **/
  public String getEchelon();

  /**
   * @return the provider's role
   **/
  public Role getRole();

  /**
   * @return the providers availability for this role
   */
  public Schedule getAvailableSchedule();

  /**
   * @param availableSchedule Schedule detailing when Provider is available
   * to provide the specified capability.
   */
  public void setAvailableSchedule(Schedule availableSchedule);

}






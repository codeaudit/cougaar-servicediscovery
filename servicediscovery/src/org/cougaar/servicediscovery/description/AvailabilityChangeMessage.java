/*
 * <copyright>
 *  Copyright 2003 BBNT Solutions, LLC
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

import java.io.Serializable;

import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.util.TimeSpan;

/**
 * Indicates a change in the availability or unavailability of a provider
 * for a given role for a given period of time.
 */
public class AvailabilityChangeMessage implements Serializable {
  /** A change has been requested but not submited **/
  public static final int REQUESTED = 1;
  /** a change is pending **/
  public static final int PENDING = 2;
  /** a change has completed but not yet noticed **/
  public static final int COMPLETED = 3;
  /** a change is complete **/
  public static final int DONE = 4;
  /** a change caused an error **/
  public static final int ERROR = 5;

  private Role role;
  private boolean registryUpdated;
  private int status;
  // Time span representing the period of availability/unavailability
  private TimeSpan span;
  // Indicates whether the role is available or unavailable for the period
  private boolean available;

  public AvailabilityChangeMessage(Role role, boolean registryUpdated, TimeSpan span, boolean available) {
    this.role = role;
    this.span = span;
    this.registryUpdated = registryUpdated;
    status = REQUESTED;
    this.available = available;
  }

  // no-arg constructor
  public AvailabilityChangeMessage() {
  };

  public void setRole(Role role) {
    this.role = role;
  }

  public Role getRole() {
    return role;
  }

  public void setRegistryUpdated(boolean registryUpdated) {
    this.registryUpdated = registryUpdated;
  }

  public boolean isRegistryUpdated() {
    return registryUpdated;
  }

  public boolean isRequested() {
    return status == REQUESTED;
  }

  public boolean isPending() {
    return status == PENDING;
  }

  public boolean isCompleted() {
    return status == COMPLETED;
  }

  public boolean isDone() {
    return status == DONE;
  }

  public boolean isError() {
    return status == ERROR;
  }

  public synchronized void setStatus(int status) {
    if (status == REQUESTED || status == PENDING || status == COMPLETED ||
        status == DONE || status == ERROR) {
      this.status = status;
    }
    else {
      throw new IllegalArgumentException("Invalid status code " + status +"\t Valid codes are 1 - 5");
    }
  }

  public int getStatus() {
    return status;
  }

  public void setTimeSpan(TimeSpan span) {
    this.span = span;
  }

  public TimeSpan getTimeSpan() {
    return span;
  }

  public void setAvailable(boolean available) {
    this.available = available;
  }

  public boolean isAvailable() {
    return available;
  }

  public String toString() {
    return super.toString() + 
      " role = " + role + 
      ", status = " + status +
      ", timespan = " + span + 
      " , available = " + available;
  }

}

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

/**
 *
 * Currently envisioned as the object that the ServiceDisruptionServlet
 * can publish, triggering action on the part of the provider agent.
 * Created by IntelliJ IDEA.
 * User: lgoldsto
 * Date: Jan 10, 2003
 * Time: 2:51:55 PM
 * To change this template use Options | File Templates.
 */
public class StatusChangeMessage implements Serializable {
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
  
  private String role;
  private boolean registryUpdated;
  private int status;

  public StatusChangeMessage(String role, boolean registryUpdated) {
    this.role = role;
    this.registryUpdated = registryUpdated;
    status = REQUESTED;
  }

  public String getRole() {
    return role;
  }

  public void setRegistryUpdated(boolean registryUpdated) {
    this.registryUpdated = registryUpdated;
  }

  public boolean registryUpdated() {
    return registryUpdated;
  }
  public boolean isRequested() { return status==REQUESTED; }
  public boolean isPending() { return status==PENDING; }
  public boolean isCompleted() { return status==COMPLETED; }
  public boolean isDone() { return status==DONE; }
  public boolean isError() { return status==ERROR; }
  public synchronized void setStatus(int status) { this.status = status; }
  public int getStatus() { return status;}
}

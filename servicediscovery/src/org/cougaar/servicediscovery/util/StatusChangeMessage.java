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
package org.cougaar.servicediscovery.util;

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
public class StatusChangeMessage {
  private String role;
  private boolean registryUpdated;

  public StatusChangeMessage(String role, boolean registryUpdated) {
    this.role = role;
    this.registryUpdated = registryUpdated;
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
}

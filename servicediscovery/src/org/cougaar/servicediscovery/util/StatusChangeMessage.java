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

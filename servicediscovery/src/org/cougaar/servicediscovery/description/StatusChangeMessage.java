/*
 * <copyright>
 *  
 *  Copyright 2003-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
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

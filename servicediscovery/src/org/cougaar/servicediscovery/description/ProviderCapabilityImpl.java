/*
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
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

  public String toString() {
    return
      "(ProviderCapability role="+myRole+" echelon="+myEchelon+
      " schedule="+myAvailableSchedule+")";
  }
}

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

import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;
import org.cougaar.util.TimeSpan;

public class MMRoleQueryBase implements MMQuery, java.io.Serializable {
  private static Logger logger = Logging.getLogger(MMRoleQueryBase.class);
  private Role myRole = null;
  private TimeSpan myTimeSpan = null;
  private boolean myObsolete = false;

  public MMRoleQueryBase(Role role) {
    myRole = role;
  }

  public MMRoleQueryBase(Role role, TimeSpan timeSpan) {
    this(role);
    myTimeSpan = timeSpan;
  }

  public void setRole(Role role) {
    if (role != null) {
      logger.warn("setRole: ignoring attempt to change Role from " + 
		  myRole + " to " + role);
    } else {
      myRole = role;
    }
  }

  public Role getRole() {
    return myRole;
  }

  public void setTimeSpan(TimeSpan timeSpan) {
    if (timeSpan != null) {
      logger.warn("setTimeSpan: ignoring attempt to change TimeSpan from " + 
		  myTimeSpan + " to " + timeSpan);
    } else {
    myTimeSpan = timeSpan;
    }
  }

  public TimeSpan getTimeSpan() {
    return myTimeSpan;
  }

  public boolean getObsolete() {
    return myObsolete;
  }

  public void setObsolete(boolean obsoleteFlag) {
    if ((myObsolete) && (!obsoleteFlag)) {
      logger.warn("setObsolete: ignoring attempt to toggle obsolete " +
		  "back to false." );
    } else {
      myObsolete = obsoleteFlag;
    }
  }

  public String toString() {
    return "Role: " + myRole + 
      " TimeSpan: " + myTimeSpan + " Obsolete: " + myObsolete;
  }

  public boolean equals(Object o) {
    if (o instanceof MMRoleQueryBase) {
      MMRoleQueryBase query = (MMRoleQueryBase) o;
      return ((query.getObsolete() == myObsolete) &&
	      (query.getRole().equals(myRole)) &&
	      (query.getTimeSpan().getStartTime() == myTimeSpan.getStartTime()) &&
	      (query.getTimeSpan().getEndTime() == myTimeSpan.getEndTime()));
    } else {
      return false;
    }
  }
}







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

import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;
import org.cougaar.util.TimeSpan;

public class MMRoleQuery implements MMQuery, java.io.Serializable {
  private static Logger logger = Logging.getLogger(MMRoleQuery.class);
  private Role myRole = null;
  private TimeSpan myTimeSpan = null;
  private ServiceInfoScorer myServiceInfoScorer = null;

  private static ServiceInfoScorer DEFAULT_SCORER = new ServiceInfoScorer() {
    public int scoreServiceInfo(ServiceInfo serviceInfo) {
      return -1;
    }
  };

  public MMRoleQuery(Role role, ServiceInfoScorer serviceInfoScorer) {
    myRole = role;
    myServiceInfoScorer = serviceInfoScorer;
  }

  public MMRoleQuery(Role role, ServiceInfoScorer serviceInfoScorer,
		     TimeSpan timeSpan) {
    this(role, serviceInfoScorer);
    myTimeSpan = timeSpan;
  }

  public MMRoleQuery() {
    super();
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

  public ServiceInfoScorer getServiceInfoScorer() {
    if (myServiceInfoScorer == null) {
      return DEFAULT_SCORER;
    } else {
      return myServiceInfoScorer;
    }
  }

  public void setServiceInfoScorer(ServiceInfoScorer serviceInfoScorer) {
    if (myServiceInfoScorer != null) {
      logger.warn("setServiceInfoScorer: ignoring attempt to change ServiceInfoScorer from " + 
		  myServiceInfoScorer + " to " + serviceInfoScorer);
    } else {
      myServiceInfoScorer = serviceInfoScorer;
    }
  }

  public String toString() {
    return "Role: " + myRole + " ServiceInfoScorer: " + myServiceInfoScorer +
      " TimeSpan: " + myTimeSpan;
  }
}







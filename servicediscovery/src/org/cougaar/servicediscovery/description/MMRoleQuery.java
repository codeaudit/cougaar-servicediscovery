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

public class MMRoleQuery extends  MMRoleQueryBase {
  private static Logger logger = Logging.getLogger(MMRoleQuery.class);

  private ServiceInfoScorer myServiceInfoScorer = null;

  private static ServiceInfoScorer DEFAULT_SCORER = new ServiceInfoScorer() {
    public int scoreServiceInfo(ServiceInfo serviceInfo) {
      return -1;
    }
  };

  public MMRoleQuery(Role role, ServiceInfoScorer serviceInfoScorer) {
    super(role);
    myServiceInfoScorer = serviceInfoScorer;
  }

  public MMRoleQuery(Role role, ServiceInfoScorer serviceInfoScorer,
		     TimeSpan timeSpan) {
    super(role, timeSpan);
    myServiceInfoScorer = serviceInfoScorer;
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
    return "Role: " + getRole() +
      " ServiceInfoScorer: " + myServiceInfoScorer +
      " TimeSpan: " + getTimeSpan() + " Obsolete: " + getObsolete();
  }

  public boolean equals(Object o) {
    if (o instanceof MMRoleQuery) {
      MMRoleQuery query = (MMRoleQuery) o;
      return ((super.equals(o))&&
	      (query.getServiceInfoScorer().equals(myServiceInfoScorer)));
    } 
    return false;
    
  }
}







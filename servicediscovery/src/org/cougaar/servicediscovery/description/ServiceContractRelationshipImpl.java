/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
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

import org.cougaar.core.util.UID;
import org.cougaar.planning.ldm.plan.HasRelationships;
import org.cougaar.planning.ldm.plan.RelationshipImpl;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.servicediscovery.transaction.ServiceContractRelay;

/**
 * A ServiceContractRelationshipImpl is the encapsulation of a time phased relationship based on
 * a service contract
 * @author  ALPINE <alpine-software@bbn.com>
 *
 **/

public class ServiceContractRelationshipImpl extends RelationshipImpl 
  implements ServiceContractRelationship {

  private UID myServiceContractUID;


  /** no-arg constructor */
  public ServiceContractRelationshipImpl() {
    super();
  }


   /** constructor for factory use that takes the start, end, role, 
    *  direct and indirect objects 
    **/
  public ServiceContractRelationshipImpl(long startTime, long endTime , 
					 Role providerRole, HasRelationships provider, 
					 HasRelationships client,
					 ServiceContractRelay relay) {
    super(startTime, endTime, providerRole, provider, client);
    setServiceContractUID(relay);
  }

  /** UID for the associated ServiceContractRelay
   * @return UID UID for the associated service contract
   */
  public UID getServiceContractUID() {
    return myServiceContractUID;
  }

  /**
   * 
   * @param relay ServiceContractRelay associated with the relationship
   */
  public void setServiceContractUID(ServiceContractRelay relay) {
    myServiceContractUID = relay.getUID();
  }

  /** 
   * equals - performs field by field comparison
   *
   * @param object Object to compare
   * @return boolean if 'same' 
   */
  public boolean equals(Object object) {
    if (!(object instanceof ServiceContractRelationship)) {
      return false;
    }

    if (super.equals(object)) {
      return getServiceContractUID().equals(((ServiceContractRelationship) object).getServiceContractUID());
    } else {
      return false;
    }
  }
    
  public String toString() {
    return super.toString() + 
      "<ServiceContractRelay UID:" + getServiceContractUID();
  }
}










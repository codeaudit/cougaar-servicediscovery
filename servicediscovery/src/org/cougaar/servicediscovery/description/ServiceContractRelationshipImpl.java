/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
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

import java.util.Collection;
import java.util.Iterator;

import org.cougaar.core.util.UID;
import org.cougaar.util.TimeSpan;

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










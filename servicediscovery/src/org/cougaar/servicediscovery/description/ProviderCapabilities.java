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

import org.cougaar.core.util.UniqueObject;
import org.cougaar.planning.ldm.plan.Role;

import java.util.Collection;

/**
 * A Collection which maintains an ordered list of provider capabilities
 */

public interface ProviderCapabilities extends java.io.Serializable, UniqueObject {

  /**
   * @return the name of the provider
   **/
  public String getProviderName();

  /**
   * @param providerName
   **/
  public void setProviderName(String providerName);


  /**
   * @return the  provider capabilities
   */ 
  public Collection getCapabilities();


  /**
   * Add a provider capability
   * @param role
   * @param echelon
   */
   public void addCapability(Role role, String echelon);

  /**
   * Add a provider capability
   * @param capability
   */
   public void addCapability(ProviderCapability capability);
}






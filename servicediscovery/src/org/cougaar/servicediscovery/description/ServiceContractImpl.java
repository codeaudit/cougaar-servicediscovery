/*
 * <copyright>
 *  Copyright 2002 BBNT Solutions, LLC
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

import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;


/**
 * The ServiceContract describes the particular service which the client agent
 * woul like to receive from the provider agent.
 */

public class ServiceContractImpl implements ServiceContract, java.io.Serializable {
  private static Logger logger = Logging.getLogger(ServiceContractImpl.class);

  private Asset myProvider;
  private Role myServiceRole;
  private Collection myServicePreferences;
  private boolean isRevoked;

  public ServiceContractImpl() {
  }


  public ServiceContractImpl(Asset provider, Role serviceRole, Collection
			    preferences) {
    setProvider(provider);
    setServiceRole(serviceRole);
    setServicePreferences(preferences);
    isRevoked = false;
  }


  /**
   * Returns the asset representing the provider
   */
  public Asset getProvider() {
    return myProvider;
  }

  /**
   * Sets the asset representing the provider
   *
   */
  public void setProvider(Asset provider) {
    // Must be cloned by caller
    myProvider = provider;
  }

  /**
   * Returns the provided service role
   * service.
   */
  public Role getServiceRole() {
    return myServiceRole;
  }

  /**
   * Sets the provided service role
   *
   */
  public void setServiceRole(Role serviceRole) {
    myServiceRole = serviceRole;
  }

  /**
   * Returns a read only collection of Preferences for the provided service.
   */
  public Collection getServicePreferences() {
    if (myServicePreferences == null) {
      return Collections.EMPTY_LIST;
    } else {
      return Collections.unmodifiableCollection(myServicePreferences);
    }
  }

  /**
   * Sets the provided service preferences for this service.
   */
  public void setServicePreferences(Collection servicePreferences) {
    myServicePreferences = new ArrayList(servicePreferences);
  }

  /**
   * Indicate that this service contract has been revoked
   */
  public void revoke() {
    isRevoked = true;
  }

  /**
   * Returns a boolean indicating whether this service contract has been revoked
   */
  public boolean isRevoked() {
    return isRevoked;
  }

}









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
 * The ServiceRequest describes the particular service which the client agent
 * woul like to receive from the provider agent.
 */

public class ServiceRequestImpl implements ServiceRequest, java.io.Serializable {
  private static Logger logger = Logging.getLogger(ServiceRequestImpl.class);

  private Asset myClient;
  private Role myServiceRole;
  private Collection myServicePreferences;

  public ServiceRequestImpl() {
  }

  
  public ServiceRequestImpl(Asset client, Role serviceRole, Collection
			    preferences) {
    setClient(client);
    setServiceRole(serviceRole);
    setServicePreferences(preferences);
  }


  /**
   * Returns the asset representing the client
   */
  public Asset getClient() {
    return myClient;
  }

  /**
   * Sets the asset representing the client
   * 
   */
  public void setClient(Asset client) {
    // Must be cloned by caller
    myClient = client;
  }

  /**
   * Returns the requested service role
   * service.
   */
  public Role getServiceRole() {
    return myServiceRole;
  }

  /**
   * Sets the asset representing the client
   * 
   */
  public void setServiceRole(Role serviceRole) {
    myServiceRole = serviceRole;
  }

  /**
   * Returns a read only collection Preferences for the requested service
   * service.
   */
  public Collection getServicePreferences() {
    if (myServicePreferences == null) {
      return Collections.EMPTY_LIST;
    } else { 
      return Collections.unmodifiableCollection(myServicePreferences);
    }
  }

  /**
   * Sets the service preferences for this service.
   */
  public void setServicePreferences(Collection servicePreferences) {
    myServicePreferences = new ArrayList(servicePreferences);
  }

}









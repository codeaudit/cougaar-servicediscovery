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

import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * The ServiceDescription describes a particular service that
 * is provided by the provider agent.
 */

public class ServiceDescriptionImpl implements ServiceDescription, java.io.Serializable {
  private static Logger logger = Logging.getLogger(ServiceDescriptionImpl.class);  

  private String myProviderName;
  private Collection myServiceClassifications;
  private Collection myServiceBindings;
  
  public ServiceDescriptionImpl() {
  }

  public ServiceDescriptionImpl(ServiceInfo serviceInfo) {
    myProviderName = serviceInfo.getProviderName();
    myServiceClassifications = serviceInfo.getServiceClassifications();
    myServiceBindings = serviceInfo.getServiceBindings();
  }
    

  /**
   * Returns the provider (agent) name.
   */
  public String getProviderName() {
    return myProviderName;
  }

  /**
   * Sets the provider (agent) name.
   * Name can only be set once.
   */
  public void setProviderName(String providerName) {
    if (myProviderName != null) {
      logger.error("Attempt to reset provider name.");
    } else {
      myProviderName = new String(providerName);
    }
  }

  /**
   * Returns a read only collection of the  service classifications for this
   * service.
   */
  public Collection getServiceClassifications() {
    if (myServiceClassifications == null) {
      return Collections.EMPTY_LIST;
    } else { 
      return Collections.unmodifiableCollection(myServiceClassifications);
    }
  }
   
  /**
   * Sets the service classifications for this service. Service 
   * classifications can only be set once.
   */
  public void setServiceClassifications(Collection serviceClassifications) {
    if (myServiceClassifications != null) {
      logger.error("Attempt to reset service classifications.");
    } else {
      myServiceClassifications = new ArrayList(serviceClassifications);
    }
  }

  /**
   * Returns the ServiceBinding for this service
   */
  public Collection getServiceBindings() {
    return myServiceBindings;
  }

  /**
   * Sets the ServiceBinding for this service. ServiceBinding can only be set
   * once.
   */
  public void setServiceBinding(Collection serviceBindings) {
    if (myServiceBindings != null) {
      logger.error("Attempt to reset service bindings.");
    } else {
      myServiceBindings = new ArrayList(serviceBindings);
    }
  }
}






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

import java.util.Collection;


/**
 * Contains the provider information returned from a registry query.
 */

public class ProviderInfo{

  String providerName = null;
  Collection businessClassifications = null;
  Collection serviceInfos = null;

  public ProviderInfo() {}

  /**
   * Constructs a ProviderInfo object.
   * @param providerName name of this provider
   * @param businessClassifications all classifications of this provider
   * @param serviceInfos all services provided by this Provider
   */
  public ProviderInfo(String providerName, Collection businessClassifications,
                      Collection serviceInfos) {
    this.providerName = providerName;
    this.businessClassifications = businessClassifications;
    this.serviceInfos = serviceInfos;
  }

  /**
   * Sets the name of this provider.
   * @param providerName name of this provider
   */
  public void setProviderName(String providerName) {
    this.providerName = providerName;
  }

  /**
   * Return the name of this provider.
   * @return String name of this provider
   */
  public String getProviderName() {
    return providerName;
  }

  /**
   * Sets all the classifications of this provider.
   * @param businessClassifications Collection of BusinessClassifications
   */
  public void setBusinessClassifications(Collection businessClassifications) {
    this.businessClassifications = businessClassifications;
  }

  /**
   * Returns all the BusinessClassifications of this provider.
   * @return Collection of BusinessClassifications, empty if none exist.
   */
  public Collection getBusinessClassifications() {
    return businessClassifications;
  }

  /**
   * Sets all ServiceInfos of this service.
   * @param serviceInfos Collection of ServiceInfos
   */
  public void setServiceInfos(Collection serviceInfos) {
    this.serviceInfos = serviceInfos;
  }

  /**
   * Returns all ServiceInfos of this service.
   * @return Collection of ServiceInfos, empty if none exist.
   */
  public Collection getServiceInfos() {
    return this.serviceInfos;
  }

  /**
   * Compares with other ProviderInfo object.
   * ProviderInfo objects are considered to be equal if the provider name
   * are the same.
   */
  public boolean equals(Object obj) {
    if(obj instanceof ProviderInfo) {
      ProviderInfo other = (ProviderInfo) obj;
      return this.providerName.equals(other.getProviderName());
    }
    return false;
  }
}

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

package org.cougaar.servicediscovery.transaction;

import java.util.ArrayList;
import java.util.Collection;

import org.cougaar.servicediscovery.description.BusinessClassification;
import org.cougaar.servicediscovery.description.ServiceClassification;

/**
 * Sets the provider and service attributes for this RegistryQuery.
 */
public class RegistryQueryImpl implements RegistryQuery {
  private String providerName = null;
  private Collection businessClassifications = null;
  private String serviceName = null;
  private Collection serviceClassifications = null;
  private Collection findQualifiers = null;

  public RegistryQueryImpl() {
  }

  public RegistryQueryImpl(String providerName, Collection businessClassifications,
                           String serviceName, Collection serviceClassifications) {
    this.providerName = providerName;
    this.businessClassifications = businessClassifications;
    this.serviceName = serviceName;
    this.serviceClassifications = serviceClassifications;
  }

  public void setProviderName(String providerName) {
    this.providerName = providerName;
  }

  public String getProviderName() {
    return providerName;
  }

  public void setBusinessClassifications(Collection businessClassifications) {
    this.businessClassifications = businessClassifications;
  }

  public Collection getBusinessClassifications() {
    return this.businessClassifications;
  }

  public void addBusinessClassification(BusinessClassification bc) {
    if (businessClassifications == null) {
      businessClassifications = new ArrayList();
    }
    businessClassifications.add(bc);
  }

    public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceClassifications(Collection serviceClassifications) {
    this.serviceClassifications = serviceClassifications;
  }

  public Collection getServiceClassifications() {
    return this.serviceClassifications;
  }

  public void addServiceClassification(ServiceClassification sc) {
    if (serviceClassifications == null) {
      serviceClassifications = new ArrayList();
    }
    serviceClassifications.add(sc);
  }

  public void orLikeKeysQualifier() {
    if (findQualifiers == null) {
      findQualifiers = new ArrayList(1);
    }
    findQualifiers.add("orLikeKeys");
  }

  public Collection getFindQualifiers() {
    return findQualifiers;
  }
}

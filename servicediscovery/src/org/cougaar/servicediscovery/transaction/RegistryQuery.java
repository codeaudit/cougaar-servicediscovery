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

import java.util.Collection;

import org.cougaar.servicediscovery.description.BusinessClassification;
import org.cougaar.servicediscovery.description.ServiceClassification;

/**
 * This interface defines the provider and service attributes used in a query
 * to find a provider.  These attributes are only a subset of the searchable
 * criteria and are dependent on the registry implementation.
 */
public interface RegistryQuery {

  public void setServiceName(String serviceName);

  public String getServiceName();

  public void setServiceClassifications(Collection serviceClassifications);

  public Collection getServiceClassifications();
  
  public void addServiceClassification(ServiceClassification sc);

  public void setProviderName(String providerName);

  public String getProviderName();

  public void setBusinessClassifications(Collection businessClassifications);

  public Collection getBusinessClassifications();

  public void addBusinessClassification(BusinessClassification bc);

  public void orLikeKeysQualifier();

  public Collection getFindQualifiers();

}

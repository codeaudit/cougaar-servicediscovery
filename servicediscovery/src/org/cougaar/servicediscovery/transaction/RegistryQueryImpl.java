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

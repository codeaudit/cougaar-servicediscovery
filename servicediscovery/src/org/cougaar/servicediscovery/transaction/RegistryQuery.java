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

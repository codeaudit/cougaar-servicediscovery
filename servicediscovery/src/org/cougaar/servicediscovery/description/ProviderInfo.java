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
   * are the same.  Note: In general provider names are not guaranteed
   * to be unique and this should be changed.
   */
  public boolean equals(Object obj) {
    if(obj instanceof ProviderInfo) {
      ProviderInfo other = (ProviderInfo) obj;
      return this.providerName.equals(other.getProviderName());
    }
    return false;
  }
}

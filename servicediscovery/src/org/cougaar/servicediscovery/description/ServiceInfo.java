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
import java.util.Iterator;


/**
 * Contains Service information returned from a registry query.
 */

public class ServiceInfo{
  String serviceName = null;
  String serviceId = null;
  Collection serviceClassifications = null;
  Collection serviceBindings = null;
  String providerName = null;
  Collection businessClassifications = null;

  public ServiceInfo() {}

  /**
   * Constructs a ServiceInfo object.
   * @param serviceName name of this service
   * @param serviceId unique indentifier of this service
   * @param serviceClassifications all classifications of this service
   * @param serviceBindings all service bindings for this service
   * @param providerName provider of this service
   * @param businessClassifications all classifications of the provider
   */
  public ServiceInfo(String serviceName, String serviceId,
                     Collection serviceClassifications,
                     Collection serviceBindings,
                     String providerName,
                     Collection businessClassifications) {
    this.serviceName = serviceName;
    this.serviceId = serviceId;
    this.serviceClassifications = serviceClassifications;
    this.serviceBindings = serviceBindings;
    this.providerName = providerName;
    this.businessClassifications = businessClassifications;
  }

  /**
   * Sets the name of this service.
   * @param serviceName name of this service
   */
  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  /**
   * Return the name of this service.
   * @return String name of this service
   */
  public String getServiceName() {
    return serviceName;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  public String getServiceId() {
    return serviceId;
  }

  /**
   * Sets all ServiceClassifications of this service.
   * @param serviceClassifications Collection of ServiceClassifications.
   */
  public void setServiceClassifications(Collection serviceClassifications) {
    this.serviceClassifications = serviceClassifications;
  }

  /**
   * Returns all ServiceClassifications of this service.
   * @return Collection of ServiceClassifications, null if none exist.
   */
  public Collection getServiceClassifications() {
    return serviceClassifications;
  }

  /**
   * Set the service bindings for this service.
   * @param serviceBindings Collection of ServiceBindings.
   */
  public void setServiceBindings(Collection serviceBindings) {
    this.serviceBindings = serviceBindings;
  }

  /**
   * Returns all ServiceBindings of this service.
   * @return Collection of ServiceBindings, null if none exist.
   */
  public Collection getServiceBindings() {
    return serviceBindings;
  }

  public void setProviderName(String providerName) {
    this.providerName = providerName;
  }

  public String getProviderName() {
    if (this.providerName != null) {
      return this.providerName;
    } else {
      return getMessageAddress();
    }
  }

  /**
   * Sets all BusinessClassifications of this service.
   * @param businessClassifications Collection of BusinessClassifications.
   */
  public void setBusinessClassifications(Collection businessClassifications) {
    this.businessClassifications = businessClassifications;
  }

  /**
   * Returns all BusinessClassifications of this service.
   * @return Collection of BusinessClassifications, empty if none exist.
   */
  public Collection getBusinessClassifications() {
    return businessClassifications;
  }

  /**
   * Compares with other ServiceInfo object.
   * ServiceInfo objects are considered to be equal if the provider name
   * and serviceId are the same.
   */
  public boolean equals(Object obj) {
    if(obj instanceof ServiceInfo){
      ServiceInfo other = (ServiceInfo) obj;
      String thisOne = this.getProviderName()+this.serviceId;
      return thisOne.equals(other.getProviderName()+other.getServiceId());
    }
    return false;
  }

  /**
   * Temporary method to provide another way to get the provider name from the UDDI registry
   * that is less costly.  The other code is using provider name as a means to contact the
   * provider.  Provider names in Cougaar are often, but not limited to, the MessageAddress i.e.,
   * the address of the agent providing the service. Provider name may not be the correct usage
   * in terms of how one agent accesses a another agent's service and we should rethink the usage
   * of this API.
   * @return String address of the agent.
   */
  private String getMessageAddress() {
    String messageAddress = null;
    if (this.serviceBindings != null) {
      for (Iterator iter = this.serviceBindings.iterator(); iter.hasNext();) {
        ServiceBinding serviceBinding = (ServiceBinding) iter.next();
        messageAddress = serviceBinding.getMessageAddress();
      }
    }
    return messageAddress;
  }
}
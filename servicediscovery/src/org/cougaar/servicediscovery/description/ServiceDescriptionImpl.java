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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

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






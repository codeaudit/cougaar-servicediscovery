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

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.relay.Relay;

import org.cougaar.planning.ldm.asset.Asset;

import org.cougaar.servicediscovery.description.ServiceContract;
import org.cougaar.servicediscovery.description.ServiceRequest;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;


/**
 * Relay used to request command chain lineage
 **/
public class ServiceContractRelay extends RelayAdapter {
  private static Logger myLogger = Logging.getLogger(ServiceContractRelay.class);

  private String myProviderName;
  private ServiceContract myServiceContract;
  private ServiceRequest myServiceRequest;

  private transient String myToString = null;

  public ServiceContractRelay() {
    super();
  }

  public ServiceContractRelay(ServiceRequest request) {
    super();
    setServiceRequest(request);
  }

  /**
   * Gets the name of the Agent from whom the service is requested
   *
   * @return String Name of the agent
   */
  public String getProviderName() {
    return myProviderName;
  }

  /**
   * Returns  the service contract provided by the provider agent
   *
   * @return ServiceContract  service contract as specified by the provider
   * agent.
   */
  public ServiceContract getServiceContract() {
    return myServiceContract;
  }

  /**
   * Sets the service contract as specified by the provider
   *
   * @param serviceContract ServiceContract as specified by the provider
   */
  public void setServiceContract(ServiceContract serviceContract) {
    myServiceContract = serviceContract;
    myToString = null;
  }

  /**
   * Returns the provider asset from the ServiceContract 
   *
   * @return Asset provider asset as specified by the ServiceContract
   * agent.
   */
  public Asset getProvider() {
    if (getServiceContract() != null) {
      return getServiceContract().getProvider();
    } else {
      return null;
    }
  }

  /**
   * Returns the service request from the client
   *
   * @return ServiceRequest  service request as specified by the client
   * agent.
   */
  public ServiceRequest getServiceRequest() {
    return myServiceRequest;
  }

  /**
   * Sets the service request (request should be only be set by the client)
   *
   * @param serviceRequest ServiceRequest as specified by the client
   */
  public void setServiceRequest(ServiceRequest serviceRequest) {
    myServiceRequest = serviceRequest;
    myToString = null;
  }

  /**
   * Returns the client asset from the ServiceRequest 
   *
   * @return Asset client asset as specified by the ServiceRequest
   * agent.
   */
  public Asset getClient() {
    if (getServiceRequest() != null) {
      return getServiceRequest().getClient();
    } else {
      return null;
    }
  }

  /**
   * Add a target message address.
   * @param target the address of the target agent.
   **/
  public void addTarget(MessageAddress target) {
    if ((myTargetSet != null) && 
	(myTargetSet.size() > 0)) {
      myLogger.error("Attempt to set multiple targets.");
      return;
    }

    super.addTarget(target);

    myProviderName = target.toString();
  }

  /**
   * Set the response that was sent from a target. For LP use only.
   * This implementation assumes that response is always different.
   * or used.
   **/
  public int updateResponse(MessageAddress target, Object response) {
    ServiceContractRelay serviceContractRelay = 
      (ServiceContractRelay) response;

    setServiceContract(serviceContractRelay.getServiceContract()); 
    
    return Relay.RESPONSE_CHANGE;
  }

  // Relay.Target:

  public Object getResponse() {
    return (myServiceContract != null ? this : null);
  }

  public String toString() {
    if (myToString == null) {
      myToString = getClass() + ": provider=<" + getProviderName() + 
	">, serviceRequest=<" +
        getServiceRequest() +
	">, serviceContract=<" +
        getServiceContract() +
        ">, UID=<" + getUID() + ">";
      myToString = myToString.intern();
    }

    return myToString;
  }
  
}











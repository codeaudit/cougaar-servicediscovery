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

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.relay.Relay;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.servicediscovery.SDFactory;
import org.cougaar.servicediscovery.description.ServiceContract;
import org.cougaar.servicediscovery.description.ServiceRequest;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * Relay used to request command chain lineage
 **/
public class ServiceContractRelayImpl extends RelayAdapter 
  implements ClientServiceContractRelay, ProviderServiceContractRelay {
  private static Logger myLogger = Logging.getLogger(ServiceContractRelay.class);

  private String myProviderName;
  private ServiceContract myServiceContract;
  private ServiceRequest myServiceRequest;

  private transient String myToString = null;

  public ServiceContractRelayImpl() {
    super();
  }

  public ServiceContractRelayImpl(ServiceRequest request) {
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
   * Sets the service contract as specified by the provider and
   * return true. Caller responsible for verifying compatibility.
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
    }
    
    return myToString;
  }
    
  /* Assume all changes are meaningful.   
   */
  protected boolean contentChanged(RelayAdapter content) {
    ServiceContractRelay serviceContractRelay =
      (ServiceContractRelay) content;
    
    setServiceRequest(serviceContractRelay.getServiceRequest());
    
    return true;
  }

}











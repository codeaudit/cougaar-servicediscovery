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

import org.cougaar.core.relay.Relay;
import org.cougaar.planning.ldm.asset.Asset;

import org.cougaar.servicediscovery.description.ServiceContract;
import org.cougaar.servicediscovery.description.ServiceRequest;

/**
 * Relay used to request command chain lineage
 **/
public interface ServiceContractRelay extends Relay {
  /**
   * Gets the name of the Agent from whom the service is requested
   *
   * @return String Name of the agent
   */
  public String getProviderName();

  /**
   * Returns  the service contract provided by the provider agent
   *
   * @return ServiceContract  service contract as specified by the provider
   * agent.
   */
  public ServiceContract getServiceContract();

  /**
   * Returns the provider asset from the ServiceContract
   *
   * @return Asset provider asset as specified by the ServiceContract
   * agent.
   */
  public Asset getProvider();

  /**
   * Returns the service request from the client
   *
   * @return ServiceRequest  service request as specified by the client
   * agent.
   */
  public ServiceRequest getServiceRequest();

  /**
   * Returns the client asset from the ServiceRequest
   *
   * @return Asset client asset as specified by the ServiceRequest
   * agent.
   */
  public Asset getClient();
}











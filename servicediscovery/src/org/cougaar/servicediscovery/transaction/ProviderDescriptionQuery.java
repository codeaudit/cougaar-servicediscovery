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

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.servicediscovery.description.ProviderDescription;

/**
 * Interface for requesting provider descriptons.
 */
public interface ProviderDescriptionQuery extends UniqueObject {
  
  /**
   * UID support from unique-object.
   * @return The UID of the object.
   */
  UID getUID();
  
  /**
   * Address of the requesting agent.
   * @return The address of the agent.
   */
  MessageAddress getSource();
  
  /**
   * Address of the agent that was contacted.
   * @return The address of the agent.
   */
  MessageAddress getTarget();

  /**
   * Unique identifier of the request for provider descriptions.
   * @return UID of the request object.
   */
  UID getRequestUID();

  /**
   * The key for looking up the provider's description.
   * @return Key that maps to provider's description.
   */
  String getKey();

  /**
   * Provider description describing the provider and its service.
   * @return ProviderDescription matching the request.  
   */
  ProviderDescription getProviderDescription();
  
  /**
   * Sets the provider description.
   * @param pd the ProviderDescription
   */
  void setProviderDescription(ProviderDescription pd);
}

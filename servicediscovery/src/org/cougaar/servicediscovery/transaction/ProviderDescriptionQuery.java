/*
 * <copyright>
 *  Copyright 2002 BBNT Solutions, LLC
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
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
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

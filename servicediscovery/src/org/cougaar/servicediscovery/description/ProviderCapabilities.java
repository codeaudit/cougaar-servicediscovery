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

import org.cougaar.core.util.UniqueObject;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.Schedule;

/**
 * A Collection which maintains an ordered list of provider capabilities
 */

public interface ProviderCapabilities extends java.io.Serializable, UniqueObject {

  /**
   * @return the name of the provider
   **/
  public String getProviderName();

  /**
   * @param providerName
   **/
  public void setProviderName(String providerName);


  /**
   * @return the  provider capabilities
   */ 
  public Collection getCapabilities();

  /**
   * @return provider capability for a specific Role
   */ 
  public ProviderCapability getCapability(Role role);


  /**
   * Add a provider capability
   * @param role
   * @param echelon
   * @param availableSchedule
   */
   public void addCapability(Role role, String echelon, 
			     Schedule availableSchedule);

  /**
   * Add a provider capability
   * @param capability
   */
   public void addCapability(ProviderCapability capability);
}






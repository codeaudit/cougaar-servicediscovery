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

import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.plan.Role;

/*
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;
*/

/**
 * The ServiceContract describes the particular service which the client agent
 * woul like to receive from the provider agent.
 */

public class ServiceContractImpl implements ServiceContract, java.io.Serializable {
  // private static Logger logger = Logging.getLogger(ServiceContractImpl.class);

  private Asset myProvider;
  private Role myServiceRole;
  private Collection myServicePreferences;
  private boolean isRevoked;

  public ServiceContractImpl() {
  }


  public ServiceContractImpl(Asset provider, Role serviceRole, Collection
			    preferences) {
    setProvider(provider);
    setServiceRole(serviceRole);
    setServicePreferences(preferences);
    isRevoked = false;
  }


  /**
   * Returns the asset representing the provider
   */
  public Asset getProvider() {
    return myProvider;
  }

  /**
   * Sets the asset representing the provider
   *
   */
  public void setProvider(Asset provider) {
    // Must be cloned by caller
    myProvider = provider;
  }

  /**
   * Returns the provided service role
   * service.
   */
  public Role getServiceRole() {
    return myServiceRole;
  }

  /**
   * Sets the provided service role
   *
   */
  public void setServiceRole(Role serviceRole) {
    myServiceRole = serviceRole;
  }

  /**
   * Returns a read only collection of Preferences for the provided service.
   */
  public Collection getServicePreferences() {
    if (myServicePreferences == null) {
      return Collections.EMPTY_LIST;
    } else {
      return Collections.unmodifiableCollection(myServicePreferences);
    }
  }

  /**
   * Sets the provided service preferences for this service.
   */
  public void setServicePreferences(Collection servicePreferences) {
    myServicePreferences = new ArrayList(servicePreferences);
  }

  /**
   * Indicate that this service contract has been revoked
   */
  public void revoke() {
    isRevoked = true;
  }

  /**
   * Returns a boolean indicating whether this service contract has been revoked
   */
  public boolean isRevoked() {
    return isRevoked;
  }

}









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
 * The ServiceRequest describes the particular service which the client agent
 * woul like to receive from the provider agent.
 */

public class ServiceRequestImpl implements ServiceRequest, java.io.Serializable {
  //private static Logger logger = Logging.getLogger(ServiceRequestImpl.class);

  private Asset myClient;
  private Role myServiceRole;
  private Collection myServicePreferences;
  private transient String myToString = null;

  public ServiceRequestImpl() {
  }

  
  public ServiceRequestImpl(Asset client, Role serviceRole, Collection
			    preferences) {
    setClient(client);
    setServiceRole(serviceRole);
    setServicePreferences(preferences);
  }


  /**
   * Returns the asset representing the client
   */
  public Asset getClient() {
    return myClient;
  }

  /**
   * Sets the asset representing the client
   * 
   */
  public void setClient(Asset client) {
    // Must be cloned by caller
    myClient = client;
  }

  /**
   * Returns the requested service role
   * service.
   */
  public Role getServiceRole() {
    return myServiceRole;
  }

  /**
   * Sets the asset representing the client
   * 
   */
  public void setServiceRole(Role serviceRole) {
    myServiceRole = serviceRole;
  }

  /**
   * Returns a read only collection Preferences for the requested service
   * service.
   */
  public Collection getServicePreferences() {
    if (myServicePreferences == null) {
      return Collections.EMPTY_LIST;
    } else { 
      return Collections.unmodifiableCollection(myServicePreferences);
    }
  }

  /**
   * Sets the service preferences for this service.
   */
  public void setServicePreferences(Collection servicePreferences) {
    myServicePreferences = new ArrayList(servicePreferences);
  }

  public String toString() {
    if (myToString == null)
      myToString = "<ServiceRequestImpl: Role: " + myServiceRole + ", Client: " + getClient() + ", # ServicePrefs: " + getServicePreferences().size() + ">";
    return myToString;
  }
}









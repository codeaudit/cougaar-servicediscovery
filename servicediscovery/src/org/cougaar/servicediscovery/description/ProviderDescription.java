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

/**
 * The ProviderDescription object represents a description of a provider
 * agent. Each provider should have only one corresponding ProviderDescription.
 * A ProviderDescription is somewhat analogous to the UDDI concept of a
 * BusinessEntity. In a cougaar society, it is required that every agent has
 * a unique name; thus ProviderDescription provider names are also unique.
 */

public interface ProviderDescription {

  /**
   * Returns the provider (agent) name.
   */
  public String getProviderName();

  public String getOrganizationType();
  public Collection getBusinessCategories();

  /**
   * Returns the ServiceProfiles for this provider;
   * will be empty if this provider does not describe any
   * services. The Collection contains ServiceProfiles.
   */
  public Collection getServiceProfiles();


  /**
   * Writes the OWL files that describe this provider. The
   * outputFileBase may be an entire directory and base, such
   * as "C:/cougaar/servicedisovery/data/agentXYZ" or just a base
   * such as "agentXYZ". If outputFileBase is the empty string,
   * the provider name will be used as a default base. Right
   * now the only file that will be written is outputFileBase.profile.owl,
   * but later grounding file(s) will aslo be written.
   */
  public void writeOWLFiles(String outputFileBase);


  /**
   * Returns a URI that can be used to get the ProviderDescription.
   * Typically the URI might be something like "cougaar://125-FSB"
   */
  public String getProviderDescriptionURI();

  /**
   * Would be some other methods for accessing additional characteristics
   * of the provider, such as their location and contact information.
   */

  public boolean parseOWL(String fileName);
}

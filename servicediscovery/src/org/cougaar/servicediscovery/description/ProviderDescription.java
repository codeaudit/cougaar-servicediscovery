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
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
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
   * Writes the DAMLS files that describe this provider. The
   * outputFileBase may be an entire directory and base, such
   * as "C:/cougaar/servicedisovery/data/agentXYZ" or just a base
   * such as "agentXYZ". If outputFileBase is the empty string,
   * the provider name will be used as a default base. Right
   * now the only file that will be written is outputFileBase.profile.daml,
   * but later grounding file(s) will aslo be written.
   */
  public void writeDAMLSFiles(String outputFileBase);


  /**
   * Returns a URI that can be used to get the ProviderDescription.
   * Typically the URI might be something like "cougaar://125-FSB"
   */
  public String getProviderDescriptionURI();

  /**
   * Would be some other methods for accessing additional characteristics
   * of the provider, such as their location and contact information.
   */

  public boolean parseDAML(String fileName);
}
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
package org.cougaar.servicediscovery.description;

import java.util.Collection;

/**
 * The ServiceProfile describes a particular service that
 * is provided by the provider agent, and is part of a ProviderDescription.
 * A ProviderDescription can contain zero or more ServiceProfiles.
 * A ServiceProfile is somewhat analogous to the UDDI BusinessService.
 */

public interface ServiceProfile {

  /**
   * Returns a human readable text description of the service provided
   */
  public String getTextDescription();

  public String getEchelonOfSupport();

  public Collection getServiceCategories();

  /**
   * Returns the URI where the grounding/binding file can be fouund.
   * Typically this file will be a WSDL file. The URI might be something
   * like "http://localhost:8080/DLAHQ/grounding"
   */
  public String getServiceGroundingURI();

  /**
   * Returns the binding type specified for this grounding.
   * We expect "SOAP" or "COUGAAR" as options.
   */
  public String getServiceGroundingBindingType();

  public String getServiceProfileID();

}

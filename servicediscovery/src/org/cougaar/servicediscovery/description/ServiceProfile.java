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

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

import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * Service binding info for the provider
 */

public class ServiceBinding implements java.io.Serializable {
  public static final String SOAP_BINDING = "SOAP:Binding";
  public static final String COUGAAR_BINDING = "COUGAAR:Binding";

  private static Logger logger = Logging.getLogger(ServiceBinding.class);
 
  private String myURI = null;
  private String myBindingType = null;
  private String messageAddress = null;

  public ServiceBinding() {
  }
  
  public ServiceBinding(String uri, String bindingType) {
    setURI(uri);
    setBindingType(bindingType);
  }

  public ServiceBinding(String uri, String bindingType, String messageAddress) {
    setURI(uri);
    setBindingType(bindingType);
    setMessageAddress(messageAddress);
  }

  public String getURI() {
    return myURI;
  }

  public void setURI(String uri) {
    if (myURI != null) {
      logger.error("Attempt to reset URI for " + this);
    } else {
      myURI = new String(uri);
    }
  }

  public String getBindingType() {
    return myBindingType;
  }

  public void setMessageAddress(String messageAddress) {
    this.messageAddress = messageAddress;
  }

  public String getMessageAddress() {
    return messageAddress;
  }

  public void setBindingType(String bindingType) {
    if (myBindingType != null) {
      logger.error("Attempt to reset binding type for " + this);
    } else {
      if (!validType(bindingType)) {
        logger.error("Unrecognized binding type: " + bindingType);
      }
      myBindingType = new String(bindingType);
    }
  }

  public static boolean validType(String bindingType) {
    return ((bindingType.equals(COUGAAR_BINDING)) ||
	    (bindingType.equals(SOAP_BINDING)));
  }
}








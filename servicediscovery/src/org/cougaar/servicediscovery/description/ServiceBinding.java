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








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

import org.cougaar.core.util.UID;
import org.cougaar.servicediscovery.description.MMQuery;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Collection;

public class MMQueryRequestImpl implements NewMMQueryRequest {
  private static Logger myLogger = Logging.getLogger(MMQueryRequestImpl.class);

  private MMQuery myQuery;
  private UID myUID;
  private Collection myResult;
  private int myResultCode;

  public MMQueryRequestImpl(MMQuery mmQuery) {
    myQuery = mmQuery;
  }


  public MMQueryRequestImpl() {
    super();
  }


  public void setQuery(MMQuery mmQuery) {
    myQuery = mmQuery;
  }

  public  MMQuery getQuery() {
    return myQuery;
  }

  public void setResult(Collection result) {
    myResult = result;
  }

  public Collection getResult() {
    return myResult;
  }


  public void setResultCode(int code) {
    myResultCode = code;
  }

  public int getResultCode() {
    return myResultCode;
  }


  public void setUID(UID newUID) {
    if (myUID != null) {
      myLogger.error("Attempt to reset UID.");
      return;
    } 

    myUID = newUID;
  }

  public UID getUID() {
    return myUID;
  }
}


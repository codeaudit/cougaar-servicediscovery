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

package org.cougaar.servicediscovery.transaction;

import java.util.Collection;

import org.cougaar.core.util.UID;
import org.cougaar.servicediscovery.description.MMQuery;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

public class MMQueryRequestImpl implements NewMMQueryRequest, java.io.Serializable {
  private static Logger myLogger = Logging.getLogger(MMQueryRequestImpl.class);

  private MMQuery myQuery = null;
  private UID myUID = null;
  private Collection myResult = null;
  private int myResultCode = 0;
  private int myQueryCount = 0;

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

  public void setQueryCount(int queryCount) {
    myQueryCount = queryCount;
  }

  public int getQueryCount() {
    return myQueryCount;
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






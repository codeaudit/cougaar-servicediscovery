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
import java.util.Collections;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.relay.Relay;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * Relay used to request command chain lineage
 **/
public class LineageRelay extends RelayAdapter {
  private static Logger myLogger = Logging.getLogger(LineageRelay.class);

  private String myAgentName;
  private Collection myLineages;
  private transient String myToString = null;

  public LineageRelay() {
    super();
  }

  /**
   * Gets the name of the Agent whose lineage is reported.
   *
   * @return String Name of the agent
   */
  public String getAgentName() {
    return myAgentName;
  }

  /**
   * Returns  the lineage lists for the target agent
   *
   * @return Collection Lineages for the targent agent
   */
  public Collection getLineages() {
    Collection lists = (myLineages == null) ? 
      Collections.EMPTY_LIST : Collections.unmodifiableCollection(myLineages);

    return lists;
  }

  /**
   * Sets the Collection of lineages for the target agent
   *
   * @param lineages Collection of lineages for the agent
   */
  public void setLineages(Collection lineages) {
    myLineages = lineages;
    myToString = null;
  }

  /**
   * Add a target message address.
   * @param target the address of the target agent.
   **/
  public void addTarget(MessageAddress target) {
    if ((myTargetSet != null) && 
	(myTargetSet.size() > 0)) {
      myLogger.error("Attempt to set multiple targets.");
      return;
    }

    super.addTarget(target);

    myAgentName = target.toString();
  }

  /**
   * Set the response that was sent from a target. For LP use only.
   * This implementation assumes that response is always different.
   * or used.
   **/
  public int updateResponse(MessageAddress target, Object response) {
    LineageRelay lineageRelay = (LineageRelay) response;

    setLineages(lineageRelay.getLineages()); 
    
    return Relay.RESPONSE_CHANGE;
  }

  // Relay.Target:

  public Object getResponse() {
    return (myLineages != null ? this : null);
  }

  public String toString() {
    if (myToString == null) {
      myToString = getClass() + ": agent=<" + getAgentName() + 
	">, lineages=<" +
        getLineages() +
        ">, UID=<" + getUID() + ">";
    }

    return myToString;
  }
  
}











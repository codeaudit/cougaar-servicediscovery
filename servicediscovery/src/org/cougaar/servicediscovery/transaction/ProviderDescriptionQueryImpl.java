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

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.util.UID;
import org.cougaar.servicediscovery.description.ProviderDescription;

/**
 * Package-private implementation of a ProviderDescriptionQuery.
 * <p>
 * This uses the Relay support to transfer the data
 * between the source agent and target agent.
 */
public class ProviderDescriptionQueryImpl 
  implements ProviderDescriptionQuery, Relay.Source, Relay.Target, Serializable {

  private final UID uid;
  private final MessageAddress source;
  private final MessageAddress target;
  private final UID requestUID;
  private final String key;
  private ProviderDescription providerDesc;
  
  private transient Set _targets;
  private transient Relay.TargetFactory _factory;

  public ProviderDescriptionQueryImpl(UID uid, MessageAddress source,
                                      MessageAddress target, UID requestUID,
                                      String key) {
    this.uid = uid;
    this.source = source;
    this.target = target;
    this.requestUID = requestUID;
    this.key = key;
    
    if ((uid == null) ||
        (source == null) ||
        (target == null) ||
        (requestUID == null)||
        (key == null )) {
      throw new IllegalArgumentException("null uid/source/target/requestUID/key");
    }

    if (source.equals(target)) {
      throw new IllegalArgumentException("Source and target addresses are equal ("+
                                         uid+", "+source+", "+target+")");
    }
    cacheTargets();
  }
  
  /**
   * UID support from unique-object.
   * @return The UID of the object.
   */
  public UID getUID() {
    return uid;
  }

  /**
   * Set the UID. 
   * @param uid the UID of the object.
   */
  public void setUID(UID uid) {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Address of the requesting agent.
   * @return The address of the agent.
   */
  public MessageAddress getSource() {
    return source;
  }

  /**
   * Address of the agent that was contacted.
   * @return The address of the agent.
   */
  public MessageAddress getTarget() {
    return target;
  }
  
  /**
   * Unique identifier of the request for provider descriptions.
   * @return UID of the request object.
   */
  public UID getRequestUID() {
    return requestUID;
  }
  
  /**
   * The key for looking up the provider's description.
   * @return Key that maps to provider's description.
   */
  public String getKey() {
    return key;
  }
  
  /**
   * Provider description describing the provider and its services.
   * @return ProviderDescription matching the request.  
   */
  public ProviderDescription getProviderDescription() {
    return providerDesc;
  }

  /**
   * Set the provider description.
   * @param providerDesc the ProviderDescripton
   */
  public void setProviderDescription(ProviderDescription providerDesc) {
    this.providerDesc = providerDesc;
    // caller *must* "publishChange()" this object to make
    // the provider desc be sent back to the caller.
  }
  
  // Relay.Source:
  private void cacheTargets() {
    _targets = Collections.singleton(target);
    _factory = new ProviderDescriptionQueryImplFactory(target);
  }

  public Set getTargets() {
    return _targets;
  }

  public Object getContent() {
    return this;
  }

  public Relay.TargetFactory getTargetFactory() {
    return _factory;
  }

  public int updateResponse(
      MessageAddress target, Object response) {
    // assert targetAgent.equals(target)
    // assert response != null
    ProviderDescriptionQueryImpl r = (ProviderDescriptionQueryImpl) response;
    ProviderDescription pd = r.getProviderDescription();
    // check for change
    if ((providerDesc == null) ?
        (pd != null) :
        (!providerDesc.equals(pd))) {
      providerDesc = pd;
      return Relay.RESPONSE_CHANGE;
    } else {
      return Relay.NO_CHANGE;
    }
  }

  // Relay.Target:

  public Object getResponse() {
    return (providerDesc != null ? this : null);
  }
  
  public int updateContent(Object content, Token token) {
    return Relay.NO_CHANGE;
  }
  
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof ProviderDescriptionQueryImpl)) { 
      return false;
    } else {
      UID u = ((ProviderDescriptionQueryImpl) o).uid;
      return uid.equals(u);
    }
  }
  
  public int hashCode() {
    return uid.hashCode();
  }
  
  private void readObject(java.io.ObjectInputStream os)
    throws ClassNotFoundException, java.io.IOException {
    os.defaultReadObject();
    cacheTargets();
  }
  
  public String toString() {
    return 
      "ProviderDescriptionQuery {"+
      "\n uid:         "+uid+
      "\n source:      "+source+
      "\n target:      "+target+
      "\n requestUID:  "+requestUID+
      "\n key:         "+key+
      "\n providerDesc: "+providerDesc+
      "\n}";
  }
  
  /**
   * Simple factory implementation.
   */
  private static class ProviderDescriptionQueryImplFactory
    implements Relay.TargetFactory, Serializable {
    
    private final MessageAddress target;

    public ProviderDescriptionQueryImplFactory(MessageAddress target) {
      this.target = target;
    }
    
    public Relay.Target create(UID uid, MessageAddress source, Object content,
                               Relay.Token token) {
      ProviderDescriptionQueryImpl p = (ProviderDescriptionQueryImpl) content;
      return new ProviderDescriptionQueryImpl(p.uid, p.source, p.target, p.requestUID,
                                              p.key);
    }
  }
}

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;

/**
 * Implementation of RelayAdapter.
 * Declared abstract because it does not include the ability to convey content.
 * Extenders are responsible for defining content semantics.
 **/
abstract public class RelayAdapter implements Relay.Source, Relay.Target, UniqueObject, Cloneable {

  protected transient Set myTargetSet = null;

  protected UID myUID = null;
  protected MessageAddress mySource = null;

  /**
   * Add a target message address.
   * @param target the address of the target agent.
   **/
  public void addTarget(MessageAddress target) {
    if (myTargetSet == null) {
      myTargetSet = new HashSet();
    }
    myTargetSet.add(target);
  }

  /**
   * Add a collection of target message addresses.
   * @param targets Collection of target agent addresses.
   **/
  public void addAllTargets(Collection targets) {
    for (Iterator iterator = targets.iterator(); iterator.hasNext();) {
      Object target = iterator.next();
      if (target instanceof MessageAddress) {
        addTarget((MessageAddress) target);
      } else {
        throw new IllegalArgumentException("Invalid target class: " + target.getClass() +
                                           " all targets must extend MessageAddress.");
      }
    }
  }

  /**
   * Remove a target message address.
   * @param target the address of the target agent to be removed.
   **/
  public void removeTarget(MessageAddress target) {
    if (myTargetSet != null) {
      myTargetSet.remove(target);
    }
  }

  public void clearTargets() {
    myTargetSet = null;
  }

  // UniqueObject interface
  /** @return the UID of a UniqueObject.  If the object was created
   * correctly (e.g. via a Factory), will be non-null.
   **/
  public UID getUID() {
    return myUID;
  }

  /** set the UID of a UniqueObject.  This should only be done by
   * an LDM factory.  Will throw a RuntimeException if
   * the UID was already set.
   **/
  public void setUID(UID uid) {
    if (myUID != null) {
      RuntimeException rt = new RuntimeException("Attempt to call setUID() more than once.");
      throw rt;
    }

    myUID = uid;
  }

  // Relay.Source interface

  /**
   * @return MessageAddress of the source
   */
  public MessageAddress getSource() {
    return mySource;
  }

  /** set the MessageAddress of the source.  This should only be done by
   * an LDM factory.  Will throw a RuntimeException if
   * the source was already set.
   **/
  public void setSource(MessageAddress source) {
    if (mySource != null) {
      RuntimeException rt = new RuntimeException("Attempt to call setSource() more than once.");
      throw rt;
    }
    mySource = source;
  }

  /**
   * Get all the addresses of the target agents to which this Relay
   * should be sent.
   **/
  public Set getTargets() {
    Set targets = (myTargetSet == null) ? Collections.EMPTY_SET
                                        : Collections.unmodifiableSet(myTargetSet);
    return targets;
  }

  /**
   * Set the addresses of the target agents to which this Relay
   * should be sent.
   **/
  public void setTargets(Set targets) {
    addAllTargets(targets);
  }

  /**
   * Get an object representing the value of this Relay suitable
   * for transmission. This implementation uses itself to represent
   * its Content.
   **/
  public Object getContent() {
    return this;
  }

  protected boolean contentChanged(RelayAdapter newCCN) {
    return false;
  }

  private static final class SimpleRelayFactory
    implements TargetFactory, java.io.Serializable {

    public static final SimpleRelayFactory INSTANCE = new SimpleRelayFactory();

    private SimpleRelayFactory() {}

    /**
    * Convert the given content and related information into a Target
    * that will be published on the target's blackboard.
    **/
    public Relay.Target create(
        UID uid,
        MessageAddress source,
        Object content,
        Token token) {
      RelayAdapter target = null;

      if (content instanceof RelayAdapter) {
        RelayAdapter relayAdapter =
          (RelayAdapter) content;

        if (relayAdapter.myTargetSet != null) {
          // intra-vm case so must clone
          try {
            target =
              (RelayAdapter) ((RelayAdapter) content).clone();

            // Relay.Target's should not have targets. Causes infinite loops
            if (target != null) {
              target.clearTargets();
            }

          } catch (CloneNotSupportedException cnse) {
            throw new IllegalArgumentException("content argument: " + content + " does not support clone.");
          }
        } else {
          target = relayAdapter;
        }

      } else {
        throw new IllegalArgumentException("content argument must extend RelayAdapter.");
      }

      // Use arguments to customize the target.
      if (!uid.equals(target.getUID())) {
        throw new IllegalArgumentException("uid argument does not match source's UID.");
      }
      target.setSource(source);

      return target;
    }

    private Object readResolve() {
      return INSTANCE;
    }
  };

  /**
  * Get a factory for creating the target.
  */
  public TargetFactory getTargetFactory() {
    return SimpleRelayFactory.INSTANCE;
  }

  /**
   * Set the response that was sent from a target. For LP use only.
   * This implemenation does nothing because responses are not needed
   * or used.
   **/
  public int updateResponse(MessageAddress target, Object response) {
    // No response expected
    return Relay.NO_CHANGE;
  }

  /**
   * Get the current Response for this target. Null indicates that
   * this target has no response.
   */
  public Object getResponse() {
    return null;
  }

  /**
   * Update the target with the new content.
   * @return true if the update changed the Relay, in which
   *    case the infrastructure should "publishChange" this
   */
  public int updateContent(Object content, Token token) {
    return (contentChanged((RelayAdapter) content) ?
            Relay.CONTENT_CHANGE : Relay.NO_CHANGE);
  }

  protected Object clone() throws CloneNotSupportedException {
    RelayAdapter clone;

    clone = (RelayAdapter) super.clone();

    // Make sure we have a distinct target hash set
    clone.clearTargets();
    if (getTargets().size() > 0) {
      clone.addAllTargets(getTargets());
    }
    return clone;
  }
}

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

package org.cougaar.servicediscovery.matchmaker;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * ServiceProvider for MatchMakerService.
 * <p>
 * MatchMakerService is a node level service
 */
public class MatchMakerServiceProviderComponent
extends GenericStateModelAdapter
implements Component 
{
  private ServiceBroker rootsb;
  private MatchMakerServiceProvider sp;

  public void setBindingSite(BindingSite bs) {
    // ignore
  }

  public void setNodeControlService(NodeControlService ncs) {
    if (ncs != null) {
      this.rootsb = ncs.getRootServiceBroker();
    }
  }

  /**
   * Registers MatchMakerService with node service broker
   */
  public void load(){
    super.load();

    if (sp == null &&
        rootsb != null &&
        !rootsb.hasService(MatchMakerService.class)) {
      // Register the service provider with the RootServiceBroker
      sp = new MatchMakerServiceProvider();
      rootsb.addService(MatchMakerService.class, sp);
    }
  }

  public void unload(){
    super.unload();
    if (sp != null && rootsb != null) {
      rootsb.revokeService(MatchMakerService.class, sp);
      sp = null;
    }
  }

  public class MatchMakerServiceProvider 
    implements ServiceProvider {

      private final MatchMakerService mmService = 
        new MatchMakerImpl();

      /**
       * @return <code>MatchMakerService</code> implementation
       */
      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (serviceClass == MatchMakerService.class) {
          return mmService;
        } else {
          throw new IllegalArgumentException(
              "MatchMakerServiceProvider does not provide a service for: "+
              serviceClass);
        }
      }

      /**
       * Currently does nothing
       */
      public void releaseService(
          ServiceBroker sb, Object requestor, Class serviceClass, Object service) {
        //if (service instanceof MatchMakerImpl) {
        //}
      }
    }
}

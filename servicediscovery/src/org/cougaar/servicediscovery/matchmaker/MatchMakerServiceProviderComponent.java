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
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.servicediscovery.matchmaker;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ContainerAPI;
import org.cougaar.core.component.ContainerSupport;
import org.cougaar.core.component.PropagatingServiceBroker;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.node.NodeControlService;

/**
 * ServiceProvider for MatchMakerService
 * MatchMakerService is a node level service
 */
public class MatchMakerServiceProviderComponent extends ContainerSupport implements ContainerAPI{
    
    // Implementation of abstract method for ContainerSupport
    protected ContainerAPI getContainerProxy(){
        return this;
    }
    
    // Implementation of abstract method for ContainerSupport
    protected String specifyContainmentPoint(){
        return "Node.AgentManager.Agent.MatchMakerServiceProvider";
    }
    
    /**
     * Needed for containerAPI which in turn is needed by getContainerProxy
     */
    public void requestStop() {
    }
    
    /**
     * Needed for containerAPI which in turn is needed by getContainerProxy
     */
    // borrowed from MetricsServiceProvider
    public final void setBindingSite(BindingSite bs) {
        super.setBindingSite(bs);
        setChildServiceBroker(new PropagatingServiceBroker(bs));
    }
    
    /**
     * Registers MatchMakerService with node service broker
     */
    public void load(){
        super.load();
        ServiceBroker sb = getServiceBroker();
        NodeControlService ncs = 
            (NodeControlService) sb.getService(this, NodeControlService.class, null);
            ServiceBroker rootsb = ncs.getRootServiceBroker();
            if(!rootsb.hasService(MatchMakerService.class)){
                // Register the service provider with the RootServiceBroker
                MatchMakerServiceProvider mmsp = new MatchMakerServiceProvider();
                rootsb.addService(MatchMakerService.class, mmsp);
            }
    }
    
    public void unload(){
        super.unload();
    }


    public class MatchMakerServiceProvider implements ServiceProvider {
        private MatchMakerService mmService;
        
        /**
         * Creates an implementation of <code>MatchMakerService</code> to be used within a node.
         */
        public MatchMakerServiceProvider(){
            mmService = new MatchMakerImpl();
        }
        
        /**
         * @return <code>MatchMakerService</code> implementation
         */
        public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
            if (serviceClass == MatchMakerService.class) {
                return mmService;
            } else {
                throw new IllegalArgumentException("MatchMakerServiceProvider does not provide a service for: "+
                serviceClass);
            }
        }
        
        /**
         * Currently does nothing
         */
        public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object service) {
            //if (service instanceof MatchMakerImpl) {
            //}
        }
    }
}

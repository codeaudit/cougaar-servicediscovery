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

import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.servicediscovery.description.MMQuery;

/**
 * <code>MatchMakerService</code> finds Services registered in YP Registry 
 * that satisfy the criteria set in the query of <code>NewMMQueryRequest</code>. 
 * The service is not supposed to be called directly by the client plugins
 * interested in the service, those plugins are to submit <code>NewMMQueryRequest</code>
 * objects to the Blackboard and to subscribe for changes in <code>NewMMQueryRequest</code> 
 * objects. <code>MatchMakerPlugin</code> subscribes to new <code>NewMMQueryRequest</code>s and calls
 * <code>MatchMakerService</code> directly.
 */
public interface MatchMakerService extends Service{
    /**
     * Finds Service Providers that satisfy the criteria set in <code>NewMMQueryRequest</code>.
     * ServiceBroker is needed for matchmaker to be able to access such services as registry
     * query service and logging service. <code>commandLineage</code> is needed for military queries
     * and may be null for the rest.
     */
    public MatchMakerQueryResultImpl findService(MMQuery query, String[] commandLineage, ServiceBroker sb);
}

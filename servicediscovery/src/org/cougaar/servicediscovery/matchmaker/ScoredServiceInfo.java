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
import org.cougaar.servicediscovery.description.ServiceInfo;

/**
 * <code>ScoredServiceInfo</code> object keeps information about a matching
 * service and its match score.
 * Array of objects of <code>ScoredServiceInfo</code> is being returned
 * by XSB Prolog to <code>MatchMakerImpl</code> and by <code>MatchMakerImpl</code> 
 * to <code>MatchMakerPlugin</code> as a result of finding matching services.
 *  The class structure is simular to <code>ScoredServiceDescriptionImpl</code> 
 * however instead of <code>ServiceDescription</code> field information about a matching service 
 * is stored in <code>ServiceInfo</code>. 
 */
public class ScoredServiceInfo{
    private ServiceInfo serviceKey;
    private float score;
    
    /** Creates a new instance of ScoredProviderInfo */
    public ScoredServiceInfo(ServiceInfo serviceKey, float score) {
        this.serviceKey = serviceKey;
        this.score = score;
    }
    
    /**
     * Get ServiceInfo information 
     */
    public ServiceInfo getServiceKey(){
        return this.serviceKey;
    }
    
    /**
     * Get match score for the service
     */
    public float getScore(){
        return this.score;
    }
    
}

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

import org.cougaar.core.blackboard.Publishable;
import org.cougaar.core.util.UID;
import org.cougaar.servicediscovery.description.MMQuery;

/**
 * MatchmakerQuery is the query to Match Maker published as a part of MMQueryRequest 
 * to the Blackboard and subscribed to by Plugins.
 */

public class MatchMakerQuery implements java.io.Serializable, Publishable, MMQuery {
    
    String query;
    float cutoff;
    UID uid;
    
    /**
     * Creates <code>MatchMakerQuery</code> object with Prolog syntax query string.
     */
    public MatchMakerQuery(String query) {
        this.query = query;
        this.cutoff = 0;
    }
    
    /**
     * Creates <code>MatchMakerQuery</code> object with Prolog syntax query string
     * and cutoff for weighted queries.
     */
    public MatchMakerQuery(String query, float cutoff) {
        this.query = query;
        this.cutoff = cutoff;
    }
    
    /**
     * Returns Prolog query string.
     */
    public String getQueryString(){
        return query;
    }
    
    /**
     * Set cutoff for weighted queries.
     */
    public void setCutoff(float cutoff){
        this.cutoff = cutoff;
    }

    /**
     * Returns cutoff for weighted queries.
     */
    public float getCutoff(){
        return this.cutoff;
    }

    /**
     * Sets query id
     */
    public void setUID(UID uid) {
        this.uid = uid;
    }
    
    /**
     * Gets query id
     */
    public UID getUID() {
        return this.uid;
    }
    
    /**
     * Returns string representation of the query
     */
    public String toString() {
        return query;
    }
    
    /**
     * For compliance with <code>Publishable</code> interface
     */
    public boolean isPersistable() {
        return true;
    }
}

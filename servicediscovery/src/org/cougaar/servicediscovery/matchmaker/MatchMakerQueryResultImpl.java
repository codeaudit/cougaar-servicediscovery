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

import java.util.Collection;

/**
 * MatchQueryResultImpl is a response to MatchMakerQuery query.
 * This response is given by MatchMakerService and used in MatchMakerPlugin. 
 * (not used outside matchmaker package)
 * As the result of
 * MatchMakerService invocation the Result Code will be set to one of possible
 * values and if the result of the query is SUCCESS, MatchQueryResultImpl will
 * have a Collection of matching services.
 */
public class MatchMakerQueryResultImpl{
    
    /** Response code set by MatchMakerService for success of the query */
    public int SUCCESS = 1;
    /** Response code that is set by MatchMakerService if the query had errors (syntax errors or other) in it*/
    public int ERROR   = 2;
    public int FAIL    = 3;
    public int DENY    = 4;
    
    Collection matchingServices;
    int code;
    
    /** Creates a new instance of MatchMakerQueryResultImpl */
    public MatchMakerQueryResultImpl() {
    }
    
    /**
     * Creates <code>MatchQueryResponse</code> object.
     * <code>MatchQueryResponse</code> object is usually created by
     * MatchMakerService.
     *
     * @param code  The result code.
     * @param matchingServices Collection of matching services
     *
     */
    public MatchMakerQueryResultImpl(int code, Collection matchingServices){
        this.matchingServices = matchingServices;
        this.code = code;
    }
    
    
    /**
     * Returns collection of services found as responses to the query.
     * Elements of the collection are instances of
     * <code>ScoredServiceInfo</code>
     */
    public Collection  getResults(){
        return this.matchingServices;
    }
    
    /**
     * Set collection of services found as responses to the query.
     * Elements of the collection should be instances of
     * <code>ScoredServiceInfo</code>
     */
    public void setResults(Collection results){
        this.matchingServices = results;
    }
    
    /**
     * Returns the ResultCode
     * @return result code
     */
    public int getResultCode() {
        return this.code;
    }
}

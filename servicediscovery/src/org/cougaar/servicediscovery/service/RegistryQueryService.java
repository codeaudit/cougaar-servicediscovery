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

package org.cougaar.servicediscovery.service;
import org.cougaar.core.component.Service;
import org.cougaar.servicediscovery.transaction.RegistryQuery;

/**
 * Service interface for querying the registry to find providers
 * that provide a particular service.
 */
public interface RegistryQueryService extends Service, YPServiceAdapter {

  /**
   * Returns providers matching the attributes in the RegistryQuery 
   * object. Uses default YPService search, i.e. query progresses
   * up the structure of YP servers until either a match is found or
   * search has reached the topmost server. 
   * @param query RegistryQuery containing the attributes to be matched.
   * @param callback  callback.invoke(Collection) of ProviderInfo objects.
   * If no matches, returns empty list. 
   */
  public void findProviders(RegistryQuery query, Callback callback);

  /**
   * Returns providers matching the attributes in the RegistryQuery 
   * object. Uses single step YPService search. Query is applied to the
   * YP server in the next YPContext. If lastYPContext argument is null, 
   * search starts with the closest YPServer. 
   * @param lastYPContext YP context where the previous search ended. 
   * Use null if starting search.
   * @param query RegistryQuery containing the attributes to be matched.
   * @param callback  CallbackWithContext, callback.setNextContext(object) with
   * yp server context, callback.invoke(Collection) of ProviderInfo objects. 
   * If no matches, returns empty list. 
   */
  public void findProviders(Object lastYPContext, RegistryQuery query, 
			    CallbackWithContext callback);

  /**
   * Returns all services matching the attributes in the RegistryQuery object.
   * Uses default YPService search, i.e. query progresses
   * up the structure of YP servers until either a match is found or
   * search has reached the topmost server. 
   * @param query RegistryQuery containing the attributes to be matched.
   * @param callback  callback.invoke(Collection) of ServiceInfo objects. If
   * no matches, returns empty list. 
   */
  public void findServices(RegistryQuery query, Callback callback);

  /**
   * Returns all services matching the attributes in the RegistryQuery object.
   * Uses single step YPService search. Query is applied to the
   * YP server in the next YPContext. If lastYPContext argument is null, 
   * search starts with the closest YPServer. 
   * @param lastYPContext YP context where the previous search ended. 
   * Use null if starting search.
   * @param query RegistryQuery containing the attributes to be matched.
   * @param callback  CallbackWithContext, callback.setNextContext(object) with
   * yp server context, callback.invoke(Collection) with ServiceInfo objects.
   * If no matches, returns empty list. 
   */
  public void findServices(Object lastYPContext, RegistryQuery query, 
			   CallbackWithContext callback);

  /**
   * Returns all services matching the attributes in the RegistryQuery object.
   * Uses default YPService search, i.e. query progresses
   * up the structure of YP servers until either a match is found or
   * search has reached the topmost server. 
   * @param query RegistryQuery containing the attributes to be matched.
   * @param callback  callback.invoke(Collection) of lightweight ServiceInfo 
   * objects. If no matches, returns empty list. 
   */
  public void findServiceAndBinding(RegistryQuery query, Callback callback);

  /**
   * Returns all services matching the attributes in the RegistryQuery object.
   * Uses single step YPService search. Query is applied to the
   * YP server in the next YPContext. If lastYPContext argument is null, 
   * search starts with the closest YPServer. 
   * @param lastYPContext YP context where the previous search ended. 
   * Use null if starting search.
   * @param query RegistryQuery containing the attributes to be matched.
   * @param callback  CallbackWithContext, callback.setNextContext(object) with
   * yp server context, callback.invoke(Collection) with lightweight 
   * ServiceInfo, objects. If no matches, returns empty list. 
   */
  public void findServiceAndBinding(Object lastYPContext, RegistryQuery query,
				    CallbackWithContext callback);
}



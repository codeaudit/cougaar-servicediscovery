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
   * Returns providers matching the attributes in the RegistryQuery 
   * object. Uses single step YPService search. Query is applied to the
   * YP server for the specified agent. Currently requires that agent 
   * also be a YPServer. Bug 3585 in CommunityService prevents a more general
   * implementation.
   * 
   * @param agentName Name of the agent whose YP server should be queried
   * @param query RegistryQuery containing the attributes to be matched.
   * @param callback  CallbackWithContext, callback.setNextContext(object) with
   * yp server context, callback.invoke(Collection) of ProviderInfo objects. 
   * If no matches, returns empty list. 
   */
  public void findProviders(String agentName, RegistryQuery query, 
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
   * Uses single step YPService search.  Query is applied to the
   * YP server for the specified agent. Currently requires that agent 
   * also be a YPServer. Bug 3585 in CommunityService prevents a more general
   * implementation.
   *
   * @param agentName Name of the agent whose YP server should be queried
   * @param query RegistryQuery containing the attributes to be matched.
   * @param callback  CallbackWithContext, callback.setNextContext(object) with
   * yp server context, callback.invoke(Collection) with ServiceInfo objects.
   * If no matches, returns empty list. 
   */
  public void findServices(String agentName, RegistryQuery query, 
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


  /**
   * Returns all services matching the attributes in the RegistryQuery 
   * object.
   * Uses single step YPService search. Query is applied to the
   * YP server for the specified agent. Currently requires that agent 
   * also be a YPServer. Bug 3585 in CommunityService prevents a more general
   * implementation.
   *
   * @param agentName Name of the agent whose YP server should be queried
   * @param query RegistryQuery containing the attributes to be matched.
   * @param callback  CallbackWithContext, callback.setNextContext(object) with
   * yp server context, callback.invoke(Collection) with lightweight 
   * ServiceInfo, objects. If no matches, returns empty list. 
   */
  public void findServiceAndBinding(String agentName, 
				    RegistryQuery query,
				    CallbackWithContext callback);
  
}









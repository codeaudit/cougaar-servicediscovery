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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.service.community.Community;
import org.cougaar.servicediscovery.description.BusinessClassificationImpl;
import org.cougaar.servicediscovery.description.Classification;
import org.cougaar.servicediscovery.description.ProviderInfo;
import org.cougaar.servicediscovery.description.ServiceBinding;
import org.cougaar.servicediscovery.description.ServiceClassificationImpl;
import org.cougaar.servicediscovery.description.ServiceInfo;
import org.cougaar.servicediscovery.transaction.RegistryQuery;
import org.cougaar.util.GenericStateModelAdapter;
import org.cougaar.yp.YPProxy;
import org.cougaar.yp.YPService;
import org.uddi4j.datatype.Name;
import org.uddi4j.datatype.binding.BindingTemplate;
import org.uddi4j.datatype.binding.BindingTemplates;
import org.uddi4j.datatype.binding.InstanceParms;
import org.uddi4j.datatype.binding.TModelInstanceInfo;
import org.uddi4j.datatype.business.BusinessEntity;
import org.uddi4j.datatype.service.BusinessService;
import org.uddi4j.datatype.service.BusinessServices;
import org.uddi4j.response.BusinessDetail;
import org.uddi4j.response.BusinessList;
import org.uddi4j.response.ServiceDetail;
import org.uddi4j.response.ServiceList;
import org.uddi4j.util.CategoryBag;
import org.uddi4j.util.FindQualifier;
import org.uddi4j.util.FindQualifiers;
import org.uddi4j.util.KeyedReference;


/**
 */
public final class UDDI4JRegistryQueryServiceComponent extends GenericStateModelAdapter
  implements Component 
{
  private static String ypAgent = 
  System.getProperty("org.cougaar.yp.ypAgent", "OSD.GOV");

  private LoggingService log;
  public void setLoggingService(LoggingService log) { 
    this.log = (log==null)?LoggingService.NULL:log; 
  }
  
  protected YPService ypService;
  public void setYPService(YPService yps) { this.ypService = yps; }

  private UIDService uidService;
  public void setUIDService(UIDService us) { uidService = us; }

  private AgentIdentificationService agentIdentificationService;
  public void setAgentIdentificationService(AgentIdentificationService ais) {
    agentIdentificationService = ais;
  }

  private ThreadService threads;
  public void setThreadService(ThreadService t) { threads=t; }

  private RegistryQueryServiceProviderImpl mySP;
  private ServiceBroker sb;

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  public void load() {
    super.load();

    // create and advertise the service
    this.mySP = new RegistryQueryServiceProviderImpl();
    sb.addService(RegistryQueryService.class, mySP);
  }

  public void unload() {
    // revoke our service
    if (mySP != null) {
      sb.revokeService(RegistryQueryService.class, mySP);
      mySP = null;
    }
    super.unload();
  }

  private class RegistryQueryServiceProviderImpl implements ServiceProvider {
    private final RegistryQueryServiceImpl myService;

    public RegistryQueryServiceProviderImpl() {
      myService = new RegistryQueryServiceImpl();
    }

    public Object getService(ServiceBroker sb, Object requestor,
                             Class serviceClass) {
      if (serviceClass == RegistryQueryService.class) {
        return myService;
      } else {
        return null;
      }
    }

    public void releaseService(ServiceBroker sb, Object requestor,
                               Class serviceClass, Object service) {
    }
  }

  private class RegistryQueryServiceImpl 
    extends UDDI4JUtility
    implements RegistryQueryService 
  {
    private int maxRows = 100;

    public RegistryQueryServiceImpl() {
      super(UDDI4JRegistryQueryServiceComponent.this.log, 
            UDDI4JRegistryQueryServiceComponent.this.ypService,
            UDDI4JRegistryQueryServiceComponent.this.threads);

    }


    /**
     * Returns providers matching the attributes in the RegistryQuery 
     * object. Uses default YPService search, i.e. query progresses
     * up the structure of YP servers until either a match is found or
     * search has reached the topmost server. 
     * @param query RegistryQuery containing the attributes to be matched.
     * @param callback  callback.invoke(Collection) of ProviderInfo objects.
     * If no matches, returns empty list. 
     */
    public void findProviders(RegistryQuery query, Callback callback) {
      // Explicit call for a specific service
      YPProxy proxy = makeProxy(ypAgent);
      
      if (query.getServiceName() != null) {
        findServiceByNames(query, callback, proxy);
      } else {
        findAllProviders(query, callback, proxy);
      }
    }
    
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
    public void findProviders(final Object lastYPContext, 
			      final RegistryQuery query, 
			      final CallbackWithContext callback) {
      // Explicit call for a specific service
      ypService.nextYPServerContext(lastYPContext, 
				    new YPService.NextContextCallback() {
	public void setNextContext(Object context){
	  callback.setNextContext(context);
	  
	  if (context != null) {
	    YPProxy proxy = 
	      makeProxy(context, 
			YPProxy.SearchMode.SINGLE_COMMUNITY_SEARCH);
	    
	    if (query.getServiceName() != null) {
	      findServiceByNames(query, callback, proxy);
	    } else {
	      findAllProviders(query, callback, proxy);
	    }
	  } else {
	    if (log.isDebugEnabled()) {
	      log.debug("No subsequent yp context for " + lastYPContext);
	    }
	    
	    callback.invoke(Collections.EMPTY_LIST);
	  }
	}
      });
      
    }
    

    /**
     * Returns providers matching the attributes in the RegistryQuery 
     * object. Uses single step YPService search. Query is applied to the
     * YP server for the specified agent.
     * 
     * @param agentName Name of the agent whose YP server should be queried
     * @param query RegistryQuery containing the attributes to be matched.
     * @param callback  CallbackWithContext, callback.setNextContext(object) with
     * yp server context, callback.invoke(Collection) of ProviderInfo objects. 
     * If no matches, returns empty list. 
     */
    public void findProviders(final String agentName, 
			      final RegistryQuery query, 
			      final CallbackWithContext callback) {
      
      // Explicit call for a specific service
      ypService.getYPServerContext(agentName,
				   new YPService.NextContextCallback() {
	public void setNextContext(Object context){
	  callback.setNextContext(agentName);
	  
	  if (context != null) {
	    if (log.isDebugEnabled()) {
	      log.debug("Found YPServerContext for " + agentName + 
			" - will continue in " + context);
	    }
	    YPProxy proxy = 
	      makeProxy(context, 
			YPProxy.SearchMode.SINGLE_COMMUNITY_SEARCH);
	    
	    if (query.getServiceName() != null) {
	      findServiceByNames(query, callback, proxy);
	    } else {
	      findAllProviders(query, callback, proxy);
	    }
	  } else {
	    if (log.isDebugEnabled()) {
	      log.debug("No yp context for " + agentName);
	    }
	    callback.invoke(Collections.EMPTY_LIST);
	  }
	}
      });
    }

    /**
     * Returns all services matching the attributes in the RegistryQuery object.
     * @param query RegistryQuery containing the attributes to be matched.
     * @note callback.invoke(Collection) of ServiceInfo objects.  If no matches, returns empty list.
     */
    public void findServices(RegistryQuery query, Callback callback) {
      YPProxy proxy = makeProxy(ypAgent);

      findAllServices(query, callback, proxy);
    }


    /**
     * Returns all services matching the attributes in the RegistryQuery object.
     * Uses single step YPService search. Query is applied to the
     * YP server in the next YPContext. If lastYPContext argument is null, 
     * search starts with the closest YPServer. 
     * @param lastYPContext YP context where the previous search ended. 
     * Use null if starting search.
     * @param query RegistryQuery containing the attributes to be matched.
     * @param callback  CallbackWithContext, callback.setNextContext(object) 
     * with yp server context, callback.invoke(Collection) with ServiceInfo 
     * objects. If no matches, returns empty list. 
     */
    public void findServices(final Object lastYPContext, 
			     final RegistryQuery query, 
			     final CallbackWithContext callback) {
      ypService.nextYPServerContext(lastYPContext, 
				    new YPService.NextContextCallback() {
	public void setNextContext(Object context){
	  callback.setNextContext(context);
	  
	  if (context != null) {
	    YPProxy proxy = 
	      makeProxy(context, 
			YPProxy.SearchMode.SINGLE_COMMUNITY_SEARCH);
	    
	    findAllServices(query, callback, proxy);
	  } else {
	    if (log.isDebugEnabled()) {
	      log.debug("No subsequent yp context for " + lastYPContext);
	    }
	    callback.invoke(Collections.EMPTY_LIST);
	  }
	}
      });
    }
    
    /**
     * Returns all services matching the attributes in the RegistryQuery object.
     * Uses single step YPService search.  Query is applied to the
     * YP server for the specified agent. Currently requires that agent 
     * also be a YPServer.
     *
     * @param agentName Name of the agent whose YP server should be queried
     * @param query RegistryQuery containing the attributes to be matched.
     * @param callback  CallbackWithContext, callback.setNextContext(object) 
     * with yp server context, callback.invoke(Collection) with ServiceInfo
     * objects. If no matches, returns empty list. 
     */
    public void findServices(final String agentName, 
			     final RegistryQuery query, 
			     final CallbackWithContext callback) {
      
      // Explicit call for a specific service
      ypService.getYPServerContext(agentName, 
				   new YPService.NextContextCallback() {
	public void setNextContext(Object context){
	  callback.setNextContext(agentName);
	  
	  if (context != null) {
	    if (log.isDebugEnabled()) {
	      log.debug("Found YPServerContext for " + agentName + 
			" - will continue in " + context);
	    }
	    YPProxy proxy = 
	      makeProxy(context, 
			YPProxy.SearchMode.SINGLE_COMMUNITY_SEARCH);
	    
	    findAllServices(query, callback, proxy);
	  } else {
	    if (log.isDebugEnabled()) {
	      log.debug("No yp context for " + agentName);
	    }
	    callback.invoke(Collections.EMPTY_LIST);
	  }
	}
      });
    }


    /**
     * Returns all services matching the attributes in the RegistryQuery object.
     * Uses default YPService search, i.e. query progresses
     * up the structure of YP servers until either a match is found or
     * search has reached the topmost server. 
     * @param query RegistryQuery containing the attributes to be matched.
     * @param callback  callback.invoke(Collection) of lightweight ServiceInfo 
     * objects. If no matches, returns empty list. 
     */
    public void findServiceAndBinding(RegistryQuery query, Callback callback) {
      YPProxy proxy = makeProxy(ypAgent);
      
      findServiceAndBinding(query, callback, proxy);
    }
    
    

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
    public void findServiceAndBinding(final Object lastYPContext, 
				      final RegistryQuery query,
				      final CallbackWithContext callback) {
      
      // Explicit call for a specific service
      ypService.nextYPServerContext(lastYPContext, 
				    new YPService.NextContextCallback() {
	public void setNextContext(Object context){
	  callback.setNextContext(context);

	  if (context != null) {
	    if (log.isDebugEnabled()) {
	      log.debug("Found next YPServerContext for " + lastYPContext + 
			" - will continue in " + context);
	    }
	    YPProxy proxy = 
	      makeProxy(context, 
			YPProxy.SearchMode.SINGLE_COMMUNITY_SEARCH);
	    
	    findServiceAndBinding(query, callback, proxy);
	  } else {
	    if (log.isDebugEnabled()) {
	      log.debug("No subsequent yp context for " + lastYPContext);
	    }
	    callback.invoke(Collections.EMPTY_LIST);
	  }
	}
      });
    }


      

    /**
     * Returns all services matching the attributes in the RegistryQuery object.
     * Uses single step YPService search. Query is applied to the
     * YP server for the specified agent. Currently requires that agent 
     * also be a YPServer. Bug in CommunityService 
     * (http://bugs.cougaar.org/show_bug.cgi?id=3585) prevents a more general
     * implementation.
     * 
     * @param agentName Name of the agent whose YP server will be queried
     * @param query RegistryQuery containing the attributes to be matched.
     * @param callback  CallbackWithContext, callback.setNextContext(object) with
     * yp server context, callback.invoke(Collection) with lightweight 
     * ServiceInfo, objects. If no matches, returns empty list. 
     */
    public void findServiceAndBinding(final String agentName, 
				      final RegistryQuery query,
				      final CallbackWithContext callback) {
      // Explicit call for a specific service
      ypService.getYPServerContext(agentName, 
				   new YPService.NextContextCallback() {
	public void setNextContext(Object context){
	  callback.setNextContext(agentName);
	  
	  if (context != null) {
	    if (log.isDebugEnabled()) {
	      log.debug("Found YPServerContext for " + agentName + 
			" - will continue in " + context);
	    }
	    YPProxy proxy = 
	      makeProxy(context, 
			YPProxy.SearchMode.SINGLE_COMMUNITY_SEARCH);
	    
	    findServiceAndBinding(query, callback, proxy);
	  } else {
	    if (log.isDebugEnabled()) {
	      log.debug("No yp context for " + agentName);
	    }
	    callback.invoke(Collections.EMPTY_LIST);
	  }
	}
      });
    }

    private void findAllServices(final RegistryQuery rq, 
				 final Callback callback,
				 YPProxy proxy) {
      class State {
        Collection services = new ArrayList();
        CategoryBag bag = null;
        FindQualifiers fq = null;
        Vector serviceNames = null;
        ServiceList serviceList = null;
        Callback step1,step2,step3,step4;
	YPProxy proxy;
	Object finalYPContext;
      }
      final State state = new State();

      state.proxy = proxy;

      // step1 create category bag 
      state.step1 = new CallbackDelegate(callback) { 
	public void invoke(Object result) {
        if (rq.getServiceClassifications() != null) {
          createCategoryBag(rq.getServiceClassifications(), state.step2, 
			    state.proxy);
        } else {
          state.step2.invoke(null);
        }
      }};


      // consume cb, set up fq, names.  invoke findService
      state.step2 = new CallbackDelegate(callback) { 
	public void invoke(Object result) {
        state.bag = (CategoryBag) result;

        if (rq.getFindQualifiers() != null) {
          state.fq = new FindQualifiers();
          // BUG: original version just took the first one
          for (Iterator it = rq.getFindQualifiers().iterator(); it.hasNext(); ) {
            String qualifier = (String) it.next();
            state.fq.getFindQualifierVector().add(new FindQualifier(qualifier));
          }
        }
        
        if (rq.getServiceName() != null) {
          state.serviceNames = new Vector(1);
          state.serviceNames.add(new Name(rq.getServiceName()));
        }

        findService(state.proxy, null, state.serviceNames, state.bag, null, state.fq, maxRows, state.step3);
      }};

      state.step3 = new CallbackDelegate(callback) { 
	public void invoke(Object result) {
        state.serviceList = (ServiceList) result;

        Vector serviceKeys = new Vector();
        for (Iterator it = state.serviceList.getServiceInfos().getServiceInfoVector().iterator(); it.hasNext(); ) {
          org.uddi4j.response.ServiceInfo si = (org.uddi4j.response.ServiceInfo) it.next();
          serviceKeys.add(si.getServiceKey());
          if (log.isDebugEnabled()) {
            log.debug("[findAllServices]ServiceName : " + si.getNameString());
          }
        }

        if (!serviceKeys.isEmpty()) {
          createServiceInfos(serviceKeys, state.step4, state.proxy);
        } else {
          if (log.isDebugEnabled()) {
            log.debug("No services were found ");
          }
          state.step4.invoke(Collections.EMPTY_LIST);
        }}};

      state.step4 = new CallbackDelegate(callback) { 
	public void invoke(Object result) {
        super.invoke(result);
      }};
      
      state.step1.invoke(null);
    }

    private void findAllProviders(final RegistryQuery rq, Callback callback,
				  final YPProxy proxy) {
      class State {
        CategoryBag bag = null;
        FindQualifiers fq = null;
        Vector providerNames = null;
        BusinessList bzList;
        Callback step1,step2,step3,step4;
	YPProxy proxy;
	Object finalYPContext;
      }
      final State state = new State();

      state.proxy = proxy;

      // step1 create category bag 
      state.step1 = new CallbackDelegate(callback) { 
	public void invoke(Object result) {
        if (rq.getBusinessClassifications() != null) {
          createCategoryBag(rq.getBusinessClassifications(), state.step2,
			    state.proxy);
        } else {
          state.step2.invoke(null);
        }
      }};

      // step2: consume category bag, do FQ and providernames, launch findBusiness
      state.step2 = new CallbackDelegate(callback) { 
	public void invoke(Object result) {
        state.bag = (CategoryBag) result;

        if (rq.getFindQualifiers() != null) {
          state.fq = new FindQualifiers();
          // BUG: original version just took the first one
          for (Iterator it = rq.getFindQualifiers().iterator(); it.hasNext(); ) {
            String qualifier = (String) it.next();
            state.fq.getFindQualifierVector().add(new FindQualifier(qualifier));
          }
        }

        if (rq.getProviderName() != null) {
          state.providerNames = new Vector(1);
          state.providerNames.add(new Name(rq.getProviderName()));
        }
        
        findBusiness(state.proxy, state.providerNames, null, null, state.bag, null, state.fq, maxRows, state.step3);
      }};

      // step3: consume businesses, createProviderInfos
      state.step3 = new CallbackDelegate(callback) { 
	public void invoke(Object result) {
        state.bzList = (BusinessList) result;
        Vector businessKeys = new Vector();
        String serviceKey = null; // serviceKey never gets set to something real - is this ok?

        for (Iterator it = state.bzList.getBusinessInfos().getBusinessInfoVector().iterator(); it.hasNext(); ) {
          org.uddi4j.response.BusinessInfo bi = (org.uddi4j.response.BusinessInfo) it.next();
          businessKeys.add(bi.getBusinessKey());
          if (log.isDebugEnabled()) {
            log.debug("[findAllProviders] Provider name: " + bi.getNameString());
          }
        }
        createProviderInfos(businessKeys, serviceKey, state.step4, 
			    state.proxy);
      }};

      // step4: consume and result providers
      state.step4 = new CallbackDelegate(callback) { 
	public void invoke(Object result) {
        super.invoke(result);
      }};

      state.step1.invoke(null);
    }

    /** @note Callback.invoke(Collection) **/
    private void findServiceByNames(final RegistryQuery rq, Callback callback,
				    YPProxy proxy) {
      class State { 
        Collection providers = new ArrayList();
        CategoryBag bag = null;
        FindQualifiers fq = null;
        Vector providerNames = null;
        BusinessList bzList;
        Vector businessKeys = new Vector();
        Callback step1,step2,step3,step4;
	YPProxy proxy;
      }
      final State state = new State();
      
      state.proxy = proxy;
      
      // step1 create category bag 
      state.step1 = new CallbackDelegate(callback) { 
	public void invoke(Object result) {
        if (rq.getBusinessClassifications() != null) {
          createCategoryBag(rq.getBusinessClassifications(), state.step2,
			    state.proxy);
        } else {
          state.step2.invoke(null);
        }
      }};

      // step2: consume category bag, do FQ and providernames, launch findBusiness
      state.step2 = new CallbackDelegate(callback) { 
	public void invoke(Object result) {
        state.bag = (CategoryBag) result;

        if (rq.getFindQualifiers() != null) {
          String qualifier = (String) rq.getFindQualifiers().iterator().next();
          state.fq = new FindQualifiers();
          state.fq.getFindQualifierVector().add(new FindQualifier(qualifier));
        }

        if (rq.getProviderName() != null) {
          state.providerNames = new Vector();
          state.providerNames.add( new Name(rq.getProviderName()));
        }

        findBusiness(state.proxy, state.providerNames, null, null, state.bag, null, state.fq, maxRows, state.step3);
      }};

      // step3: consume findBusiness, invoke createProviderInfos
      state.step3 = new CallbackDelegate(callback) { 
	public void invoke(Object result) {
        state.bzList  = (BusinessList) result;
        for (Iterator it = state.bzList.getBusinessInfos().getBusinessInfoVector().iterator(); it.hasNext(); ) {
          org.uddi4j.response.BusinessInfo bi = (org.uddi4j.response.BusinessInfo) it.next();
          state.businessKeys.add(bi.getBusinessKey());
          if (log.isDebugEnabled()) {
            log.debug("[findAllProviders] Provider name: " + bi.getNameString());
            log.debug("[findServiceByName] Service name: " + rq.getServiceName());
          }
        }
        if (!state.businessKeys.isEmpty()) {
          createProviderInfos(state.businessKeys, rq.getServiceName(), 
			      state.step4, state.proxy);
        } else {
          if (log.isDebugEnabled()) {
            log.debug("No providers of that service were found ");
          }
          state.step4.invoke(Collections.EMPTY_LIST);
        }}};

      state.step4 = new CallbackDelegate(callback) { 
	public void invoke(Object result) {
        super.invoke(result);
      }};
      state.step1.invoke(null);
    }

    /**
     * Returns all services matching the attributes in the RegistryQuery object.
     * @param query RegistryQuery containing the attributes to be matched.
     * @note callback.invoke(Collection) of lightweight ServiceInfo objects.
     * If no matches, returns empty list.
     */
    private void findServiceAndBinding(final RegistryQuery query, final Callback callback, YPProxy proxy) {

      class State { 
        Collection services = new ArrayList();
        CategoryBag bag = null;
        FindQualifiers fq = null;
        Vector serviceNames = null;
        Vector serviceKeys = new Vector();
        Callback step1,step2,step3,sub1,step4,step5;
	YPProxy proxy;
      }
      final State state = new State();

      state.proxy = proxy;
      // step 1
      state.step1 = new CallbackDelegate(callback) { 
	public void invoke(Object result) {
        Collection scs = query.getServiceClassifications();
        if (scs != null) {
          createCategoryBag(scs, state.step2, state.proxy);
        } else {
          state.step2.invoke(null);
        }
      }};

      // step 2: consume CategoryBag, start FQ
      state.step2 = new CallbackDelegate(callback) { 
	public void invoke(Object result) {
        state.bag = (CategoryBag) result;

        if (query.getFindQualifiers() != null) {
          state.fq = new FindQualifiers();
          for (Iterator it = query.getFindQualifiers().iterator(); it.hasNext(); ) {
            String qualifier = (String) it.next();
            state.fq.getFindQualifierVector().add(new FindQualifier(qualifier));
          }
        }

        if (query.getServiceName() != null) {
          state.serviceNames = new Vector();
          state.serviceNames.add( new Name(query.getServiceName()));
        }

        findService(state.proxy, null, state.serviceNames, state.bag, null, state.fq, maxRows, state.step3);
      }};

      // step 3: consume service
      state.step3 = new CallbackDelegate(callback) { 
	public void invoke(Object result) {
	  ServiceList serviceList = (ServiceList) result;
	  loop(serviceList.getServiceInfos().getServiceInfoVector(),
	       state.sub1,
	       state.step4);
	}
      };

      state.sub1 = new CallbackDelegate(callback) { 
	public void invoke(Object args) {
        Object result = ((Object[])args)[0];
        Callback next = (Callback) ((Object[])args)[1];
        org.uddi4j.response.ServiceInfo si = (org.uddi4j.response.ServiceInfo) result;
        state.serviceKeys.add(si.getServiceKey());
        if (log.isDebugEnabled()) {
          log.debug("[findServiceAndBinding]ServiceName : " + si.getNameString());
        }
        next.invoke(null);      // pop back up
      }};

      state.step4 = new CallbackDelegate(callback) { 
	public void invoke(Object result) {
        if (!state.serviceKeys.isEmpty()) {
          createSimpleServiceInfos(state.serviceKeys, state.step5, 
				   state.proxy);
        } else {
          if (log.isDebugEnabled()) {
            log.debug("No services were found ");
          }
          super.invoke(Collections.EMPTY_LIST);
        }
      }};

      state.step5 = new CallbackDelegate(callback) { 
	public void invoke(Object result) {
        super.invoke((Collection) result);
      }};

      state.step1.invoke(null);
    }


    /** @note Callback.invoke(Collection) **/
    private void createProviderInfos(Vector businessKeys, 
				     final String serviceName, 
				     Callback callback, 
				     YPProxy proxy) {
      final Collection providers = new ArrayList();

      class State { 
	Callback step2,step3,sub1,sub2; 
	Callback jump; 
	YPProxy proxy;
      };
      
      final State state = new State();

      state.proxy = proxy;
      // step 1 at end...

      // step 2 consumes the business detail and then loops, ending in step 3
      state.step2 = new CallbackDelegate(callback) { 
	public void invoke(Object result) {
	  BusinessDetail bd = (BusinessDetail) result;
	  loop(bd.getBusinessEntityVector(), state.sub1, state.step3);
	}};

      // nesting for documentation purposes...
      {
        state.sub1 = new CallbackDelegate(callback) { 
	  public void invoke(Object args) {
	    Object result = ((Object[])args)[0];
	    state.jump = (Callback) ((Object[])args)[1];

	    BusinessEntity be = (BusinessEntity) result;
	    createProviderInfo1(be, serviceName, state.sub2, state.proxy);
	  }};

        state.sub2 = new CallbackDelegate(callback) { 
	  public void invoke(Object result) {
	    ProviderInfo pi = (ProviderInfo) result;
	    providers.add(pi);
	    // return back up...
	    state.jump.invoke(null);
	  }};
      }
      
      state.step3 = new CallbackDelegate(callback) { 
	public void invoke(Object result) {
	  // result==step1
	  super.invoke(providers);
	}};
      
      
      // step 1
      getBusinessDetail(state.proxy, businessKeys, state.step2);
    }

    private void createProviderInfo1(final BusinessEntity be, final String serviceName, Callback callback, YPProxy proxy) {
      class State { 
	Collection bzClass; 
	BusinessService bs; 
	Callback step2,step3,step4,step5,stepA; 
	Collection scs;
	YPProxy proxy;
      };
      final State state = new State();
      state.proxy = proxy;
      // step 1 below
      
      state.step2 = new CallbackDelegate(callback) { 
	public void invoke(Object result) {
	  if (serviceName != null) {
	    // only retrieve the specified service (darn - I thought it needed a callback)
	    state.step3.invoke(getBusinessService(be.getBusinessServices().getBusinessServiceVector(), serviceName));
	  } else {
	    // get them all
	    if (be.getBusinessServices() != null) {
	      createServiceInfos(be.getBusinessServices(), 
				 be.getDefaultNameString(), 
				 state.bzClass, 
				 state.stepA, 
				 state.proxy);
	    } else {
	      if (log.isDebugEnabled()) {
		log.debug("No services registered with provider " + be.getDefaultNameString());
	      }
	      state.stepA.invoke(Collections.EMPTY_LIST);
	    }
	  }
	}};
      
      // step3 consumes the business service and gets the SCs (to step4)n
      state.step3 = new CallbackDelegate(callback) { 
	public void invoke(Object result) {
	  state.bs = (BusinessService) result;
	  getServiceClassifications(state.bs.getCategoryBag(), state.step4,
				    state.proxy);
	}};
      state.step4 = new CallbackDelegate(callback) { 
	public void invoke(Object result) {
	  state.scs = (Collection) result;
	  getServiceBindings(state.bs.getBindingTemplates(), state.step5,
			     state.proxy);
	}};
      state.step5 = new CallbackDelegate(callback) { 
	public void invoke(Object result) {
	  Collection sbs = (Collection) result;
	  ServiceInfo si = new ServiceInfo(state.bs.getDefaultNameString(), 
					   state.bs.getServiceKey(),
					   state.scs,
					   sbs,
					   be.getDefaultNameString(),
					   state.bzClass);
	  ArrayList l = new ArrayList(1);
	  l.add(si);
	  state.stepA.invoke(l);
      }};
      
      // this had nested incorrectly.
      state.stepA = new CallbackDelegate(callback) { 
	public void invoke(Object result) {
	  Collection serviceInfos = (Collection) result;
	  super.invoke(new ProviderInfo(be.getDefaultNameString(), state.bzClass, serviceInfos));
	}};
      
      // step 1
      getBusinessClassifications(be.getCategoryBag(), state.step2, state.proxy);
    }
    
    /** find the service in the vector with the correct name **/
    private BusinessService getBusinessService(Vector services, String serviceName) {
      Enumeration e = services.elements();
      BusinessService businessService = null;
      while (e.hasMoreElements()) {
        BusinessService bs = (BusinessService) e.nextElement();
        if (bs.getDefaultNameString().equals(serviceName)) {
          businessService = bs;
        }
      }
      return businessService;
    }

    /** @note Callback.invoke(Collection) **/
    private void createServiceInfos(Vector serviceKeys, Callback callback,
				    YPProxy proxy) {

      final Collection serviceInfos = new ArrayList();
      class State { 
	Enumeration e; 
	Callback step2, step3, step4, step5, step6; 
	ServiceInfo serviceInfo; 
	BusinessService bs;
	YPProxy proxy;
      };
      final State state = new State();
      
      state.proxy = proxy;
      // step1 is below

      // step2 catches the ServiceDetail and invokes the loop
      state.step2 = new CallbackDelegate(callback) {
          public void invoke(Object result) {
            ServiceDetail sd = (ServiceDetail) result;
            state.e = sd.getBusinessServiceVector().elements();
            state.step3.invoke(null);
          }};


      // step 3 starts the loop.
      state.step3 = new CallbackDelegate(callback) {
          public void invoke(Object result) {
            if (result != null) {
              Collection sbs = (Collection) result;
              state.serviceInfo.setServiceBindings(sbs);
              serviceInfos.add(state.serviceInfo);
            }
            if (state.e.hasMoreElements()) {
              state.bs = (BusinessService) state.e.nextElement();
              state.serviceInfo = new ServiceInfo();
              state.serviceInfo.setServiceId(state.bs.getServiceKey());
              state.serviceInfo.setServiceName(state.bs.getDefaultNameString());
              getServiceClassifications(state.bs.getCategoryBag(), 
					state.step4, state.proxy);
            } else {
              super.invoke(serviceInfos);
            }}};

      // step 4 consumes ServiceClassifications and gets the BusinessDetails
      state.step4 = new CallbackDelegate(callback) {
          public void invoke(Object result) {
            state.serviceInfo.setServiceClassifications((Collection) result);
            getBusinessDetail(state.proxy, state.bs.getBusinessKey(), state.step5);
          }};
      // step 5 consumes the BusinessDetail and fills it out, ending in step6
      state.step5 = new CallbackDelegate(callback) { 
	public void invoke(Object result) {
	  BusinessDetail bd = (BusinessDetail) result;
	  addBusinessDetail(state.serviceInfo, bd, state.step6, state.proxy);
	}};
      
      // step 6 adds the SCs
      state.step6 = new CallbackDelegate(callback) {
          public void invoke(Object result) {
            //consumes ???
            getServiceBindings(state.bs.getBindingTemplates(), state.step3,
			       state.proxy);
          }};
      
      getServiceDetail(state.proxy, serviceKeys, state.step2); // step1
    }


    private void addBusinessDetail(final ServiceInfo serviceInfo, BusinessDetail bd, Callback callback, final YPProxy proxy) {
      final Enumeration businessEnum = bd.getBusinessEntityVector().elements();
      BusinessEntity be = null;
      while (businessEnum.hasMoreElements()) {
        // Ugh!  Just duplicating the logic here, but this looks wrong to me!
        be = (BusinessEntity) businessEnum.nextElement();
      }


      if (be == null) {
        callback.invoke(serviceInfo);
      } else {
        serviceInfo.setProviderName(be.getDefaultNameString());
        getBusinessClassifications(be.getCategoryBag(), 
				   new CallbackDelegate(callback) {
	  public void invoke(Object result) {
	    serviceInfo.setBusinessClassifications((Collection) result);
	    super.invoke(serviceInfo);
	  }},
	  proxy);
      }
    }


    /** @note Callback.invoke(Collection) **/
    private void createSimpleServiceInfos(Vector serviceKeys, 
					  Callback callback, 
					  YPProxy proxy) {
      final Collection serviceInfos = new ArrayList();
      class State {
	Enumeration e; 
	Callback step2, step3, step4; 
	ServiceInfo serviceInfo; 
	BusinessService bs;
	YPProxy proxy;
      };

      final State state = new State();
      state.proxy = proxy;
      // step1 is below

      // step2 catches the ServiceDetail and invokes the loop
      state.step2 = new CallbackDelegate(callback) {
	public void invoke(Object result) {
	  ServiceDetail sd = (ServiceDetail) result;
	  state.e = sd.getBusinessServiceVector().elements();
	  state.step3.invoke(null);
	}};

      // step 3 starts the loop.
      state.step3 = new CallbackDelegate(callback) {
	public void invoke(Object result) {
	  if (result != null) {
	    Collection sbs = (Collection) result;
	    state.serviceInfo.setServiceBindings(sbs);
	    serviceInfos.add(state.serviceInfo);
	  }
	  if (state.e.hasMoreElements()) {
	    state.bs = (BusinessService) state.e.nextElement();
	    state.serviceInfo = new ServiceInfo();
	    state.serviceInfo.setServiceId(state.bs.getServiceKey());
	    state.serviceInfo.setServiceName(state.bs.getDefaultNameString());
	    getServiceClassifications(state.bs.getCategoryBag(), 
				      state.step4, state.proxy);
	  } else {
	    super.invoke(serviceInfos);
	  }}};

      // step 4 adds the SCs
      state.step4 = new CallbackDelegate(callback) {
          public void invoke(Object result) {
            state.serviceInfo.setServiceClassifications((Collection) result);
            getServiceBindings(state.bs.getBindingTemplates(), state.step3,
			       state.proxy);
          }};
      
      
      getServiceDetail(state.proxy, serviceKeys, state.step2); // step1

    }


    /** @note Callback.invoke(Collection) **/
    private void createServiceInfos(BusinessServices bs, 
				    final String providerName, 
				    final Collection bzClass, 
				    Callback callback,
				    YPProxy proxy)  {
      final Enumeration e = bs.getBusinessServiceVector().elements();
      final Collection serviceInfos = new ArrayList();
      class State {
	BusinessService svc;
	Collection scs;
	Callback chain1; 
	Callback chain2;
	YPProxy proxy;
      };
      final State state = new State();

      state.proxy = proxy;

      // two-step iteration.  ugh.
      state.chain1 = new CallbackDelegate(callback) {
          public void invoke(Object result) {
            if (result != null) {
              // called from getServiceBindings
              Collection sbs = (Collection) result;
              BusinessService svc = state.svc;
              ServiceInfo serviceInfo = new ServiceInfo(svc.getDefaultNameString(), 
                                                        svc.getServiceKey(),
                                                        state.scs,
                                                        sbs,
                                                        providerName, 
                                                        bzClass);
              serviceInfos.add(serviceInfo);
            } // else first time through - nothing to do here

            if (e.hasMoreElements()) {
              // more work to do - ho hum
              state.svc = (BusinessService) e.nextElement(); // grab svc
              getServiceClassifications(state.svc.getCategoryBag(), 
					state.chain2, state.proxy); // continue in chain2
            } else {
              // done, finish the job
              super.invoke(serviceInfos);
            }
          }};

      state.chain2 = new CallbackDelegate(callback) {
          public void invoke(Object result) {
            state.scs = (Collection) result;
            getServiceBindings(state.svc.getBindingTemplates(), 
			       state.chain1,
			       state.proxy);
          }
        };
            
      state.chain1.invoke(null);
    }


    /**
     * Have to create a transient scheme in order to query
     * based on a classification
     * @note Callback.invoke(CategoryBag)
     */
    private void createCategoryBag(Collection classifications, 
				   Callback callback, 
				   final YPProxy proxy) {
      final CategoryBag bag = new CategoryBag();
      final Iterator i = classifications.iterator();
      
      Callback chain = new CallbackDelegate(callback) {
          public void invoke(Object result) {
            if (result != null) {
              bag.getKeyedReferenceVector().add(result);
            }
            if (i.hasNext()) {
              Classification rc = (Classification) i.next();              
              getKeyedReference(proxy,
                                rc.getClassificationSchemeName(),
                                rc.getClassificationName(),
                                rc.getClassificationCode(),
                                this);
            } else {
              super.invoke(bag);
            } 
          }};
      chain.invoke(null);
    }


    //
    // utilities
    //

    // Note - we use this chaining callback to iterate for the next few calls.
    //
    private void getServiceBindings(BindingTemplates bindingTemplates, 
				    Callback callback, final YPProxy proxy) {
      final Collection serviceBindings = new ArrayList();
      final Enumeration e = bindingTemplates.getBindingTemplateVector().elements();

      // this iterates via tail-recursion... shouldn't be a problem, but we could unwind it 
      Callback chain = new CallbackDelegate(callback) {
          public void invoke(Object result) {
            if (result != null) { serviceBindings.add(result); } // first call is null to get the loop going
            if (e.hasMoreElements()) {
              BindingTemplate binding = (BindingTemplate) e.nextElement();
              getServiceBinding(binding, this, proxy); // (heh heh heh)
            } else {
              super.invoke(serviceBindings);
            }
          }};
      chain.invoke(null);
    }


    /** @note Callback.invoke(Collection) **/
    private void getServiceClassifications(CategoryBag bag, 
					   Callback callback, 
					   final YPProxy proxy) {
      final Collection serviceClassifications = new ArrayList();
      final Enumeration e = bag.getKeyedReferenceVector().elements();
      Callback chain = new CallbackDelegate(callback) {
          public void invoke(Object result) {
            if (result != null) { serviceClassifications.add(result); }
            if (e.hasMoreElements()) {
              KeyedReference kr = (KeyedReference) e.nextElement();
              createClassification( new ServiceClassificationImpl(), kr, 
				    this, proxy);
            } else {
              super.invoke(serviceClassifications);
            }
          }
        };
      chain.invoke(null);
    }

    /** @note Callback.invoke(Collection) **/
    private void getBusinessClassifications(CategoryBag bag, Callback callback,
					    final YPProxy proxy) {
      final Collection businessClassifications = new ArrayList();
      final Enumeration e = bag.getKeyedReferenceVector().elements();

      Callback chain = new CallbackDelegate(callback) {
          public void invoke(Object result) {
            if (result != null) { businessClassifications.add(result); }
            if (e.hasMoreElements()) {
              KeyedReference kr = (KeyedReference) e.nextElement();
              createClassification(new BusinessClassificationImpl(), kr, 
				   this, proxy);
            } else {
              super.invoke(businessClassifications);
            }
          }};
      chain.invoke(null);
    }


    /** @note Callback.invoke(Classification) **/
    private void createClassification(final Classification classification,
                                      final KeyedReference kr,
                                      Callback callback,
				      YPProxy proxy) {
      findTModelName(proxy,
                     kr.getTModelKey(),
                     new CallbackDelegate(callback) {
                       public void invoke(Object result) {
                         String tModelName = (String) result;
                         if (tModelName != null) {
                           classification.setClassificationCode(kr.getKeyValue());
                           classification.setClassificationName(kr.getKeyName());
                           classification.setClassificationSchemeName(tModelName);
                         } else {
                           log.error("A serious error occured, tModelName is null for "+kr);
                         }
                         super.invoke(classification);
                       }});
    }


    private void getServiceBinding(final BindingTemplate binding, 
				   Callback callback, 
				   YPProxy proxy) {
      Enumeration e = binding.getTModelInstanceDetails().getTModelInstanceInfoVector().elements();
      if (!e.hasMoreElements()) {
        callback.handle(new RuntimeException("No service binding for "+binding));
        return;
      }

      final TModelInstanceInfo tio = (TModelInstanceInfo) e.nextElement();
      findTModelName(proxy,
                     tio.getTModelKey(),
                     new CallbackDelegate(callback) {
                       public void invoke(Object result) { 
                         String bindingType = (String) result;
                         InstanceParms ip = 
			   tio.getInstanceDetails().getInstanceParms();
                         ServiceBinding sb = 
			   new ServiceBinding(binding.getAccessPoint().getText(),
					      bindingType, ip.getText());
                         super.invoke(sb);
                       }});
    }


  }


  private YPProxy makeProxy(Object ypContext) {
      YPProxy proxy = null;

      if (ypContext == null) {
	proxy = ypService.getYP();
      } else if (ypContext instanceof String) {
	proxy = ypService.getYP((String) ypContext);
      } else if (ypContext instanceof MessageAddress) {
	proxy = ypService.getYP((MessageAddress) ypContext);
      } else if (ypContext instanceof Community) {
	proxy = ypService.getYP((Community) ypContext, 
				YPProxy.SearchMode.HIERARCHICAL_COMMUNITY_SEARCH);
      } else {
	throw new IllegalArgumentException("Invalid datatype for ypContext - " +
					   ypContext.getClass() +
					   " - must be String, MessageAddress, or Community.");
      }

      return proxy;
    }


  private YPProxy makeProxy(Object ypContext, int searchMode) {
      YPProxy proxy = null;

      if (ypContext == null) {
	proxy = ypService.getYP(searchMode);
      } else if (ypContext instanceof Community) {
	proxy = ypService.getYP((Community) ypContext, searchMode);
      } else {
	throw new IllegalArgumentException("Invalid datatype for ypContext " + 
					   " with YPProxy.SearchMode - " +
					   ypContext.getClass() +
					   " - must be Community");
      }

      return proxy;
    }

}

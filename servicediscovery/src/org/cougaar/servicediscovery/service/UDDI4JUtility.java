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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.yp.OneShotMachine;
import org.cougaar.yp.YPFuture;
import org.cougaar.yp.YPProxy;
import org.cougaar.yp.YPService;
import org.uddi4j.datatype.binding.AccessPoint;
import org.uddi4j.datatype.binding.BindingTemplate;
import org.uddi4j.datatype.binding.BindingTemplates;
import org.uddi4j.datatype.binding.InstanceDetails;
import org.uddi4j.datatype.binding.InstanceParms;
import org.uddi4j.datatype.binding.TModelInstanceDetails;
import org.uddi4j.datatype.binding.TModelInstanceInfo;
import org.uddi4j.datatype.tmodel.TModel;
import org.uddi4j.response.AuthToken;
import org.uddi4j.response.TModelDetail;
import org.uddi4j.response.TModelInfo;
import org.uddi4j.response.TModelInfos;
import org.uddi4j.response.TModelList;
import org.uddi4j.util.CategoryBag;
import org.uddi4j.util.DiscoveryURLs;
import org.uddi4j.util.FindQualifiers;
import org.uddi4j.util.IdentifierBag;
import org.uddi4j.util.KeyedReference;
import org.uddi4j.util.TModelBag;

/** 
 * Utility class for holding common functions of YP/UDDI4J SD interations,
 **/
public class UDDI4JUtility 
  implements YPServiceAdapter   // import Callback
{
  protected final LoggingService log;
  protected final YPService ypService;
  protected final ThreadService threads;
  protected boolean cacheEnabled;
  
  public UDDI4JUtility(LoggingService log, YPService ypService, ThreadService threads) {
    this.log = log;
    this.ypService = ypService;
    this.threads = threads;
    this.cacheEnabled = !(Boolean.getBoolean("org.cougaar.servicediscovery.service.tModelCacheDisabled"));
  }

  /** Override to pre/post-fix log strings **/
  protected String logString(String s) {
    return s;
  }

  protected BindingTemplates createBindingTemplates(String uri, TModelInstanceDetails tModelInstanceDetails) {
    BindingTemplates bindings = new BindingTemplates();
    BindingTemplate binding = new BindingTemplate("", tModelInstanceDetails);
    AccessPoint accessPoint = new AccessPoint(uri, "http");
    binding.setAccessPoint(accessPoint);
    bindings.getBindingTemplateVector().add(binding);
    return bindings;
  }

  // doesn't require a machine - single invocation with quick completion
  // callback.invoke(TModelInstanceDetails)
  // callback.handle(NoSuchTModelKeyException)
  protected void createTModelInstance(YPProxy proxy, String tModelName, final String messageAddress, final Callback callback) {
    String suffix = ":Binding";
    Callback chain = new Callback () {
        public void invoke(Object o) {
          String tModelKey = (String) o;
          TModelInstanceDetails tModelInstanceDetails = null;
          Vector tModelInstanceInfoVector = new Vector();
          TModelInstanceInfo tModelInstanceInfo = 
            new TModelInstanceInfo(tModelKey);
          InstanceDetails id = new InstanceDetails();
          id.setInstanceParms(new InstanceParms(messageAddress));
          tModelInstanceInfo.setInstanceDetails(id);
          tModelInstanceInfoVector.add(tModelInstanceInfo);
          tModelInstanceDetails = new TModelInstanceDetails();
          tModelInstanceDetails.setTModelInstanceInfoVector(tModelInstanceInfoVector);
          callback.invoke(tModelInstanceDetails);
        }
        public void handle(Exception e) {
          callback.handle(e);
        }
      };
    findTModelKey(proxy, tModelName+suffix, chain);
  }

  // doesn't require a machine - single invocation with quick completion
  // callback.invoke(KeyedReference)
  // callback.handle(NoSuchTModelKeyException)
  protected void getKeyedReference(YPProxy proxy,
                                   final String tModelName, final String attribute, final String value, final Callback callback) {
    if (log.isInfoEnabled()) {
      log.info(logString("enter getKeyedReference("+tModelName+", "+attribute+", "+value+")"));
    }

    Callback chain = new Callback () {
        public void invoke(Object o) {
          String key = (String) o;
          KeyedReference kr = new KeyedReference(attribute, value);
          kr.setTModelKey(key);
          if (log.isInfoEnabled()) {
            log.info(logString("exit getKeyedReference("+tModelName+", "+attribute+", "+value+")="+key));
          }
          callback.invoke(kr);
        }
        public void handle(Exception e) {
          if (log.isInfoEnabled()) {
            log.info(logString("exception getKeyedReference("+tModelName+", "+attribute+", "+value+")"),e);
          }
          callback.handle(e);
        }
      };

    findTModelKey(proxy, tModelName, chain);
  }


  /** cache for findTModelKey, tModelName to tModelKey.  Access should be synchronized on schemeKeys 
   *
   * Caching currently disabled. Current single hash table implementation does
   * not work with distributed yp servers. Key/Name pair is different for each
   * server. Resolution to a specific yp server occurs in the yp module after
   * servicediscovery has formulated the UDDI4J call. 
   *
   * @note HashMap<String,String> 
   **/

  private final HashMap cachedKeys = new HashMap(11);
  protected String getCachedKey(String name) {
    /* Disable for distributed YP - see RFE 
     */
    if (cacheEnabled) {
      synchronized (cachedKeys) {
	return (String) cachedKeys.get(name);
      }
    } else {
      return null;
    }
  }
  protected void setCachedKey(String name, String key) {
    if (cacheEnabled) {
      synchronized (cachedKeys) {
	cachedKeys.put(name, key);
      }
    }
  }
  private final HashMap cachedNames = new HashMap(11);
  protected String getCachedName(String key) {
    if (cacheEnabled) {
      synchronized (cachedNames) {
	return (String) cachedNames.get(key);
      }
    } else {
      return null;
    }
  }
  protected void setCachedName(String key, String name) {
    if (cacheEnabled) {
      synchronized (cachedNames) {
	cachedNames.put(key, name);
      }
    }
  }

  protected void setCache(String key, String name) {
    setCachedKey(name,key);
    setCachedName(key,name);
  }

  /** get the TModel key from YP, possibly using a previous value.
   * returns the value via the callback.  On success, Callback.invoke
   * will pass in a String;
   **/
  protected void findTModelKey(YPProxy proxy, final String tModelName, final Callback callback) {
    {
      String k = getCachedKey(tModelName);
      if (k != null) {
        callback.invoke(k);
        return;
      }
    }

    if (log.isInfoEnabled()) {
      log.info(logString("enter findTModelKey("+tModelName+")"));
    }

    YPFuture fut = proxy.find_tModel(tModelName, null, null, null, 1);
    Callback smc = new CallbackDelegate(callback) {
        public void invoke(Object result) {
          TModelList tlist = (TModelList) result;

          if (log.isInfoEnabled()) {
            if (tlist == null) {
              log.info(logString("findTModelKey: unable to find " + tModelName));
            } else if (log.isInfoEnabled()) {
              log.info(logString("findTModelKey: found " + tModelName));
            }
          }

          if (tlist == null) {
            callback.handle(new NoSuchTModelKeyException("Unable to find tModel for " + tModelName));
            return;
          }

          TModelInfos infos = tlist.getTModelInfos();
          Vector tms = infos.getTModelInfoVector();

          if (tms.size() == 0) {
            callback.handle(new NoSuchTModelKeyException("Unable to find tModel for " + tModelName));
            return;
          }
          String key = ((TModelInfo) tms.elementAt(0)).getTModelKey();
            
          setCache(key, tModelName);
          if (log.isInfoEnabled()) {
            log.info(logString("exit findTModelKey("+tModelName+")="+key));
          }
          super.invoke(key);
        }
      };

    launch(fut, smc);
  }

  protected void findTModelName(YPProxy proxy, final String tModelKey, final Callback callback) {
    {
      String n = getCachedName(tModelKey);
      if (n != null) {
        callback.invoke(n);
        return;
      }
    }

    YPFuture fut = proxy.get_tModelDetail(tModelKey);
    Callback smc = new CallbackDelegate(callback) {
        public void invoke(Object result) {
          TModelDetail tDetail = (TModelDetail) result;
          Vector tList = tDetail.getTModelVector();
          if (tList.size() > 0) {
            TModel tm = (TModel) tList.elementAt(0);
            String name = tm.getNameString();
            setCache(tModelKey, name);
            super.invoke(name);
          } else {
            log.info("Requested TModel was not found in registry " + tModelKey);
            super.invoke(null);
          }
        }
      };
    launch(fut, smc);
  }


  protected void launch(YPFuture fut, Callback callback) {
    (new OneShotMachine(fut, callback, ypService, threads)).start();
  }
    
  /** @note Callback.invoke(ServiceDetail) **/
  protected void getServiceDetail(YPProxy proxy, Vector serviceKeys, Callback callback) {
    YPFuture fut = proxy.get_serviceDetail(serviceKeys);
    launch(fut, callback);
  }

  /** @note Callback.invoke(ServiceDetail) **/
  protected void getServiceDetail(YPProxy proxy, String serviceKey, Callback callback) {
    YPFuture fut = proxy.get_serviceDetail(serviceKey);
    launch(fut, callback);
  }

  protected void findService(YPProxy proxy, 
                             String businessKey, Vector names, CategoryBag categoryBag, TModelBag  tModelBag, 
                             FindQualifiers findQualifiers, int maxRows,
                             Callback callback) {
    YPFuture fut = proxy.find_service(businessKey,names,categoryBag,tModelBag,findQualifiers, maxRows);
    launch(fut, callback);
  }

  protected void findBusiness(YPProxy proxy,
                              Vector names, DiscoveryURLs discoveryURLs, IdentifierBag identifierBag,
                              CategoryBag categoryBag, TModelBag tModelBag, FindQualifiers findQualifiers,
                              int maxRows,
                              Callback callback) {
    YPFuture fut = proxy.find_business(names, discoveryURLs, identifierBag, categoryBag, tModelBag, findQualifiers, maxRows);
    launch(fut, callback);
  }

  protected void saveBusiness(YPProxy proxy,
                              AuthToken token,
                              Vector entities,
                              Callback callback) {
    YPFuture fut = proxy.save_business(token.getAuthInfoString(), entities);
    launch(fut, callback);
  }

  /** @note Callback.invoke(BusinessDetail) **/
  protected void getBusinessDetail(YPProxy proxy, String key, Callback callback) {
    YPFuture fut = proxy.get_businessDetail(key);
    launch(fut, callback);
  }

  /** @note Callback.invoke(BusinessDetail) **/
  protected void getBusinessDetail(YPProxy proxy, Vector keys, Callback callback) {
    YPFuture fut = proxy.get_businessDetail(keys);
    launch(fut, callback);
  }

  public void getAuthToken(YPProxy proxy, String username, String password, Callback callback) {
    YPFuture fut = proxy.get_authToken(username, password);
    launch(fut, callback);
  }

  public void discardAuthToken(YPProxy proxy, AuthToken token, Callback callback) {
    YPFuture fut = proxy.discard_authToken(token.getAuthInfoString());
    launch(fut, callback);
  }

  public static abstract class CallbackDelegate implements Callback {
    private final Callback delegate;
    public CallbackDelegate(Callback delegate) { this.delegate = delegate; }
    public void invoke(Object result) {
      delegate.invoke(result);
    }
    public void handle(Exception e) {
      delegate.handle(e);
    }
  }
    
  /** Loop over the collection, calling sub.invoke(element) and finally calling next.invoke(sub)
   * @note Callback.invoke(sub) 
   **/
  protected void loop(Collection collection, final Callback sub, Callback next) {
    class State { Iterator it; Callback chain; };
    final State state = new State();
    
    
    state.it = collection.iterator();
    state.chain = new CallbackDelegate(next) { 
      public void invoke(Object result) {
      if (state.it.hasNext()) {
        sub.invoke(new Object[] {state.it.next(), state.chain});
      } else {
        super.invoke(sub);
      }}};
    state.chain.invoke(null);
  }
}

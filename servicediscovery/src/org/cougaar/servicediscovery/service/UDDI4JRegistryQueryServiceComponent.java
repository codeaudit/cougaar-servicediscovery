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
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;
import org.cougaar.servicediscovery.description.*;
import org.cougaar.servicediscovery.description.ServiceInfo;
import org.cougaar.servicediscovery.transaction.RegistryQuery;
import org.cougaar.util.GenericStateModelAdapter;
import org.uddi4j.client.UDDIProxy;
import org.uddi4j.UDDIException;
import org.uddi4j.util.CategoryBag;
import org.uddi4j.util.KeyedReference;
import org.uddi4j.util.FindQualifiers;
import org.uddi4j.util.FindQualifier;
import org.uddi4j.datatype.Name;
import org.uddi4j.datatype.binding.BindingTemplates;
import org.uddi4j.datatype.binding.BindingTemplate;
import org.uddi4j.datatype.binding.TModelInstanceInfo;
import org.uddi4j.datatype.binding.InstanceParms;
import org.uddi4j.datatype.tmodel.TModel;
import org.uddi4j.datatype.service.BusinessService;
import org.uddi4j.datatype.service.BusinessServices;
import org.uddi4j.datatype.business.BusinessEntity;
import org.uddi4j.transport.TransportException;
import org.uddi4j.response.*;

import java.util.*;


/**
 *
 *
 *
 */
public final class UDDI4JRegistryQueryServiceComponent extends GenericStateModelAdapter
  implements Component {

  private LoggingService log;
  private RegistryQueryServiceProviderImpl mySP;
  private UIDService uidService;
  private ServiceBroker sb;

  public void setBindingSite(BindingSite bs) {
    // only care about the service broker
    this.sb = bs.getServiceBroker();
  }

  public void load() {
    super.load();

    this.log = (LoggingService)
      sb.getService(this, LoggingService.class, null);
    if (log == null) {
      log = LoggingService.NULL;
    }

    this.uidService = (UIDService)
      sb.getService(this, UIDService.class, null);
    if (uidService == null) {
      throw new RuntimeException("Unable to obtain UID service");
    }

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
    if (uidService != null) {
      sb.releaseService(this, UIDService.class, uidService);
      uidService = null;
    }

    //release all services
    if ((log != null) && (log != LoggingService.NULL)) {
      sb.releaseService(this, LoggingService.class, log);
      log = null;
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

  private class RegistryQueryServiceImpl implements RegistryQueryService {
    private String queryURL = null;
    private HashMap tModelKeysCache = new HashMap();
    private UDDIProxy proxy = null;
    private int maxRows = 100;
    private HashMap tModelNameCache = new HashMap();

    public RegistryQueryServiceImpl() {
      queryURL = System.getProperty("org.cougaar.servicediscovery.registry.queryURL");
    }
    /**
     * Establishes a connection to a registry.
     */
    private boolean makeConnection() {
      // Define connection configuration properties.
      proxy = new UDDIProxy();
      try {
        proxy.setInquiryURL(queryURL);
      } catch (java.net.MalformedURLException e) {
        if (log.isErrorEnabled()) {
          log.error("query or publish URL is wrong", e);
        }
        return false;
      }
      return true;
    }

    /**
     * Finds providers based on the searchable attributes contained in the
     * RegistryQuery object.
     * @param query RegistryQuery containing the query attibutes.
     * @return Collection of ProviderInfo objects.  Can return an empty list.
     */
    public Collection findProviders(RegistryQuery query) {
      // TODO:  ADD some kind of retry mechanism????
      if (!makeConnection()) {
        if (log.isErrorEnabled()) {
          log.error("findProviders: make connection failed ");
          return Collections.EMPTY_LIST;
        }
      }
      // if we get a connection, proceed
      Collection providerInfos;
      // Explicit call for a specific service
      if (query.getServiceName() != null) {
        providerInfos = findServiceByNames(query);
      }
      else {
        providerInfos = findAllProviders(query);
      }
      return providerInfos;
    }

    /**
     * Finds all services based on the searchable attributes contained in the
     * RegistryQuery object.
     * @param query RegistryQuery containing the query attibutes.
     * @return Collection of ServiceInfo objects.  Can return an empty list.
     */
    public Collection findServices(RegistryQuery query) {
      // TODO:  ADD some kind of retry mechanism????
      if (!makeConnection()) {
        if (log.isErrorEnabled()) {
          log.error("findProviders: make connection failed ");
          return Collections.EMPTY_LIST;
        }
      }
      Collection services = findAllServices(query);
      return services;
    }

    private Collection findAllServices(RegistryQuery rq) {
      Collection services = new ArrayList();

      CategoryBag bag = null;
      if (rq.getServiceClassifications() != null) {
        bag = createCategoryBag(rq.getServiceClassifications());
      }

      FindQualifiers fq = null;
      if (rq.getFindQualifiers() != null) {
        String qualifier = (String) rq.getFindQualifiers().iterator().next();
        fq = new FindQualifiers();
        fq.getFindQualifierVector().add(new FindQualifier(qualifier));
      }

      // for now, we only support exact name matches
      Vector serviceNames = null;
      if (rq.getServiceName() != null) {
        serviceNames = new Vector();
        serviceNames.add( new Name(rq.getServiceName()));
      }
      try {
        ServiceList serviceList = proxy.find_service(null, serviceNames,
                                                     bag, null, fq, maxRows);
        Enumeration serviceInfos = serviceList.getServiceInfos().getServiceInfoVector().elements();
        Vector serviceKeys = new Vector();
        while (serviceInfos.hasMoreElements()) {
          org.uddi4j.response.ServiceInfo si = (org.uddi4j.response.ServiceInfo) serviceInfos.nextElement();
          serviceKeys.add(si.getServiceKey());
          if (log.isDebugEnabled()) {
            log.debug("[findAllServices]ServiceName : " + si.getNameString());
          }
        }
        if (!serviceKeys.isEmpty()) {
          services = createServiceInfos(serviceKeys);
        } else {
          if (log.isDebugEnabled()) {
            log.debug("No services were found ");
          }
          return Collections.EMPTY_LIST;
        }
      } catch (UDDIException e) {
        if (log.isErrorEnabled()) {
          DispositionReport dr = e.getDispositionReport();
          log.error("UDDIException faultCode:" + e.getFaultCode() +
                    "\n operator:" + dr.getOperator() +
                    "\n generic:"  + dr.getGeneric() +
                    "\n errno:"    + dr.getErrno() +
                    "\n errCode:"  + dr.getErrCode() +
                    "\n errInfoText:" + dr.getErrInfoText(), e);
        }
      } catch (TransportException e) {
        if (log.isErrorEnabled()) {
          log.error("Exception", e);
        }
      }
      return services;
    }

    private Collection findAllProviders(RegistryQuery rq) {
      Collection providers = null;

      CategoryBag bag = null;
      if (rq.getBusinessClassifications() != null) {
        bag = createCategoryBag(rq.getBusinessClassifications());
      }

      FindQualifiers fq = null;
      if (rq.getFindQualifiers() != null) {
        String qualifier = (String) rq.getFindQualifiers().iterator().next();
        fq = new FindQualifiers();
        fq.getFindQualifierVector().add(new FindQualifier(qualifier));
      }

      // for now, we only support exact name matches
      Vector providerNames = null;
      if (rq.getProviderName() != null) {
        providerNames = new Vector();
        providerNames.add( new Name(rq.getProviderName()));
      }

      try {
        BusinessList bzList  = proxy.find_business(providerNames, null, null, bag, null, fq, maxRows);
        Enumeration businessInfos = bzList.getBusinessInfos().getBusinessInfoVector().elements();
        Vector businessKeys = new Vector();
        String serviceKey = null;
        while (businessInfos.hasMoreElements()) {
          org.uddi4j.response.BusinessInfo bi = (org.uddi4j.response.BusinessInfo) businessInfos.nextElement();
          businessKeys.add(bi.getBusinessKey());
          if (log.isDebugEnabled()) {
            log.debug("[findAllProviders] Provider name: " + bi.getNameString());
          }
        }
        providers = createProviderInfos(businessKeys, serviceKey);
      } catch (UDDIException e) {
        if (log.isErrorEnabled()) {
          DispositionReport dr = e.getDispositionReport();
          log.error("UDDIException faultCode:" + e.getFaultCode() +
                    "\n operator:" + dr.getOperator() +
                    "\n generic:"  + dr.getGeneric() +
                    "\n errno:"    + dr.getErrno() +
                    "\n errCode:"  + dr.getErrCode() +
                    "\n errInfoText:" + dr.getErrInfoText(), e);
        }
      } catch (TransportException e) {
        if (log.isErrorEnabled()) {
          log.error("Exception", e);
        }
      }
      return providers;
    }

    private Collection findServiceByNames(RegistryQuery rq) {

      Collection providers = new ArrayList();

      CategoryBag bag = null;
      if (rq.getBusinessClassifications() != null) {
        bag = createCategoryBag(rq.getBusinessClassifications());
      }

      FindQualifiers fq = null;
      if (rq.getFindQualifiers() != null) {
        String qualifier = (String) rq.getFindQualifiers().iterator().next();
        fq = new FindQualifiers();
        fq.getFindQualifierVector().add(new FindQualifier(qualifier));
      }

      // for now, we only support exact name matches
      Vector providerNames = null;
      if (rq.getProviderName() != null) {
        providerNames = new Vector();
        providerNames.add( new Name(rq.getProviderName()));
      }

      try {
        BusinessList bzList  = proxy.find_business(providerNames, null, null, bag, null, fq, maxRows);
        Enumeration businessInfos = bzList.getBusinessInfos().getBusinessInfoVector().elements();
        Vector businessKeys = new Vector();
        while (businessInfos.hasMoreElements()) {
          org.uddi4j.response.BusinessInfo bi = (org.uddi4j.response.BusinessInfo) businessInfos.nextElement();
          businessKeys.add(bi.getBusinessKey());
          if (log.isDebugEnabled()) {
            log.debug("[findAllProviders] Provider name: " + bi.getNameString());
            log.debug("[findServiceByName] Service name: " + rq.getServiceName());
          }
        }
        if (!businessKeys.isEmpty()) {
          providers = createProviderInfos(businessKeys, rq.getServiceName());
        } else {
          if (log.isDebugEnabled()) {
            log.debug("No services were found ");
          }
          return Collections.EMPTY_LIST;
        }
      } catch (UDDIException e) {
        if (log.isErrorEnabled()) {
          DispositionReport dr = e.getDispositionReport();
          log.error("UDDIException faultCode:" + e.getFaultCode() +
                    "\n operator:" + dr.getOperator() +
                    "\n generic:"  + dr.getGeneric() +
                    "\n errno:"    + dr.getErrno() +
                    "\n errCode:"  + dr.getErrCode() +
                    "\n errInfoText:" + dr.getErrInfoText(), e);
        }
      } catch (TransportException e) {
        if (log.isErrorEnabled()) {
          log.error("Exception", e);
        }
      }
      return providers;
    }

    public Collection findServiceAndBinding(RegistryQuery query) {
      // TODO:  ADD some kind of retry mechanism????
      if (!makeConnection()) {
        if (log.isErrorEnabled()) {
          log.error("findServiceAndBinding: make connection failed ");
          return Collections.EMPTY_LIST;
        }
      }
      Collection services = new ArrayList();
      CategoryBag bag = null;
      if (query.getServiceClassifications() != null) {
        bag = createCategoryBag(query.getServiceClassifications());
      }

      FindQualifiers fq = null;
      if (query.getFindQualifiers() != null) {
        String qualifier = (String) query.getFindQualifiers().iterator().next();
        fq = new FindQualifiers();
        fq.getFindQualifierVector().add(new FindQualifier(qualifier));
      }

      // for now, we only support exact name matches
      Vector serviceNames = null;
      if (query.getServiceName() != null) {
        serviceNames = new Vector();
        serviceNames.add( new Name(query.getServiceName()));
      }
      try {
        ServiceList serviceList = proxy.find_service(null, serviceNames,
                                                     bag, null, fq, maxRows);
        Enumeration serviceInfos = serviceList.getServiceInfos().getServiceInfoVector().elements();
        Vector serviceKeys = new Vector();
        while (serviceInfos.hasMoreElements()) {
          org.uddi4j.response.ServiceInfo si = (org.uddi4j.response.ServiceInfo) serviceInfos.nextElement();
          serviceKeys.add(si.getServiceKey());
          if (log.isDebugEnabled()) {
            log.debug("[findServiceAndBinding]ServiceName : " + si.getNameString());
          }
        }
        if (!serviceKeys.isEmpty()) {
          services = createSimpleServiceInfos(serviceKeys);
        } else {
          if (log.isDebugEnabled()) {
            log.debug("No services were found ");
          }
          return Collections.EMPTY_LIST;
        }
      } catch (UDDIException e) {
        if (log.isErrorEnabled()) {
          DispositionReport dr = e.getDispositionReport();
          log.error("UDDIException faultCode:" + e.getFaultCode() +
                    "\n operator:" + dr.getOperator() +
                    "\n generic:"  + dr.getGeneric() +
                    "\n errno:"    + dr.getErrno() +
                    "\n errCode:"  + dr.getErrCode() +
                    "\n errInfoText:" + dr.getErrInfoText(), e);
        }
      } catch (TransportException e) {
        if (log.isErrorEnabled()) {
          log.error("Exception", e);
        }
      }
      return services;
    }

    private Collection createProviderInfos(Vector businessKeys, String serviceName) {
      Collection providers = new ArrayList();
      try {
        BusinessDetail bd = proxy.get_businessDetail(businessKeys);
        Enumeration enum = bd.getBusinessEntityVector().elements();
        while(enum.hasMoreElements()) {
          BusinessEntity be = (BusinessEntity) enum.nextElement();
          ProviderInfo providerInfo = new ProviderInfo();
          providerInfo.setProviderName(be.getDefaultNameString());
          Collection bzClass = getBusinessClassifications(be.getCategoryBag());
          providerInfo.setBusinessClassifications(bzClass);
          // only retrieve the specified service
          Collection serviceInfos = new ArrayList();
          if (serviceName != null) {
            BusinessService bs = getBusinessService(be.getBusinessServices().getBusinessServiceVector(),
                                                    serviceName);

            ServiceInfo si = new ServiceInfo(bs.getDefaultNameString(), bs.getServiceKey(),
                                             getServiceClassifications(bs.getCategoryBag()),
                                             getServiceBindings(bs.getBindingTemplates()),
                                             be.getDefaultNameString(),
                                             bzClass);
            serviceInfos.add(si);
          } else {
            // Get all the services
            if (be.getBusinessServices() != null) {
              serviceInfos = createServiceInfos(be.getBusinessServices(), be.getDefaultNameString(), bzClass);
            } else {
              if (log.isDebugEnabled()) {
                log.debug("No services registered with provider " + be.getDefaultNameString());
              }
              serviceInfos = Collections.EMPTY_LIST;
            }
            providers.add(new ProviderInfo(be.getDefaultNameString(), bzClass, serviceInfos));
          }
        }
      } catch (UDDIException e) {
        if (log.isErrorEnabled()) {
          DispositionReport dr = e.getDispositionReport();
          log.error("UDDIException faultCode:" + e.getFaultCode() +
                    "\n operator:" + dr.getOperator() +
                    "\n generic:"  + dr.getGeneric() +
                    "\n errno:"    + dr.getErrno() +
                    "\n errCode:"  + dr.getErrCode() +
                    "\n errInfoText:" + dr.getErrInfoText(), e);
        }
      } catch (TransportException e) {
        if (log.isErrorEnabled()) {
          log.error("Exception", e);
        }
      }
      return providers;
    }

    private BusinessService getBusinessService(Vector services, String serviceName) {
      Enumeration enum = services.elements();
      BusinessService businessService = null;
      while (enum.hasMoreElements()) {
        BusinessService bs = (BusinessService) enum.nextElement();
        if (bs.getDefaultNameString().equals(serviceName)) {
          businessService = bs;
        }
      }
      return businessService;
    }

    private Collection createServiceInfos(Vector serviceKeys) {
      Collection serviceInfos = new ArrayList();

      try {
        ServiceDetail sd = proxy.get_serviceDetail(serviceKeys);
        Enumeration serviceEnum = sd.getBusinessServiceVector().elements();
        while(serviceEnum.hasMoreElements()) {
          BusinessService bs = (BusinessService) serviceEnum.nextElement();
          ServiceInfo serviceInfo = new ServiceInfo();
          serviceInfo.setServiceId(bs.getServiceKey());
          serviceInfo.setServiceName(bs.getDefaultNameString());
          serviceInfo.setServiceClassifications(getServiceClassifications(bs.getCategoryBag()));
          serviceInfo.setServiceBindings(getServiceBindings(bs.getBindingTemplates()));
          BusinessDetail bd = proxy.get_businessDetail(bs.getBusinessKey());
          Enumeration businessEnum = bd.getBusinessEntityVector().elements();
          while (businessEnum.hasMoreElements()) {
            BusinessEntity be = (BusinessEntity) businessEnum.nextElement();
            serviceInfo.setProviderName(be.getDefaultNameString());
            serviceInfo.setBusinessClassifications(getBusinessClassifications(be.getCategoryBag()));
          }
          if (log.isDebugEnabled()) {
            log.debug("Creating ServiceInfo " + serviceInfo.getProviderName());
          }

          serviceInfos.add(serviceInfo);
        }
      } catch (UDDIException e) {
        if (log.isErrorEnabled()) {
          DispositionReport dr = e.getDispositionReport();
          log.error("UDDIException faultCode:" + e.getFaultCode() +
                    "\n operator:" + dr.getOperator() +
                    "\n generic:"  + dr.getGeneric() +
                    "\n errno:"    + dr.getErrno() +
                    "\n errCode:"  + dr.getErrCode() +
                    "\n errInfoText:" + dr.getErrInfoText(), e);
        }
      } catch (TransportException e) {
        if (log.isErrorEnabled()) {
          log.error("Exception", e);
        }
      }
      return serviceInfos;
    }

    private Collection createSimpleServiceInfos(Vector serviceKeys) {
      Collection serviceInfos = new ArrayList();

      try {
        ServiceDetail sd = proxy.get_serviceDetail(serviceKeys);
        Enumeration serviceEnum = sd.getBusinessServiceVector().elements();
        while(serviceEnum.hasMoreElements()) {
          BusinessService bs = (BusinessService) serviceEnum.nextElement();
          ServiceInfo serviceInfo = new ServiceInfo();
          serviceInfo.setServiceId(bs.getServiceKey());
          serviceInfo.setServiceName(bs.getDefaultNameString());
          serviceInfo.setServiceClassifications(getServiceClassifications(bs.getCategoryBag()));
          serviceInfo.setServiceBindings(getServiceBindings(bs.getBindingTemplates()));
          serviceInfos.add(serviceInfo);
        }
      } catch (UDDIException e) {
        if (log.isErrorEnabled()) {
          DispositionReport dr = e.getDispositionReport();
          log.error("UDDIException faultCode:" + e.getFaultCode() +
                    "\n operator:" + dr.getOperator() +
                    "\n generic:"  + dr.getGeneric() +
                    "\n errno:"    + dr.getErrno() +
                    "\n errCode:"  + dr.getErrCode() +
                    "\n errInfoText:" + dr.getErrInfoText(), e);
        }
      } catch (TransportException e) {
        if (log.isErrorEnabled()) {
          log.error("Exception", e);
        }
      }
      return serviceInfos;
    }

    private Collection createServiceInfos(BusinessServices bs, String providerName, Collection bzClass)  {
      Enumeration serviceEnum = bs.getBusinessServiceVector().elements();
      Collection serviceInfos = new ArrayList();
      while(serviceEnum.hasMoreElements()) {
        BusinessService svc = (BusinessService) serviceEnum.nextElement();
        ServiceInfo serviceInfo = new ServiceInfo(svc.getDefaultNameString(), svc.getServiceKey(),
                                                  getServiceClassifications(svc.getCategoryBag()),
                                                  getServiceBindings(svc.getBindingTemplates()),
                                                  providerName, bzClass);
        serviceInfos.add(serviceInfo);
      }
      return serviceInfos;
    }

    /**
     * Have to create a transient scheme in order to query
     * based on a classification
     */
    private CategoryBag createCategoryBag(Collection classifications) {
      CategoryBag bag = new CategoryBag();
      for (Iterator i = classifications.iterator(); i.hasNext();) {
        Classification rc = (Classification) i.next();
        bag.getKeyedReferenceVector().add(getKeyedReference(rc.getClassificationSchemeName(),
                                                            rc.getClassificationName(),
                                                            rc.getClassificationCode()));
      }
      return bag;
    }

    private KeyedReference getKeyedReference(String tModelName, String attribute, String value) {
      String key = findTModelKey(tModelName);
      if (key == null) {
        return null;
      }
      KeyedReference kr = new KeyedReference(attribute, value);
      kr.setTModelKey(key);
      return kr;
    }

    private String findTModelKey(String tModelName) {
      if(!tModelKeysCache.containsKey(tModelName)) {
        TModelList tlist = null;
        try {
          tlist = proxy.find_tModel(tModelName, null, null, null, 1);
        } catch (Exception e) {
          if (log.isErrorEnabled()) {
            log.error("Caught an Exception finding tModel.", e);
          }
        }
        TModelInfos infos = tlist.getTModelInfos();
        Vector tms = infos.getTModelInfoVector();
        if (tms.size() > 0) {
          tModelKeysCache.put(tModelName, ((TModelInfo) tms.elementAt(0)).getTModelKey());
          // cache the key - name pair as well
          tModelNameCache.put(((TModelInfo) tms.elementAt(0)).getTModelKey(), tModelName);
        } else {
          if (log.isWarnEnabled()) {
            log.warn("Requested TModel was not found in registry " + tModelName);
            return null;
          }
        }
      }
      return (String) tModelKeysCache.get(tModelName);
    }


    private String findTModelName(String tModelKey) {
      if(!tModelNameCache.containsKey(tModelKey)) {
        TModelDetail tDetail = null;
        try {
          tDetail = proxy.get_tModelDetail(tModelKey);
        } catch (Exception e) {
          if (log.isErrorEnabled()) {
            log.error("Caught an Exception finding tModel.", e);
          }
        }
        Vector tList = tDetail.getTModelVector();
        if (tList.size() > 0) {
          TModel tm = (TModel) tList.elementAt(0);
          tModelNameCache.put(tModelKey, tm.getNameString());
          // cache the key - name pair as well
          tModelKeysCache.put(tm.getNameString(), tModelKey);
        } else {
          if (log.isWarnEnabled()) {
            log.warn("Requested TModel was not found in registry " + tModelKey);
            return null;
          }
        }
      }
      return (String) tModelNameCache.get(tModelKey);
    }

    private Collection getServiceClassifications(CategoryBag bag) {
      Collection serviceClassifications = new ArrayList();
      Enumeration enum = bag.getKeyedReferenceVector().elements();
      while (enum.hasMoreElements()) {
        KeyedReference kr = (KeyedReference) enum.nextElement();
        serviceClassifications.add(createClassification( new ServiceClassificationImpl(), kr));
      }
      return serviceClassifications;
    }

    private Collection getBusinessClassifications(CategoryBag bag) {
      Collection businessClassifications = new ArrayList();
      Enumeration enum = bag.getKeyedReferenceVector().elements();
      while (enum.hasMoreElements()) {
        KeyedReference kr = (KeyedReference) enum.nextElement();
        businessClassifications.add(createClassification( new BusinessClassificationImpl(), kr));
      }
      return businessClassifications;
    }

    private Classification createClassification(Classification classification,
                                                KeyedReference kr) {
      String tModelName = findTModelName(kr.getTModelKey());
      if (tModelName != null) {
        classification.setClassificationCode(kr.getKeyValue());
        classification.setClassificationName(kr.getKeyName());
        classification.setClassificationSchemeName(tModelName);
      } else {
        // TODO: handle this more appropriately; mostly like fails due to failed registry response
        if (log.isErrorEnabled()) {
          log.error("A serious error occured, tModelName is null, ");
        }
      }

      return classification;
    }

    private Collection getServiceBindings(BindingTemplates bindingTemplates) {
      Collection serviceBindings = new ArrayList();

      Enumeration enum = bindingTemplates.getBindingTemplateVector().elements();
      while (enum.hasMoreElements()) {
        BindingTemplate binding = (BindingTemplate) enum.nextElement();
        serviceBindings.add(getServiceBinding(binding));
      }
      return serviceBindings;
    }

    private ServiceBinding getServiceBinding(BindingTemplate binding) {
      Enumeration enum = binding.getTModelInstanceDetails().getTModelInstanceInfoVector().elements();
      String bindingType = null;
      InstanceParms ip = null;
      if (enum.hasMoreElements()) {
        TModelInstanceInfo tio = (TModelInstanceInfo) enum.nextElement();
        bindingType = findTModelName(tio.getTModelKey());
        ip = tio.getInstanceDetails().getInstanceParms();
      }
      return new org.cougaar.servicediscovery.description.ServiceBinding(binding.getAccessPoint().getText(),
                                                                         bindingType,
                                                                         ip.getText());
    }
  }
}










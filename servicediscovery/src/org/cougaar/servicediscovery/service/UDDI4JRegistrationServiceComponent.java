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


package org.cougaar.servicediscovery.service;


import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.servicediscovery.description.AdditionalQualificationRecord;
import org.cougaar.servicediscovery.description.BusinessCategory;
import org.cougaar.servicediscovery.description.ProviderDescription;
import org.cougaar.servicediscovery.description.ServiceCategory;
import org.cougaar.servicediscovery.description.ServiceClassification;
import org.cougaar.servicediscovery.description.ServiceProfile;
import org.cougaar.servicediscovery.util.LockPool;
import org.cougaar.util.UnaryPredicate;


import org.uddi4j.client.UDDIProxy;
import org.uddi4j.response.*;
import org.uddi4j.UDDIException;
import org.uddi4j.util.KeyedReference;
import org.uddi4j.util.CategoryBag;
import org.uddi4j.util.FindQualifiers;
import org.uddi4j.util.FindQualifier;
import org.uddi4j.datatype.business.BusinessEntity;
import org.uddi4j.datatype.service.BusinessService;
import org.uddi4j.datatype.service.BusinessServices;
import org.uddi4j.datatype.binding.*;
import org.uddi4j.transport.TransportException;
import org.uddi4j.client.*;
import org.uddi4j.datatype.Name;
import org.uddi4j.datatype.binding.*;
import org.uddi4j.datatype.business.*;
import org.uddi4j.datatype.service.*;

import java.util.*;



/**
 * This component provides access to the Registration Service
 * which allows a service provider agent to register
 * the services it provides in the registry.
 *
 * Registered Services are kept both locally and on the Blackboard.
 * The redundant storage is to provide quick access by having the information stored
 * locally within the Agent, but also have the ability to re-hydrate if the agent gets killed.
 *
 * @see RegistrationService
 */


public final class UDDI4JRegistrationServiceComponent
  extends ComponentPlugin
{

 /**  Cougaar service used for logging **/
  protected LoggingService log;
  private RegistrationServiceProviderImpl mySP;

  /** Local storage of ProviderDescriptions **/
  private Hashtable storedData = new Hashtable(5);

  /** List of objects to add to the blackboard **/
  private final List todo = new ArrayList(5);
  private AuthToken authorization;
  private UDDIProxy proxy;
  HashMap schemeKeys = new HashMap();

  public void load() {
    super.load();
    this.log = (LoggingService)
      getServiceBroker().getService(this, LoggingService.class, null);
    if (log == null) {
      log = LoggingService.NULL;
    }
    // create and advertise our service
    this.mySP = new RegistrationServiceProviderImpl();
    getServiceBroker().addService(RegistrationService.class, mySP);
  }

  public void unload() {
    // revoke our service
    if (mySP != null) {
      getServiceBroker().revokeService(RegistrationService.class, mySP);
      mySP = null;
    }
    // release all services
    if ((log != null) && (log != LoggingService.NULL)) {
      getServiceBroker().releaseService(this, LoggingService.class, log);
      log = null;
    }
    super.unload();
  }

  /**
   * Populate the Agent will all known ProviderDescriptions.
   * The only reason ProviderDescriptions would exist is if the
   * agent is re-hydrating.
   */
  protected void setupSubscriptions() {
    populateLocalHash();
  }

  protected void execute() {
    addAll();
  }

  private void populateLocalHash() {
      Iterator iter = (blackboard.query(new UnaryPredicate() {
          public boolean execute(Object o) {
            if(o instanceof MyBlackboardObject) {
              return true;
            } else {
              return false;
            }}})).iterator();

      while(iter.hasNext()) {
        MyBlackboardObject mbb = (MyBlackboardObject)iter.next();
        storedData.put(mbb.getKey(), mbb.getData());
      }
    }

  private class RegistrationServiceProviderImpl
    implements ServiceProvider {

    private final RegistrationServiceImpl regService;

    public RegistrationServiceProviderImpl() {
      // keep only one instance
      regService = new RegistrationServiceImpl();
    }

    public Object getService(ServiceBroker sb,
                             Object requestor,
                             Class serviceClass) {
      if (serviceClass == RegistrationService.class) {
        return regService;
      } else {
        return null;
      }
    }

    public void releaseService(ServiceBroker sb,
                               Object requestor,
                               Class serviceClass,
                               Object service) {
    }
  }

  private class RegistrationServiceImpl
    implements RegistrationService {
    HashMap schemes = new HashMap();

    //The following 4 uddi registry parameters are set via System properties.
    String username = null;
    String password = null;
    String queryURL = null;
    String publishURL = null;

    public RegistrationServiceImpl() {
      // FIXME: CHECK VALUES EXIST!!!!
      username = System.getProperty("org.cougaar.servicediscovery.registry.username");
      password = System.getProperty("org.cougaar.servicediscovery.registry.password");
      queryURL = System.getProperty("org.cougaar.servicediscovery.registry.queryURL");
      publishURL = System.getProperty("org.cougaar.servicediscovery.registry.publishURL");
    }

    /**
     * Adds a new ProviderDescription object.
     *
     * @param pd ProviderDescription for this provider.
     * @return success if the Provider was added without error
     */
    public boolean addProviderDescription(ProviderDescription pd) {
      boolean success=false;

      Object token = getLock();
      if(makeConnection()) {
        if(storeProviderDescription(pd))
          success = executePublish(pd);
      }
      freeLock(token);
      return success;
    }

    /**
     * Establishes a connection to a registry.
     *
     */
    private boolean makeConnection() {
      // Define connection configuration properties.

      proxy = new UDDIProxy();

      try {
        proxy.setInquiryURL(queryURL);
        proxy.setPublishURL(publishURL);
        //authorization = proxy.get_authToken(username, password);
        initializeAuthToken();
        return true;
      } catch (java.net.MalformedURLException e) {
        if (log.isErrorEnabled()) {
          log.error("query or publish URL is wrong", e);
        }
        return false;
      }
    }


    private void initializeAuthToken() {
      try {
        authorization = proxy.get_authToken(username, password);
      } catch (UDDIException e) {
        DispositionReport dr = e.getDispositionReport();
        log.error("UDDIException faultCode:" + e.getFaultCode() +
                  "\n operator:" + dr.getOperator() +
                  "\n generic:"  + dr.getGeneric() +
                  "\n errno:"    + dr.getErrno() +
                  "\n errCode:"  + dr.getErrCode() +
                  "\n errInfoText:" + dr.getErrInfoText(), e);
      } catch (TransportException e) {
        if (log.isErrorEnabled()) {
          log.error("Caught an Exception getting authorization", e);
        }
      }
    }



    /**
     * Creates an organization, its classification, and its
     * services, and saves it to the registry.
     *
     * @param pd        the ProviderDescription object
     */
    private boolean executePublish(ProviderDescription pd) {

      boolean success = true;

      // Save business
      //Add classifications at organization level.
      CategoryBag bzBag = new CategoryBag();
      if (pd.getProviderName() == null)  {
        success = false;
        if (log.isErrorEnabled()) {
          log.error("Provider name is null, unable to register. ");
        }
        return success;
      }

      BusinessEntity be = new BusinessEntity("", pd.getProviderName());
      for (Iterator iter = pd.getBusinessCategories().iterator(); iter.hasNext();) {
        BusinessCategory bc = (BusinessCategory) iter.next();
        bzBag.getKeyedReferenceVector().add(getKeyedReference(bc.getCategorySchemeName(),
                                                              bc.getCategoryName(), bc.getCategoryCode()));
      }
      be.setCategoryBag(bzBag);
      Vector entities = new Vector();

      // Create services and service classifications
      Vector services = new Vector();
      Collection serviceDescriptions = pd.getServiceProfiles();
      BusinessServices businessServices = new BusinessServices ();

      for (Iterator i = serviceDescriptions.iterator();i.hasNext() ;) {
        ServiceProfile sd = (ServiceProfile)i.next();
        BusinessService bSvc = new BusinessService("");
        bSvc.setDefaultName(new Name(sd.getServiceProfileID()));
        Collection serviceCategories = sd.getServiceCategories();
        CategoryBag myBag = new CategoryBag();
        for (Iterator it = serviceCategories.iterator(); it.hasNext() ;) {
          ServiceCategory sc = (ServiceCategory)it.next();
          myBag.getKeyedReferenceVector().add(getKeyedReference(sc.getCategorySchemeName(), sc.getCategoryName(),
                                                                sc.getCategoryCode()));

          // add additional service type qualifiers
          for (Iterator j = sc.getAdditionalQualifications().iterator(); j.hasNext();) {
            AdditionalQualificationRecord aqr = (AdditionalQualificationRecord) j.next();
            myBag.getKeyedReferenceVector().add(getKeyedReference(sc.getCategorySchemeName(),
                                                                  aqr.getQualificationName(),
                                                                  aqr.getQualificationValue()));
          }
        }
        bSvc.setCategoryBag(myBag);
        bSvc.setBusinessKey ("");
        if(sd.getTextDescription().trim().length() != 0) {
          bSvc.setDefaultDescriptionString(sd.getTextDescription());
        }
        BindingTemplates bindings = createBindingTemplates(sd.getServiceGroundingURI(),
                                                           createTModelInstance(sd.getServiceGroundingBindingType(),
                                                                                pd.getProviderName()));
        bSvc.setBindingTemplates(bindings);
        services.add(bSvc);
      }
      businessServices.setBusinessServiceVector (services);

      try {
        be.setBusinessServices (businessServices);
        entities.add(be);
        proxy.save_business(authorization.getAuthInfoString(), entities);
      } catch (UDDIException e) {
        DispositionReport dr = e.getDispositionReport();
        if (dr.getErrCode().equals(DispositionReport.E_authTokenExpired)) {
          if (log.isDebugEnabled()) {
            log.debug("Auth Token expired, getting a new one " + getAgentIdentifier());
          }
          initializeAuthToken();
          try {
            proxy.save_business(authorization.getAuthInfoString(), entities);
          } catch (UDDIException ex) {
            DispositionReport d = ex.getDispositionReport();
            if (log.isErrorEnabled()) {

              log.error("UDDIException faultCode:" + e.getFaultCode() +
                        "\n operator:" + d.getOperator() +
                        "\n generic:"  + d.getGeneric() +
                        "\n errno:"    + d.getErrno() +
                        "\n errCode:"  + d.getErrCode() +
                        "\n errInfoText:" + d.getErrInfoText(), e);
            }
            return false;
          }
          catch (TransportException te) {
            if (log.isErrorEnabled()) {
              log.error("Caught a TransportException saving business", e);
            }
            return false;
          }
        } else {
          if (log.isErrorEnabled()) {

            log.error("UDDIException faultCode:" + e.getFaultCode() +
                      "\n operator:" + dr.getOperator() +
                      "\n generic:"  + dr.getGeneric() +
                      "\n errno:"    + dr.getErrno() +
                      "\n errCode:"  + dr.getErrCode() +
                      "\n errInfoText:" + dr.getErrInfoText(), e);
          }
          return false;
        }
      } catch (TransportException e) {
        if (log.isErrorEnabled()) {
          log.error("Caught a TransportException saving business", e);
        }
        return false;
      }
      return success;
    }


    private BindingTemplates createBindingTemplates(String uri, TModelInstanceDetails tModelInstanceDetails) {

      BindingTemplates bindings = new BindingTemplates();
      BindingTemplate binding = new BindingTemplate("", tModelInstanceDetails);
      AccessPoint accessPoint = new AccessPoint(uri, "http");
      binding.setAccessPoint(accessPoint);
      bindings.getBindingTemplateVector().add(binding);
      return bindings;
    }

    private TModelInstanceDetails createTModelInstance(String tModelName, String messageAddress) {
      String suffix = ":Binding";
      TModelInstanceDetails tModelInstanceDetails = null;
      Vector tModelInstanceInfoVector = new Vector();
      TModelInstanceInfo tModelInstanceInfo = new TModelInstanceInfo(findTModelKey(tModelName+suffix));
      InstanceDetails id = new InstanceDetails();
      id.setInstanceParms(new InstanceParms(messageAddress));
      tModelInstanceInfo.setInstanceDetails(id);
      tModelInstanceInfoVector.add(tModelInstanceInfo);
      tModelInstanceDetails = new TModelInstanceDetails();
      tModelInstanceDetails.setTModelInstanceInfoVector(tModelInstanceInfoVector);
      return tModelInstanceDetails;
    }

    private KeyedReference getKeyedReference(String tModelName, String attribute, String value) {
      String key = findTModelKey(tModelName);
      KeyedReference kr = new KeyedReference(attribute, value);
      kr.setTModelKey(key);
      return kr;
    }

    private String findTModelKey(String tModelName) {
      if(!schemeKeys.containsKey(tModelName)) {
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
        schemeKeys.put(tModelName, ((TModelInfo) tms.elementAt(0)).getTModelKey());
      }
      return (String) schemeKeys.get(tModelName);
    }


    public boolean addServiceDescription(String providerKey, ServiceProfile sd){
      Object token = getLock();
      freeLock(token);
      return true;
    }

//  FIXME -NEED TO DELETE THE PROVIDER DESC STORED LOCALLY AS WELL
    public boolean deleteProviderDescription(String providerKey){
      return false;
    }

    /**
     * Use a local HashMap for speed, but also place
     * object on the Blackboard for persistence.
     */
    private boolean storeProviderDescription(ProviderDescription pd) {
      if(pd != null) {
        MyBlackboardObject mbb = new MyBlackboardObject();


        if (pd.getProviderName() != null) {
          // Place on the Blackboard for persistence.
          mbb.setKey(pd.getProviderName());
          mbb.setData(pd);
          addLater(mbb);
          storedData.put(pd.getProviderName(),mbb);
        } else {
          if (log.isErrorEnabled()) {
            log.error("Cannot store ProviderDescription, ProviderName is null!");
          }
          return false;
        }
      } else {
        if (log.isErrorEnabled()) {
          log.error("Received a NULL ProviderDescription");
        }
        return false;
      }
      return true;
    }


    public ProviderDescription getProviderDescription(Object key){
      MyBlackboardObject mbb = (MyBlackboardObject)storedData.get(key);
      if (mbb == null) {
          if(log.isWarnEnabled()) {
            log.warn("MyBlackboardObject is null for key: " + key.toString());
          }
        return null;
      }
      ProviderDescription pd = (ProviderDescription)mbb.getData();
      return pd;
    }


    //Below methods not yet implemented
    public boolean updateProviderDescription(String providerKey, ProviderDescription pd){
      return true;
    }


    public boolean updateServiceDescription(String providerName, Collection serviceCategories){
      boolean success = true;

      Object token = getLock();

      Vector namePatterns = new Vector();
      namePatterns.add(new Name(providerName));
      Vector services = new Vector();

      // Setting FindQualifiers to 'caseSensitiveMatch'
      FindQualifiers findQualifiers = new FindQualifiers();
      Vector qualifier = new Vector();
      qualifier.add(new FindQualifier("caseSensitiveMatch"));
      findQualifiers.setFindQualifierVector(qualifier);
      BusinessList businessList;

      try {
        businessList = proxy.find_business(namePatterns, null, null, null, null, findQualifiers, 5);
        Vector businessInfoVector  = businessList.getBusinessInfos().getBusinessInfoVector();
        BusinessInfo businessInfo = null;
        for (int i = 0; i < businessInfoVector.size(); i++) {
          businessInfo = (BusinessInfo)businessInfoVector.elementAt(i);
          if (log.isDebugEnabled()) {
            log.debug("updateServiceDescription for organization: " + businessInfo.getNameString());
          }
          break;
        }
        if(businessInfo == null) {
          if (log.isWarnEnabled()) {
            log.warn("updateServiceDescription, cannot find registration for: " + providerName);
          }

          freeLock(token);
          return false;
        }
        Enumeration enum = businessInfo.getServiceInfos().getServiceInfoVector().elements();
        Vector serviceKeys = new Vector();
        while (enum.hasMoreElements()) {
          ServiceInfo service = (ServiceInfo) enum.nextElement();
          serviceKeys.add(service.getServiceKey());
        }
        CategoryBag updateBag = new CategoryBag();

        for(Iterator iter = serviceCategories.iterator(); iter.hasNext();) {
          ServiceClassification serviceClass = (ServiceClassification) iter.next();
          updateBag.getKeyedReferenceVector().add(getKeyedReference(serviceClass.getClassificationSchemeName(),
                                                                    serviceClass.getClassificationName(),
                                                                    serviceClass.getClassificationCode()));
        }
        ServiceDetail sd = proxy.get_serviceDetail(serviceKeys);
        Enumeration e = sd.getBusinessServiceVector().elements();
        while (e.hasMoreElements()) {
          BusinessService bs  = (BusinessService)e.nextElement();
          CategoryBag thisBag = bs.getCategoryBag();
          thisBag.getKeyedReferenceVector().addAll(updateBag.getKeyedReferenceVector());
          services.add(bs);
        }
        proxy.save_service(authorization.getAuthInfoString(), services);
      } catch (UDDIException e) {
        DispositionReport dr = e.getDispositionReport();
        if (dr.getErrCode().equals(DispositionReport.E_authTokenExpired)) {
          if (log.isDebugEnabled()) {
            log.debug("Auth token expired, getting a new one " + getAgentIdentifier());
          }
          initializeAuthToken();
          try {
            proxy.save_service(authorization.getAuthInfoString(), services);
          } catch (UDDIException ex) {
            DispositionReport d = ex.getDispositionReport();
            if (log.isErrorEnabled()) {

              log.error("UDDIException faultCode:" + e.getFaultCode() +
                        "\n operator:" + d.getOperator() +
                        "\n generic:"  + d.getGeneric() +
                        "\n errno:"    + d.getErrno() +
                        "\n errCode:"  + d.getErrCode() +
                        "\n errInfoText:" + d.getErrInfoText(), e);
            }
            return false;
          }
          catch (TransportException te) {
            if (log.isErrorEnabled()) {
              log.error("Caught a TransportException saving business", e);
            }
            return false;
          }
        } else {
          if (log.isErrorEnabled()) {

            log.error("UDDIException faultCode:" + e.getFaultCode() +
                      "\n operator:" + dr.getOperator() +
                      "\n generic:"  + dr.getGeneric() +
                      "\n errno:"    + dr.getErrno() +
                      "\n errCode:"  + dr.getErrCode() +
                      "\n errInfoText:" + dr.getErrInfoText(), e);
          }

          return false;
        }
      } catch (TransportException e) {
        if (log.isErrorEnabled()) {
          log.error("Exception", e);
        }
        success = false;
      }

      freeLock(token);
      return success;
    }


    // delete a service description given provider name and service categories.
    // this method currently only deletes one service description
    public boolean deleteServiceDescription(String providerName, Collection serviceCategories) {
      boolean success = true;
      Object token = getLock();
      ServiceInfo service = null;

      Vector namePatterns = new Vector();
      namePatterns.add(new Name(providerName));

      FindQualifiers findQualifiers = new FindQualifiers();
      Vector qualifier = new Vector();
      // find the service based on the service categories
      qualifier.add(new FindQualifier(FindQualifier.serviceSubset));
      findQualifiers.setFindQualifierVector(qualifier);

      CategoryBag bag = new CategoryBag();
      for(Iterator iter = serviceCategories.iterator(); iter.hasNext();) {
        ServiceClassification serviceClass = (ServiceClassification) iter.next();
        bag.getKeyedReferenceVector().add(getKeyedReference(serviceClass.getClassificationSchemeName(),
                                                            serviceClass.getClassificationName(),
                                                            serviceClass.getClassificationCode()));
      }

      BusinessList businessList;
      try {
        businessList = proxy.find_business(namePatterns, null, null, bag, null, findQualifiers, 1);
        Vector businessInfoVector  = businessList.getBusinessInfos().getBusinessInfoVector();
        BusinessInfo businessInfo = null;
        for (int i = 0; i < businessInfoVector.size(); i++) {
          businessInfo = (BusinessInfo)businessInfoVector.elementAt(i);
          if (log.isDebugEnabled()) {
            log.debug("updateServiceDescription for organization: " + businessInfo.getNameString());
          }
          break;
        }
        if(businessInfo == null) {
          if (log.isWarnEnabled()) {
            log.warn("deleteServiceDescription, cannot find registration for: " + providerName);
          }

          freeLock(token);
          return false;
        }
        Enumeration enum = businessInfo.getServiceInfos().getServiceInfoVector().elements();
        //String serviceKey = new Vector();
        if (enum.hasMoreElements()) {
          service = (ServiceInfo) enum.nextElement();
          proxy.delete_service(authorization.getAuthInfoString(), service.getServiceKey());
          success = true;
        } else {
          if (log.isWarnEnabled()) {
            log.warn("deleteServiceDescription, cannot find service for: " + providerName);
          }
          freeLock(token);
          return false;
        }
      } catch (UDDIException e) {
        DispositionReport dr = e.getDispositionReport();
        if (dr.getErrCode().equals(DispositionReport.E_authTokenExpired)) {
          if (log.isDebugEnabled()) {
            log.debug("Auth token expired, gettting a new token " + getAgentIdentifier());
          }
          initializeAuthToken();
          try {
            proxy.delete_service(authorization.getAuthInfoString(), service.getServiceKey());
          } catch (UDDIException ex) {
            DispositionReport d = ex.getDispositionReport();
            if (log.isErrorEnabled()) {

              log.error("UDDIException faultCode:" + e.getFaultCode() +
                        "\n operator:" + d.getOperator() +
                        "\n generic:"  + d.getGeneric() +
                        "\n errno:"    + d.getErrno() +
                        "\n errCode:"  + d.getErrCode() +
                        "\n errInfoText:" + d.getErrInfoText(), e);
            }
            return false;
          }
          catch (TransportException te) {
            if (log.isErrorEnabled()) {
              log.error("Caught a TransportException deleting service ", e);
            }
            return false;
          }
        } else {
          if (log.isErrorEnabled()) {

            log.error("UDDIException faultCode:" + e.getFaultCode() +
                      "\n operator:" + dr.getOperator() +
                      "\n generic:"  + dr.getGeneric() +
                      "\n errno:"    + dr.getErrno() +
                      "\n errCode:"  + dr.getErrCode() +
                      "\n errInfoText:" + dr.getErrInfoText(), e);
          }

          return false;
        }
      } catch (TransportException e) {
        if (log.isErrorEnabled()) {
          log.error("Exception", e);
        }
        success = false;
      }

      freeLock(token);
      return success;
    }
  }


  /**
   * Add all queued up objects to the blackboard.
   * @param bb
   */
  protected void addLater(MyBlackboardObject bb) {
    synchronized (todo) {
      todo.add(bb);
    }
    blackboard.signalClientActivity();
  }


  private void addAll() {
    int n;
    List l;
    synchronized (todo) {
      n = todo.size();
      if (n <= 0) {
        return;
      }
      l = new ArrayList(todo);
      todo.clear();
    }
    for (int i = 0; i < n; i++) {
      blackboard.publishAdd(l.get(i));
    }
  }


  /**
   * Simple class used to hold ServiceDescriptions on the Blackboard.
   * The Blackboard is used to persist objects in case the Agent goes away.
   *
   */
  public class MyBlackboardObject {
    private String uuid;
    private String key;
    private Object data;


    /**
     * Gets the UUID for local object
     * @return String uuid
     */
    public String getUUID() {
      return this.uuid;
    }


    /**
     * Sets the UUID for this provider
     * @param uuid
     */
    public void setUUID(String uuid) {
      this.uuid = uuid;
    }


    /**
     * Gets the key for this object.
     *
     * @return HashMap key
     */
    public String getKey() {
      return this.key;
    }


    /**
     * Sets the key for the HashMap
     * @param key
     */
    public void setKey(String key) {
      this.key = key;
    }


    /**
     * Gets the data associated with this object.
     * @return
     */
    public Object getData() {
      return this.data;
    }


    /**
     * Sets the data associated with this object.
     * @param data
     */
    public void setData(Object data) {
      this.data = data;
    }
  }

  private Object getLock() {
    //    System.out.println(getAgentIdentifier()+ ": waiting on lock");
    Object lockToken = LockPool.getCurrentPool().getLock();
    //    System.out.println(getAgentIdentifier() + ": got lock");
    return lockToken;
  }

  private void freeLock(Object lockToken) {
    //    System.out.println(getAgentIdentifier() + ": freeing lock");
    LockPool.getCurrentPool().freeLock(lockToken);
  }
}

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

package org.cougaar.servicediscovery.util.yp;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import org.apache.xerces.parsers.*;

import org.cougaar.core.component.*;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.log.*;
import org.cougaar.yp.YPService;
import org.cougaar.servicediscovery.util.UDDIConstants;

import org.uddi4j.UDDIException;
import org.uddi4j.client.UDDIProxy;

import org.uddi4j.datatype.tmodel.*;
import org.uddi4j.response.AuthToken;
import org.uddi4j.response.DispositionReport;
import org.uddi4j.response.TModelDetail;
import org.uddi4j.util.*;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.Vector;

/**
 *
 */
public class PublishTaxonomy extends ComponentSupport {
  private LoggingService logger;

  private static UDDIProxy proxy;
  private static String userid;
  private static String password;

  public void initialize() {
    super.initialize();

    ServiceBroker sb = getServiceBroker();
    LoggingService logger = 
      (LoggingService) sb.getService(this, LoggingService.class, null);
    YPService ypService = 
      (YPService) sb.getService(this, YPService.class, null);
    String ypAgent = System.getProperty("org.cougaar.yp.ypAgent");
      
    proxy = ypService.getYP(ypAgent);

    initTaxonomy(proxy);

    proxy = null;
  }

  private void initTaxonomy(UDDIProxy proxyArg) {
    userid = "cougaar";
    password = "cougaarPass";
    
    proxy = proxyArg;

    try {
      genTaxonomy(UDDIConstants.MILITARY_SERVICE_SCHEME, UDDIConstants.MILITARY_SERVICE_SCHEME_UUID);
      genTaxonomy(UDDIConstants.MILITARY_ECHELON_SCHEME, UDDIConstants.MILITARY_ECHELON_SCHEME_UUID);
      genTaxonomy(UDDIConstants.ORGANIZATION_TYPES, UDDIConstants.ORGANIZATION_TYPES_UUID);
      genTaxonomy(UDDIConstants.SOURCING_CAPABILITY_SCHEME, UDDIConstants.SOURCING_CAPABILITY_SCHEME_UUID);
      genTaxonomy(UDDIConstants.SUPPORT_COMMAND_ASSIGNMENT, UDDIConstants.SUPPORT_COMMAND_ASSIGNMENTI_UUID);

      createBindingTModels();
      
    } catch (Exception e){
      e.printStackTrace();
    }
  }

  private void createTaxonomy(String name, String key, String file) throws UDDIException {
    try {
      TModel tModel = new TModel();
      tModel.setName(name);
      Vector tModels = new Vector();
      tModels.addElement(tModel);

      TModelDetail tModelDetail = 
	proxy.save_tModel(proxy.get_authToken(userid, password).getAuthInfoString(), tModels);

      String tModelKey = ((TModel) (tModelDetail.getTModelVector().elementAt(0))).getTModelKey();

      FileInputStream fis = new FileInputStream(file);
      DOMParser parser = new DOMParser();
      parser.parse(new InputSource(fis));
      tModel = new TModel(parser.getDocument().getDocumentElement());

      tModel.setName(name);
      tModel.setTModelKey(tModelKey);

      // Add TModelKey to KeyedReferences
      CategoryBag categoryBag = tModel.getCategoryBag();
      for (int index = 0; index < categoryBag.size(); index++) {
	KeyedReference keyedReference = categoryBag.get(index);
	keyedReference.setTModelKey(tModelKey);
      }
     
      tModelDetail = 
	proxy.save_tModel(proxy.get_authToken(userid, password).getAuthInfoString(), tModels);

    } catch (UDDIException ue) {
      DispositionReport dr = ue.getDispositionReport();
      if (dr!=null) {
	logger.error("UDDIException faultCode:" + ue.getFaultCode() +
		     "\n operator:" + dr.getOperator() +
		     "\n generic:"  + dr.getGeneric() +
		     "\n errno:"    + dr.getErrno() +
		     "\n errCode:"  + dr.getErrCode() +
		     "\n errInfoText:" + dr.getErrInfoText());
      }
      ue.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }


  }

  private void createBindingTModels() {
    // Creating TModels for our binding templates
    Vector tModels = new Vector();
    Vector krList = new Vector();
    
    TModel cougaarTModel = new TModel("", "COUGAAR:Binding");
    cougaarTModel.setDefaultDescriptionString("Protocol for COUGAAR services");
    tModels.add(cougaarTModel);
    try {
      TModelDetail tModelDetail = 
	proxy.save_tModel(proxy.get_authToken(userid, password).getAuthInfoString(), tModels);

      tModels = tModelDetail.getTModelVector();
      cougaarTModel = (TModel) tModels.elementAt(0);

      CategoryBag categoryBag = new CategoryBag();
      KeyedReference wsdlKr = new KeyedReference("uddi-org:types", "wsdlSpec");
      wsdlKr.setTModelKey(cougaarTModel.getTModelKey());
      krList.add(wsdlKr);
      categoryBag.setKeyedReferenceVector(krList);
      cougaarTModel.setCategoryBag(categoryBag);

      tModels.clear();
      tModels.add(cougaarTModel);

      tModelDetail = 
	proxy.save_tModel(proxy.get_authToken(userid, password).getAuthInfoString(), tModels);

    // Handle possible errors
    } catch (UDDIException e) {
      DispositionReport dr = e.getDispositionReport();
      if (dr!=null) {
	logger.error("UDDIException faultCode:" + e.getFaultCode() +
		     "\n operator:" + dr.getOperator() +
		     "\n generic:"  + dr.getGeneric() +
		     "\n errno:"    + dr.getErrno() +
		     "\n errCode:"  + dr.getErrCode() +
		     "\n errInfoText:" + dr.getErrInfoText());
      }
      e.printStackTrace();
      // Catch any other exception that may occur
    } catch (Exception e) {
      e.printStackTrace();
    }

    tModels.clear();

    TModel soapTModel = new TModel("", "SOAP:Binding");
    soapTModel.setDefaultDescriptionString("SOAP binding for non-COUGAAR services");
    tModels.add(soapTModel);
    try {
      TModelDetail tModelDetail = 
	proxy.save_tModel(proxy.get_authToken(userid, password).getAuthInfoString(), tModels);

      tModels = tModelDetail.getTModelVector();
      soapTModel = (TModel) tModels.elementAt(0);
    
      CategoryBag categoryBag = new CategoryBag();
      KeyedReference soapKr = new KeyedReference("uddi-org:types", "soapSpec");
      soapKr.setTModelKey(soapTModel.getTModelKey());
      // described by WSDL
      krList = new Vector();
      krList.add(soapKr);
      KeyedReference wsdlKr = new KeyedReference("uddi-org:types", "wsdlSpec");
      wsdlKr.setTModelKey(soapTModel.getTModelKey());
      krList.add(wsdlKr);
      categoryBag.setKeyedReferenceVector(krList);
      soapTModel.setCategoryBag(categoryBag);

      tModels.clear();
      tModels.add(soapTModel);
    
      tModelDetail = 
	proxy.save_tModel(proxy.get_authToken(userid, password).getAuthInfoString(), tModels);

    // Handle possible errors
    } catch (UDDIException e) {
      DispositionReport dr = e.getDispositionReport();
      if (dr!=null) {
	logger.error("UDDIException faultCode:" + e.getFaultCode() +
		     "\n operator:" + dr.getOperator() +
		     "\n generic:"  + dr.getGeneric() +
		     "\n errno:"    + dr.getErrno() +
		     "\n errCode:"  + dr.getErrCode() +
		     "\n errInfoText:" + dr.getErrInfoText());
      }
      e.printStackTrace();
      // Catch any other exception that may occur
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static boolean validPath(String path) {
    return (new File(path)).exists();
  }

  private boolean genTaxonomy(String name, String uuid) throws UDDIException {
    String file_ext = "-yp.xml";

    String basePath = System.getProperty("org.cougaar.install.path") + File.separator +
      "servicediscovery" + File.separator + "data" + File.separator + "taxonomies" + File.separator;

    if(validPath(basePath + name + file_ext)) {
      createTaxonomy(name, uuid, basePath + name + file_ext);
      return true;
    }

    logger.error("Invalid Path: " + basePath + name + file_ext);
    return false;
  }


}






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
import org.cougaar.yp.*;
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
  private LoggingService myLogger;

  private static String UDDI_USERID = "cougaar";
  private static String UDDI_PASSWORD = "cougaarPass";

  private YPProxy myYPProxy;
  private YPService myYPService;
  private AuthToken myAuthToken;

  public static String []TMODELNAMES = 
     { UDDIConstants.MILITARY_SERVICE_SCHEME,
       UDDIConstants.MILITARY_ECHELON_SCHEME,
       UDDIConstants.ORGANIZATION_TYPES,
       UDDIConstants.SOURCING_CAPABILITY_SCHEME,
       UDDIConstants.SUPPORT_COMMAND_ASSIGNMENT };

  public void initialize() {
    super.initialize();

    ServiceBroker sb = getServiceBroker();
    LoggingService myLogger = 
      (LoggingService) sb.getService(this, LoggingService.class, null);
    myYPService = 
      (YPService) sb.getService(this, YPService.class, null);
    String ypAgent = System.getProperty("org.cougaar.yp.ypAgent");
      
    myYPProxy = myYPService.getYP(ypAgent);

    initTaxonomy();

    myYPProxy  = null;
  }

  private void initTaxonomy() {
    try {
      myAuthToken = (AuthToken) myYPService.submit(myYPProxy.get_authToken(UDDI_USERID, UDDI_PASSWORD)).get();

      for (int index = 0; index < TMODELNAMES.length; index++) {
	genTaxonomy(TMODELNAMES[index]);
      }
      createBindingTModels();

      myYPService.submit(myYPProxy.discard_authToken(myAuthToken.getAuthInfoString())).get();
      
    } catch (Exception e){
      e.printStackTrace();
    }
  }

  private TModelDetail saveTModel(Vector tModels) throws UDDIException {
    TModelDetail tModelDetail = 
      (TModelDetail) myYPService.submit(myYPProxy.save_tModel(myAuthToken.getAuthInfoString(), tModels)).get();

    return tModelDetail;
  }

  private void createTaxonomy(String name, String file) throws UDDIException {
    try {
      TModel tModel = new TModel();
      tModel.setName(name);
      Vector tModels = new Vector();
      tModels.addElement(tModel);

      TModelDetail tModelDetail;
      tModelDetail = saveTModel(tModels);

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
      tModelDetail = saveTModel(tModels);
    } catch (UDDIException ue) {
      DispositionReport dr = ue.getDispositionReport();
      if (dr!=null) {
	myLogger.error("UDDIException faultCode:" + ue.getFaultCode() +
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
      TModelDetail tModelDetail = saveTModel(tModels);

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

      tModelDetail = saveTModel(tModels);
    // Handle possible errors
    } catch (UDDIException e) {
      DispositionReport dr = e.getDispositionReport();
      if (dr!=null) {
	myLogger.error("UDDIException faultCode:" + e.getFaultCode() +
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
      TModelDetail tModelDetail;
      tModelDetail = saveTModel(tModels);

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
    
      tModelDetail = saveTModel(tModels);
    // Handle possible errors
    } catch (UDDIException e) {
      DispositionReport dr = e.getDispositionReport();
      if (dr!=null) {
	myLogger.error("UDDIException faultCode:" + e.getFaultCode() +
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

  private boolean genTaxonomy(String name) throws UDDIException {
    String file_ext = "-yp.xml";

    String basePath = System.getProperty("org.cougaar.install.path") + File.separator +
      "servicediscovery" + File.separator + "data" + File.separator + "taxonomies" + File.separator;

    if(validPath(basePath + name + file_ext)) {
      createTaxonomy(name, basePath + name + file_ext);
      return true;
    }

    myLogger.error("Invalid Path: " + basePath + name + file_ext);
    return false;
  }


}






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
import org.cougaar.yp.YPService;
import org.cougaar.servicediscovery.util.UDDIConstants;

import org.uddi4j.UDDIException;
import org.uddi4j.client.UDDIProxy;

import org.uddi4j.datatype.tmodel.*;
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
  private static UDDIProxy proxy;

  private static String userid;
  private static String password;

  public void initialize() {
    super.initialize();

    ServiceBroker sb = getServiceBroker();
    YPService ypService = (YPService) sb.getService(this, YPService.class, null);

    proxy = ypService.getYP("3ID");
    test(proxy);

    proxy = null;
  }


  public static void createTaxonomy(String name, String key, String file) throws UDDIException {
    try {
      FileInputStream fis = new FileInputStream(file);
      DOMParser parser = new DOMParser();
      parser.parse(new InputSource(fis));
      TModel tModel = new TModel(parser.getDocument().getDocumentElement());

      tModel.setTModelKey(key);
      tModel.setName(name);

      // Add TModelKey to KeyedReferences
      CategoryBag categoryBag = tModel.getCategoryBag();
      for (int index = 0; index < categoryBag.size(); index++) {
	KeyedReference keyedReference = categoryBag.get(index);
	keyedReference.setTModelKey(key);
      }
      
      Vector tModels = new Vector();
      tModels.addElement(tModel);
      TModelDetail tModelDetail = 
	proxy.save_tModel(proxy.get_authToken(userid, password).getAuthInfoString(), tModels);
    } catch (UDDIException ue) {
      DispositionReport dr = ue.getDispositionReport();
      if (dr!=null) {
	System.out.println("UDDIException faultCode:" + ue.getFaultCode() +
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

  public static void createBindingTModels() {
    // Creating TModels for our binding templates
    Vector tModels = new Vector();
    Vector krList = new Vector();
    
    TModel cougaarTModel = new TModel("", "COUGAAR:Binding");
    cougaarTModel.setDefaultDescriptionString("Protocol for COUGAAR services");
    tModels.add(cougaarTModel);
    try {
      TModelDetail tModelDetail = 
	proxy.save_tModel(proxy.get_authToken(userid, password).getAuthInfoString(), tModels);

      System.out.println("published COUGAAR:Binding tmodel");

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

      System.out.println("published COUGAAR:Binding model with keyedRef");
    // Handle possible errors
    } catch (UDDIException e) {
      DispositionReport dr = e.getDispositionReport();
      if (dr!=null) {
	System.out.println("UDDIException faultCode:" + e.getFaultCode() +
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

      System.out.println("published SOAP:Binding tmodel");

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

      System.out.println("published SOAP:Binding model with keyedRef");
    // Handle possible errors
    } catch (UDDIException e) {
      DispositionReport dr = e.getDispositionReport();
      if (dr!=null) {
	System.out.println("UDDIException faultCode:" + e.getFaultCode() +
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

  private static boolean genTaxonomy(String name, String uuid) throws UDDIException {
    String file_ext = "-yp.xml";

    String basePath = System.getProperty("org.cougaar.install.path") + File.separator +
      "servicediscovery" + File.separator + "data" + File.separator + "taxonomies" + File.separator;

    if(validPath(basePath + name + file_ext)) {
      createTaxonomy(name, uuid, basePath + name + file_ext);
      return true;
    }

    System.out.println("ERROR: Invalid Path: " + basePath + name + file_ext);
    return false;
  }


  /**
   * Publishes taxonomies.  Properties used by this program can be set in pubtax.bat
   * or COUGAAR_INSTALL_PATH\servicediscovery\data\common\waspUtil.props prior to
   * each run.  The taxonomies to be published are taken as command line arguments;
   * all taxonomies can be published by using the argument "--all".
   */
  public static void main(String args[]) {

    String ALL = "--all";
    String MILITARY_SERVICE = "--militaryservice";
    String MILITARY_ECHELON = "--militaryechelon";
    String SUPPORTED_COMMAND = "--supportedcommand";
    String ORGANIZATION_TYPE = "--orgtype";
    String SOURCING_CAPABILITY = "--sourcingcapability";

    System.out.println("Publishing taxonomy...");

    userid = "cougaar";
    password = "cougaarPass";

    //Taxonomies to be published are specified in the batch script that launches this program.
    //See pubtax.bat for details.

    if (args.length == 0) {
      System.err.println("Usage: PublishTaxonomy [" + ALL + ", " + MILITARY_SERVICE + ", " + MILITARY_ECHELON +
                         ", " + SUPPORTED_COMMAND + ", " +
                         ORGANIZATION_TYPE + ", " + SOURCING_CAPABILITY + "]");
    } else {
      try {
        boolean allFlag = false;
        for (int i = 0; i < args.length; i++) {
          if (args[i].equalsIgnoreCase(ALL)) {
            System.out.println("Publishing all Taxonomies");
            allFlag = true;

            try {
              genTaxonomy(UDDIConstants.MILITARY_SERVICE_SCHEME, UDDIConstants.MILITARY_SERVICE_SCHEME_UUID);
              genTaxonomy(UDDIConstants.MILITARY_ECHELON_SCHEME, UDDIConstants.MILITARY_ECHELON_SCHEME_UUID);
              genTaxonomy(UDDIConstants.ORGANIZATION_TYPES, UDDIConstants.ORGANIZATION_TYPES_UUID);
              genTaxonomy(UDDIConstants.SOURCING_CAPABILITY_SCHEME, UDDIConstants.SOURCING_CAPABILITY_SCHEME_UUID);
              genTaxonomy(UDDIConstants.SUPPORT_COMMAND_ASSIGNMENT, UDDIConstants.SUPPORT_COMMAND_ASSIGNMENTI_UUID);

            } catch (UDDIException e) {
              e.printStackTrace();  //To change body of catch statement use Options | File Templates.
            }
          } else if (args[i].equalsIgnoreCase(MILITARY_SERVICE) && !allFlag) {
            System.out.println("Publishing: " + UDDIConstants.MILITARY_SERVICE_SCHEME);
            genTaxonomy(UDDIConstants.MILITARY_SERVICE_SCHEME, UDDIConstants.MILITARY_SERVICE_SCHEME_UUID);

          } else if (args[i].equalsIgnoreCase(MILITARY_ECHELON) && !allFlag) {
            System.out.println("Publishing: " + UDDIConstants.MILITARY_ECHELON_SCHEME);
            genTaxonomy(UDDIConstants.MILITARY_ECHELON_SCHEME, UDDIConstants.MILITARY_ECHELON_SCHEME_UUID);

          } else if (args[i].equalsIgnoreCase(SUPPORTED_COMMAND) && !allFlag) {
            System.out.println("Publishing: " + UDDIConstants.SUPPORT_COMMAND_ASSIGNMENT);
            genTaxonomy(UDDIConstants.SUPPORT_COMMAND_ASSIGNMENT, UDDIConstants.SUPPORT_COMMAND_ASSIGNMENTI_UUID);

          } else if (args[i].equalsIgnoreCase(ORGANIZATION_TYPE) && !allFlag) {
            System.out.println("Publishing: " + UDDIConstants.ORGANIZATION_TYPES);
            genTaxonomy(UDDIConstants.ORGANIZATION_TYPES, UDDIConstants.ORGANIZATION_TYPES_UUID);

          } else if (args[i].equalsIgnoreCase(SOURCING_CAPABILITY) && !allFlag) {
            System.out.println("Publishing: " + UDDIConstants.SOURCING_CAPABILITY_SCHEME);
            genTaxonomy(UDDIConstants.SOURCING_CAPABILITY_SCHEME, UDDIConstants.SOURCING_CAPABILITY_SCHEME_UUID);

          } else {
            System.out.println("Unknown value: " + args[i]);
            System.err.println("Usage: PublishTaxonomy [" + ALL + ", " + MILITARY_SERVICE + ", " + MILITARY_ECHELON +
                               ", " + SUPPORTED_COMMAND + ", " +
                               ORGANIZATION_TYPE + ", " + SOURCING_CAPABILITY + "]");
          }
        }
      } catch (UDDIException e) {
        System.err.println(e.getMessage());
      }
    }
  }

  public static void test(UDDIProxy proxyArg) {
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
}






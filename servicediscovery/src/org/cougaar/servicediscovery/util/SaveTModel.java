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

/**
 * Created by IntelliJ IDEA.
 * User: lgoldsto
 * Date: Dec 12, 2002
 * Time: 9:30:05 AM
 * This class publishes two tModels used for service bindings.
 */
package org.cougaar.servicediscovery.util;

import org.uddi4j.client.UDDIProxy;
import org.uddi4j.response.AuthToken;
import org.uddi4j.response.TModelDetail;
import org.uddi4j.response.DispositionReport;
import org.uddi4j.util.KeyedReference;
import org.uddi4j.util.CategoryBag;
import org.uddi4j.datatype.tmodel.TModel;
import org.uddi4j.UDDIException;

import java.util.Properties;
import java.util.Vector;

public class SaveTModel {

  Properties config = null;
  private String queryURL;
  private String publishURL;
  private String userid;
  private String password;
  private String hostname;
  private String serverPort;

  public static void main (String args[]) {
    SaveTModel app = new SaveTModel();
    System.out.println("\n*********** Creating TModel ***********");
    try{
      app.run();
    }catch(Exception e){
      System.err.println(e.getMessage());
    }
    System.exit(0);
  }

  public void run() throws Exception{
    /*
    Properties defaults = new Properties();
    defaults.setProperty("org.cougaar.servicediscovery.registry.admin.username", "admin");
    defaults.setProperty("org.cougaar.servicediscovery.registry.admin.password", "changeit");
    defaults.setProperty("org.cougaar.servicediscovery.registry.publishURL", "http://localhost:8080/uddi/publishing");
    defaults.setProperty("org.cougaar.servicediscovery.registry.queryURL", "http://localhost:8080/uddi/inquiry");

    Properties props = new Properties(defaults);
    queryURL = props.getProperty("org.cougaar.servicediscovery.registry.queryURL");
    publishURL = props.getProperty("org.cougaar.servicediscovery.registry.publishURL");
    userid =props.getProperty("org.cougaar.servicediscovery.registry.admin.username");
    password = props.getProperty("org.cougaar.servicediscovery.registry.admin.password");
    */
    hostname = System.getProperty("org.cougaar.servicediscovery.registry.hostname");
    if(hostname == null || hostname.length() == 0){
      throw new Exception("Missing or invalid host name for UDDI Server");
    }
    serverPort = System.getProperty("org.cougaar.servicediscovery.registry.server.port");
    if(serverPort == null || serverPort.length() == 0){
      throw new Exception("Missing or invalid port for UDDI Server");
    }
    queryURL = "http://" + hostname + ":" + serverPort + "/uddi/inquiry";
    publishURL = "http://" + hostname + ":" + serverPort + "/uddi/publishing";

    userid = System.getProperty("org.cougaar.servicediscovery.registry.admin.username");
    if(userid == null || userid.length() == 0){
      userid = "admin";
    }
    password = System.getProperty("org.cougaar.servicediscovery.registry.admin.password");
    if(password == null || password.length() == 0){
      password = "changeit";
    }

    // Construct a UDDIProxy object
    UDDIProxy proxy = new UDDIProxy();

    try {
      // Select the desired UDDI server node
      proxy.setInquiryURL(queryURL);
      proxy.setPublishURL(publishURL);

      // Get an authorization token
      System.out.println("\nGet authtoken");

      // Pass in userid and password registered at the UDDI site
      AuthToken token = proxy.get_authToken(userid, password);

      System.out.println("Returned authToken:" + token.getAuthInfoString());

      // Creating TModels for our binding templates
      Vector tModels = new Vector();
      Vector krList = new Vector();
      KeyedReference wsdlKr;

      TModel cougaarTModel = new TModel("", "COUGAAR:Binding");
      cougaarTModel.setDefaultDescriptionString("Protocol for COUGAAR services");
      CategoryBag categoryBag = new CategoryBag();
      wsdlKr = new KeyedReference("uddi-org:types", "wsdlSpec");
      wsdlKr.setTModelKey("UUID:C1ACF26D-9672-4404-9D70-39B756E62AB4");
      krList.add(wsdlKr);
      categoryBag.setKeyedReferenceVector(krList);
      cougaarTModel.setCategoryBag(categoryBag);
      tModels.add(cougaarTModel);

      TModel soapTModel = new TModel("", "SOAP:Binding");
      soapTModel.setDefaultDescriptionString("SOAP binding for non-COUGAAR services");
      categoryBag = new CategoryBag();
      KeyedReference soapKr = new KeyedReference("uddi-org:types", "soapSpec");
      soapKr.setTModelKey("UUID:C1ACF26D-9672-4404-9D70-39B756E62AB4");
      // described by WSDL
      krList = new Vector();
      krList.add(soapKr);
      wsdlKr = new KeyedReference("uddi-org:types", "wsdlSpec");
      wsdlKr.setTModelKey("UUID:C1ACF26D-9672-4404-9D70-39B756E62AB4");
      krList.add(wsdlKr);
      categoryBag.setKeyedReferenceVector(krList);
      soapTModel.setCategoryBag(categoryBag);
      tModels.add(soapTModel);

      System.out.println("\nSaving a TModel");
      TModelDetail tModelDetail = proxy.save_tModel(token.getAuthInfoString(), tModels);
      System.out.println("\nTModels saved ");
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
}

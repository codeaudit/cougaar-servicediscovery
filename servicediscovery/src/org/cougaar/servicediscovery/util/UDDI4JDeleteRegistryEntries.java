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

package org.cougaar.servicediscovery.util;

import org.uddi4j.UDDIException;
import org.uddi4j.client.UDDIProxy;
import org.uddi4j.response.AuthToken;
import org.uddi4j.response.DispositionReport;
import org.uddi4j.response.BusinessInfos;
import org.uddi4j.response.BusinessInfo;
import org.uddi4j.response.RegisteredInfo;
import org.uddi4j.transport.TransportException;

import java.util.Enumeration;
import java.util.Vector;


public class UDDI4JDeleteRegistryEntries {

    private UDDIProxy proxy;
    private AuthToken authorization;

    public static void main(String[] args) {

      System.out.println("Deleting registry entries...");

      //Get properties
      String queryURL = System.getProperty("org.cougaar.servicediscovery.registry.queryURL",
                                             "http://localhost:8080/uddi/inquiry");
      String publishURL = System.getProperty("org.cougaar.servicediscovery.registry.publishURL",
                                             "http://localhost:8080/uddi/publishing");
      String username = System.getProperty("org.cougaar.servicediscovery.registry.user.username", "ulservices");
      String password = System.getProperty("org.cougaar.servicediscovery.registry.user.password", "ulservices");

      //If no username/password was supplied as a property, check if it was supplied
      //as a command line arg.  If neither, terminate the program.
      if(username.length() == 0 || password.length() == 0){
        if(args.length == 2){
          username = args[0];
          password = args[1];
        }else{
          System.err.println("ERROR: No username/password was supplied.");
          System.exit(1);
        }
      }

      //Establish defaults
      if(queryURL.length() == 0){
        queryURL = "http://localhost:8080/uddi/inquiry";
      }
      if(publishURL.length() == 0){
        publishURL = "http://localhost:8080/uddi/publishing";
      }
      UDDI4JDeleteRegistryEntries dre = new UDDI4JDeleteRegistryEntries();
      dre.deleteOrganizations(username, password, queryURL, publishURL);
    }

    public UDDI4JDeleteRegistryEntries() {
    }

    private void deleteOrganizations(String username, String password, String queryURL, String publishURL) {

      long start = System.currentTimeMillis();

      makeConnection(queryURL, publishURL, username, password);
      try {
        RegisteredInfo ri = proxy.get_registeredInfo(authorization.getAuthInfoString());
        BusinessInfos infos = ri.getBusinessInfos();
        if (ri.getBusinessInfos().size() < 1) {
          System.out.println("No business entries for user --> " + username);
          proxy.discard_authToken(authorization.getAuthInfoString());
          return;
        }
        Enumeration enum = infos.getBusinessInfoVector().elements();
        Vector myKeys = new Vector();
        while(enum.hasMoreElements()) {
          BusinessInfo bi = (BusinessInfo)enum.nextElement();
          myKeys.add(bi.getBusinessKey());
        }
        System.out.println("Attempting to delete " + myKeys.size() + " entries.");
        DispositionReport dr =  proxy.delete_business(authorization.getAuthInfoString(), myKeys);
        System.out.println("Delete Business Results:  " + dr.success() +
                  "\n operator: " + dr.getOperator() +
                  "\n generic: "  + dr.getGeneric() +
                  "\n errno: "    + dr.getErrno() +
                  "\n errCode: "  + dr.getErrCode() +
                  "\n errInfoText: " + dr.getErrInfoText() +
                  "\n numResults: " + dr.getNumResults());
        proxy.discard_authToken(authorization.getAuthInfoString());
      } catch (UDDIException e) {
        DispositionReport dr = e.getDispositionReport();
        System.out.println("Error Deleting Businesses" +
                  "\n operator: " + dr.getOperator() +
                  "\n generic: "  + dr.getGeneric() +
                  "\n errno: "    + dr.getErrno() +
                  "\n errCode: "  + dr.getErrCode() +
                  "\n errInfoText: " + dr.getErrInfoText() +
                  "\n numResults: " + dr.getNumResults());
      } catch (TransportException e) {
        e.printStackTrace();  //To change body of catch statement use Options | File Templates.
      }

      long stop = System.currentTimeMillis();
      System.out.println("Total time for delete: " + (stop - start) / 1000 + " seconds.");
    }

  private void makeConnection(String queryURL, String publishURL, String username, String password) {
      // Define connection configuration properties.
      proxy = new UDDIProxy();

      try {
        proxy.setInquiryURL(queryURL);
        proxy.setPublishURL(publishURL);

        authorization = proxy.get_authToken(username, password);

      } catch (java.net.MalformedURLException e) {
          System.err.println("query or publish URL is wrong + e");
      } catch (UDDIException e) {
        DispositionReport dr = e.getDispositionReport();
        System.err.println("UDDIException faultCode:" + e.getFaultCode() +
                  "\n operator: " + dr.getOperator() +
                  "\n generic: "  + dr.getGeneric() +
                  "\n errno: "    + dr.getErrno() +
                  "\n errCode: "  + dr.getErrCode() +
                  "\n errInfoText: " + dr.getErrInfoText());
      } catch (TransportException e) {
          System.err.println("Caught an Exception getting authorization" + e);
      }
  }
}

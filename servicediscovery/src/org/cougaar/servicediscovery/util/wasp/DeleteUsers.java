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

package org.cougaar.servicediscovery.util.wasp;

import org.idoox.uddi.UDDIException;
import org.idoox.uddi.account.AccountApi;
import org.idoox.uddi.client.structure.v1.base.AuthInfo;
import org.idoox.wasp.Context;
import org.idoox.webservice.client.WebServiceLookup;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Deletes user accounts and their related entities from the
 * registry.  Admin authorization is required.
 */
public class DeleteUsers {

  /**
   * Delete users.
   * @param delete_users  a list of usernames for deletion
   * @throws Exception
   */
  public static void deleteUsers(ArrayList delete_users)
    throws Exception {

    System.out.println("Deleting users ...");

    String portId = System.getProperty("org.cougaar.servicediscovery.registry.server.port", "8080");
    String userid = System.getProperty("org.cougaar.servicediscovery.registry.admin.username", "admin");
    String password = System.getProperty("org.cougaar.servicediscovery.registry.admin.password", "changeit");
    if(portId.length() == 0) portId = "8080";
    if(userid.length() == 0) userid = "admin";
    if(password.length() == 0) password = "changeit";


    WebServiceLookup lookup = (WebServiceLookup) Context.getInstance(Context.WEBSERVICE_LOOKUP);

    AccountApi accountApi =
      (AccountApi) lookup.lookup("http://localhost:" + portId + "/uddi/account", AccountApi.class);

    String delme = null;
    AuthInfo info = accountApi.getAuthInfo(userid, password);
    for (Iterator i = delete_users.iterator(); i.hasNext();) {
      delme = (String) i.next();
      System.out.println("Deleting user: " + delme);
      try{
        accountApi.deleteUserAccount(delme, info);
      }catch(UDDIException udex){
        if(udex.getErrno() == 10150){ //the error code for UDDIErrorCodes.E_UNKNOWN_USER
          System.err.println("User " + delme + " not found.");
        }else{
          udex.printStackTrace();
        }
      }
    }
    accountApi.discardAuthInfo(info);
    System.out.println("Users deleted.");
  }

  /**
   * Main.
   */
  public static void main(String args[])
    throws Exception {
    ArrayList users = new ArrayList();
    // Delete the pre-installed demo user accounts
    users.add("john_demo");
    users.add("demo_user");
    users.add("demo_user1");
    users.add("demo_user2");

    deleteUsers(users);

    System.out.println(".....finished.");
  }
}

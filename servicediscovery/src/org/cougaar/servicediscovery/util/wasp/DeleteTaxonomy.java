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

import org.cougaar.servicediscovery.util.UDDIConstants;
import org.idoox.uddi.UDDIException;
import org.idoox.uddi.client.api.v2.UDDIApiInquiry;
import org.idoox.uddi.client.api.v2.UDDIApiPublishing;
import org.idoox.uddi.client.api.v2.UDDILookup;
import org.idoox.uddi.client.api.v2.request.inquiry.FindTModel;
import org.idoox.uddi.client.api.v2.request.inquiry.MaxRows;
import org.idoox.uddi.client.api.v2.request.publishing.Cred;
import org.idoox.uddi.client.api.v2.request.publishing.DeleteTModel;
import org.idoox.uddi.client.api.v2.request.publishing.GetAuthToken;
import org.idoox.uddi.client.api.v2.request.publishing.UserID;
import org.idoox.uddi.client.api.v2.response.AuthToken;
import org.idoox.uddi.client.api.v2.response.TModelInfo;
import org.idoox.uddi.client.api.v2.response.TModelInfos;
import org.idoox.uddi.client.api.v2.response.TModelList;
import org.idoox.uddi.client.structure.v2.base.KeyedReference;
import org.idoox.uddi.client.structure.v2.base.Name;
import org.idoox.uddi.client.structure.v2.tmodel.TModelKey;
import org.idoox.uddi.taxonomy.TaxonomyApi;
import org.idoox.uddi.tools.AdminToolApi;
import org.idoox.wasp.Context;
import org.idoox.webservice.client.WebServiceLookup;

/**
 * This is a simple standalone example which shows how to delete a taxonomy
 * from the taxonomy service.
 */
public class DeleteTaxonomy {

  private static String adminName; //admin userid
  private static String password; //admin password
  private static String  taxonomyUrl = null;
  private static String inquiryUrl = null;
  private static UDDIApiPublishing publishing = null;
  private static TaxonomyApi taxonomyApi = null;
  private static AdminToolApi adminTool = null;

  /**
   * Delete a taxonomy from the taxonomy service.
   */

  public static TModelKey findTModelByName(String name) throws Exception {
    System.out.println("NAME = " + name);


    FindTModel findTModel = new FindTModel();
    // set expected name of the tModel for which you will search
    findTModel.setName(new Name(name));

    // you can specify maximum number of results
    findTModel.setMaxRows(new MaxRows("10"));

    TModelList tModelList = findTModel(findTModel);
    //show results
    return showTModelList(tModelList);

  }

  public static TModelList findTModel(FindTModel findTModel) throws Exception {
    System.out.println(" ");
    System.out.println("Searching for tModels in progress ...");

    UDDIApiInquiry inquiry = UDDILookup.getInquiry(inquiryUrl);

    return inquiry.find_tModel(findTModel);
  }


  private static TModelKey showTModelList(TModelList tModelList) throws Exception {
    if (tModelList == null) {
      System.err.println("ERROR: TModel list is null!");
      return null;
    } else {
      // Parsing TModelList
      TModelInfos tModelInfos = tModelList.getTModelInfos();
      int size = tModelInfos.size();
      System.out.println(" ");
      System.out.println("TModels count: " + size);
      System.out.println(" ");

      TModelInfo tModelInfo = tModelInfos.getFirst();

      TModelKey result;
      if (tModelInfo == null)
        return null;
      else
        result = tModelInfo.getTModelKey();
      int num = 0;

      while (tModelInfo != null) {
        System.out.println("*******************************************************************************");
        System.out.println("tModel " + ++num);
        TModelKey key = tModelInfo.getTModelKey();
        System.out.println("tModel ID=" + tModelInfo.getTModelKey());
        System.out.println("*******************************************************************************");
        System.out.println("Deleting  ");



        String authToken = taxonomyApi.getAdminAuthToken(adminName, password);
        System.out.println(" Deleted taxonomy ");
        try {
          taxonomyApi.deleteTaxonomy(key, authToken);
        } catch(Exception e) {
          System.out.println("I ignored an exception!");
        }

        // new delete tModel structure
        DeleteTModel deleteTModel = new DeleteTModel(); //copy start here /prg
        // setting login information as AuthInfo structure
        AuthToken token = publishing.get_authToken(new GetAuthToken(new UserID(adminName), new Cred(password)));
        deleteTModel.setAuthInfo(token.getAuthInfo());
        // adding tModel which will be deleted
        deleteTModel.addTModelKey(key);

        try{
          publishing.delete_tModel(deleteTModel);
          System.out.println(" Deleted TModel ");
        }catch(UDDIException udex){
          System.err.println("Could not delete tModel with key " + key.getValue());
          System.err.println(udex.getMessage());
        }
        TModelKey [] tModelKeys = {key};

        try{
          adminTool.deleteTModel(tModelKeys, authToken);
          System.out.println(" Deleted deprecated tModel key "); //copy end here /prg
        }catch(UDDIException udex){
          System.err.println("Could not delete deprecated tModel with key " + key.getValue());
          System.err.println(udex.getMessage());
        }
        tModelInfo = tModelInfos.getNext();
      }
      return result;
    }
  }

  /**
   * Returns a string representation of a KeyedReference which can be printed to the screen
   *
   * @param x         a KeyedReference
   * @return          string representation of a KeyedReference which can be printed to the screen
   */
  public static String show(KeyedReference x) {
    return x.getKeyName() + ":" + x.getKeyValue().getValue() + ":" + x.getTModelKey();
  }

  /**
   * Deletes taxonomies.  Input values for this program, including properties
   * and input arguments, can be set in deletetax.bat prior to each run.  If deletetax.bat
   * is not used to launch this, then the following default properties are assumed:
   * <ul>
   *   <li>UDDI server port: 8080</li>
   *   <li>Username: admin</li>
   *   <li>Password: changeit</li>
   * </ul>
   * The tModel keys of the taxonomies to be deleted are also best set in deletetax.bat,
   * but can be set from the command line by including each, separated by a space,
   * after the program name.  For example:
   * <p>
   * DeleteTaxonomy uuid:f0b01564-b8f0-b015-dad5-b49598339719
   * <p>
   * Multiple taxonomies can be specified in this manner, although the command
   * will become confusingly long if too many taxonomies are specified.  To keep the
   * command manageable, use deletetax.bat.
   */
  public static void main (String args[]) {

    String ALL = "--all";
    String MILITARY_SERVICE = "--militaryservice";
    String MILITARY_ECHELON = "--militaryechelon";
    String PLATFORMS_SUPPORTED = "--platformssupported";
    String SUPPORTED_COMMAND = "--supportedcommand";
    String ORGANIZATION_TYPE = "--orgtype";
    String SOURCING_CAPABILITY = "--sourcingcapability";

    adminName = System.getProperty("org.cougaar.servicediscovery.registry.admin.username", "admin");
    if(adminName.equals("")){
      adminName = "admin";
    }
    password = System.getProperty("org.cougaar.servicediscovery.registry.admin.password", "changeit");
    if(password.equals("")){
      password = "changeit";
    }
    String publishUrl =  "http://" + System.getProperty("org.cougaar.servicediscovery.registry.hostname")+ ":" +
            System.getProperty("org.cougaar.servicediscovery.registry.server.port") + "/uddi/publishing";
    String adminUrl =  "http://" + System.getProperty("org.cougaar.servicediscovery.registry.hostname")+ ":" +
            System.getProperty("org.cougaar.servicediscovery.registry.server.port") + "/uddi/admintool";
    inquiryUrl =  "http://" + System.getProperty("org.cougaar.servicediscovery.registry.hostname")+ ":" +
        System.getProperty("org.cougaar.servicediscovery.registry.server.port") + "/uddi/inquiry";
    taxonomyUrl =  "http://" + System.getProperty("org.cougaar.servicediscovery.registry.hostname")+ ":" +
        System.getProperty("org.cougaar.servicediscovery.registry.server.port") + "/uddi/taxonomy";

    if (args.length == 0) {
      System.err.println("Usage: DeleteTaxonomy [" + ALL + ", " + MILITARY_SERVICE + ", " + MILITARY_ECHELON +
                         ", " + PLATFORMS_SUPPORTED + ", " + SUPPORTED_COMMAND + ", " +
                         ORGANIZATION_TYPE + ", " + SOURCING_CAPABILITY + "]");
    } else {

      try {
        // Get the WASP lookup.
        WebServiceLookup lookup = (WebServiceLookup)Context.getInstance(Context.WEBSERVICE_LOOKUP);
        // Get the taxonomy service stub for particular URL.
        taxonomyApi = (TaxonomyApi)lookup.lookup(taxonomyUrl, TaxonomyApi.class);
        publishing = UDDILookup.getPublishing(publishUrl);
        adminTool = (AdminToolApi) lookup.lookup(adminUrl, AdminToolApi.class);
      } catch (Exception e) {
        System.err.println("Can't connect taxonomy sevice " + taxonomyUrl + ". " + e.getMessage());
        System.exit(-1);
      }

      boolean allFlag = false;
      for (int i = 0; i < args.length; i++) {
        try {
          if (args[i].equalsIgnoreCase(ALL)) {
            System.out.println("Deleting all Taxonomies");
            allFlag = true;
            try {
              findTModelByName(UDDIConstants.MILITARY_SERVICE_SCHEME);
              findTModelByName(UDDIConstants.MILITARY_ECHELON_SCHEME);
              findTModelByName(UDDIConstants.ORGANIZATION_TYPES);
              findTModelByName(UDDIConstants.SOURCING_CAPABILITY_SCHEME);
              findTModelByName(UDDIConstants.PLATFORMS_SUPPORTED);
              findTModelByName(UDDIConstants.SUPPORT_COMMAND_ASSIGNMENT);

            } catch (Exception e) {
              e.printStackTrace();  //To change body of catch statement use Options | File Templates.
            }
          } else if (args[i].equalsIgnoreCase(MILITARY_SERVICE) && !allFlag) {
            System.out.println("Deleting: " + UDDIConstants.MILITARY_SERVICE_SCHEME);
            findTModelByName(UDDIConstants.MILITARY_SERVICE_SCHEME);

          } else if (args[i].equalsIgnoreCase(MILITARY_ECHELON) && !allFlag) {
            System.out.println("Deleting: " + UDDIConstants.MILITARY_ECHELON_SCHEME);
            findTModelByName(UDDIConstants.MILITARY_ECHELON_SCHEME);

          } else if (args[i].equalsIgnoreCase(PLATFORMS_SUPPORTED) && !allFlag) {
            System.out.println("Deleting: " + UDDIConstants.PLATFORMS_SUPPORTED);
            findTModelByName(UDDIConstants.PLATFORMS_SUPPORTED);

          } else if (args[i].equalsIgnoreCase(SUPPORTED_COMMAND) && !allFlag) {
            System.out.println("Deleting: " + UDDIConstants.SUPPORT_COMMAND_ASSIGNMENT);
            findTModelByName(UDDIConstants.SUPPORT_COMMAND_ASSIGNMENT);

          } else if (args[i].equalsIgnoreCase(ORGANIZATION_TYPE) && !allFlag) {
            System.out.println("Deleting: " + UDDIConstants.ORGANIZATION_TYPES);
            findTModelByName(UDDIConstants.ORGANIZATION_TYPES);

          } else if (args[i].equalsIgnoreCase(SOURCING_CAPABILITY) && !allFlag) {
            System.out.println("Deleting: " + UDDIConstants.SOURCING_CAPABILITY_SCHEME);
            findTModelByName(UDDIConstants.SOURCING_CAPABILITY_SCHEME);

          } else {
            System.out.println("Unknown value: " + args[i]);
            System.err.println("Usage: DeleteTaxonomy [" + ALL + ", " + MILITARY_SERVICE + ", " + MILITARY_ECHELON +
                               ", " + PLATFORMS_SUPPORTED + ", " + SUPPORTED_COMMAND + ", " +
                               ORGANIZATION_TYPE + ", " + SOURCING_CAPABILITY + "]");
          }
        } catch(Exception e) {}
      }
    }
  }
}

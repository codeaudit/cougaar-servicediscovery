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
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.servicediscovery.util.wasp;

import org.cougaar.servicediscovery.util.UDDIConstants;
import org.idoox.myuddi.client.api.v2.UDDIApiPublishingAcl;
import org.idoox.myuddi.client.api.v2.UDDILookup;
import org.idoox.myuddi.client.api.v2.request.publishing.GetAuthToken;
import org.idoox.myuddi.client.api.v2.request.publishing.SaveTModel;
import org.idoox.myuddi.client.api.v2.response.AclTModelDetail;
import org.idoox.uddi.UDDIException;
import org.idoox.uddi.client.api.v2.request.publishing.Cred;
import org.idoox.uddi.client.api.v2.request.publishing.UserID;
import org.idoox.uddi.client.api.v2.response.AuthToken;
import org.idoox.uddi.client.structure.v2.base.CategoryBag;
import org.idoox.uddi.client.structure.v2.base.Description;
import org.idoox.uddi.client.structure.v2.base.KeyName;
import org.idoox.uddi.client.structure.v2.base.KeyValue;
import org.idoox.uddi.client.structure.v2.base.KeyedReference;
import org.idoox.uddi.client.structure.v2.base.KeyedReferences;
import org.idoox.uddi.client.structure.v2.base.Name;
import org.idoox.uddi.client.structure.v2.tmodel.TModel;
import org.idoox.uddi.client.structure.v2.tmodel.TModelKey;
import org.idoox.uddi.configurator.ConfigurationConfig;
import org.idoox.uddi.configurator.ConfiguratorApi;
import org.idoox.uddi.taxonomy.TaxonomyApi;
import org.idoox.uddi.tools.AdminToolApi;
import org.idoox.wasp.Context;
import org.idoox.wasp.MessageAttachment;
import org.idoox.webservice.client.WebServiceLookup;
import org.idoox.webservice.client.WebServiceLookupException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 *
 */
public class PublishTaxonomy {

  public static ConfiguratorApi configurator = null;
  public static TaxonomyApi taxonomy = null;
  public static UDDIApiPublishingAcl publishing = null;
  public static AdminToolApi adminTool = null;
  public static String adminAuthToken;
  private static String userid;
  private static String password;
  private static final String CHECKED = "Checked";
  private static final String UNCHECKED = "Unchecked";

  public static void lookup(String host) throws UDDIException {
    try {
      WebServiceLookup lookup =
        (WebServiceLookup) Context.getInstance(
          Context.WEBSERVICE_LOOKUP);
      configurator = (ConfiguratorApi) lookup.lookup(host + "/uddi/configurator",
                                                     ConfiguratorApi.class);
      taxonomy = (TaxonomyApi) lookup.lookup(host + "/uddi/taxonomy",TaxonomyApi.class);
      adminTool = (AdminToolApi) lookup.lookup(host + "/uddi/admintool",
                                               AdminToolApi.class);
      publishing = UDDILookup.getPublishing(host + "/uddi/publishing");
      adminAuthToken = configurator.getAdminAuthToken(userid, password);
    } catch (WebServiceLookupException e) {
      throw new UDDIException(e.getMessage());
    }
  }


//  public static String createTModel(String name, String key, String user,
//                                    String password) throws UDDIException {
  public static String createTModel(String name, String key, String checked) throws UDDIException {
      AuthToken authToken = publishing.get_authToken(
      new GetAuthToken(new UserID(userid), new Cred(password)));
    KeyedReferences krs = new KeyedReferences();
    krs.add(new KeyedReference(
      new TModelKey("uuid:c1acf26d-9672-4404-9d70-39b756e62ab4"),
      new KeyName("Categorization (taxonomy)"),
      new KeyValue("categorization")));
    krs.add(new KeyedReference(
      new TModelKey("uuid:c1acf26d-9672-4404-9d70-39b756e62ab4"),
      new KeyName(checked),
      new KeyValue(checked.toLowerCase())));

    CategoryBag categoryBag = new CategoryBag();

    categoryBag.setKeyedReferences(krs);

    SaveTModel st =	new SaveTModel();

    TModel tm = new TModel();
    tm.setName(new Name(name));
    tm.setCategoryBag(categoryBag);

    st.addTModel(tm);
    st.setAuthInfo(authToken.getAuthInfo());
    AclTModelDetail tModelDetail = publishing.save_tModel(st);
    TModelKey tModelKey = tModelDetail.getAclTModels().getFirst().getTModelKey();
    if (key == null) {
      return tModelKey.getValue();
    }

    System.out.println("Temp key: " + tModelKey.getValue());
    adminTool.replaceTModelKey(tModelKey, new TModelKey(key), adminAuthToken);
    System.out.println("\nCreated " + name + "  tModel with key " + key);
    return key;
  }

//  public static void createUncheckedTModel(String name,  String desc, String user,
//                                           String password) throws UDDIException {
  public static void createUncheckedTModel(String name, String desc) throws UDDIException {
    AuthToken authToken = publishing.get_authToken(
      //new GetAuthToken(new UserID(user), new Cred(password)));
      new GetAuthToken(new UserID(userid), new Cred(password)));
    KeyedReferences krs = new KeyedReferences();
    krs.add(new KeyedReference(
      new TModelKey("uuid:c1acf26d-9672-4404-9d70-39b756e62ab4"),
      new KeyName("Categorization (taxonomy)"),
      new KeyValue("categorization")));
    krs.add(new KeyedReference(
      new TModelKey("uuid:c1acf26d-9672-4404-9d70-39b756e62ab4"),
      new KeyName("Unchecked"),
      new KeyValue("unchecked")));

    CategoryBag categoryBag = new CategoryBag();

    categoryBag.setKeyedReferences(krs);


    SaveTModel st =	new SaveTModel();

    TModel tm = new TModel();
    tm.setName(new Name(name));
    tm.setCategoryBag(categoryBag);
    Description d = new Description(desc);
    tm.addDescription(d);

    st.addTModel(tm);
    st.setAuthInfo(authToken.getAuthInfo());
    AclTModelDetail tModelDetail = publishing.save_tModel(st);
    TModelKey tModelKey = tModelDetail.getAclTModels().getFirst().getTModelKey();
    System.out.println("Created Unchecked tModel with key: " + tModelKey.getValue());
  }

  public static void createTaxonomy(String tModelKey, String file)
    throws UDDIException {
    // save taxonomy
    try {
      FileInputStream fis = new FileInputStream(file);
      MessageAttachment att = new MessageAttachment();
      att.setData(fis);
      taxonomy.saveTaxonomy(adminAuthToken, att);
      System.out.println("Created taxonomy with tModel key " + tModelKey);
    } catch (FileNotFoundException e) {
      throw new UDDIException(e.getMessage());
    } catch (IOException e) {
      throw new UDDIException(e.getMessage());
    }

    // update configuration
    ConfigurationConfig cc = configurator.getConfiguration(adminAuthToken);
    cc.getOperator().getInsideTaxonomy().addTmodelKey(tModelKey);
    configurator.setConfiguration(cc, adminAuthToken);
    configurator.reload(adminAuthToken);
  }

  private static boolean validPath(String path) {
    return (new File(path)).exists();
  }

  private static boolean genTaxonomy(String name, String uuid, String checked) throws UDDIException {
    String file_ext = "-wasp.xml";

    String basePath = System.getProperty("org.cougaar.install.path") + File.separator +
        "servicediscovery" + File.separator + "data" + File.separator + "taxonomies" + File.separator;

    if(validPath(basePath + name + file_ext)) {
      createTaxonomy(createTModel(name, uuid, checked), basePath + name + file_ext);
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
    String PLATFORMS_SUPPORTED = "--platformssupported";
    String SUPPORTED_COMMAND = "--supportedcommand";
    String ORGANIZATION_TYPE = "--orgtype";
    String SOURCING_CAPABILITY = "--sourcingcapability";

    System.out.println("Publishing taxonomy...");
    // Define defaults for the admin username and password.  This prevents them from being stored in cvs.
    // Of course it doesn't prevent someone from looking at the source code for the password!
    Properties defaults = new Properties();
    defaults.setProperty("org.cougaar.servicediscovery.registry.admin.username", "admin");
    defaults.setProperty("org.cougaar.servicediscovery.registry.admin.password", "changeit");

    Properties props = new Properties(defaults);

    try {
      String fullpath = System.getProperty("org.cougaar.install.path") + File.separator +
          "servicediscovery" + File.separator + "data" + File.separator + "common" + File.separator;
      props.load(new FileInputStream(new File(fullpath + "waspUtil.props")));
    } catch (Exception e) {
      System.err.println("PublishTaxonomy: java could not read properties file, relying on -D args.");
    }

    userid =props.getProperty("org.cougaar.servicediscovery.registry.admin.username");
    password = props.getProperty("org.cougaar.servicediscovery.registry.admin.password");

    //Taxonomies to be published are specified in the batch script that launches this program.
    //See pubtax.bat for details.

      if (args.length == 0) {
        System.err.println("Usage: PublishTaxonomy [" + ALL + ", " + MILITARY_SERVICE + ", " + MILITARY_ECHELON +
                           ", " + PLATFORMS_SUPPORTED + ", " + SUPPORTED_COMMAND + ", " +
                           ORGANIZATION_TYPE + ", " + SOURCING_CAPABILITY + "]");
      } else {
        try {
          lookup("http://" + System.getProperty("org.cougaar.servicediscovery.registry.hostname") + ":" +
                 System.getProperty("org.cougaar.servicediscovery.registry.server.port"));

          boolean allFlag = false;
          for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase(ALL)) {
              System.out.println("Publishing all Taxonomies");
              allFlag = true;

              try {
                genTaxonomy(UDDIConstants.MILITARY_SERVICE_SCHEME, UDDIConstants.MILITARY_SERVICE_SCHEME_UUID, CHECKED);
                genTaxonomy(UDDIConstants.MILITARY_ECHELON_SCHEME, UDDIConstants.MILITARY_ECHELON_SCHEME_UUID, CHECKED);
                genTaxonomy(UDDIConstants.ORGANIZATION_TYPES, UDDIConstants.ORGANIZATION_TYPES_UUID, CHECKED);
                genTaxonomy(UDDIConstants.SOURCING_CAPABILITY_SCHEME, UDDIConstants.SOURCING_CAPABILITY_SCHEME_UUID, CHECKED);
                genTaxonomy(UDDIConstants.PLATFORMS_SUPPORTED, UDDIConstants.PLATFORMS_SUPPORTED_UUID, UNCHECKED);
                genTaxonomy(UDDIConstants.SUPPORT_COMMAND_ASSIGNMENT, UDDIConstants.SUPPORT_COMMAND_ASSIGNMENTI_UUID, UNCHECKED);

              } catch (UDDIException e) {
                e.printStackTrace();  //To change body of catch statement use Options | File Templates.
              }
            } else if (args[i].equalsIgnoreCase(MILITARY_SERVICE) && !allFlag) {
              System.out.println("Publishing: " + UDDIConstants.MILITARY_SERVICE_SCHEME);
              genTaxonomy(UDDIConstants.MILITARY_SERVICE_SCHEME, UDDIConstants.MILITARY_SERVICE_SCHEME_UUID, CHECKED);

            } else if (args[i].equalsIgnoreCase(MILITARY_ECHELON) && !allFlag) {
              System.out.println("Publishing: " + UDDIConstants.MILITARY_ECHELON_SCHEME);
              genTaxonomy(UDDIConstants.MILITARY_ECHELON_SCHEME, UDDIConstants.MILITARY_ECHELON_SCHEME_UUID, CHECKED);

            } else if (args[i].equalsIgnoreCase(PLATFORMS_SUPPORTED) && !allFlag) {
              System.out.println("Publishing: " + UDDIConstants.PLATFORMS_SUPPORTED);
              genTaxonomy(UDDIConstants.PLATFORMS_SUPPORTED, UDDIConstants.PLATFORMS_SUPPORTED_UUID, UNCHECKED);

            } else if (args[i].equalsIgnoreCase(SUPPORTED_COMMAND) && !allFlag) {
              System.out.println("Publishing: " + UDDIConstants.SUPPORT_COMMAND_ASSIGNMENT);
              genTaxonomy(UDDIConstants.SUPPORT_COMMAND_ASSIGNMENT, UDDIConstants.SUPPORT_COMMAND_ASSIGNMENTI_UUID, UNCHECKED);

           } else if (args[i].equalsIgnoreCase(ORGANIZATION_TYPE) && !allFlag) {
              System.out.println("Publishing: " + UDDIConstants.ORGANIZATION_TYPES);
             genTaxonomy(UDDIConstants.ORGANIZATION_TYPES, UDDIConstants.ORGANIZATION_TYPES_UUID, CHECKED);

           } else if (args[i].equalsIgnoreCase(SOURCING_CAPABILITY) && !allFlag) {
              System.out.println("Publishing: " + UDDIConstants.SOURCING_CAPABILITY_SCHEME);
             genTaxonomy(UDDIConstants.SOURCING_CAPABILITY_SCHEME, UDDIConstants.SOURCING_CAPABILITY_SCHEME_UUID, CHECKED);

           } else {
              System.out.println("Unknown value: " + args[i]);
             System.err.println("Usage: PublishTaxonomy [" + ALL + ", " + MILITARY_SERVICE + ", " + MILITARY_ECHELON +
                                ", " + PLATFORMS_SUPPORTED + ", " + SUPPORTED_COMMAND + ", " +
                                ORGANIZATION_TYPE + ", " + SOURCING_CAPABILITY + "]");
            }
          }
        } catch (UDDIException e) {
          System.err.println(e.getMessage());
        }
      }
  }
}

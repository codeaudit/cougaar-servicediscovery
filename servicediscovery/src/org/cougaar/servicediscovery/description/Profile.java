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

/**
 * This class describes a DAML-S profile for Jena. This class will probably
 * be replaced by a class written by the DAML-S people, but we need a placeholder
 * for now.
 */
package org.cougaar.servicediscovery.description;

import com.hp.hpl.mesa.rdf.jena.common.ErrorHelper;
import com.hp.hpl.mesa.rdf.jena.common.PropertyImpl;
import com.hp.hpl.mesa.rdf.jena.common.ResourceImpl;
import com.hp.hpl.mesa.rdf.jena.model.Property;
import com.hp.hpl.mesa.rdf.jena.model.Resource;
import org.cougaar.util.ConfigFinder;

/**
 */
public class Profile {

  protected static String cougaar_uri ="";
  protected static final String profile_uri ="http://www.daml.org/services/daml-s/2001/10/Profile.daml#";
  protected static final String service_uri = "http://www.daml.org/services/daml-s/2001/10/Service.daml#";

  //ServiceProfile level properties
  static String nSERVICEPROFILE = "ServiceProfile";
  public static Resource SERVICEPROFILE;
  static String nSERVICECATEGORY = "serviceCategory";
  public static Property SERVICECATEGORY;
  static String nCUSTOMERPREFERENCE = "customerPreference";
  public static Property CUSTOMERPREFERENCE;
  static String nTEXTDESCRIPTION = "textDescription";
  public static Property TEXTDESCRIPTION;
  static String nINPUT = "input";
  public static Property INPUT;
  static String nOUTPUT = "output";
  public static Property OUTPUT;
  static String nPRESENTEDBY = "isPresentedBy";
  public static Property PRESENTEDBY;

  //ServiceProvider related properties
  static String nPROVIDEDBY = "providedBy";
  public static Property PROVIDEDBY;
  static String nPROVIDERNAME = "name";
  public static Property PROVIDERNAME;
  static String nORGANIZATIONTYPE = "organizationType";
  public static Property ORGANIZATIONTYPE;

  //Grounding related properties
  static String nGROUNDING = "ServiceGrounding";
  public static Resource GROUNDING;
  static String nWSDLGROUNDING = "WsdlGrounding";
  public static Resource WSDLGROUNDING;
  static String nWSDLDOCUMENT = "wsdlDocument";
  public static Property BINDINGTYPE;
  static String nBINDINGTYPE = "bindingType";
  public static Property WSDLDOCUMENT;
  static String nSUPPORTEDBY = "isSupportedBy";
  public static Property SUPPORTEDBY;

  //ServiceCategory level properties
  static String nSERVICENAME = "serviceCategoryName";
  public static Property SERVICENAME;
  static String nSERVICECODE = "serviceCategoryCode";
  public static Property SERVICECODE;
  static String nSERVICESCHEME = "serviceNamingScheme";
  public static Property SERVICESCHEME;
  static String nADDITIONALQUALIFICATIONS = "additionalQualification";
  public static Property ADDITIONALQUALIFICATIONS;

  //AdditionalQualifications level properties
  static String nQUALIFICATIONNAME = "qualificationName";
  public static Property QUALIFICATIONNAME;
  static String nQUALIFICATIONVALUE = "qualificationValue";
  public static Property QUALIFICATIONVALUE;

  static {
    try {
      String s = ConfigFinder.getInstance().locateFile("cougaar.daml").getAbsolutePath();
      s = s.replaceFirst(":","");
      s = s.replace('\\', '/');
      cougaar_uri = "file://" + s + "#";
      // cougaar_uri = "http://localhost:8800/$DAML/cougaar.daml#";

      //ServiceProfile level properties
      SERVICEPROFILE = new ResourceImpl(cougaar_uri, nSERVICEPROFILE);
      SERVICECATEGORY = new PropertyImpl(cougaar_uri, nSERVICECATEGORY);
      CUSTOMERPREFERENCE = new PropertyImpl(cougaar_uri, nCUSTOMERPREFERENCE);
      TEXTDESCRIPTION = new PropertyImpl(profile_uri, nTEXTDESCRIPTION);
      OUTPUT = new PropertyImpl(profile_uri, nOUTPUT);
      INPUT = new PropertyImpl(profile_uri, nINPUT);
      PRESENTEDBY = new PropertyImpl(service_uri, nPRESENTEDBY);


      //ServiceProvider related properties
      PROVIDEDBY = new PropertyImpl(profile_uri, nPROVIDEDBY);
      PROVIDERNAME = new PropertyImpl(profile_uri, nPROVIDERNAME);
      ORGANIZATIONTYPE = new PropertyImpl(cougaar_uri, nORGANIZATIONTYPE);

      //Grounding related properties
      WSDLGROUNDING = new ResourceImpl(cougaar_uri, nWSDLGROUNDING);
      WSDLDOCUMENT = new PropertyImpl(cougaar_uri, nWSDLDOCUMENT);
      SUPPORTEDBY = new PropertyImpl(service_uri, nSUPPORTEDBY);
      GROUNDING = new PropertyImpl(service_uri, nGROUNDING);
      BINDINGTYPE = new PropertyImpl(cougaar_uri, nBINDINGTYPE);

      //ServiceCategory level properties
      SERVICENAME = new PropertyImpl(cougaar_uri, nSERVICENAME);
      SERVICECODE = new PropertyImpl(cougaar_uri, nSERVICECODE);
      SERVICESCHEME = new PropertyImpl(cougaar_uri, nSERVICESCHEME);
      ADDITIONALQUALIFICATIONS = new PropertyImpl(cougaar_uri, nADDITIONALQUALIFICATIONS);

      //AdditionalQualifications level properties
      QUALIFICATIONNAME = new PropertyImpl(cougaar_uri, nQUALIFICATIONNAME);
      QUALIFICATIONVALUE = new PropertyImpl(cougaar_uri, nQUALIFICATIONVALUE);

    } catch (Exception e) {
      ErrorHelper.logInternalError("RDF", 1, e);
    }
  }
}

/*
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

/**
 * This class describes a OWL profile for Jena. This class will probably
 * be replaced by a class written by the OWL people, but we need a placeholder
 * for now.
 */
package org.cougaar.servicediscovery.description;



import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;
import com.hp.hpl.jena.rdf.model.impl.RDFDefaultErrorHandler;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 */
public class Profile {

  protected static final String cougaar_uri = "http://cougaar.owl#";
  protected static final String profile_uri = "http://www.daml.org/services/owl-s/1.0/Profile.owl#";
  protected static final String service_uri = "http://www.daml.org/services/owl-s/1.0/Service.owl#"; 

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
  static String nECHELONOFSUPPORT = "echelonOfSupport";
  public static Property ECHELONOFSUPPORT;

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
      // Use Jena cache, allowing install independent service profiles

      //ServiceProfile level properties
      SERVICEPROFILE = new ResourceImpl(cougaar_uri, nSERVICEPROFILE);
      SERVICECATEGORY = new PropertyImpl(cougaar_uri, nSERVICECATEGORY);
      CUSTOMERPREFERENCE = new PropertyImpl(cougaar_uri, nCUSTOMERPREFERENCE);
      TEXTDESCRIPTION = new PropertyImpl(profile_uri, nTEXTDESCRIPTION);
      OUTPUT = new PropertyImpl(profile_uri, nOUTPUT);
      INPUT = new PropertyImpl(profile_uri, nINPUT);
      PRESENTEDBY = new PropertyImpl(service_uri, nPRESENTEDBY);
      ECHELONOFSUPPORT = new PropertyImpl(cougaar_uri, nECHELONOFSUPPORT);

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
        RDFDefaultErrorHandler errorHandler = new RDFDefaultErrorHandler();
        errorHandler.error(e);
    }
  }
}










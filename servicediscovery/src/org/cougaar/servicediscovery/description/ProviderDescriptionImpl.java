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

package org.cougaar.servicediscovery.description;

import com.hp.hpl.jena.daml.DAMLClass;
import com.hp.hpl.jena.daml.DAMLInstance;
import com.hp.hpl.jena.daml.DAMLModel;
import com.hp.hpl.jena.daml.common.DAMLModelImpl;
import com.hp.hpl.mesa.rdf.jena.model.RDFException;
import com.hp.hpl.mesa.rdf.jena.model.Resource;
import com.hp.hpl.mesa.rdf.jena.model.Statement;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.ConfigFinder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Implements ProviderDescription
 * Wraps the Jena parser object (DAML model) with convenience accessors
 * At the ProviderDescription level, a DAMLS ServiceProvider is associated
 * with a number of ServiceDescriptions (which correspond to DAMLS ServiceProfiles).
 */

public class ProviderDescriptionImpl implements ProviderDescription {

  //store the model that Jena parses from the file
  private DAMLModel model = null;
  private String filename;

  private LoggingService log;

  public static void main (String args[]) {
    ProviderDescription pd = new ProviderDescriptionImpl(null);
    pd.parseDAML("NEWARK.profile.daml");
    System.out.println(pd.toString());
  }

  /**
   * This constructor builds an instance using a DAML-S file that is expected
   * to contain at least one service profile and one service provider.
   */
  public ProviderDescriptionImpl(LoggingService log) {
    this.log = log;
  }

  public boolean parseDAML(String fileName) {
    this.filename = fileName;

    InputStream in = null;
    try {
      in = ConfigFinder.getInstance().open(fileName);
    } catch (IOException e1) {
      if (log != null && log.isErrorEnabled()) {
        log.error("Error:  Cannot open file, " + fileName, e1);
      }
      return false;
    }

    model = new DAMLModelImpl();

    //There is some intermittent problem with a link to SRI
    //from one of the necessary daml base pages.
    //this code seems to deal with that link problem but trying to access
    //it a different way.
    model.getLoader().addImportBlock("http://www.ai.sri.com/daml/ontologies/sri-basic/1-0/Time.daml");
    model.getLoader().setUseImportBlocking(true);

    InputStreamReader reader = new InputStreamReader(in);
    try {
      model.read(reader, "");
    } catch (RDFException e1) {
      if (log != null && log.isErrorEnabled()) {
        log.error("Error parsing DAML file [" + fileName + "]\n" +
                  "  Error Number: " + e1.getErrorCode() + "\n" +
                  "  Error Message: " + e1.getMessage() + "\n" +
                  "  File: " + filename, e1);
      }
    }
    return model.getLoadSuccessful();
  }

  public String getProviderName() {
    String name = null;

    try {
      Statement serviceProvider = this.getServiceProvider();
      if(serviceProvider !=null) {
        name = serviceProvider.getProperty(Profile.PROVIDERNAME).getString();
      }
    }
    catch(RDFException e) {
      if (log != null && log.isErrorEnabled()) {
        log.error("Error getting Provider Name \n" +
                  "  Error Number: " + e.getErrorCode() + "\n" +
                  "  Error Message: " + e.getMessage() + "\n" +
                  "  File: " + filename, e);
      }
    }
    return name;
  }

  public Collection getBusinessCategories(){
    ArrayList ret = new ArrayList();
    String orgType = this.getOrganizationType();
    if(!orgType.equals("None")) {
      ret.add(new BusinessCategoryImpl("OrganizationTypes", orgType, orgType));
    }
    return ret;
  }

  public String getOrganizationType() {
    String group = "None";
    try {
      Statement serviceProvider = this.getServiceProvider();
      if((serviceProvider != null) && (serviceProvider.getResource() instanceof DAMLInstance)) {
        DAMLInstance inst = (DAMLInstance)serviceProvider.getResource();
        Iterator iter = inst.getRDFTypes(false);
        while(iter.hasNext()) {
          DAMLClass dclass = (DAMLClass)iter.next();
          String subclassName = dclass.getLocalName();
          int index = subclassName.indexOf("ServiceProvider");
          if(index>0) {
            group = subclassName.substring(0,index);
          }
        }
      }
    } catch(RDFException e) {
      if (log != null && log.isErrorEnabled()) {
        log.error("Error getOrganizationType failed \n" +
                  "  Error Number: " + e.getErrorCode() + "\n" +
                  "  Error Message: " + e.getMessage(), e);
      }
    }
    return group;
  }

  private Statement getServiceProvider() {
    Statement serviceProvider = null;

    try {
      Collection serviceProfiles = this.getServicePs();
      Iterator iter = serviceProfiles.iterator();
      //This is a little strange. You can have multiple service profiles
      //but each one should have an identical service provider. So, just
      //read the information from the first service provider.
      while(iter.hasNext()) {
        DAMLInstance serviceProfile = (DAMLInstance)iter.next();
        //get the service provider
        if(serviceProfile.hasProperty(Profile.PROVIDEDBY)){
          //should be only one
          serviceProvider = serviceProfile.getProperty(Profile.PROVIDEDBY);
          break;
        }
      }
    } catch(RDFException e) {
      if (log != null && log.isErrorEnabled()) {
        log.error("Error getting Service Provider \n" +
                  "  Error Number: " + e.getErrorCode() + "\n" +
                  "  Error Message: " + e.getMessage() + "\n" +
                  "  File: " + filename, e);
      }
    }
    return serviceProvider;
  }

  private Collection getServicePs() {
    ArrayList serviceProfiles = new ArrayList();
    Iterator instances = model.listDAMLInstances();
    while (instances.hasNext()) {
      DAMLInstance inst = (DAMLInstance)instances.next();
      if(inst.hasRDFType(Profile.SERVICEPROFILE.getURI())){
        serviceProfiles.add(inst);
      }
    }
    if (serviceProfiles.isEmpty() && log.isInfoEnabled()) {
      log.info("Info: getServicePs() for [" + filename + "] is returning an empty collection.\n" +
               "DAMLInstance does not contain: " + Profile.SERVICEPROFILE.getURI());
    }

    return serviceProfiles;
  }

  private Collection getServiceGroundings() {
    ArrayList serviceGroundings = new ArrayList();
    Iterator instances = model.listDAMLInstances();
    while (instances.hasNext()) {
      DAMLInstance inst = (DAMLInstance)instances.next();
//      if(inst.hasRDFType(Profile.WSDLGROUNDING.getURI())){
      if(inst.hasRDFType(Profile.GROUNDING.getURI())){
        serviceGroundings.add(inst);
      }
    }
    if (serviceGroundings.isEmpty() && log.isInfoEnabled()) {
      log.info("Info: getServiceGroundings() for [" +  filename + "] is returning an empty collection.\n" +
               "DAMLInstance does not contain: " + Profile.GROUNDING.getURI());
    }

    return serviceGroundings;
  }

  public Collection getServiceProfiles() {
    Collection serviceProfiles = this.getServicePs();
    Collection serviceGroundings = this.getServiceGroundings();

    //we need to match up the isProvidedBy and isSupportedBy so
    //that we are pairing the correct grounding and profile.

    Iterator profiles = serviceProfiles.iterator();
    ArrayList serviceDescriptions = new ArrayList();
    //for each profile
    while(profiles.hasNext()) {
      DAMLInstance profile = (DAMLInstance)profiles.next();
      Resource theProfileService = null;
      //find the Service object
      try {
        theProfileService =profile.getProperty(Profile.PRESENTEDBY).getResource();
      } catch(RDFException e) {
        if (log != null && log.isErrorEnabled()) {
          log.error("Error getting ServiceProfiles \n" +
                    "  Error Number: " + e.getErrorCode() + "\n" +
                    "  Error Message: " + e.getMessage() + "\n" +
                    "  File: " + filename,    e);
        }
      }
      Iterator groundings = serviceGroundings.iterator();
      //for each grounding
      while(groundings.hasNext()) {
        DAMLInstance grounding = (DAMLInstance)groundings.next();
        Resource theGroundingService = null;
        try {
          theGroundingService =grounding.getProperty(Profile.SUPPORTEDBY).getResource();
          //see if it has a matching Service object
          if(theGroundingService == theProfileService) {
            //if so, this profile/grounding pair makes a service profile object
            serviceDescriptions.add(new ServiceProfileImpl(profile, grounding));
          }
        } catch(RDFException e) {
          if (log != null && log.isErrorEnabled()) {
            log.error("Error cannot find isSupporteBy in grounding \n" +
                      "  Error Number: " + e.getErrorCode() + "\n" +
                      "  Error Message: " + e.getMessage() + "\n" +
                      "  File: " + filename , e);
          }
        }
      }
    }
    return serviceDescriptions;
  }

  public String getProviderDescriptionURI() {
    return "cougaar://" + getProviderName();
  }

  public String toString() {
    String ret = "ProviderDescription(name: " +this.getProviderName();
    ret = ret.concat(" pd uri: " + this.getProviderDescriptionURI())
               + " OrgType: " + this.getOrganizationType();
    Iterator it = getServiceProfiles().iterator();
    while(it.hasNext()){
      ret = ret.concat(" " +it.next().toString());
    }
    ret = ret.concat(")");
    return ret;
  }

  /**
   * not implemented yet
   */
  public void writeDAMLSFiles(String outputFileBase){
  }
}

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

package org.cougaar.servicediscovery.description;

import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.daml.DAMLClass;
import com.hp.hpl.jena.ontology.daml.DAMLInstance;
import com.hp.hpl.jena.ontology.daml.DAMLModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFException;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.shared.JenaException;

import org.cougaar.servicediscovery.Constants;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.net.URL;
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
  private String fileName;
  private String cougaarInstallPath;

  private static Logger logger = Logging.getLogger(ProviderDescription.class);

  private static Boolean LOCKED;

  static {
    LOCKED = Boolean.FALSE;
  }

  public static void main (String args[]) {

    ProviderDescription pd = 
      new ProviderDescriptionImpl();
    pd.parseDAML("123-MSB.profile.daml");
    System.out.println(pd.toString());
  }

  /**
   * This constructor builds an instance using a DAML-S file that is expected
   * to contain at least one service profile and one service provider.
   */
  public ProviderDescriptionImpl() {
    this.cougaarInstallPath = System.getProperty("org.cougaar.install.path", "");
  }

  public boolean parseDAML(String damlFileName) {
    synchronized (LOCKED) {
      if (logger.isDebugEnabled()) {
	logger.debug("ProviderDescription.parseDAML for " + 
		     damlFileName + " LOCKED == " +  LOCKED);
      }
      if (LOCKED == Boolean.TRUE) {
	return false;
      } else {
	LOCKED = Boolean.TRUE;
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug("ProviderDescription.parseDAML starting to parse " + 
		   damlFileName);
    }

    this.fileName = damlFileName;

    InputStream in = null;
    try {

      URL inURL = new URL(Constants.getServiceProfileURL(), damlFileName);

      in = inURL.openStream();

    } catch (java.net.MalformedURLException mue) {
      logger.error("Error:  Cannot open file, " + fileName, mue);
    } catch (IOException e1) {
      logger.error("Error:  Cannot open file, " + fileName, e1);
    } finally {
      if (in == null) {
	LOCKED = Boolean.FALSE;
	return false;
      }
    }

    model = ModelFactory.createDAMLModel();
    OntDocumentManager mgr = model.getDocumentManager();

    String prefix = "file:";
    if(cougaarInstallPath.length()>0) {
        prefix = prefix + cougaarInstallPath + File.separator;
    }
    prefix = prefix + "servicediscovery" + File.separator + "data" +
            File.separator + "cached" + File.separator;


    mgr.addAltEntry("http://cougaar.daml",
              prefix + "cougaar.daml");
    mgr.addAltEntry("http://www.w3.org/1999/02/22-rdf-syntax-ns",
              prefix + "rdfSyntaxNS.rdf");
    mgr.addAltEntry("http://www.w3.org/2000/01/rdf-schema",
              prefix + "rdfSchema.rdf");
    mgr.addAltEntry("http://www.daml.org/2001/03/daml+oil",
              prefix + "damlOil.daml");
    mgr.addAltEntry("http://www.w3.org/2000/10/XMLSchema.xsd",
              prefix + "XMLSchema.xsd");
    mgr.addAltEntry("http://www.daml.org/services/daml-s/2001/10/Service.daml",
              prefix + "Service.daml");
    mgr.addAltEntry("http://www.daml.org/services/daml-s/2001/10/Process.daml",
              prefix + "Process.daml");
    mgr.addAltEntry("http://www.daml.org/services/daml-s/2001/10/Profile.daml",
              prefix + "Profile.daml");
    mgr.addAltEntry("http://www.ai.sri.com/daml/ontologies/sri-basic/1-0/Time.daml",
              prefix + "Time.daml");

    OntModelSpec s = new OntModelSpec( OntModelSpec.DAML_MEM);
    s.setDocumentManager( mgr );

    System.out.println("");

    try {
        model.read(in, "");
    } catch (RDFException e1) {
      String helpMessage =
        "If the following StackTrace includes \"IO error while reading URL:\"\n" +
        " you should examine the URL and confirm that it exists and\n" +
        " is currently accessible. If the URL includes www.daml.org,\n" +
        " this error means the daml site is temporarily unavailable.\n" +
        " If the URL is a filepath or includes \"cougaar.daml\", this error\n" +
        " probably means that your profile.daml files are inconsistent with\n" +
        " your installation. One common way for this to happen is to have\n" +
        " generated your profile.daml files from a perl script using an\n" +
        " agent-input.txt file containing an incorrect or incorrectly formatted\n" +
        " cougaarInstallPath. Alternatively, you may have generated your\n" +
        " profile.daml files from a ruby script while your %COUGAAR_INSTALL_PATH%\n" +
        " environment variable was not set correctly. Check these things and try\n" +
        " regenerating your profile.daml files. \n";
      logger.error(helpMessage + "Error parsing DAML file [" + fileName + "]\n" +
		"  Error Number: " + e1.getErrorCode() + "\n" +
		"  Error Message: " + e1.getMessage() + "\n" +
		"  Error StackTrace: " + e1.getStackTrace() + "\n" +
		"  File: " + fileName, e1);
    } catch (JenaException eJ){
      logger.error("JenaException in "+ fileName + " " + eJ.getMessage());
    }

    boolean success = model.getLoadSuccessful();

    LOCKED = Boolean.FALSE;
    
    return success;
  }

  public String getProviderName() {
    String name = null;

    try {
      Statement serviceProvider = this.getServiceProvider();
      if(serviceProvider !=null) {
        name = serviceProvider.getProperty(Profile.PROVIDERNAME).getString();
      }
    } catch(RDFException e) {
      logger.error("Error getting Provider Name \n" +
		"  Error Number: " + e.getErrorCode() + "\n" +
		"  Error Message: " + e.getMessage() + "\n" +
		"  File: " + fileName, e);
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
      logger.error("Error getOrganizationType failed \n" +
		"  Error Number: " + e.getErrorCode() + "\n" +
		"  Error Message: " + e.getMessage(), e);
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
      logger.error("Error getting Service Provider \n" +
		"  Error Number: " + e.getErrorCode() + "\n" +
		"  Error Message: " + e.getMessage() + "\n" +
		"  File: " + fileName, e);
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
    if (serviceProfiles.isEmpty() && logger.isInfoEnabled()) {
      logger.info("Info: getServicePs() for [" + fileName + "] is returning an empty collection.\n" +
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

    if (serviceGroundings.isEmpty() && logger.isInfoEnabled()) {
      logger.info("Info: getServiceGroundings() for [" +  fileName + "] is returning an empty collection.\n" +
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
	logger.error("Error getting ServiceProfiles \n" +
		  "  Error Number: " + e.getErrorCode() + "\n" +
		  "  Error Message: " + e.getMessage() + "\n" +
		  "  File: " + fileName,    e);
      }
      Iterator groundings = serviceGroundings.iterator();
      //for each grounding
      while(groundings.hasNext()) {
        DAMLInstance grounding = (DAMLInstance)groundings.next();
        Resource theGroundingService = null;
        try {
          theGroundingService =grounding.getProperty(Profile.SUPPORTEDBY).getResource();
          //see if it has a matching Service object
          if(theGroundingService.equals(theProfileService)) {
            //if so, this profile/grounding pair makes a service profile object
            serviceDescriptions.add(new ServiceProfileImpl(profile, grounding));
          }
        } catch(RDFException e) {
	  logger.error("Error cannot find isSupporteBy in grounding \n" +
		    "  Error Number: " + e.getErrorCode() + "\n" +
		    "  Error Message: " + e.getMessage() + "\n" +
		    "  File: " + fileName , e);
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

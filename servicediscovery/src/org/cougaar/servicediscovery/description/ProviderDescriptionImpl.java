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
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFException;
import com.hp.hpl.jena.rdf.model.RDFNode; 
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.vocabulary.RDF;

import org.cougaar.servicediscovery.Constants;
import org.cougaar.util.Configuration;
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
 * Wraps the Jena parser object ( model) with convenience accessors
 * At the ProviderDescription level, an OWL ServiceProvider is associated
 * with a number of ServiceDescriptions (which correspond to OWL ServiceProfiles).
 */

public class ProviderDescriptionImpl implements ProviderDescription {

  //store the model that Jena parses from the file
  protected OntModel model = null;
  private String fileName;
  private String cougaarInstallPath;

  private static Logger logger = Logging.getLogger(ProviderDescription.class);

  private static Boolean LOCKED;

  static {
    LOCKED = Boolean.FALSE;
  }

  public static void main (String args[]) {

    ProviderDescriptionImpl pd = 
      new ProviderDescriptionImpl();
    boolean status = pd.parseOWL("1-AD.ARMY.MIL.profile.owl");

    logger.shout("main: Model class = " + pd.model.getClass());

    Resource serviceProfileResource = 
      pd.model.getResource(Profile.SERVICEPROFILE.getURI());
    
    logger.shout("main: serviceProfileResource = " + serviceProfileResource);
    
    Collection serviceProfiles = pd.getServicePs();
    for (Iterator iterator = serviceProfiles.iterator();
	 iterator.hasNext();) { 
      Resource serviceProfile = (Resource) iterator.next();

      logger.shout("\n serviceProfileResource = " + serviceProfile + 
		   " class = " + serviceProfile.getClass() +
		   " has uri = " + serviceProfile.getURI());      
      for (Iterator propertiesIterator = serviceProfile.listProperties();
	   propertiesIterator.hasNext();) {
	logger.shout("\t property = " + propertiesIterator.next());
      }
    }

    Statement serviceProvider = pd.getServiceProvider();
    logger.shout("\n serviceProvider = " + serviceProvider + 
		 " resource = " + serviceProvider.getResource() +
		 " resource class = " + 
		 serviceProvider.getResource().getClass());
    logger.shout("\n serviceProviderName = " + pd.getProviderName());
    
    serviceProfiles = pd.getServiceProfiles();
    for (Iterator iterator = serviceProfiles.iterator();
	 iterator.hasNext();) { 
      ServiceProfile serviceProfile = (ServiceProfile) iterator.next();

      logger.shout("\n serviceProfile = " + serviceProfile);
    }
    
    logger.shout("\n organizationType  = " + pd.getOrganizationType());

    logger.shout("\n ServiceProfile uri = " + 
		 Profile.SERVICEPROFILE.getURI());
  }

  /**
   * This constructor builds an instance using a OWL file that is expected
   * to contain at least one service profile and one service provider.
   */
  public ProviderDescriptionImpl() {
    this.cougaarInstallPath = System.getProperty("org.cougaar.install.path", "");
  }

  public boolean parseOWL(String owlFileName) {
    return parseOWL(Constants.getServiceProfileURL(), owlFileName);
  }

  public boolean parseOWL(URL serviceProfileURL, String owlFileName) {

    synchronized (LOCKED) {
      if (logger.isDebugEnabled()) {
	logger.debug("ProviderDescription.parseOWL for " + 
		     owlFileName + " LOCKED == " +  LOCKED);
      }
      if (LOCKED == Boolean.TRUE) {
	return false;
      } else {
	LOCKED = Boolean.TRUE;
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug("ProviderDescription.parseOWL starting to parse " + 
		   owlFileName);
    }

    this.fileName = owlFileName;

    InputStream in = null;
    try {
      URL inURL = new URL(serviceProfileURL, owlFileName);
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

    model = ModelFactory.createOntologyModel();
    OntDocumentManager mgr = model.getDocumentManager();

    String prefix = "file:";
    if(cougaarInstallPath.length()>0) {
        prefix = prefix + cougaarInstallPath + File.separator;
    }
    prefix = prefix + "servicediscovery" + File.separator + "data" +
            File.separator + "cached" + File.separator;

    OntModelSpec s = new OntModelSpec( OntModelSpec.OWL_MEM);
    s.setDocumentManager( mgr );

    mgr.addAltEntry("http://cougaar.owl",
              prefix + "cougaar.owl");
    mgr.addAltEntry("http://www.w3.org/1999/02/22-rdf-syntax-ns",
              prefix + "rdfSyntaxNS.rdf");
    mgr.addAltEntry("http://www.w3.org/2000/01/rdf-schema",
              prefix + "rdfSchema.rdf");
    mgr.addAltEntry("http://www.w3.org/2002/07/owl",
              prefix + "owl.rdf");
    mgr.addAltEntry("http://www.w3.org/2000/10/XMLSchema.xsd",
              prefix + "XMLSchema.xsd");
    mgr.addAltEntry("http://www.daml.org/services/owl-s/1.0/Service.owl",
              prefix + "Service.owl");
    mgr.addAltEntry("http://www.daml.org/services/owl-s/1.0/Process.owl",
              prefix + "Process.owl");
    mgr.addAltEntry("http://www.daml.org/services/owl-s/1.0/Profile.owl",
              prefix + "Profile.owl");
    mgr.addAltEntry("http://www.ai.sri.com/daml/ontologies/sri-basic/1-0/Time.daml",
              prefix + "Time.daml");

    try {
        model.read(in, "");
    } catch (RDFException e1) {
      String helpMessage =
        "If the following StackTrace includes \"IO error while reading URL:\"\n" +
        " you should examine the URL and confirm that it exists and\n" +
        " is currently accessible. If the URL includes www.daml.org,\n" +
        " this error means the daml site is temporarily unavailable.\n" +
        " If the URL is a filepath or includes \"cougaar.owl\", this error\n" +
        " probably means that your profile.owl files are inconsistent with\n" +
        " your installation. One common way for this to happen is to have\n" +
        " generated your profile.owl files from a perl script using an\n" +
        " agent-input.txt file containing an incorrect or incorrectly formatted\n" +
        " cougaarInstallPath. Alternatively, you may have generated your\n" +
        " profile.owl files from a ruby script while your %COUGAAR_INSTALL_PATH%\n" +
        " environment variable was not set correctly. Check these things and try\n" +
        " regenerating your profile.owl files. \n";
      logger.error(helpMessage + "Error parsing OWL file [" + fileName + "]\n" +
		"  Error Number: " + e1.getErrorCode() + "\n" +
		"  Error Message: " + e1.getMessage() + "\n" +
		"  Error StackTrace: " + e1.getStackTrace() + "\n" +
		"  File: " + fileName, e1);
    } catch (JenaException eJ){
      logger.error("JenaException in "+ fileName + " " + eJ.getMessage());
    }

    if (logger.isDebugEnabled()) {
      logger.debug("ProviderDescription.parseOWL for " + 
		   owlFileName + " model  == " +  model);
      Statement serviceProvider = getServiceProvider();
      logger.debug("\n serviceProvider = " + serviceProvider + 
		   " resource = " + serviceProvider.getResource() +
		   " resource class = " + 
		   serviceProvider.getResource().getClass());
      logger.debug("\n serviceProviderName = " + getProviderName());
      
      Collection serviceProfiles = getServiceProfiles();
      for (Iterator iterator = serviceProfiles.iterator();
	   iterator.hasNext();) { 
	ServiceProfile serviceProfile = (ServiceProfile) iterator.next();
	
	logger.debug("\n serviceProfile = " + serviceProfile);
      }
      logger.debug("\n organizationType  = " + getOrganizationType());
    }	

    LOCKED = Boolean.FALSE;
    return true;
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
      if (serviceProvider != null) {
        Resource resource = serviceProvider.getResource();
	
	for (Iterator rdfTypeIterator = resource.listProperties(RDF.type);
	     rdfTypeIterator.hasNext();) {
	  Statement statement = (Statement) rdfTypeIterator.next();
	  Resource rdfResource = statement.getResource(); 
	  String localClassName = rdfResource.getLocalName();
	  int index = localClassName.indexOf("ServiceProvider");
	  if (index > 0) {
	    return localClassName.substring(0, index);
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
      Collection serviceProfiles = getServicePs();

      for (Iterator iterator = serviceProfiles.iterator();
	   iterator.hasNext();) {
	//This is a little strange. You can have multiple service profiles
	//but each one should have an identical service provider. So, just
	//read the information from the first service provider.
        Resource serviceProfile = (Resource)iterator.next();

	if (logger.isDebugEnabled()) {
	  logger.debug("getServiceProvider(): serviceProfile = " + 
		       serviceProfile + " Profile.PROVIDEDBY = " +
		       Profile.PROVIDEDBY);
	}
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
    Resource serviceProfileResource = 
      model.getResource(Profile.SERVICEPROFILE.getURI());
    ArrayList serviceProfiles = new ArrayList();

    for (Iterator iterator = model.listObjects();
	 iterator.hasNext();) { 
      RDFNode rdfNode = (RDFNode) iterator.next();
      if (rdfNode instanceof Resource) {
	Resource resource = (Resource) rdfNode;
	if (resource.hasProperty(RDF.type, serviceProfileResource)) {
	  serviceProfiles.add(resource);
	}
      }
    }

    if (serviceProfiles.isEmpty() && logger.isInfoEnabled()) {
      logger.info("Info: getServicePs() for [" + fileName + "] is returning an empty collection.\n" +
               "Model does not contain: " + Profile.SERVICEPROFILE.getURI());
    }
    
    return serviceProfiles;
  }
  
  private Collection getServiceGroundings() {
    Resource groundingResource = 
      model.getResource(Profile.GROUNDING.getURI());
    ArrayList serviceGroundings = new ArrayList();

    for (Iterator iterator = model.listObjects();
	 iterator.hasNext();) { 
      RDFNode rdfNode = (RDFNode) iterator.next();
      if (rdfNode instanceof Resource) {
	Resource resource = (Resource) rdfNode;
	if (resource.hasProperty(RDF.type, groundingResource)) {
	  serviceGroundings.add(resource);
	}
      }
    }

    if (serviceGroundings.isEmpty() && logger.isInfoEnabled()) {
      logger.info("Info: getServiceGroundings() for [" +  fileName + "] is returning an empty collection.\n" +
               "Model does not contain: " + Profile.GROUNDING.getURI());
    }

    return serviceGroundings;
  }

  public Collection getServiceProfiles() {
    Collection serviceProfiles = this.getServicePs();
    Collection serviceGroundings = this.getServiceGroundings();

    //we need to match up the isProvidedBy and isSupportedBy so
    //that we are pairing the correct grounding and profile.

    ArrayList serviceDescriptions = new ArrayList();

    Resource presentedbyResource = Profile.PRESENTEDBY;
    for (Iterator iterator = serviceProfiles.iterator();
	 iterator.hasNext();) { 
      Resource profile = (Resource) iterator.next();
      Resource profileService = null;
      //find the Service object
      try {
	profileService = 
	  profile.getProperty(Profile.PRESENTEDBY).getResource();
      } catch(RDFException e) {
	logger.error("Error getting ServiceProfiles \n" +
		     "  Error Number: " + e.getErrorCode() + "\n" +
		     "  Error Message: " + e.getMessage() + "\n" +
		     "  File: " + fileName,    e);
      }
	
      for (Iterator groundingIterator = serviceGroundings.iterator();
	   groundingIterator.hasNext();) { 
	Resource grounding = (Resource) groundingIterator.next();
	Resource groundingService = null;
	try {
	  groundingService = 
	    grounding.getProperty(Profile.SUPPORTEDBY).getResource();
	  
	  if (groundingService.equals(profileService)) {
	    //if so, this profile/grounding pair makes a service profile object
	    serviceDescriptions.add(new ServiceProfileImpl(profile, grounding));
	  }
	} catch(RDFException e) {
	  logger.error("Error cannot find isSupportedBy in grounding \n" +
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
  public void writeOWLFiles(String outputFileBase){
  }

}

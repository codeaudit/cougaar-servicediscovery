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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.hp.hpl.jena.rdf.model.RDFException;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

/**
 * Implements ServiceProfile
 */

public class ServiceProfileImpl implements ServiceProfile {

  private Resource serviceProfile;
  private Resource serviceGrounding;

  public ServiceProfileImpl(Resource serviceProfile, Resource serviceGrounding) {
    this.serviceProfile = serviceProfile;
    this.serviceGrounding = serviceGrounding;
  }

  public Collection getServiceCategories(){
    ArrayList serviceCategories = new ArrayList();
    if(serviceProfile != null) {
      try {
        if(serviceProfile.hasProperty(Profile.SERVICECATEGORY)){
          StmtIterator cats = serviceProfile.listProperties(Profile.SERVICECATEGORY);
          while(cats.hasNext()) {
            //can be multiple service categories heres
            Statement st = cats.nextStatement();
            RDFNode node = st.getObject();
            if (node instanceof Resource) {
              Resource serviceCat = (Resource)node;
              serviceCategories.add(new ServiceCategoryImpl(serviceCat));
            }
          }
        }
      }
      catch(RDFException e) {
        System.out.println("Failed: " + e);
      }
    }
    return serviceCategories;
  }

  public String getServiceProfileID(){
    String ret = " ";
    ret = serviceProfile.getURI();
    ret = ret.substring(1);
    return ret;
  }

  public String getEchelonOfSupport(){
    String echelonOfSupport = "";
    try {
      //get the text description
      if(serviceProfile.hasProperty(Profile.ECHELONOFSUPPORT)){
        StmtIterator echelon = serviceProfile.listProperties(Profile.ECHELONOFSUPPORT);
        while(echelon.hasNext()) {
          //should only be one string in this property
          echelonOfSupport = echelon.nextStatement().getString();
        }
      }
    }
    catch(RDFException e) {
      System.out.println("Failed: " + e);
    }
    return echelonOfSupport;
  }

  public String getTextDescription() {
    String textDescription = "";
    try {
      //get the text description
      if(serviceProfile.hasProperty(Profile.TEXTDESCRIPTION)){
        StmtIterator descs = serviceProfile.listProperties(Profile.TEXTDESCRIPTION);
        while(descs.hasNext()) {
          //should only be one string in this property
          textDescription = descs.nextStatement().getString();
        }
      }
    }
    catch(RDFException e) {
      System.out.println("Failed: " + e);
    }
    return textDescription;
  }

   public String getServiceGroundingURI() {
     String uri = "";
     try {
       if(serviceGrounding.hasProperty(Profile.WSDLDOCUMENT)) {
         StmtIterator uris = serviceGrounding.listProperties(Profile.WSDLDOCUMENT);
         while(uris.hasNext()) {
           //assume only one of these for now
           uri = uris.nextStatement().getString();
         }
       }
     }
     catch(RDFException e) {
       System.out.println("Failed: " + e);
     }
     return uri;
   }

   public String getServiceGroundingBindingType() {
     String bindingType = "SOAP";
     try {
       if(serviceGrounding.hasProperty(Profile.BINDINGTYPE)) {
         StmtIterator bindingTypes = serviceGrounding.listProperties(Profile.BINDINGTYPE);
         while(bindingTypes.hasNext()) {
           //assume only one of these for now
           bindingType = bindingTypes.nextStatement().getString();
         }
       }
     }
     catch(RDFException e) {
       System.out.println("Failed: " + e);
     }
     return bindingType;

   }

/*
        //do we have any inputs
        if(serviceProfile.hasProperty(Profile.INPUT)){
          StmtIterator ins = serviceProfile.listProperties(Profile.INPUT);
          while(ins.hasNext()) {
            Statement st = ins.next();
            RDFNode node = st.getObject();
            if(node instanceof Resource) {
              Resource input = (Resource)node;
              System.out.println(input.getLocalName());
            }
          }
        }
        //do we have any outputs
        if(serviceProfile.hasProperty(Profile.OUTPUT)){
          StmtIterator outs = serviceProfile.listProperties(Profile.OUTPUT);
          while(outs.hasNext()) {
            Statement st = outs.next();
            RDFNode node = st.getObject();
            if(node instanceof Resource) {
              Resource output = (Resource)node;
              System.out.println(output.getLocalName());
            }
          }
        }*/

  public String toString() {
    String ret = "ServiceProfile " + "ID: "+this.getServiceProfileID() + " (text: " + this.getTextDescription();
    ret = ret.concat(" wsdl uri: " + this.getServiceGroundingURI());
    ret = ret.concat(" binding type: " + this.getServiceGroundingBindingType());
    Iterator it = getServiceCategories().iterator();
    while(it.hasNext()){
      ret = ret.concat(" " + it.next().toString());
    }
    ret = ret.concat(")");
    return ret;
  }

}

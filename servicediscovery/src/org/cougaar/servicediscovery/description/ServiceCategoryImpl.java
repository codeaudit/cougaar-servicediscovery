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
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

public class ServiceCategoryImpl implements ServiceCategory {
  private Resource serviceCategory;

  public ServiceCategoryImpl(Resource serviceCategory){
    this.serviceCategory = serviceCategory;
  }

  public String getCategoryName() {
    String name = "";
    if(serviceCategory != null) {
      try {
        if(serviceCategory.hasProperty(Profile.SERVICENAME)) {
          name = serviceCategory.getProperty(Profile.SERVICENAME).getString();
        }
      }
      catch(RDFException e) {
        System.out.println("Failed: " + e);
      }
    }
    return name;
  }

  public String getCategoryCode() {
    String code = "";
    if(serviceCategory != null) {
      try {
        if(serviceCategory.hasProperty(Profile.SERVICECODE)) {
          code = serviceCategory.getProperty(Profile.SERVICECODE).getString();
        }
        else {
          code = this.getCategoryName();
        }
      }
      catch(RDFException e) {
        System.out.println("Failed: " + e);
      }
    }
    return code;
  }

  public String getCategorySchemeName() {
    String namingScheme = "";
    if(serviceCategory != null) {
      try {
        if(serviceCategory.hasProperty(Profile.SERVICESCHEME)) {
          namingScheme = serviceCategory.getProperty(Profile.SERVICESCHEME).getString();
        }
      }
      catch(RDFException e) {
        System.out.println("Failed: " + e);
      }
    }
    return namingScheme;
  }

  public Collection getAdditionalQualifications() {
    ArrayList additionalQualifications = new ArrayList();
    if(serviceCategory != null) {
      try {
        if(serviceCategory.hasProperty(Profile.ADDITIONALQUALIFICATIONS)){
          StmtIterator quals = serviceCategory.listProperties(Profile.ADDITIONALQUALIFICATIONS);

          while(quals.hasNext()) {
            //there may be multiples
            Statement st2 = quals.nextStatement();
            RDFNode node2 = st2.getObject();
            if(node2 instanceof Resource){
              Resource currentQual = (Resource)node2;
              additionalQualifications.add(new AdditionalQualificationRecord(currentQual));
            }
          }
        }
      }
      catch(RDFException e) {
        System.out.println("Failed: " + e);
      }
    }
    return additionalQualifications;
  }

  public String toString() {
    String ret = "ServiceCategory(scheme: " + this.getCategorySchemeName() + " name: " +
                 this.getCategoryName() + " code: " + this.getCategoryCode();
    Iterator it = this.getAdditionalQualifications().iterator();
    while(it.hasNext()){
      ret = ret.concat(" " +  it.next().toString() + " ");
    }
    ret = ret.concat(")");
    return ret;
  }
}

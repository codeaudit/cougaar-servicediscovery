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

import com.hp.hpl.mesa.rdf.jena.model.RDFException;
import com.hp.hpl.mesa.rdf.jena.model.RDFNode;
import com.hp.hpl.mesa.rdf.jena.model.Resource;
import com.hp.hpl.mesa.rdf.jena.model.Statement;
import com.hp.hpl.mesa.rdf.jena.model.StmtIterator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

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
            Statement st2 = quals.next();
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

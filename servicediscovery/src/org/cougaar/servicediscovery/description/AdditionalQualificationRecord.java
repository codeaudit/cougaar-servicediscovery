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

package org.cougaar.servicediscovery.description;

import com.hp.hpl.mesa.rdf.jena.model.RDFException;
import com.hp.hpl.mesa.rdf.jena.model.Resource;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

public class AdditionalQualificationRecord {

  private Resource qualification;

  public AdditionalQualificationRecord(Resource qualification){
    this.qualification = qualification;
  }

  public String getQualificationName() {
    String name = "";
    if(qualification != null) {
      try {
        if(qualification.hasProperty(Profile.QUALIFICATIONNAME)) {
          name = qualification.getProperty(Profile.QUALIFICATIONNAME).getString();
        }
      }
      catch(RDFException e) {
        System.out.println("Failed: " + e);
      }
    }
    return name;
  }
  public String getQualificationValue() {
    String value = "";
    if(qualification != null) {
      try {
        if(qualification.hasProperty(Profile.QUALIFICATIONVALUE)) {
          value = qualification.getProperty(Profile.QUALIFICATIONVALUE).getString();
        }
      }
      catch(RDFException e) {
        System.out.println("Failed: " + e);
      }
    }
    return value;
  }

  public String toString() {
    String ret = "AdditionalQualificationRecord(name: " + getQualificationName() + " value: " +
                 getQualificationValue() + ")";
    return ret;
  }
}

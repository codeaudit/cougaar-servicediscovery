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

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.RDFException;


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

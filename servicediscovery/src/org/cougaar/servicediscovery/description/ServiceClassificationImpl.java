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

import org.cougaar.servicediscovery.util.UDDIConstants;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

public class ServiceClassificationImpl implements ServiceClassification {

  private String code;
  private String name;
  private String scheme;

  public ServiceClassificationImpl() {
  }

  public ServiceClassificationImpl(String code, String name, String scheme){
    this.code = code;
    this.name = name;
    this.scheme = scheme;
  }

  public String getClassificationName() {
    return name;
  }

  public String getClassificationCode() {
    return code;
  }

  public String getClassificationSchemeName() {
    return scheme;
  }

  public void setClassificationName(String name) {
    this.name = name;
  }

  public void setClassificationCode(String code) {
    this.code = code;
  }

  public void setClassificationSchemeName(String scheme) {
    this.scheme = scheme;
  }

  public String toString() {
    String ret = "ServiceClassification(scheme: " + this.getClassificationSchemeName() + " name: " +
                 this.getClassificationName() + " code: " + this.getClassificationCode() + ")";
    return ret;
  }
}
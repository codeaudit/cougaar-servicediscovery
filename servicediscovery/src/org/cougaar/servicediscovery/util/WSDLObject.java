/*
 *  <copyright>
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
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 *  </copyright>
 */
package org.cougaar.servicediscovery.util;

import java.util.Vector;

/**
 * Encapsulates wsdl document with soap binding as java object. Only supports one service
 * per wsdl document.
 * <ul>
 *   <li> docLocation : location of the original wsdl document. </li>
 *   <li> soapLocation :location of the soap server hosting the service.</li>
 *   <li> encodingStyle : style used to encode soap messages.</li>
 *   <li> targetNameSpace: soap service namespace.</li>
 *   <li> methods : names of different methods exposed by the service.</li>
 *   <li> hasSoapBinding : false by default, true if soap:address is specified.</li>
 *
 *@author    HSingh
 *@version   $Id: WSDLObject.java,v 1.2 2003-01-22 17:30:45 mthome Exp $
 */

public class WSDLObject {
	private String docLocation;
	private String soapLocation;
	private String encodingStyle;
	private String targetNameSpace;
	private Vector methods;
	private boolean hasSoapBinding;

	public WSDLObject() { }

	/**
	 *@param docLocation  location of the original wsdl document
	 */
	public WSDLObject(String docLocation) {
		this.docLocation = docLocation;
		this.methods = new Vector();
		this.hasSoapBinding = false;
	}

	/**
	 *@return   location of the original wsdl document
	 */
	public String getDocLocation() {
		return this.docLocation;
	}

	/**
	 *@param docLocation  location of the original wsdl document
	 */
	public void setDocLocation(String docLocation) {
		this.docLocation = docLocation;
	}

	/**
	 *@return   style used to encode soap messages.
	 */
	public String getEncodingStyle() {
		return this.encodingStyle;
	}

	/**
	 *@param encodingStyle  style used to encode soap messages.
	 */
	public void setEncodingStyle(String encodingStyle) {
		this.encodingStyle = encodingStyle;
	}

	/**
	 *@return   soap service namespace.
	 */
	public String getTargetNameSpace() {
		return this.targetNameSpace;
	}

	/**
	 *@param targetNameSpace  soap service namespace.
	 */
	public void setTargetNameSpace(String targetNameSpace) {
		this.targetNameSpace = targetNameSpace;
	}

	/**
	 *@return   location of the soap server hosting the service.
	 */
	public String getSoapLocation() {
		return this.soapLocation;
	}

	/**
	 *@param soapLocation  location of the soap server hosting the service.
	 */
	public void setSoapLocation(String soapLocation) {
		this.soapLocation = soapLocation;
	}

	/**
	 *@return   names of different methods exposed by the service.
	 */
	public Vector getMethods() {
		return this.methods;
	}

	/**
	 *@param methodName  adds one method to the list of available methods.
	 */
	public void addMethod(String methodName) {
		this.methods.add(methodName);
	}

	/**
	 *@param hasSoapBinding  false by default, true if soap:address is specified.
	 */
	public void hasSoapBinding(boolean hasSoapBinding) {
		this.hasSoapBinding = hasSoapBinding;
	}

	/**
	 *@return   false by default, true if soap:address is specified.
	 */
	public boolean hasSoapBinding() {
		return this.hasSoapBinding;
	}


	public String toString() {
		return new String("doc location  : " + this.docLocation + "\n" +
			"soap location : " + this.soapLocation + "\n" +
			"tgt name space: " + this.targetNameSpace + "\n" +
			"encoding style: " + this.encodingStyle + "\n" +
			"methods       : " + this.methods + "\n");
	}
}


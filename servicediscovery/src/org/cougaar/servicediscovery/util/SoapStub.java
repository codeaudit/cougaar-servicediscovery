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
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 *  </copyright>
 */
package org.cougaar.servicediscovery.util;

import java.net.URL;
import java.util.Vector;

import org.apache.soap.Constants;
import org.apache.soap.Fault;
import org.apache.soap.SOAPException;
import org.apache.soap.rpc.Call;
import org.apache.soap.rpc.Parameter;
import org.apache.soap.rpc.Response;

/**
 * WebService stubs called to invoke methods on server side using SOAP.
 *
 *@author    HSingh
 *@version   $Id: SoapStub.java,v 1.2 2003-01-22 17:30:45 mthome Exp $
 */
public class SoapStub {

	/**
	 * Stub for isAvailable method offered by Grainger & Newark web services.
	 *
	 *@param partNumber     manufacturers part number
	 *@param manufacturer   name of manufacturer
	 *@param wsdlObj        WSDLObject with information about the web sevice
	 *@return               true if part is available.
	 *@exception Exception  soap exception
	 */
	public boolean checkAvailability(String partNumber,
		String manufacturer, WSDLObject wsdlObj) throws Exception {
		Vector params = new Vector();
		params.addElement(new Parameter("partNumber", String.class, partNumber, null));
		params.addElement(new Parameter("manufacturer", String.class, manufacturer, null));

		String methodName = "isAvailable";
		Boolean b = (Boolean) makeCall(wsdlObj, params, methodName);

		if(b != null) {
			return b.booleanValue();
		}

		return false;
	}

	/**
	 * Stub for getPartPrice method offered by Grainger & Newark web services.
	 *
	 *@param partNumber     manufacturers part number
	 *@param manufacturer   name of manufacturer
	 *@param wsdlObj        WSDLObject with information about the web sevice
	 *@return               price of part or 0 if no price found.
	 *@exception Exception  soap exception
	 */
	public float getPrice(String partNumber, String manufacturer,
		WSDLObject wsdlObj) throws Exception {
		Vector params = new Vector();
		params.addElement(
			new Parameter("partNumber", String.class, partNumber, null));
		params.addElement(
			new Parameter("manufacturer", String.class, manufacturer, null));

		String methodName = "getPartPrice";
		Float f = (Float) makeCall(wsdlObj, params, methodName);

		if(f != null) {
			return f.floatValue();
		}

		return 0;
	}

	/**
	 * Stub for getPartDescription method offered by Grainger & Newark web
	 * services.
	 *
	 *@param partNumber     manufacturers part number
	 *@param manufacturer   name of manufacturer
	 *@param wsdlObj        WSDLObject with information about the web sevice
	 *@return               Description of the part or null
	 *@exception Exception  soap exception
	 */
	public String getDescription(String partNumber, String manufacturer,
		WSDLObject wsdlObj) throws Exception {
		Vector params = new Vector();
		params.addElement(
			new Parameter("partNumber", String.class, partNumber, null));
		params.addElement(
			new Parameter("manufacturer", String.class, manufacturer, null));

		String methodName = "getPartDescription";
		String s = (String) makeCall(wsdlObj, params, methodName);

		return s;
	}

	/**
	 *@param wsdlObj        WSDLObject with information about the web service
	 *@param params         parameters for the method
	 *@param methodName     name of the method being invoked
	 *@return               return Object
	 *@exception Exception  possible soap exception
	 */
	private Object makeCall(WSDLObject wsdlObj, Vector params,
		String methodName) throws Exception {
		// Build the call.

		Call call = new Call();
		call.setTargetObjectURI(wsdlObj.getTargetNameSpace());
		call.setMethodName(methodName);
		call.setEncodingStyleURI(wsdlObj.getEncodingStyle());
		call.setParams(params);

		// Invoke the call.
		Response resp = null;
		URL url = new URL(wsdlObj.getSoapLocation());
		resp = call.invoke(url, "");

		// Check the response.
		if(!resp.generatedFault()) {
			Parameter ret = resp.getReturnValue();
			Object value = ret.getValue();
			return value;
		} else {
			return null;
		}
	}
}


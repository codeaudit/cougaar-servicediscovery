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

package org.cougaar.servicediscovery.util;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *  Parses wsdl documents with soap bindings.
 *
 *@author     HSingh
 *@version    $Id: WSDLParser.java,v 1.5 2004-03-18 20:51:00 mthome Exp $
 */

public class WSDLParser extends DefaultHandler {

	public static void main(String argv[]) {
		if(argv.length != 1) {
			//System.err.println("Usage: WSDLParser [uri]");
			System.exit(1);
		}

		DefaultHandler handler = new WSDLParser();

		SAXParserFactory factory = SAXParserFactory.newInstance();
		try {
			// Parse the input
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(argv[0], handler);
		} catch(Throwable t) {
			t.printStackTrace();
		}
		System.exit(0);
	}


	private WSDLObject wsdlObj;


	public WSDLParser() { }

	public WSDLParser(String uri) {
		this.wsdlObj = new WSDLObject(uri);
	}

	public WSDLObject getWSDLObject() {
		return this.wsdlObj;
	}


	public void startDocument() throws SAXException { }

	public void endDocument() throws SAXException { }


	public void startElement(String namespaceURI,
		String sName,
		String qName,
		Attributes attrs) throws SAXException {

		String eName = sName;

		if("".equals(eName)) {
			eName = qName;
		}

		handleElement(eName, attrs);
	}


	public void endElement(String namespaceURI,
		String sName,
		String qName) throws SAXException { }


	public void characters(char buf[], int offset, int len) throws SAXException { }


	private void handleElement(String elementName, Attributes attrs) throws SAXException {
		if(elementName.equals("soap:operation")) {
			wsdlObj.addMethod(getAttributeValue("soapAction", attrs));
		} else if(elementName.equals("soap:address")) {
			wsdlObj.setSoapLocation(getAttributeValue("location", attrs));
			wsdlObj.hasSoapBinding(true);
		} else if(elementName.equals("soap:body")) {
			wsdlObj.setTargetNameSpace(getAttributeValue("namespace", attrs));
			wsdlObj.setEncodingStyle(getAttributeValue("encodingStyle", attrs));
		} else {
			;
		}
	}


	private String getAttributeValue(String AttribName, Attributes attrs) {
		if(attrs != null) {
			for(int i = 0; i < attrs.getLength(); i++) {
				if(attrs.getQName(i).equals(AttribName)) {
					return attrs.getValue(i);
				} else {
					;
				}
			}
			return null;
		} else {
			return null;
		}

	}


	/**
	 *  Parses wsdl file and returns a filled out WSDLObject.
	 *
	 *@param  locationUri  URI pointing to the wsdl file in interest.
	 *@return              WSDLObject
	 */
	public WSDLObject parse(String locationUri) {
		this.wsdlObj = new WSDLObject(locationUri);

		SAXParserFactory factory = SAXParserFactory.newInstance();

		try {
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(locationUri, this);

		} catch(Throwable t) {}

		return this.wsdlObj;
	}
}


/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
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
 
package org.cougaar.servicediscovery.servlet;

import javax.servlet.Servlet;

import org.cougaar.core.service.LoggingService;
import org.cougaar.core.servlet.SimpleServletComponent;
import org.cougaar.core.servlet.SimpleServletSupport;

/** 
 * <pre>
 * A special servlet component for the MatchMakerQueryServlet.
 *
 * Can't use SimpleServletComponent because we need additional services.
 * Specifically we need the LoggingService.
 *
 **/


public class SupplyAuxQueryServletComponent extends SimpleServletComponent {

    protected Servlet createServlet() {

	SupplyAuxQueryServlet saqServlet = new SupplyAuxQueryServlet();
	
	LoggingService logService = (LoggingService)
	    serviceBroker.getService(saqServlet,
				     LoggingService.class,
				     null);




	// create the support
	SimpleServletSupport support;
	try {
	    support = createSimpleServletSupport(saqServlet);
	} catch (Exception e) {
	    throw new RuntimeException(
				       "Unable to create Servlet support: "+
				       e.getMessage());
	}
	
	// set the support
	try {
	    saqServlet.setSimpleServletSupport(support);
	} catch (Exception e) {
	    throw new RuntimeException(
				       "Unable to set Servlet support: "+
				       e.getMessage());
	}

	// set the logging service
	try {
	    saqServlet.setLoggingService(logService);
	} catch (Exception e) {
	    throw new RuntimeException(
				       "Unable to set LoggingService: "+
				       e.getMessage());
	}

	return saqServlet;

    }

}


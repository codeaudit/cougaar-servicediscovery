/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
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
 
package org.cougaar.servicediscovery.servlet;

import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.servlet.SimpleServletSupport;
import org.cougaar.core.servlet.SimpleServletComponent;
import org.cougaar.planning.ldm.PlanningFactory;

import javax.servlet.Servlet;

/** 
 * <pre>
 * A special servlet component for the MatchMakerQueryServlet.
 *
 * Can't use SimpleServletComponent because we need additional services.
 * Specifically we need the LoggingService.
 *
 **/


public class MatchMakerQueryServletComponent extends SimpleServletComponent {

    protected Servlet createServlet() {

	MatchMakerQueryServlet mmServlet = new MatchMakerQueryServlet();
	
	LoggingService logService = (LoggingService)
	    serviceBroker.getService(mmServlet,
				     LoggingService.class,
				     null);


    DomainService domainService = (DomainService)
	    serviceBroker.getService(mmServlet,
				     DomainService.class,
				     null);



	// create the support
	SimpleServletSupport support;
	try {
	    support = createSimpleServletSupport(mmServlet);
	} catch (Exception e) {
	    throw new RuntimeException(
				       "Unable to create Servlet support: "+
				       e.getMessage());
	}
	
	// set the support
	try {
	    mmServlet.setSimpleServletSupport(support);
	} catch (Exception e) {
	    throw new RuntimeException(
				       "Unable to set Servlet support: "+
				       e.getMessage());
	}

	// set the logging service
	try {
	    mmServlet.setLoggingService(logService);
	} catch (Exception e) {
	    throw new RuntimeException(
				       "Unable to set LoggingService: "+
				       e.getMessage());
	}

    // set the Root Factory
	try {
    PlanningFactory ldmf = (PlanningFactory)
      domainService.getFactory("planning");
    mmServlet.setRootFactory(ldmf);
  } catch (Exception e) {
    throw new RuntimeException(
      "Unable to set LoggingService: "+
      e.getMessage());
  }

      return mmServlet;

    }

}


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

package org.cougaar.servicediscovery.servlet;

import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.servicediscovery.util.StatusChangeMessage;
import org.cougaar.util.UnaryPredicate;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;


public class ServiceDisruptionServlet extends BaseServletComponent implements BlackboardClient {

	private BlackboardService blackboard;
        private static String ROLE = "AircraftMaintenanceProvider";

	protected String getPath() {
		return "/serviceDisruption";
	}

	protected Servlet createServlet() {
		// get the blackboard service
		blackboard = (BlackboardService) serviceBroker.getService(this,
			BlackboardService.class, null);
		if(blackboard == null) {
			throw new RuntimeException(
				"Unable to obtain blackboard service");
		}

		return new MyServlet();
	}


	public void setBlackboardService(BlackboardService blackboard) {
		this.blackboard = blackboard;
	}


	// BlackboardClient method:
	public String getBlackboardClientName() {
		return toString();
	}

	// unused BlackboardClient method:
	public long currentTimeMillis() {
		return new Date().getTime();
	}

	// unused BlackboardClient method:
	public boolean triggerEvent(Object event) {
		return false;
	}

	public void unload() {
		super.unload();
		// release the blackboard service
		if(blackboard != null) {
			serviceBroker.releaseService(
				this, BlackboardService.class, servletService);
			blackboard = null;
		}
	}

	private class MyServlet extends HttpServlet {
		UnaryPredicate pred =
			new UnaryPredicate() {
				public boolean execute(Object o) {
					return o instanceof StatusChangeMessage;
				}
			};

		public void doGet(
			HttpServletRequest req,
			HttpServletResponse res) throws IOException {
			PrintWriter out = res.getWriter();
			out.println(headWithTitle("Service Disruption") + "<BODY><H1>");
                        boolean airMaintDisrupted = false;
			String action = "serviceDisruption";
			Collection col;
			try {
				blackboard.openTransaction();
				col = blackboard.query(pred);

				if(!col.isEmpty()) {
                                  Iterator it = col.iterator();
                                  while(it.hasNext()) {
                                    StatusChangeMessage m = (StatusChangeMessage) it.next();
                                    if(m.getRole().equals(ROLE)) {
                                      airMaintDisrupted = true;
                                    }
                                  }
				}
			} finally {
				blackboard.closeTransactionDontReset();
				out.println(ROLE +" service is disrupted: " + airMaintDisrupted + "</H1></body>");
				out.println("<FORM ACTION=" + action + " METHOD=POST>\n" +
					"  <CENTER><INPUT TYPE=\"SUBMIT\" NAME=\"TOGGLE\" VALUE=\"DISRUPT SERVICE\"></CENTER>\n" +
					"</FORM>");
				out.flush();
			}
		}

		public void doPost(
			HttpServletRequest req,
			HttpServletResponse res) throws IOException {
			PrintWriter out = res.getWriter();
			out.println(headWithTitle("Service Disruption") + "<BODY><H1>");
                        boolean airMaintDisrupted = false;
			String action = "serviceDisruption";
			Collection col;
			try {
				blackboard.openTransaction();
				col = blackboard.query(pred);

				if(col.isEmpty()) {
                                  StatusChangeMessage m = new StatusChangeMessage(ROLE, false);
                                  airMaintDisrupted = true;
                                  blackboard.publishAdd(m);
				} else {
                                  Iterator it = col.iterator();
                                   while(it.hasNext()) {
                                     StatusChangeMessage m = (StatusChangeMessage) it.next();
                                     if(m.getRole().equals(ROLE)) {
                                       airMaintDisrupted = true;
                                     }
                                   }
				}
			} finally {
				blackboard.closeTransactionDontReset();
				out.println(ROLE+" service is disrupted: " + airMaintDisrupted + "</H1></body></html>");
				out.println("<FORM ACTION=" + action + " METHOD=POST>\n" +
					"  <CENTER><INPUT TYPE=\"SUBMIT\" NAME=\"TOGGLE\" VALUE=\"DISRUPT SERVICE\"></CENTER>\n" +
					"</FORM>");
				out.flush();
			}
		}

		public void execute(
			HttpServletRequest req,
			HttpServletResponse res) throws IOException {
			PrintWriter out = res.getWriter();
			out.println(headWithTitle("Service Disruption") + "<BODY><H1>");
                        boolean airMaintDisrupted = false;

			Collection col;
			try {
                          //when does this ever get called?
				blackboard.openTransaction();
				col = blackboard.query(pred);

			} finally {
				blackboard.closeTransactionDontReset();
				out.println("</H1></body></html>");
				out.flush();
			}

		}
	}

	private String headWithTitle(String title) {
		return "<HEAD><TITLE>" + title + "</TITLE></HEAD>";
	}
}


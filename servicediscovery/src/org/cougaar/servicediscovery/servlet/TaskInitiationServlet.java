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

 /**
  * Access this servlet at http://hostname:port/$AGENT_NAME/TaskInitiation
  *
  * This servlet is intended to be used in the same agent as the
  * GenerateMaintainTaskPlugin, with the net result that sets of Maintain
  * tasks get generated when the user pushes the servlet button.
  *
  * This servlet expects that no one else in this agent is going to be
  * publishing or changing Switch objects. If someone else does publish or
  * change Switch objects, it will interfere with how this servlet works.
  *
  * If there is no Switch yet at this agent, bringing up this servlet
  * creates and publishes a Switch (with default state). Pushing the button
  * on the servlet will cause the Switch state to be set to false and a
  * publish change. Note that this servlet does not toggle the state of the
  * Switch away from false--it just keeps publishing changes to it.
  *
  * This may be misuse of the intended purpose of a Switch object.
  */

package org.cougaar.servicediscovery.servlet;

import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.servicediscovery.util.Switch;
import org.cougaar.util.UnaryPredicate;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Date;


/**
 */
public class TaskInitiationServlet extends BaseServletComponent implements BlackboardClient {

	private BlackboardService blackboard;
        private int numSets = 0;

	protected String getPath() {
		return "/taskInitiation";
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
					return o instanceof Switch;
				}
			};

		public void doGet(
			HttpServletRequest req,
			HttpServletResponse res) throws IOException {
			PrintWriter out = res.getWriter();
			out.println(headWithTitle("Task Initiation") + "<BODY><H1>");
			Switch sw = null;
			String action = "taskInitiation";
			Collection col;
			try {
				blackboard.openTransaction();
				col = blackboard.query(pred);

				if(col.isEmpty()) {
					sw = new Switch();
					blackboard.publishAdd(sw);
				} else {
					sw = (Switch) col.iterator().next();
				}
			} finally {
				blackboard.closeTransactionDontReset();
				out.println("Number of Task Sets Initiated: " + numSets + "</H1></body>");
				out.println("<FORM ACTION=" + action + " METHOD=POST>\n" +
					"  <CENTER><INPUT TYPE=\"SUBMIT\" NAME=\"TOGGLE\" VALUE=\"SEND TASKS\"></CENTER>\n" +
					"</FORM>");
				out.flush();
			}
		}

		public void doPost(
			HttpServletRequest req,
			HttpServletResponse res) throws IOException {
			PrintWriter out = res.getWriter();
			out.println(headWithTitle("Task Initiation") + "<BODY><H1>");
			Switch sw = null;
			String action = "taskInitiation";
			Collection col;
			try {
				blackboard.openTransaction();
				col = blackboard.query(pred);

				if(col.isEmpty()) {
					sw = new Switch();
					blackboard.publishAdd(sw);
                                        numSets = 0;
				} else {
					sw = (Switch) col.iterator().next();
                                        if(sw.getState())
                                          sw.toggle();
					blackboard.publishChange(sw);
                                        numSets++;
				}
			} finally {
				blackboard.closeTransactionDontReset();
				out.println("Number of Task Sets Initiated: " + numSets + "</H1></body></html>");
				out.println("<FORM ACTION=" + action + " METHOD=POST>\n" +
					"  <CENTER><INPUT TYPE=\"SUBMIT\" NAME=\"TOGGLE\" VALUE=\"SEND TASKS\"></CENTER>\n" +
					"</FORM>");
				out.flush();
			}
		}

		public void execute(
			HttpServletRequest req,
			HttpServletResponse res) throws IOException {
			PrintWriter out = res.getWriter();
			out.println(headWithTitle("Task Initiation") + "<BODY><H1>");
			Switch sw = null;

			Collection col;
			try {
				blackboard.openTransaction();
				col = blackboard.query(pred);

				if(col.isEmpty()) {
					sw = new Switch();
					blackboard.publishAdd(sw);
                                        numSets = 0;
				} else {
					sw = (Switch) col.iterator().next();
                                        if(sw.getState())
                                          sw.toggle();
					blackboard.publishChange(sw);
                                        numSets++;
				}
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


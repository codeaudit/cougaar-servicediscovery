/*
 *  <copyright>
 *   
 *   Copyright 2002-2004 BBNT Solutions, LLC
 *   under sponsorship of the Defense Advanced Research Projects
 *   Agency (DARPA).
 *  
 *   You can redistribute this software and/or modify it under the
 *   terms of the Cougaar Open Source License as published on the
 *   Cougaar Open Source Website (www.cougaar.org).
 *  
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *   A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *   OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *   SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *   LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *   DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *   THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *   (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *   OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *   
 *  </copyright>
 */
package org.cougaar.servicediscovery.servlet;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Date;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.servicediscovery.util.Switch;
import org.cougaar.util.UnaryPredicate;


/**
 * Servlet pubishes org.cougaar.servicediscovery.util.Switch object in the
 * agent's blackboard in an ON state. Provides toggle button to change the state
 * of the Switch object currently on the blackboard.<br>
 * http://&lt;hostname&gt;:&lt;port&gt;
 *
 * /$Agent/switchServlet
 *
 */
public class SwitchServlet extends BaseServletComponent implements BlackboardClient {

	private BlackboardService blackboard;

	/**
	 * Location of this servlet http://<hostname>:<port>
	 *
	 * /$Agent/switchServlet
	 *
	 *@return   The path value
	 */
	protected String getPath() {
		return "/switchServlet";
	}

	protected Servlet createServlet() {
		//Get blackboard service
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

	public String getBlackboardClientName() {
		return toString();
	}

	public long currentTimeMillis() {
		return new Date().getTime();
	}

	public boolean triggerEvent(Object event) {
		return false;
	}

	public void unload() {
		super.unload();
		//Release blackboard service
		if(blackboard != null) {
			serviceBroker.releaseService(
				this, BlackboardService.class, servletService);
			blackboard = null;
		}
	}

	/**
	 * Switch servlet.
	 *
	 */
	private class MyServlet extends HttpServlet {
		UnaryPredicate pred =
			new UnaryPredicate() {
				public boolean execute(Object o) {
					return o instanceof Switch;
				}
			};

		/**
		 * Returns the current state of the switch object on the blackboard. If the
		 * object does not exist adds it the blackboard
		 *
		 *@param req              Http request
		 *@param res              Http response
		 *@exception IOException  Possible Exception
		 */
		public void doGet(
			HttpServletRequest req,
			HttpServletResponse res) throws IOException {
			PrintWriter out = res.getWriter();
			out.println(headWithTitle("Switch Toggle") + "<BODY><H1>");
			Switch sw = null;
			String action = "switchServlet";
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
				out.println("Current State: " + sw.toString() + "</H1></body>");
				out.println("<FORM ACTION=" + action + " METHOD=POST>\n" +
					"  <CENTER><INPUT TYPE=\"SUBMIT\" VALUE=\"TOGGLE\"></CENTER>\n" +
					"</FORM>");
				out.flush();
			}
		}

		/**
		 * Toggles the state of the switch object.
		 *
		 *@param req              Http request
		 *@param res              Http response
		 *@exception IOException  Possible exception
		 */
		public void doPost(
			HttpServletRequest req,
			HttpServletResponse res) throws IOException {
			PrintWriter out = res.getWriter();
			out.println(headWithTitle("Switch Toggle") + "<BODY><H1>");
			Switch sw = null;
			String action = "switchServlet";
			Collection col;
			try {
				blackboard.openTransaction();
				col = blackboard.query(pred);

				if(col.isEmpty()) {
					sw = new Switch();
					blackboard.publishAdd(sw);
				} else {
					sw = (Switch) col.iterator().next();
					sw.toggle();
					blackboard.publishChange(sw);
				}
			} finally {
				blackboard.closeTransactionDontReset();
				out.println("Changed to: " + sw.toString() + "</H1></body></html>");
				out.println("<FORM ACTION=" + action + " METHOD=POST>\n" +
					"  <CENTER><INPUT TYPE=\"SUBMIT\" VALUE=\"TOGGLE\"></CENTER>\n" +
					"</FORM>");
				out.flush();
			}
		}
	}

	private String headWithTitle(String title) {
		return "<HEAD><TITLE>" + title + "</TITLE></HEAD>";
	}
}


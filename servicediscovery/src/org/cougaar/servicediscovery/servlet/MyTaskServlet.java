/*
    <copyright>
    Copyright 2002 BBNT Solutions, LLC
    under sponsorship of the Defense Advanced Research Projects Agency (DARPA)
    and the Defense Logistics Agency (DLA).
    This program is free software; you can redistribute it and/or modify
    it under the terms of the Cougaar Open Source License as published by
    DARPA on the Cougaar Open Source Website (www.cougaar.org).
    THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
    PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
    IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
    MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
    ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
    HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
    DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
    TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
    PERFORMANCE OF THE COUGAAR SOFTWARE.
    </copyright>
  */
package org.cougaar.servicediscovery.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

import javax.servlet.Servlet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.blackboard.BlackboardClient;


import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.DomainService;

import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.core.mts.MessageAddress;

import org.cougaar.glm.ldm.asset.ClassIXRepairPart;
import org.cougaar.glm.ldm.asset.Organization;

import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.CommunityPGImpl;
import org.cougaar.planning.ldm.asset.NewClusterPG;
import org.cougaar.planning.ldm.asset.NewItemIdentificationPG;
import org.cougaar.planning.ldm.asset.NewTypeIdentificationPG;
import org.cougaar.planning.ldm.asset.PropertyGroupSchedule;

import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.ldm.PlanningFactory;


/**
 * Servlet allocates supply task to specified agent.<br>
 * http://&lt;hostname&gt;:&lt;port&gt;/$Agent/taskServlet
 *
 *@author    HSingh
 *@version   $Id: MyTaskServlet.java,v 1.2 2003-01-22 17:30:44 mthome Exp $
 */
public class MyTaskServlet extends BaseServletComponent implements BlackboardClient {

	private BlackboardService blackboard;
	private PrintWriter out;
	private DomainService domainService = null;
	private static Calendar myCalendar = Calendar.getInstance();
	private final static String UTC = "Organization";
	private static long DEFAULT_START_TIME = -1;
	private static long DEFAULT_END_TIME = -1;
  private PlanningFactory planFactory;

	static {
		myCalendar.set(1990, 0, 1, 0, 0, 0);
		DEFAULT_START_TIME = myCalendar.getTime().getTime();

		myCalendar.set(2010, 0, 1, 0, 0, 0);
		DEFAULT_END_TIME = myCalendar.getTime().getTime();
	}

	public void setDomainService(DomainService aDomainService) {
		domainService = aDomainService;
	}

	public DomainService getDomainService() {
		return domainService;
	}

	protected String getPath() {
		return "/taskServlet";
	}

	protected Servlet createServlet() {
		blackboard = (BlackboardService) serviceBroker.getService(this,
			BlackboardService.class, null);
		if(blackboard == null) {
			throw new RuntimeException(
				"Unable to obtain blackboard service");
		}
    planFactory = (PlanningFactory) getDomainService().getFactory("planning");
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

	private class MyServlet extends HttpServlet {

		public void doGet(HttpServletRequest req, HttpServletResponse res)
			 throws IOException {

			out = res.getWriter();
			out.println(headWithTitle("Publish Task") + "<BODY>");

			String action = "taskServlet";

			out.println("<FORM ACTION=" + action + " METHOD=POST>\n");
			out.println("<table><tr><td>NSN:</td><td>" + getChoices()
				 + "</td></tr>");
			out.println("<tr><td>PROVIDER:</td><td><INPUT TYPE=TEXT NAME="
				 + "\"provider\"></td></tr></table>");
			out.println("<CENTER><INPUT TYPE=SUBMIT VALUE=\"PUBLISH\"></CENTER>"
				 + "</FORM></BODY>");
			out.flush();
		}

		public void doPost(HttpServletRequest req, HttpServletResponse res)
			 throws IOException {

			String nsn = req.getParameter("nsn");
			String provider = req.getParameter("provider");

			out = res.getWriter();
			out.println(headWithTitle("Task published") + "<BODY>");

			try {
				blackboard.openTransaction();
				publishTask(nsn, provider);
			} finally {
				blackboard.closeTransactionDontReset();
				out.println("<a href=taskServlet>back</a></body>");
				out.flush();
			}
		}
	}

	private String headWithTitle(String title) {
		return "<HEAD><TITLE>" + title + "</TITLE></HEAD>";
	}

	private void publishTask(String nsnS, String providerName) {
		Asset nsn = makePrototype(nsnS);

		NewTask task = planFactory.newTask();
		task.setDirectObject(nsn);
		task.setVerb(org.cougaar.glm.ldm.Constants.Verb.Supply);
    Plan realityPlan = planFactory.getRealityPlan();
		task.setPlan(realityPlan);

		AspectValue endTAV = TimeAspectValue.create(AspectType.END_TIME, DEFAULT_END_TIME);
		ScoringFunction endScoreFunc =
			ScoringFunction.createStrictlyAtValue(endTAV);
		Preference endPreference = planFactory.newPreference(AspectType.END_TIME, endScoreFunc);

		Vector preferenceVector = new Vector(1);
		preferenceVector.addElement(endPreference);
		task.setPreferences(preferenceVector.elements());
    blackboard.publishAdd(task);
    out.println("Published task : " + true);
		Organization org = createOrganization(providerName);

		int[] aspects = {AspectType.END_TIME};
		double[] results = {DEFAULT_END_TIME};
		boolean isSuccess = true;
		AllocationResult estAR = planFactory.newAllocationResult(0.5,
			isSuccess, aspects, results);

		Allocation allocation = planFactory.createAllocation(task.getPlan(), task,
			org, estAR, Role.ASSIGNED);

		out.println("Allocating task to: " + providerName + "<BR>");
		out.println("Direct Object:" +
			allocation.getTask().getDirectObject().getTypeIdentificationPG().getTypeIdentification() + "<BR>");
    blackboard.publishAdd(allocation);
		out.println("Published allocation: " + true + "<BR>");
	}

	private Asset makePrototype(String itemID) {

		Asset cix = planFactory.getPrototype(itemID);
		if(cix == null) {
			cix = planFactory.createPrototype(ClassIXRepairPart.class, itemID);
		}

		return cix;
	}

	private Organization createOrganization(String orgStr) {
		final String uic = orgStr;

		Organization org = (Organization) planFactory.createAsset("Organization");
		org.initRelationshipSchedule();
		org.setLocal(false);

		((NewTypeIdentificationPG) org.getTypeIdentificationPG()).setTypeIdentification(UTC);
		NewItemIdentificationPG itemIdProp =
			(NewItemIdentificationPG) org.getItemIdentificationPG();
		itemIdProp.setItemIdentification(uic);
		itemIdProp.setNomenclature(orgStr);
		itemIdProp.setAlternateItemIdentification(orgStr);

		NewClusterPG cpg = (NewClusterPG) org.getClusterPG();
		cpg.setMessageAddress(MessageAddress.getMessageAddress(orgStr));

		CommunityPGImpl communityPG =
			(CommunityPGImpl) planFactory.createPropertyGroup(CommunityPGImpl.class);
		PropertyGroupSchedule schedule = new PropertyGroupSchedule();

		ArrayList communities = new ArrayList(1);
		communities.add("COUGAAR");
		communityPG.setCommunities(communities);
		communityPG.setTimeSpan(DEFAULT_START_TIME, DEFAULT_END_TIME);
		schedule.add(communityPG);
		org.setCommunityPGSchedule(schedule);

		return org;
	}

	private String getChoices() {
		return "<select name=\"nsn\">" +
			"<option value=\"NSN/4710007606205\" selected>"
			 + "WUC1:NSN1:4710007606205 Metal tube assembly" +
			"<option value=\"NSN/4320012017527\">"
			 + "WUC1:NSN2:4320012017527 Rotary pump" +
			"<option value=\"NSN/5930008432366\">"
			 + "WUC1:NSN3:5930008432366 Pressure switch" +
			"<option value=\"NSN/1730007603370\">"
			 + "WUC2:NSN1:1730007603370 Ground safety pin" +
			"<option value=\"NSN/5945002010273\">"
			 + "WUC2:NSN2:5945002010273 Solid state switch" +
			"<option value=\"NSN/5930011951836\">"
			 + "WUC2:NSN3:5930011951836 Sensitive switch" +
			"<option value=\"NSN/4310004145989\">"
			 + "WUC3:NSN1:4310004145989 Reciprocating compressor" +
			"<option value=\"NSN/6105007262754\">"
			 + "WUC3:NSN2:6105007262754 AC Motor" +
			"<option value=\"NSN/3110005656233\">"
			 + "WUC3:NSN3:3110005656233 Rod end roller bearing" +
			"</select>";
	}
}


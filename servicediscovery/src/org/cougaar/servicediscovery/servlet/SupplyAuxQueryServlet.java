/*
 *
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.service.LoggingService;
import org.cougaar.core.servlet.SimpleServletSupport;
import org.cougaar.glm.ldm.Constants;
import org.cougaar.glm.ldm.asset.SupplyClassPG;
import org.cougaar.glm.plugins.AssetUtils;
import org.cougaar.glm.plugins.TaskUtils;
import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.TypeIdentificationPG;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.util.UnaryPredicate;

/**
 * A <code>Servlet</code>, loaded by the
 * <code>SimpleServletComponent</code>, that generates
 * an HTML view of SupplyAuxQuery data.
 *
 * @see org.cougaar.core.servlet.SimpleServletComponent
 */
public class SupplyAuxQueryServlet
        extends HttpServlet {
    private SimpleServletSupport support;
    private LoggingService logger;

    public void setSimpleServletSupport(SimpleServletSupport support) {
        this.support = support;
    }

    public void setLoggingService(LoggingService loggingService) {
        this.logger = loggingService;
    }

    public void doGet(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException, ServletException {
        // create a new "AuxQueryPrinter" context per request
        AuxQueryPrinter ct = new AuxQueryPrinter(support, logger, request, response);
        ct.execute();
    }


    public void doPost(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException, ServletException {
        // create a new "AuxQueryPrinter" context per request
        AuxQueryPrinter ct = new AuxQueryPrinter(support, logger, request, response);
        ct.execute();
    }

    /**
     * Inner-class to hold state and generate the response.
     */
    protected static class AuxQueryPrinter {

        // since "AuxQueryPrinter" is a static inner class, here
        // we hold onto the support API.
        //
        // this makes it clear that AuxQueryPrinter only uses
        // the "support" from the outer class.
        private SimpleServletSupport support;
        private HttpServletRequest request;
        private HttpServletResponse response;
        private LoggingService logger;

        private TaskUtils taskUtils;
        private AssetUtils assetUtils;

        // writer from the request for HTML output
        private PrintWriter out;

        public AuxQueryPrinter(SimpleServletSupport support,
                               LoggingService logger,
                               HttpServletRequest request,
                               HttpServletResponse response) {
            this.support = support;
            this.request = request;
            this.response = response;
            this.logger = logger;
            this.assetUtils = new AssetUtils();
            this.taskUtils = new TaskUtils();
        }

        public void execute() throws IOException, ServletException {
            this.out = response.getWriter();

            String viewType = request.getParameter("viewType");
            if ("viewSpecificTask".equals(viewType)) {
                // single selected task
                String taskUID = request.getParameter("UID");
                Task task = getSpecificTask(taskUID);
                printTaskAuxQueriesAsHTML(taskUID, task);
            } else if ("viewAllTasks".equals(viewType)) {
                // all
                Collection col = getAllSupplyTasks();
                printTasksAsHTML(col);
            } else if ("viewDefault".equals(viewType)) {
                // same as viewAll ?
                Collection col = getAllSupplyTasks();
                printTasksAsHTML(col);
            } else {
                // frame
                printFrame();
            }
        }

        protected void printFrame() {
            // generate outer frame page:
            //   top:    select "/agent"
            //   bottom: query frame
            out.print(
                    "<html><head><title>SupplyAuxQuery Data</title></head>"+
                    "<frameset rows=\"10%,90%\">\n"+
                    "<frame src=\""+
                    "/agents?format=select&suffix="+
                    support.getEncodedAgentName()+
                    "\" name=\"agentFrame\">\n"+
                    "<frame src=\"/$"+
                    support.getEncodedAgentName()+
                    support.getPath()+"?viewType=viewDefault"+
                    "\" name=\"queryFrame\">\n"+
                    "</frameset>\n"+
                    "<noframes>Please enable frame support</noframes>"+
                    "</html>\n");
        }

        private static String formatLabel(String lbl) {
            int nchars = lbl.length();
            if (nchars > 24) return lbl;
            return lbl + "                        ".substring(nchars);
        }

        private static String formatLongString(String str,
                                               int maxLength) {
            int nchars = str.length();
            int start = 0;
            String resultStr = "";
            while (start < nchars) {
                resultStr = (resultStr +
                        str.substring(start, Math.min(nchars, start + maxLength)) +
                        "\n");
                start = start + maxLength;
            }
            return resultStr;
        }

        private static String formatQueryStr(String queryStr) {
            return formatLongString(queryStr, 100);
        }

        private static String formatInteger(int n) {
            return formatInteger(n, 5);
        }

        private static String formatInteger(int n, int w) {
            String r = String.valueOf(n);
            return "        ".substring(0, w - r.length()) + r;
        }

        private static String formatFloat(float n) {
            return formatFloat(n, 6);
        }

        private static String formatFloat(float n, int w) {
            String r = String.valueOf(n);
            return "        ".substring(0, w - r.length()) + r;
        }

        private static String formatPercent(double percent) {
            return formatInteger((int) (percent * 100.0), 3) + "%";
        }


        private static String printResultCode(int i) {
            if (i == 1)
                return "Success:" + i;
            else
                return "Failure:" + i;
        }

        protected final static SupplyTaskPredicate SUPPLY_TASK_PREDICATE =
                new SupplyTaskPredicate();

        protected static class SupplyTaskPredicate implements UnaryPredicate {
            public boolean execute(Object o) {
                if (o instanceof Task) {
                    Task task = (Task) o;
                    return (task.getVerb().equals(Constants.Verb.SUPPLY));
                }
                return false;
            }
        }

        protected class SpecificTaskPredicate implements UnaryPredicate {
            private String taskUID;

            SpecificTaskPredicate(String aTaskUID) {
                super();
                this.taskUID = aTaskUID;
            }

            public boolean execute(Object o) {
                if (o instanceof Task) {
                    Task task = (Task) o;
                    return (taskUID.equals(task.getUID().toString()));
                }
                return false;
            }
        }


        protected Task getSpecificTask(String taskUID) {
            if (taskUID == null) {
                return null;
            }
            Collection col = support.queryBlackboard(new SpecificTaskPredicate(taskUID));
            if ((col == null) || (col.size() < 1)) {
                System.out.println("Null Collection returned from blackboard");
                return null;
            } else {
                if ((col.size() > 1) && (logger.isErrorEnabled())) {
                    logger.error("More than one Task at " + support.getEncodedAgentName() + " with UID:" + taskUID);
                }
                Iterator it = col.iterator();
                return ((Task) it.next());
            }
        }

        protected Collection getAllSupplyTasks() {
            Collection col = support.queryBlackboard(SUPPLY_TASK_PREDICATE);
            return col;
        }

        protected void printClusterSelectionComboBox(String title) {
            // javascript based on PlanViewServlet
            out.print(
                    "<html><head>"+
                    "<script language=\"JavaScript\">\n"+
                    "<!--\n"+
                    "function mySubmit() {\n"+
                    "  var obj = top.agentFrame.document.agent.name;\n"+
                    "  var encAgent = obj.value;\n"+
                    "  if (encAgent.charAt(0) == '.') {\n"+
                    "    alert(\"Please select an agent name\")\n"+
                    "    return false;\n"+
                    "  }\n"+
                    "  document.myForm.target=\"queryFrame\"\n"+
                    "  document.myForm.action=\"/$\"+encAgent+\""+
                    support.getPath()+"\"\n"+
                    "  return true\n"+
                    "}\n"+
                    "// -->\n"+
                    "</script>\n"+
                    "</head><body>\n"+
                    "<h2><center>"+
                    title+
                    " at "+
                    support.getEncodedAgentName()+
                    "</center></h2>\n"+
                    "<form name=\"myForm\" method=\"get\" "+
                    "onSubmit=\"return mySubmit()\">\n"+
                "<input type=hidden name=\"viewType\" value=\"");
            String viewType = request.getParameter("viewType");
            if (viewType != null) {
                out.print(viewType);
            }
            out.print(
                    "\">"+
                    "<input type=submit name=\"formSubmit\" value=\"Reload\">"+
                    "<br>\n</form>");
        }

        /**
         * Write the given <code>MatchMakerQueryRequest</code> as formatted HTML.
         */
        protected void printTaskAuxQueriesAsHTML(String taskUID, Task task) {
            printClusterSelectionComboBox(
                    "Auxilliary Queries on Task "+taskUID);
            if (task != null) {
                Collection col = extractAuxiliaryQueriesFromTask(task);
                printAuxQueriesAsHTMLTable(col);
            } else {
                out.print("<pre>No task with UID "+taskUID+"</pre>");
            }
            out.print(
                    "\n<a href=\""+
                    support.getPath()+
                    "?viewType=viewAllTasks");
            out.print("\" target=\"queryFrame\">View all Supply Tasks</a>\n");
            out.print("</body></html>");
            out.flush();
        }


        public static Collection extractAuxiliaryQueriesFromTask(Task task) {
            PlanElement pe = task.getPlanElement();
            if (pe != null) {
                AllocationResult result = pe.getReportedResult();
                if(result == null) {
                    result = pe.getEstimatedResult();
                }
                if (result != null) {
                    return extractAuxiliaryQueriesFromAR(task.getAuxiliaryQueryTypes(),
                                                         result);
                }
            }
            return null;
        }

        public static Collection
                extractAuxiliaryQueriesFromAR(int[] auxQueryTypes,
                                              AllocationResult ar) {
            ArrayList list = new ArrayList();
            if (ar != null) {
                for (int i = 0; i < auxQueryTypes.length; i++) {
                    String result = (String) ar.auxiliaryQuery(auxQueryTypes[i]);
                    if (result != null) {
                        list.add(result);
                    }
                }
            }
            return list;
        }




        protected void printTasksAsHTML(Collection col) {
            printClusterSelectionComboBox("Supply Tasks");
            if((col != null) && (col.size() > 0)) {
                printTasksAsHTMLTable(col);
            }
            else {
                out.print("&nbsp;&nbsp;<br>No Supply Tasks at this agent.</br>\n");
            }

            out.print("</body></html>");
            out.flush();
        }

        protected void printTasksAsHTMLTable(Collection col) {
            beginTasksHTMLTable("Supply Tasks", null);

            Iterator tasksIT = col.iterator();
            int ctr = 0;

            while (tasksIT.hasNext()) {

                Task task = (Task) tasksIT.next();
                String taskUID = task.getUID().toString();

                Asset asset = task.getDirectObject();
                // Check for aggregate assets and grab the prototype
                if (asset instanceof AggregateAsset) {
                    asset = ((AggregateAsset) asset).getAsset();
                }
                SupplyClassPG pg = (SupplyClassPG) asset.searchForPropertyGroup(SupplyClassPG.class);

                String supplyType="";
                if(pg!= null) {
                    supplyType = pg.getSupplyType();
                }

                double qty = taskUtils.getQuantity(task);

                String assetString =assetUtils.getAssetIdentifier(asset);
                TypeIdentificationPG typeID = asset.getTypeIdentificationPG();
                if(typeID != null) {
                    String nomenclature = typeID.getNomenclature();
                    if((nomenclature != null) && (!nomenclature.trim().equals(""))){
                          assetString = assetString + " (" + nomenclature + ")";
                    }
               }


                out.print("<tr align=center><td>" +
                          "\n<a href=\""+
                          support.getPath()+
                          "?viewType=viewSpecificTask&UID="+
                          taskUID + "\">" + taskUID + "</a>\n");
                out.print("</td><td align=left>");
                out.print(assetString);
                //out.print("</td><td>");
                //out.print(supplyType);
                out.print("</td><td>");
                out.print(qty);
                out.print("</td><td>");
                out.print(new Date(taskUtils.getStartTime(task)));
                out.print("</td><td>");
                out.print(new Date(taskUtils.getEndTime(task)));
                out.print("</td></tr>\n");
            }
            out.print("</table>\n<p>\n");
        }

        protected void printAuxQueriesAsHTMLTable(Collection col) {
            if (col != null) {
                if (col.size() < 1) {
                    out.print("&nbsp;&nbsp;<br>No Auxiliary Queries in the Result</br>");
                } else {
                    Iterator auxQueriesIT = col.iterator();
                    beginAuxQueriesHTMLTable("Auxiliary Queries", null);

                    while (auxQueriesIT.hasNext()) {
                        String aq = (String) auxQueriesIT.next();
                        //beginServiceDescriptionAsHTML(sdd);
                        printAuxQueryAsHTML(aq);
                    }
                    endHTMLTable();
                }
            } else {
                out.print("&nbsp;&nbsp;<br>No Allocation Result at this Time</br>");
            }
        }



        protected void beginTasksHTMLTable(
                String title, String subTitle) {
            out.print(
                    "<table border=1 cellpadding=3 cellspacing=1 width=\"100%\">\n" +
                    "<tr bgcolor=lightblue><th align=left colspan=5>");
            out.print(title);
            if (subTitle != null) {
                out.print(
                        "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<tt><i>");
                out.print(subTitle);
                out.print("</i></tt>");
            }
            out.print(
                    "</th></tr>\n" +
                    "<tr>" +
                    "<th>UID</th>" +
                    "<th>Direct Object NSN</th>" +
                    "<th>Qty</th>" +
                    "<th>Start Time</th>" +
                    "<th>End Time</th>" +
                    "</tr>\n");
        }


        /**
         * Begin a table of <tt>printQueryAsHTMLTable</tt> entries.
         */
        protected void beginAuxQueriesHTMLTable(
                String title, String subTitle) {
            out.print(
                    "<table border=1 cellpadding=3 cellspacing=1 width=\"100%\">\n" +
                    "<tr bgcolor=lightblue><th align=left colspan=1>");
            out.print(title);
            if (subTitle != null) {
                out.print(
                        "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp<tt><i>");
                out.print(subTitle);
                out.print("</i></tt>");
            }
            out.print("</th></tr>\n");
        }



        /**
         * End a table of <tt>printQueryAsHTMLTable & printResultAsHTMLTable</tt> entries.
         */
        protected void endHTMLTable() {
            out.print(
                    "</table>\n" +
                    "<p>\n");
        }


        /**
         * Write the given <code>Auxiliary Query</code> as formatted HTML.
         */
        protected void printAuxQueryAsHTML(String auxQuery) {
            out.print("<tr align=left><td>");
            out.print(auxQuery);
            out.print("</td></tr>\n");
        }
    }
}




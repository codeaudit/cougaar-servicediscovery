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

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.DecimalFormat;

import javax.servlet.*;
import javax.servlet.http.*;

import org.cougaar.core.blackboard.Subscription;
import org.cougaar.core.servlet.ServletUtil;
import org.cougaar.core.servlet.SimpleServletSupport;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.util.UID;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.planning.ldm.plan.*;

import org.cougaar.util.UnaryPredicate;

import org.cougaar.planning.servlet.data.Failure;
import org.cougaar.planning.servlet.data.xml.*;
import org.cougaar.planning.servlet.data.completion.*;

//These should be changed to logistics type in due time.
import org.cougaar.glm.ldm.Constants; //might have to be glm, not logistics version
import org.cougaar.glm.ldm.asset.SupplyClassPG;
import org.cougaar.glm.plugins.AssetUtils;
import org.cougaar.glm.plugins.TaskUtils;
import org.cougaar.servicediscovery.transaction.MMQueryRequest;
import org.cougaar.servicediscovery.transaction.NewMMQueryRequest;
import org.cougaar.servicediscovery.matchmaker.MatchMakerQuery;
import org.cougaar.servicediscovery.description.ScoredServiceDescription;
import org.cougaar.servicediscovery.description.ServiceClassification;
import org.cougaar.servicediscovery.description.ServiceBinding;

/**
 * A <code>Servlet</code>, loaded by the
 * <code>SimpleServletComponent</code>, that generates
 * HTML, XML, and serialized-Object views of Task completion
 * information.
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

        public static final int FORMAT_DATA = 0;
        public static final int FORMAT_XML = 1;
        public static final int FORMAT_HTML = 2;

        private boolean anyArgs;
        private int format;
        private boolean showTables;


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

        // base url
        private String baseURL;

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
            format = FORMAT_HTML;
        }

        public void execute() throws IOException, ServletException {
            boolean viewAllTasks = false;

            // save the absolute address
            this.baseURL =
                    request.getScheme() +
                    "://" +
                    request.getServerName() +
                    ":" +
                    request.getServerPort() +
                    "/";

            // create a URL parameter visitor
            ServletUtil.ParamVisitor vis =
                    new ServletUtil.ParamVisitor() {
                        public void setParam(String name, String value) {
                            if (eq("format", name)) {
                                anyArgs = true;
                                if (eq("data", value)) {
                                    format = FORMAT_DATA;
                                } else if (eq("xml", value)) {
                                    format = FORMAT_XML;
                                } else if (eq("html", value)) {
                                    format = FORMAT_HTML;
                                }
                                else if (eq("data", name)) {
                                    anyArgs = true;
                                    format = FORMAT_DATA;
                                } else if (eq("xml", name)) {
                                    anyArgs = true;
                                    format = FORMAT_XML;
                                } else if (eq("html", name)) {
                                    anyArgs = true;
                                    format = FORMAT_HTML;
                                }
                            }
                        }
                    };

            // visit the URL parameters
            ServletUtil.parseParams(vis, request);


            String viewType = request.getParameter("viewType");
            if ("viewSpecificTask".equals(viewType)) {
                String taskUID = request.getParameter("UID");

                Task task = getSpecificTask(taskUID);

                this.out = response.getWriter();
                printTaskAuxQueriesAsHTML(task);
                return;
            } else if ("viewAllTasks".equals(viewType)) {
                viewAllTasks = true;
            }


            // get result
            Collection col = getAllSupplyTasks();
            viewAllTasks = true;


            // write data
            try {
                if (format == FORMAT_HTML) {
                    // html
                    this.out = response.getWriter();
                    printTasksAsHTML(col);
                } else {
                    // unsupported
                    OutputStream out = response.getOutputStream();
                    if (format == FORMAT_DATA) {
                        // serialize
                        ObjectOutputStream oos = new ObjectOutputStream(out);
                        oos.writeObject(col);
                        oos.flush();
                    } else {
                        // xml
                        out.write(("<?xml version='1.0'?>\n").getBytes());
                        XMLWriter w =
                                new XMLWriter(
                                        new OutputStreamWriter(out));
                        //result.toXML(w);
                        w.flush();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
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

        // startsWithIgnoreCase
        private static final boolean eq(String a, String b) {
            return a.regionMatches(true, 0, b, 0, a.length());
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

        protected void printTasksVanillaHeader() {
            out.print(
                    "<html>\n" +
                    "<head>\n" +
                    "<title>");
            out.print(support.getEncodedAgentName());
            out.print(
                    "</title>" +
                    "</head>\n" +
                    "<body>" +
                    "<h2><center>Supply Tasks at ");
            out.print(support.getEncodedAgentName());
            out.print("</center></h2>\n");
        }


        protected void printTaskVanillaHeader(Task t) {
            out.print(
                    "<html>\n" +
                    "<head>\n" +
                    "<title>");
            out.print(support.getEncodedAgentName());
            out.print("</title>" +
                      "</head>\n" +
                      "<body>" +
                      "<h2><center>Auxilliary Queries on task " +
                      t.getUID().toString() + " at ");
            out.print(support.getEncodedAgentName());
            out.print("</center></h2>\n");
        }


        protected void printClusterSelectionComboBox() {
            // javascript based on PlanViewServlet
            out.print(
                    "<html>\n" +
                    "<script language=\"JavaScript\">\n" +
                    "<!--\n" +
                    "function mySubmit() {\n" +
                    "  var tidx = document.myForm.formCluster.selectedIndex\n" +
                    "  var cluster = document.myForm.formCluster.options[tidx].text\n" +
                    "  document.myForm.action=\"/$\"+cluster+\"");
            out.print(support.getPath());
            out.print("\"\n" +
                      "  return true\n" +
                      "}\n" +
                      "// -->\n" +
                      "</script>\n" +
                      "<head>\n" +
                      "<title>");
            out.print(support.getEncodedAgentName());
            out.print(
                    "</title>" +
                    "</head>\n" +
                    "<body>" +
                    "<h2><center>SupplyTaskAuxQuery at ");
            out.print(support.getEncodedAgentName());
            out.print(
                    "</center></h2>\n" +
                    "<form name=\"myForm\" method=\"get\" " +
                    "onSubmit=\"return mySubmit()\">\n" +
                    "MatchMakerQuery data at " +
                    "<select name=\"formCluster\">\n");
            // lookup all known cluster names
            List names = support.getAllEncodedAgentNames();
            int sz = names.size();
            for (int i = 0; i < sz; i++) {
                String n = (String) names.get(i);
                out.print("  <option ");
                if (n.equals(support.getEncodedAgentName())) {
                    out.print("selected ");
                }
                out.print("value=\"");
                out.print(n);
                out.print("\">");
                out.print(n);
                out.print("</option>\n");
            }
            out.print(
                    "</select>, \n" +
                    "<input type=\"submit\" name=\"formSubmit\" value=\"Reload\"><br>\n" +
                    "</form>\n");

        }

        /**
         * Write the given <code>MatchMakerQueryRequest</code> as formatted HTML.
         */
        protected void printTaskAuxQueriesAsHTML(Task task) {
            // printClusterSelectionComboBox();
            if (task != null) {
                printTaskVanillaHeader(task);
                Collection col = extractAuxiliaryQueriesFromTask(task);
                printAuxQueriesAsHTMLTable(col);
            } else {
                out.print("<pre>No task by that UID</b></pre>");
            }
            out.print("\n<a href=\"supplytask_auxquery?viewType=viewAllTasks");
            out.print("\">View all Supply Tasks</a>\n");
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
//printClusterSelectionComboBox();
            printTasksVanillaHeader();
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
                          "\n<a href=\"supplytask_auxquery?viewType=viewSpecificTask&UID=" + taskUID + "\">" + taskUID + "</a>\n");
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

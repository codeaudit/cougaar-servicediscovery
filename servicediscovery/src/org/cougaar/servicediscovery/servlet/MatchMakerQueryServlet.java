/*
 *
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
import org.cougaar.core.servlet.ServletUtil;
import org.cougaar.core.servlet.SimpleServletSupport;
import org.cougaar.planning.servlet.data.xml.XMLWriter;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.servicediscovery.description.MMRoleQuery;
import org.cougaar.servicediscovery.description.ScoredServiceDescription;
import org.cougaar.servicediscovery.description.ServiceClassification;
import org.cougaar.servicediscovery.matchmaker.MatchMakerQuery;
import org.cougaar.servicediscovery.transaction.MMQueryRequest;
import org.cougaar.servicediscovery.transaction.NewMMQueryRequest;
import org.cougaar.util.UnaryPredicate;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

/**
 * A <code>Servlet</code>, loaded by the
 * <code>SimpleServletComponent</code>, that generates
 * HTML, XML, and serialized-Object views of Task completion
 * information.
 *
 * @see org.cougaar.core.servlet.SimpleServletComponent
 */
public class MatchMakerQueryServlet
        extends HttpServlet {
    private SimpleServletSupport support;
    private LoggingService logger;
    private PlanningFactory ldmFactory;

    public void setSimpleServletSupport(SimpleServletSupport support) {
        this.support = support;
    }

    public void setLoggingService(LoggingService loggingService) {
        this.logger = loggingService;
    }

    public void setRootFactory(PlanningFactory rootFactory) {
        this.ldmFactory = rootFactory;
    }

    public void doGet(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException, ServletException {
        // create a new "MMQueryPrinter" context per request
        MMQueryPrinter ct = new MMQueryPrinter(support, ldmFactory, logger, request, response);
        ct.execute();
    }


    public void doPost(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException, ServletException {
        // create a new "MMQueryPrinter" context per request
        MMQueryPrinter ct = new MMQueryPrinter(support, ldmFactory, logger, request, response);
        ct.execute();
    }

    /**
     * Inner-class to hold state and generate the response.
     */
    protected static class MMQueryPrinter {

        public static final int FORMAT_DATA = 0;
        public static final int FORMAT_XML = 1;
        public static final int FORMAT_HTML = 2;

        private boolean anyArgs;
        private int format;



        // since "MMQueryPrinter" is a static inner class, here
        // we hold onto the support API.
        //
        // this makes it clear that MMQueryPrinter only uses
        // the "support" from the outer class.
        private SimpleServletSupport support;
        private HttpServletRequest request;
        private HttpServletResponse response;
        private LoggingService logger;
        private PlanningFactory ldmFactory;

        // writer from the request for HTML output
        private PrintWriter out;

        // base url
        private String baseURL;

        public MMQueryPrinter(SimpleServletSupport support,
                              PlanningFactory ldmFactory,
                              LoggingService logger,
                              HttpServletRequest request,
                              HttpServletResponse response) {
            this.support = support;
            this.request = request;
            this.response = response;
            this.ldmFactory = ldmFactory;
            this.logger = logger;
            format = FORMAT_HTML;
        }

        public void execute() throws IOException, ServletException {
            boolean viewAllQueries = false;

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
            if ("viewSpecificQuery".equals(viewType)) {
                String queryUID = request.getParameter("UID");


                MMQueryRequest result = getSpecificMMQueryRequest(queryUID);

                this.out = response.getWriter();
                printMatchMakerQueryAsHTML(result);
                return;
            } else if ("viewAllQueries".equals(viewType)) {
                viewAllQueries = true;
            }

            // get result
            Collection col = getMMQueryRequests();
            MMQueryRequest request = null;

            //MWD - to introduce default of printing entire result when only on MMQueryRequest
            //delete or comment out below
            //viewAllQueries = true;

            if (!viewAllQueries) {
                if (col.size() <= 1) {
                    if (col.size() == 1) {
                        request = (MMQueryRequest) col.iterator().next();
                    }
                } else {
                    viewAllQueries = true;
                }
            }

            // write data
            try {
                if (format == FORMAT_HTML) {
                    // html
                    this.out = response.getWriter();
                    if (viewAllQueries) {
                        printQueriesAsHTML(col);
                    } else {
                        printMatchMakerQueryAsHTML(request);
                    }
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

        protected static final UnaryPredicate MATCH_MAKER_REQUEST_PRED =
                new UnaryPredicate() {
                    public boolean execute(Object o) {
                        return (o instanceof NewMMQueryRequest);
                    }
                };

        protected class SpecificMatchMakerQuery implements UnaryPredicate {
            private String queryUID;

            SpecificMatchMakerQuery(String aQueryUID) {
                super();
                this.queryUID = aQueryUID;
            }

            public boolean execute(Object o) {
                if (o instanceof NewMMQueryRequest) {
                    MMQueryRequest q = (MMQueryRequest) o;
                    return (queryUID.equals(q.getUID().toString()));
                }
                return false;
            }
        }



        protected MMQueryRequest getSpecificMMQueryRequest(String queryUID) {
            Collection col = support.queryBlackboard(new SpecificMatchMakerQuery(queryUID));
            if ((col == null) || (col.size() < 1)) {
                System.out.println("Null Collection returned from blackboard");
                return null;
            } else {
                if ((col.size() > 1) && (logger.isErrorEnabled())) {
                    logger.error("More than one NewMMQueryRequest at " + support.getEncodedAgentName());
                }
                Iterator it = col.iterator();
                return ((MMQueryRequest) it.next());
            }
        }

        protected Collection getMMQueryRequests() {
            Collection col = support.queryBlackboard(MATCH_MAKER_REQUEST_PRED);
            return col;
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
                    "<h2><center>MatchMakerQuery at ");
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
        protected void printMatchMakerQueryAsHTML(MMQueryRequest result) {
            printClusterSelectionComboBox();
            printTableAsHTML(result);
            if (result != null) {
                out.print("\n<a href=\"matchmaker_query?viewType=viewAllQueries");
                out.print("\">View all Queries</a>\n");
            }
            out.print("</body></html>");
            out.flush();
        }


        protected void printTableAsHTML(MMQueryRequest request) {
            if (request != null) {
                if (request.getQuery() instanceof MatchMakerQuery) {
                    MatchMakerQuery query = (MatchMakerQuery) request.getQuery();
                    printQueryAsHTMLTable(query);
                } else if (request.getQuery() instanceof MMRoleQuery) {
                    MMRoleQuery query = (MMRoleQuery) request.getQuery();
                    printQueryAsHTMLTable(query);
                } else {
                    logger.error("Unknown query type:" + request.getQuery());
                }
                printResultAsHTMLTable(request);
            } else {
                out.print("<pre>No Match Maker query requests at this Agent</b></pre>");
            }
        }


        protected void printQueryAsHTMLTable(MatchMakerQuery query) {
            String queryStr = query.getQueryString();
            QueryToHTMLTranslator qt = QueryToHTMLTranslator.parseQueryStrToTranslator(queryStr);
            printQueryAsHTMLTable(qt);
        }

        protected void printQueryAsHTMLTable(MMRoleQuery query) {
            QueryToHTMLTranslator qt = QueryToHTMLTranslator.createTranslatorFromMMRoleQuery(query);
            printQueryAsHTMLTable(qt);
        }

        protected void printQueryAsHTMLTable(QueryToHTMLTranslator qt) {
            qt.setRootFactory(ldmFactory);
            out.print(qt.beginHTMLQueryTable("Query", null));
            out.print(qt.toHTMLQueryTableRow());
            out.print(qt.endHTMLQueryTable());
        }


        protected void printQueriesAsHTML(Collection col) {
            printClusterSelectionComboBox();
            printQueriesAsHTMLTable(col);
            out.print("</body></html>");
            out.flush();
        }

        protected void printQueriesAsHTMLTable(Collection col) {


            HashMap transMap = sortQueries(col);

            Collection transKeys = transMap.keySet();
            Iterator keyIT = transKeys.iterator();

            while (keyIT.hasNext()) {

                String aKey = (String) keyIT.next();

                Collection requests = (Collection) transMap.get(aKey);

                Iterator requestsIT = requests.iterator();
                int ctr = 0;

                while (requestsIT.hasNext()) {

                    MMQueryRequest mmqr = (MMQueryRequest) requestsIT.next();


                    QueryToHTMLTranslator qt = null;

                    if (mmqr.getQuery() instanceof MatchMakerQuery) {

                        MatchMakerQuery mmq = (MatchMakerQuery) mmqr.getQuery();

                        String queryStr = mmq.getQueryString();
                        qt = QueryToHTMLTranslator.parseQueryStrToTranslator(queryStr);
                    } else if (mmqr.getQuery() instanceof MMRoleQuery) {
                        MMRoleQuery mmrq = (MMRoleQuery) mmqr.getQuery();
                        qt = QueryToHTMLTranslator.createTranslatorFromMMRoleQuery(mmrq);
                    } else {
                        logger.error("Unknown query class type:" + mmqr.getQuery());
                    }


                    qt.setRootFactory(ldmFactory);
                    String queryUID = mmqr.getUID().toString();

                    if (qt != null) {
                        if (ctr == 0) {
                            out.print(qt.beginHTMLQueriesTable("Queries"));
                        }
                        out.print(qt.toHTMLQueriesTableRow(queryUID, mmqr));
                        ctr++;
                        if (ctr == requests.size()) {
                            out.print(qt.endHTMLQueriesTable());
                        }
                    } else {
                        System.out.println("Unparseable Query=" + mmqr.getQuery());
                    }

                }
            }
        }


        protected HashMap sortQueries(Collection col) {
            Iterator requestsIT = col.iterator();
            HashMap map = new HashMap();

            while (requestsIT.hasNext()) {

                MMQueryRequest mmqr = (MMQueryRequest) requestsIT.next();

                String queryType = null;

                if (mmqr.getQuery() instanceof MatchMakerQuery) {
                    MatchMakerQuery mmq = (MatchMakerQuery) mmqr.getQuery();
                    String queryStr = mmq.getQueryString();
                    queryType = QueryToHTMLTranslator.parseQueryStrToQueryType(queryStr);
                } else if (mmqr.getQuery() instanceof MMRoleQuery) {
                    queryType = QueryToHTMLTranslator.getQueryTypeForMMRoleQuery();
                } else {
                    logger.error("Unknown query type:" + mmqr.getQuery());
                }

                Vector requests = (Vector) map.get(queryType);

                if (requests == null) {
                    requests = new Vector();
                    map.put(queryType, requests);
                }
                requests.add(mmqr);
            }

            return map;
        }

        protected void printResultAsHTMLTable(MMQueryRequest request) {
            Collection scoredDescriptions = request.getResult();
            if (scoredDescriptions != null) {
                String statusString = null;
                if (scoredDescriptions.size() < 1) {
                    statusString = "Failed to find any providers";
                    //out.print("&nbsp;&nbsp;<br>Result Returned is empty</br>");
                }
                else if(scoredDescriptions.size() == 1) {
                    statusString = "1 provider successfully found";
                }
                else {
                    statusString = "" + scoredDescriptions.size() +
                            " providers successfully found";
                }
                beginResultHTMLTable("Results:", statusString);
                Iterator sdIT = scoredDescriptions.iterator();

                while (sdIT.hasNext()) {
                    ScoredServiceDescription sdd = (ScoredServiceDescription) sdIT.next();
                    beginServiceDescriptionAsHTML(sdd);
                    printServiceDescriptionAsHTML(sdd);
                }
                endHTMLTable();
            } else {
                out.print("&nbsp;&nbsp;<br>No Result at this Time</br>");
            }
        }

        /**
         * Begin a table of <tt>printQueryAsHTMLTable</tt> entries.
         */
        protected void beginQueryHTMLTable(
                String title, String subTitle, QueryToHTMLTranslator example) {
            out.print(
                    "<table border=1 cellpadding=3 cellspacing=1 width=\"100%\">\n" +
                    "<tr bgcolor=lightblue><th align=left colspan=3>");
            out.print(title);
            if (subTitle != null) {
                out.print(
                        "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<tt><i>");
                out.print(subTitle);
                out.print("</i></tt>");
            }
            out.print(example.beginHTMLQueryTable(title, subTitle));
        }

        protected void beginQueriesHTMLTable(
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
                    "<th>Echelon</th>" +
                    "<th>Class Code</th>" +
                    "<th>Class Scheme</th>" +
                    "<th>Has Result?</th>" +
                    "</tr>\n");
        }


        /**
         * Begin a table of <tt>printQueryAsHTMLTable</tt> entries.
         */
        protected void beginResultHTMLTable(
                String title, String subTitle) {
            out.print(
                    "<table border=1 cellpadding=3 cellspacing=1 width=\"100%\">\n" +
                    "<tr bgcolor=lightblue><th align=left colspan=3>");
            out.print(title);
            if (subTitle != null) {
                out.print(
                        "&nbsp;&nbsp;&nbsp;&nbsp;");
                out.print(subTitle);
                //out.print("</i></tt>");
            }
            out.print("</th></tr>\n");
        }


        protected void beginServiceDescriptionAsHTML(ScoredServiceDescription sdd) {

            out.print("<tr bgcolor=lightgrey> <td align=center colspan=3><b> Provider:");
            out.print(sdd.getProviderName());
            out.print("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp    Score=");
            out.print(formatFloat(sdd.getScore()));
            out.print("</b></td></tr>\n");
            out.print(
                    "<tr>" +
                    "<th>Scheme</th>" +
                    "<th>Code</th>" +
                    "<th>Name</th>" +
                    "</tr>\n");
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
         * Write the given <code>AbstractTask</code> as formatted HTML.
         */
        protected void printServiceDescriptionAsHTML(ScoredServiceDescription sdd) {
            Iterator scIT = sdd.getServiceClassifications().iterator();

            while (scIT.hasNext()) {
                ServiceClassification sc = (ServiceClassification) scIT.next();
                out.print("<tr align=left><td>");
                out.print(sc.getClassificationSchemeName());
                out.print("</td><td>");
                out.print(sc.getClassificationCode());
                out.print("</td><td>");
                out.print(sc.getClassificationName());
                out.print("</td></tr>\n");
            }


        }
    }
}

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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.service.LoggingService;
import org.cougaar.core.servlet.SimpleServletSupport;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.servicediscovery.description.MMQuery;
import org.cougaar.servicediscovery.description.ScoredServiceDescription;
import org.cougaar.servicediscovery.description.ServiceClassification;
import org.cougaar.servicediscovery.transaction.MMQueryRequest;
import org.cougaar.servicediscovery.transaction.NewMMQueryRequest;
import org.cougaar.util.UnaryPredicate;

/**
 * A <code>Servlet</code>, loaded by the
 * <code>SimpleServletComponent</code>, that generates
 * an HTML view of MatchMakerQuery data.
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
        }

        public void execute() throws IOException, ServletException {
            this.out = response.getWriter();

            String viewType = request.getParameter("viewType");
            if ("viewSpecificQuery".equals(viewType)) {
                // single query w/ matching UID
                String queryUID = request.getParameter("UID");
                MMQueryRequest result = getSpecificMMQueryRequest(queryUID);
                printMatchMakerQueryAsHTML(result);
            } else if ("viewAllQueries".equals(viewType)) {
                // all
                Collection col = getMMQueryRequests();
                printQueriesAsHTML(col);
            } else if ("viewDefault".equals(viewType)) {
                Collection col = getMMQueryRequests();
                if (col.isEmpty()) {
                    // none
                    printMatchMakerQueryAsHTML(null);
                } else if (col.size() == 1) {
                    // single query
                    MMQueryRequest request = 
                        (MMQueryRequest) col.iterator().next();
                    printMatchMakerQueryAsHTML(request);
                } else {
                    // all
                    printQueriesAsHTML(col);
                }
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
                    "<html><head>"+
                    "<title>MatchMakerQuery Data</title></head>"+
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

        private static String formatFloat(float n) {
            return formatFloat(n, 6);
        }

        private static String formatFloat(float n, int w) {
            String r = String.valueOf(n);
            return "        ".substring(0, w - r.length()) + r;
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
                    "<h2><center>MatchMakerQuery Data at "+
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
        protected void printMatchMakerQueryAsHTML(MMQueryRequest result) {
            printClusterSelectionComboBox();
            printTableAsHTML(result);
            if (result != null) {
                out.print(
                        "\n<a href=\""+
                        support.getPath()+
                        "?viewType=viewAllQueries\""+
                        " target=\"queryFrame\">"+
                        "View all Queries</a>\n");
            }
            out.print("</body></html>");
            out.flush();
        }


        protected void printTableAsHTML(MMQueryRequest request) {
            if (request != null) {
	        printQueryAsHTMLTable(request.getQuery());
                printResultAsHTMLTable(request);
            } else {
                out.print("<pre>No Match Maker query requests at this Agent</b></pre>");
            }
        }

        protected void printQueryAsHTMLTable(MMQuery query) {
            QueryToHTMLTranslator qt = QueryToHTMLTranslator.createTranslatorForQuery(query);
	    
	    if (qt == null) {
	      logger.error("Unknown query type:" + query);
	    } else {
	      printQueryAsHTMLTable(qt);
	    }
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

                ArrayList uids = new ArrayList();
                HashMap mmRequests = new HashMap();

                //sort the requests by UID
                while (requestsIT.hasNext()) {

                    MMQueryRequest mmqr = (MMQueryRequest) requestsIT.next();
                    String queryUID = mmqr.getUID().toString();

                    uids.add(queryUID);
                    mmRequests.put(queryUID,mmqr);
                }
                Collections.sort(uids);
                Collections.reverse(uids);

                Iterator uidIT = uids.iterator();

                while(uidIT.hasNext()) {
                    String queryUID = (String) uidIT.next();
                    MMQueryRequest mmqr = (MMQueryRequest) mmRequests.get(queryUID);

                    QueryToHTMLTranslator qt = 
		      QueryToHTMLTranslator.createTranslatorForQuery(mmqr.getQuery());
		    if (qt == null) {
                        logger.error("Unknown query class type:" + 
				     mmqr.getQuery());
			continue;
                    }

                    qt.setRootFactory(ldmFactory);

		    if (ctr == 0) {
		      out.print(qt.beginHTMLQueriesTable("Queries"));
		    }
		    out.print(qt.toHTMLQueriesTableRow(queryUID, mmqr));
		    ctr++;
		    if (ctr == requests.size()) {
		      out.print(qt.endHTMLQueriesTable());
		    }

                }
            }
        }


        protected HashMap sortQueries(Collection col) {
            Iterator requestsIT = col.iterator();
            HashMap map = new HashMap();

            while (requestsIT.hasNext()) {

                MMQueryRequest mmqr = (MMQueryRequest) requestsIT.next();

                String queryType = QueryToHTMLTranslator.getQueryType(mmqr.getQuery());
		if (queryType == null) {
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
                    "<th>Minimum Echelon</th>" +
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
            out.print("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp    Penalty Score=");
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



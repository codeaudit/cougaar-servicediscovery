/*
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

import org.cougaar.servicediscovery.util.UDDIConstants;
import org.cougaar.servicediscovery.matchmaker.MatchMakerQueryGenerator;
import org.cougaar.servicediscovery.transaction.MMQueryRequest;


/**
 * <pre>
 *
 * The ServiceByNameQueryTranslator is a concrete class that one can
 * translate a Service By Name query string into or out of.
 * The object contains the key bits of information that are
 * in the query.
 *
 *
 **/


public class ServiceByNameQueryTranslator extends QueryToHTMLTranslator {

    private String providerName;
    private String serviceName;

    public static String QUERY_TYPE = "ServiceByNameQuery";

    private static String QUERY_TITLE = "Service By Name Query";

    private static String START_OF_QUERY = "(object(_,'";
    private static String QUERY_STR_2 = "', ProviderObjId, ultralog), " +
            "attr(id(Service,ServiceSource),id('providedBy','http://www.daml.org/services/daml-s/0.7/Service.daml'),id(ProviderObjId, ultralog))," +
            "object(_,'";
    private static String QUERY_STR_3 = "', Service,ServiceSource))";

    private static String KEY_PART_OF_QUERY = KEY_PART_OF_SERVICE_BY_NAME_QUERY;

    public ServiceByNameQueryTranslator(String aProviderName,
                                        String aServiceName) {
        this.providerName = aProviderName;
        this.serviceName = aServiceName;
    }


    public String getProviderName() {
        return providerName;
    }

    public String getServiceName() {
        return serviceName;
    }


    public String toQueryString() {
        return
                MatchMakerQueryGenerator.queryForServiceByName(getProviderName(),
                                                               getServiceName());
    }

    public String toString() {
        return ("ServiceByNameQueryTranslator - provider=" + providerName +
                " serviceName=" + serviceName);
    }

    public String queryType() {
        return QUERY_TYPE;
    }

    public String queryTitle() {
        return QUERY_TITLE;
    }

    public String getHTMLQueryText() {
        return (" Find provider named " + getProviderName());
    }

    public String beginHTMLQueryTable(String title, String subTitle) {
        String retStr = ("<table border=1 cellpadding=3 cellspacing=1 width=\"100%\">\n" +
                "<tr bgcolor=lightblue><th align=left colspan=1>");
        retStr = retStr + title;
        if (subTitle != null) {
            retStr = retStr + ("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<tt><i>" +
                    subTitle + "</i></tt>");
        }

        return retStr;

        /** MWD Remove
        return (retStr + "</th></tr>\n" +
                "<tr>" +
                "<th>Provider Name</th>" +
                "<th>Service Name</th>" +
                "</tr>\n");
         **/
    }

    public String toHTMLQueryTableRow() {
        return ("<tr align=left><td>" +
                getHTMLQueryText() + "</td></tr>\n");
    }

    public String endHTMLQueryTable() {
        return ("</table>\n" +
                "<p>\n");
    }

    public String beginHTMLQueriesTable(String title, String subTitle) {
        String retStr = ("<table border=1 cellpadding=3 cellspacing=1 width=\"100%\">\n" +
                "<tr bgcolor=lightblue><th align=left colspan=3>");
        retStr = retStr + title;
        if (subTitle != null) {
            retStr = retStr + ("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<tt><i>" +
                    subTitle + "</i></tt>");
        }
        return (retStr +
                "</th></tr>\n" +
                "<tr>" +
                "<th>UID</th>" +
                "<th>Query</th>" +
                "<th>Has Result?</th>" +
                "</tr>\n");
    }

    public String toHTMLQueriesTableRow(String queryUID, MMQueryRequest mmqr) {
        String retStr = ("<tr align=center><td>" +
                "\n<a href=\"matchmaker_query?viewType=viewSpecificQuery&queryType=" + queryType() + "&UID=" + queryUID +
                "\">" + queryUID + "</a>\n");
        retStr = retStr + ("</td><td align=left>" + getHTMLQueryText() + "</td><td>");
        if (mmqr.getResult() == null) {
            retStr += "No";
        } else {
            retStr += "Yes";
        }
        return (retStr + "</td></tr>\n");
    }

    public String endHTMLQueriesTable() {
        return ("</table>\n" +
                "<p>\n");
    }


    public static ServiceByNameQueryTranslator parseQueryString(String queryString) {
        String aProvider, aService;
        int start,end;
        if (queryString.indexOf(KEY_PART_OF_QUERY) != -1) {
            start = START_OF_QUERY.length();
            end = queryString.indexOf(QUERY_STR_2);
            aProvider = queryString.substring(start, end);
            start = end + QUERY_STR_2.length();
            queryString = queryString.substring(start);
            end = queryString.indexOf(QUERY_STR_3);
            aService = queryString.substring(0, end);
            return new ServiceByNameQueryTranslator(aProvider, aService);
        }
        return null;
    }


    private static String TRIAL_QUERY =
            MatchMakerQueryGenerator.queryForServiceByName("Grainger", "Grainger Service");

    public static void main(String[] args) {
        ServiceByNameQueryTranslator dnsnq = ServiceByNameQueryTranslator.parseQueryString(TRIAL_QUERY);
        boolean doesEqual = TRIAL_QUERY.equals(dnsnq.toQueryString());
        System.out.println(" The " + dnsnq + " doesEqual: " + doesEqual);
        if (!doesEqual) {
            printDiff(TRIAL_QUERY, dnsnq.toQueryString());
        }

    }

}



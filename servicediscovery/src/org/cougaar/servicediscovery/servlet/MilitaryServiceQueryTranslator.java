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

import org.cougaar.servicediscovery.description.MMRoleQuery;
import org.cougaar.servicediscovery.transaction.MMQueryRequest;
import org.cougaar.servicediscovery.util.UDDIConstants;


/**
 * <pre>
 *
 * The MilitaryServiceQueryTranslator is a concrete class that one can
 * translate a Military Entity query string into or out of.
 * The object contains the key bits of information that are
 * in the query.
 *
 *
 **/


public class MilitaryServiceQueryTranslator extends QueryToHTMLTranslator {

    private String clientName;
    private String echelon;
    private String classCode;
    private String classScheme;
    private float lineageRelaxationPenalty;

    public static String QUERY_TYPE = "MilitaryServiceQuery";
    private static String QUERY_TITLE = "Military Service Query";


    public MilitaryServiceQueryTranslator(String aClientName,
                                          String anEchelon,
                                          String aClassCode,
                                          String aClassScheme,
                                          float relaxationPenalty) {
        clientName = aClientName;
        echelon = anEchelon;
        classCode = aClassCode;
        classScheme = aClassScheme;
        lineageRelaxationPenalty = relaxationPenalty;
    }


    public String getClientName() {
        return clientName;
    }

    public String getEchelon() {
        return echelon;
    }

    public String getClassCode() {
        return classCode;
    }

    public String getClassScheme() {
        return classScheme;
    }

    public float getLineageRelaxationPenalty() {
        return lineageRelaxationPenalty;
    }

    public String toQueryString() {
        return "";
    }

    public String toString() {
        return ("MilitaryServiceQueryTranslator - clientName=" + clientName +
                " echelon=" + echelon + " classCode=" + classCode +
                " classScheme=" + classScheme + " lineageRelaxationPenalty=" +
                lineageRelaxationPenalty);
    }

    public String queryType() {
        return QUERY_TYPE;
    }
    public String queryTitle() {
        return QUERY_TITLE;
    }

    public String beginHTMLQueryTable(String title, String subTitle) {
        String retStr = ("<table border=1 cellpadding=3 cellspacing=1 width=\"100%\">\n" +
                "<tr bgcolor=lightblue><th align=left colspan=1>");
        retStr = retStr + title;
        if (subTitle != null) {
            retStr = retStr + ("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp<tt><i>" +
                    subTitle + "</i></tt>");
        }

        return retStr;

    }

    public String getHTMLQueryText() {
        return ("Find " + getEchelon() + "-level of type " +
                getClassCode() + " (using " +
                getClassScheme() + ")");
    }

    public String toHTMLQueryTableRow() {
        return ("<tr align=left><td>" +
                getHTMLQueryText() +
                "</td></tr>\n");
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
                "\n<a href=\"matchmaker_query?viewType=viewSpecificQuery&queryType=" +
                queryType() + "&UID=" + queryUID +
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


    public static MilitaryServiceQueryTranslator
                createFromMMRoleQuery(MMRoleQuery rq){
        return new MilitaryServiceQueryTranslator(null,rq.getEchelon(),
                                                  rq.getRole().getName(),
                                                  UDDIConstants.MILITARY_SERVICE_SCHEME,
                                                  10);

    }


}



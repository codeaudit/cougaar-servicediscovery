/*
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
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.q
 * </copyright>
 */

package org.cougaar.servicediscovery.servlet;

import org.cougaar.servicediscovery.util.UDDIConstants;
import org.cougaar.servicediscovery.matchmaker.MatchMakerQueryGenerator;
import org.cougaar.servicediscovery.transaction.MMQueryRequest;
import org.cougaar.servicediscovery.description.MMRoleQuery;


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

    private static String START_OF_QUERY = START_OF_MILITARY_SERVICE_QUERY;
    private static String QUERY_STR_2 = "'')',ultralog),id('SupportedBy',ultralog),Support)::widen(1,";

    private static String QUERY_STR_3 = ",_X),isa(id('";
    private static String QUERY_STR_4 = "','" + UDDIConstants.MILITARY_ECHELON_SCHEME + "'),id('";
    private static String QUERY_STR_5 = "','" + UDDIConstants.MILITARY_ECHELON_SCHEME + "'))::widen(1,RealEch)," + "attr(Support,id('" + UDDIConstants.MILITARY_ECHELON_SCHEME + "',ultralog),RealEch)," + "isa(Support, id('";
    private static String QUERY_STR_6 = "', '";
    private static String QUERY_STR_7 = "')),";


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
        return MatchMakerQueryGenerator.militaryServiceQuery(clientName,
                                                             echelon,
                                                             classCode,
                                                             classScheme,
                                                             lineageRelaxationPenalty);
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

        /**  MWD Remove
        return (retStr + "</th></tr>\n" +
                "<tr>" +
                "<th>Echelon</th>" +
                "<th>Class Code</th>" +
                "<th>Class Scheme</th>" +
                "</tr>\n");

         ***/
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


    public static MilitaryServiceQueryTranslator parseQueryString(String queryString) {
        String aClientName,anEchelon,aClassCode,aClassScheme,relaxationPenaltyStr;
        float relaxationPenalty;
        int start,end;
        if (queryString.startsWith(START_OF_QUERY)) {
            start = START_OF_QUERY.length();
            end = queryString.indexOf(QUERY_STR_2);
            aClientName = queryString.substring(start, end);
            start = end + QUERY_STR_2.length();
            queryString = queryString.substring(start);
            end = queryString.indexOf(QUERY_STR_3);
            relaxationPenaltyStr = queryString.substring(0, end);
            relaxationPenalty = Float.parseFloat(relaxationPenaltyStr);
            start = end + QUERY_STR_3.length();
            end = queryString.indexOf(QUERY_STR_4);
            anEchelon = queryString.substring(start, end);
            start = end + QUERY_STR_4.length() + anEchelon.length() + QUERY_STR_5.length();
            queryString = queryString.substring(start);
            end = queryString.indexOf(QUERY_STR_6);
            aClassCode = queryString.substring(0, end);
            start = end + QUERY_STR_6.length();
            end = queryString.indexOf(QUERY_STR_7);
            aClassScheme = queryString.substring(start, end);

            return new MilitaryServiceQueryTranslator(aClientName,
                                                      anEchelon,
                                                      aClassCode,
                                                      aClassScheme,
                                                      relaxationPenalty);

        }
        return null;
    }


    public static MilitaryServiceQueryTranslator
                createFromMMRoleQuery(MMRoleQuery rq){
        return new MilitaryServiceQueryTranslator(null,rq.getEchelon(),
                                                  rq.getRole().getName(),
                                                  UDDIConstants.MILITARY_SERVICE_SCHEME,
                                                  10);

    }

    private static String TRIAL_QUERY = "(attr(id('mil_entity(''1-36-INFBN'')',ultralog),id('SupportedBy',ultralog),Support)::widen(1,10.0,_X),isa(id('BRIGADE','MilitaryEchelonScheme'),id('BRIGADE','MilitaryEchelonScheme'))::widen(1,RealEch),attr(Support,id('MilitaryEchelonScheme',ultralog),RealEch),isa(Support, id('SparePartsProvider', 'M ilitaryServiceScheme')),attr(ProfileObj, id(serviceCategory,'http://www.daml.org/services/daml-s/0.7 /Profile.daml'), Support),attr(id(Service, _), id(presents,'http://www.daml.org/services/daml-s/0.7/ Service.daml'), ProfileObj))";

    public static void main(String[] args) {
        MilitaryServiceQueryTranslator msq = MilitaryServiceQueryTranslator.parseQueryString(TRIAL_QUERY);
        boolean doesEqual = TRIAL_QUERY.equals(msq.toQueryString());
        System.out.println(" The " + msq + " doesEqual: " + doesEqual);
        if (!doesEqual) {
            printDiff(TRIAL_QUERY, msq.toQueryString());
        }

    }

}



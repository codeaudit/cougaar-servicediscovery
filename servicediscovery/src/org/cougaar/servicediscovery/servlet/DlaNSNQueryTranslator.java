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

import org.cougaar.servicediscovery.matchmaker.MatchMakerQueryGenerator;
import org.cougaar.servicediscovery.transaction.MMQueryRequest;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.TypeIdentificationPG;


/**
 * <pre>
 *
 * The DlaNSNQueryTranslator is a concrete class that one can
 * translate a DLA NSN query string into or out of.
 * The object contains the key bits of information that are
 * in the query.
 *
 *
 **/


public class DlaNSNQueryTranslator extends QueryToHTMLTranslator {

    private String nsn;

    public static String QUERY_TYPE = "DlaNSNQuery";

    private static String QUERY_TITLE = "DLA NSN Query";

    private static String START_OF_QUERY = "(attr(id('";
    private static String QUERY_STR_2 = "', ultralog), id('hasNAICSCategory',ultralog), NaicsCat),"+"(isa(PartProvCat,NaicsCat);" +
	"attr(NaicsCat,id('coveredBy',ultralog),DistNaicsCat)," +
	"isa(PartProvCat,DistNaicsCat)" +
       "),";


    private static String KEY_PART_OF_QUERY = KEY_PART_OF_DLA_NSN_QUERY;


    public DlaNSNQueryTranslator(String aNSN) {
        nsn = aNSN;
    }


    public String getNSN() {
        if(nsn.startsWith("NSN/")) {
            return nsn;
        }
        else return "NSN/" + nsn;
    }


    public String toQueryString() {
        return MatchMakerQueryGenerator.dlaNsnQuery(nsn);
    }

    public String toString() {
        return ("DlaNSNQueryTranslator - nsn=" + getNSN());
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

        /** MWD remove
        return (retStr + "</th></tr>\n" +
                "<tr>" +
                "<th>NSN</th>" +
                "</tr>\n");

         **/
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
                "\n<a href=\"matchmaker_query?viewType=viewSpecificQuery&queryType=" + queryType() + "&UID=" + queryUID +
                "\">" + queryUID + "</a>\n");
        retStr = retStr + ("</td><td>" + getHTMLQueryText() +
                "</td><td>");
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

    private String getNomenclature(String itemID) {

        Asset asset = ldmFactory.getPrototype(itemID);
        if(asset != null){
            TypeIdentificationPG typeID = asset.getTypeIdentificationPG();
            if(typeID != null) {
                //String assetName = typeID.getTypeIdentification();
                String nomenclature = typeID.getNomenclature();
                return nomenclature;
            }
        }
        else {
            System.out.println("Returned null asset for item ID:" + itemID);
        }
        return null;
    }

    public String getHTMLQueryText() {
        String retString = " Find parts provider for " + getNSN();
        String nomenclature = getNomenclature(getNSN());
        if(nomenclature != null) {
            retString = retString + " (" +
                    nomenclature + ")";
        }
        return retString;
    }


    public static DlaNSNQueryTranslator parseQueryString(String queryString) {
        String aNSN;
        int start,end;
        if (queryString.indexOf(KEY_PART_OF_QUERY) != -1) {
            start = START_OF_QUERY.length();
            end = queryString.indexOf(QUERY_STR_2);
            aNSN = queryString.substring(start, end);
            return new DlaNSNQueryTranslator(aNSN);
        }
        return null;
    }



    private static String TRIAL_QUERY =
	MatchMakerQueryGenerator.dlaNsnQuery("1234567891011");

    public static void main(String[] args) {
        DlaNSNQueryTranslator dnsnq = DlaNSNQueryTranslator.parseQueryString(TRIAL_QUERY);
        boolean doesEqual = TRIAL_QUERY.equals(dnsnq.toQueryString());
        System.out.println(" The " + dnsnq + " doesEqual: " + doesEqual);
        if (!doesEqual) {
            printDiff(TRIAL_QUERY, dnsnq.toQueryString());
        }

    }

}



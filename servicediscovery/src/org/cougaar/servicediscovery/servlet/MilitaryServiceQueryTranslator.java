/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.servicediscovery.servlet;

import org.cougaar.servicediscovery.description.LineageEchelonScorer;
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
    private String minimumEchelon;
    private String classCode;
    private String classScheme;
    private float lineageRelaxationPenalty;

    public static String QUERY_TYPE = "MilitaryServiceQuery";
    private static String QUERY_TITLE = "Military Service Query";


    public MilitaryServiceQueryTranslator(String aClientName,
                                          String anMinimumEchelon,
                                          String aClassCode,
                                          String aClassScheme,
                                          float relaxationPenalty) {
        clientName = aClientName;
        minimumEchelon = anMinimumEchelon;
        classCode = aClassCode;
        classScheme = aClassScheme;
        lineageRelaxationPenalty = relaxationPenalty;
    }


    public String getClientName() {
        return clientName;
    }

    public String getMinimumEchelon() {
        return minimumEchelon;
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
                " minimumEchelon=" + minimumEchelon + " classCode=" + classCode +
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
        return ("Find " + getMinimumEchelon() + "-level or higher of type " +
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
	String minimumEchelon = "";
	if (rq.getServiceInfoScorer() instanceof LineageEchelonScorer) {
	  minimumEchelon = 
	    ((LineageEchelonScorer) rq.getServiceInfoScorer()).getMinimumEchelon();
	}
        return new MilitaryServiceQueryTranslator(null, minimumEchelon,
                                                  rq.getRole().getName(),
                                                  UDDIConstants.MILITARY_SERVICE_SCHEME,
                                                  10);

    }


}



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

import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.servicediscovery.description.MMQuery;
import org.cougaar.servicediscovery.description.MMRoleQuery;
import org.cougaar.servicediscovery.transaction.MMQueryRequest;

/**
 * <pre>
 *
 * The QueryToHTMLTranslator abstract class defines the method api
 * for translators for each type of query that will be displayed
 * in the MatchMakerQueryServlet.
 *
 *
 **/


public abstract class QueryToHTMLTranslator {

    protected PlanningFactory ldmFactory=null;

    public abstract String toQueryString();

    public abstract String toString();

    public abstract String queryType();

    public abstract String queryTitle();


    public abstract String beginHTMLQueryTable(String title, String subTitle);

    public abstract String toHTMLQueryTableRow();

    public abstract String endHTMLQueryTable();

    public abstract String beginHTMLQueriesTable(String title, String subTitle);

    public abstract String toHTMLQueriesTableRow(String queryUID, MMQueryRequest mmqr);

    public abstract String endHTMLQueriesTable();

    public void setRootFactory(PlanningFactory ldmFactory) {
        this.ldmFactory = ldmFactory;
    }


    public static QueryToHTMLTranslator createTranslatorForQuery(MMQuery mmq) {
      if (mmq instanceof MMRoleQuery) {
	return MilitaryServiceQueryTranslator.createFromMMRoleQuery((MMRoleQuery) mmq);
      } else {
	return null;
      }
      
    }

    public static String getQueryType(MMQuery mmq) {
      if (mmq instanceof MMRoleQuery) {
        return MilitaryServiceQueryTranslator.QUERY_TYPE;
      } else {
	return null;
      }
    }

    public String beginHTMLQueriesTable(String title) {
        return beginHTMLQueriesTable(title,"Type: " + queryTitle());
    }



    public static void printDiff(String str1, String str2) {
        int ctr = 0;
        boolean stillEqual = true;
        while (stillEqual) {
            stillEqual = (str1.charAt(ctr) == str2.charAt(ctr));
            ctr++;
        }
        if (ctr < str1.length()) {
            System.out.println("Rest of String 1: " + str1.substring(ctr));
            System.out.println("Rest of String 2: " + str2.substring(ctr));
        }

    }

}









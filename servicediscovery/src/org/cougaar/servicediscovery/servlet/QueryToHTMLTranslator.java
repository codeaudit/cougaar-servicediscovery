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









/*
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
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

import org.cougaar.core.service.LoggingService;
import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.util.ConfigFinder;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class CougaarDAMLServlet extends BaseServletComponent {

  private LoggingService log;
  protected StringBuffer damlFile = null;

  protected String getPath() {
    return "/cougaar.daml";
  }

  protected Servlet createServlet() {
    damlFile = new StringBuffer();

    // get the logging service
    log = (LoggingService) serviceBroker.getService(this, LoggingService.class, null);
    if( log == null ) {
      throw new RuntimeException("Cannot find logging service!");
    }

    // Load my daml file and store it on my blackboard.
    try {
      InputStream  fileStream = ConfigFinder.getInstance().open("cougaar.daml");
      if (fileStream != null) {
        if(log.isDebugEnabled()) {
          log.debug("Located cougaar daml");
        }
        BufferedReader input = new BufferedReader(new InputStreamReader(fileStream));
        for(String str = input.readLine(); str != null; str = input.readLine()) {
          damlFile.append(str + "\n");
        }
      } else {
        if(log.isErrorEnabled()) {
          log.error("Cannot locate DAML profile file: cougaar.daml");
        }
        throw new RuntimeException("Error: cannot locate daml cougaar.daml");
      }
    } catch( Exception e) {
      if(log.isErrorEnabled()) {
        log.error("Exception reading profile", e);
      }
    }
    return new MyServlet();
  }

  private class MyServlet extends HttpServlet {
     public void doGet(
         HttpServletRequest req,
         HttpServletResponse res) throws IOException {
       Worker worker = new Worker(res);
       worker.execute(damlFile.toString());
     }
   }

  private static class Worker {

    private HttpServletResponse response;

    public Worker(HttpServletResponse response) {
      this.response = response;
    }

    public void execute(String file) throws IOException {
      writeResponse(file);
    }

    private void writeResponse(String file) throws IOException {
      response.setContentType("text/html");
      PrintWriter out = response.getWriter();
      out.println(file);
    }
  }
}

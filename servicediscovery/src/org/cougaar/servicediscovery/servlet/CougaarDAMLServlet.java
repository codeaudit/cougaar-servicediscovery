/*
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.service.LoggingService;
import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.util.ConfigFinder;

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

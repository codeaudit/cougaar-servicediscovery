/*
 * <copyright>
 *  Copyright 2002 BBNT Solutions, LLC
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

import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.core.service.LoggingService;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.Set;
import java.util.Iterator;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by IntelliJ IDEA.
 * User: bkrisler
 * Date: Dec 11, 2002
 * Time: 11:14:58 AM
 * To change this template use Options | File Templates.
 */
public class CommonWSDLServlet extends BaseServletComponent {
  private LoggingService log;
  private HashMap wsdlMap;

  public void load() {
    super.load();
  }

  /**
   * Returns the path for this servlet.
   * @return /general
   */
  protected String getPath() {
    return "/general";
  }

  /**
   * Creates the Generic WSDL Servlet.
   * Performs all initialization, reads in all the generic WSDL files.
   *
   * @return new WSDL Servlet.
   */
  protected Servlet createServlet() {
    wsdlMap = new HashMap();

    // get the logging service
    log = (LoggingService) serviceBroker.getService(this, LoggingService.class, null);
    if( log == null ) {
      throw new RuntimeException("Cannot find logging service!");
    }

    File dir = new File(System.getProperty("org.cougaar.install.path") + File.separator + "servicediscovery" +
                        File.separator + "data" + File.separator + "servicegroundings");

    String[] list = new String[4];
    list[0] = new String("cougaar.wsdl");
    list[1] = new String("MaintenanceProviderCougaarGrounding.wsdl");
    list[2] = new String("SupplyProviderCougaarGrounding.wsdl");
    list[3] = new String("TransportProviderCougaarGrounding.wsdl");

    for (int j = 0; j < list.length; j++) {
      StringBuffer wsdl = new StringBuffer();
      try {
        InputStream fileStream = new FileInputStream(dir.toString() + File.separator + list[j]);
        if (fileStream != null) {
          BufferedReader input = new BufferedReader(new InputStreamReader(fileStream));
          for(String str = input.readLine(); str != null; str = input.readLine()) {
            wsdl.append(str + "\n");
          }
          if (log.isDebugEnabled()) {
            log.debug("Adding: " + list[j]);
          }
          wsdlMap.put(list[j], wsdl);
        }
      } catch (Exception e) {
        if (log.isErrorEnabled()) {
          log.error("Caught an Exception on file: " + list[j], e);
        }
      }
    }

    return new CommonWSDLServlet.MyServlet();
  }

  private class MyServlet extends HttpServlet {
     public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
       CommonWSDLServlet.GetWSDL gw = new CommonWSDLServlet.GetWSDL(req, res, wsdlMap);
       gw.execute();
     }
   }

  private static class GetWSDL {
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HashMap wsdlMap;
    private StringBuffer result;

    public GetWSDL(HttpServletRequest request,
                   HttpServletResponse response,
                   HashMap wsdlMap) {
      this.request = request;
      this.response = response;
      this.wsdlMap = wsdlMap;
    }

    public void execute() throws IOException{
      parseParams();
      writeResponse();
    }

    private void parseParams() {
      Enumeration params = request.getParameterNames();

      if(params == null) {
        result = new StringBuffer("You must specify a parameter: \n\tfile=[filename] \n\tlist=[xml|comma|]");
        return;
      }

      while (params.hasMoreElements()) {
        String param = (String)params.nextElement();
        if(param.equals("file")) {
          processFile();
        } else if (param.equals("list")) {
          listFiles();
        } else {
          result = new StringBuffer("You must specify a parameter: \n\tfile=[filename] \n\tlist=[xml|comma|]");
        }
      }
    }

    /**
     * Process the file parameter.  This parameter is of the form: <br>
     * file=[filename] <br>
     * The file name is case sensitive.  If the file is a valid name,
     * the contents of the file is returned.
     */
    private void processFile() {
      String[] values = request.getParameterValues("file");
      Set keys = wsdlMap.keySet();
      for (int i = 0; i < values.length; i++) {
        String value = values[i];
        if(keys.contains(value)) {
          result = (StringBuffer)wsdlMap.get(value);
          break;
        }
      }

      if (result == null) {
        result = new StringBuffer("You must specify a parameter: \n\tfile=[filename] \n\tlist=[xml|comma|]");
      }
    }

    /**
     * Lists all WSDL files known to this agent.
     * Files can be listed in three different formats: HTML, XML and Comma separated.
     */
    private void listFiles() {
      Iterator iter = wsdlMap.keySet().iterator();
      result = new StringBuffer();

      String[] values = request.getParameterValues("list");
      if(values[0].equals("xml")) {
        result.append("<FileList>" +"\n");

        while(iter.hasNext()) {
          result.append("  <File>");
          result.append(iter.next());
          result.append("</File>" + "\n");
        }
        result.append("</FileList>" + "\n");
      } else if(values[0].equals("comma")) {
        boolean first = true;
        while(iter.hasNext()) {
          if(!first) {
            result.append(", ");
          } else {
            first = false;
          }
          result.append(iter.next());
        }
        result.append("\n");
      } else {
        result.append("<html><head><title>WSDL Files</title></head><body><ul>");
        while(iter.hasNext()) {
          result.append("<li>" + iter.next() + "</li>");
        }
        result.append("</ul></body></html>");
      }

    }

    private void writeResponse() throws IOException{
      response.setContentType("text/html");
      PrintWriter out = response.getWriter();
      out.print(result.toString());
    }
  }
}

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.servlet.BaseServletComponent;

/**
 * <code>WSDLServlet</code> is a servlet that loads in all
 * wsdl files associated with a servlet and serves up the
 * files when requested.  Files loaded are based on the agents
 * name, using the regular expression:  ^[A-Z]-" + agentName + "\\.wsdl
 * The servlet can take one of two parameters: <br>
 * <ul>
 * <li><pre>file=[filename]</pre>  Where filename is the name of the wsdl file.
 * <li><pre>list=[|xml|comma]</pre>  Lists all known files in the format specified.
 * </ul>
 *
 */
public class  WSDLServlet extends BaseServletComponent {

  private LoggingService log;
  private String agentId;
  private HashMap wsdlMap;

  public void load() {
    AgentIdentificationService ais = (AgentIdentificationService)
      serviceBroker.getService(
        this, AgentIdentificationService.class, null);
    if (ais != null) {
      this.agentId = ais.getMessageAddress().toString();
      serviceBroker.releaseService(
        this, AgentIdentificationService.class, ais);
    }
    super.load();
  }

  /**
   * Returns the path for this servlet.
   * @return /wsdl
   */
  protected String getPath() {
    return "/wsdl";
  }

  /**
   * Creates the WSDL Servlet.
   * Performs all initialization, reads in the WSDL files associated
   * with the agent that contains this servlet.  File are loaded
   * based on the regular expression: ^[A-Z]-" + agentId + "\\.wsdl
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

    String[] list;
    FilenameFilter filter = new AgentFileFilter(agentId.toString());
    if (log.isDebugEnabled()) {
      log.debug("Using grounding directory: " + dir.toString());
    }

    list = dir.list(filter);
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

    return new MyServlet();
  }

  private class MyServlet extends HttpServlet {
     public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
       GetWSDL gw = new GetWSDL(req, res, wsdlMap);
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

  private class AgentFileFilter implements FilenameFilter {
    String agentId;

    public AgentFileFilter(String agentId) {
      this.agentId = agentId;
      System.out.println("agentId = " + agentId);
    }

    public boolean accept(File dir, String name) {
      String pattern = "^[A-Z]-" + agentId + "\\.wsdl";
      return name.matches(pattern);
    }
  }
}

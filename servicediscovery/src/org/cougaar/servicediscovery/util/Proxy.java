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

package org.cougaar.servicediscovery.util;

import java.util.*;
import java.net.*;
import java.io.FileReader;
import java.io.*;

/**
 * Simple single-threaded HTTP proxy.
 *
 * Catches all GET requests and tries to service them with its local files.
 * Any request it cant service will result in a 404 error response.
 *
 * To service the requests:
 * 1) Check the hashtable of URL -> files.  This table is created from a text file, set
 *    by the -tableFile command line param.
 * 2) Strip the URL of everything but the filename, and append a .daml if no extension
 *    is there.  Check the base file directory (specified by the -fileDir command line
 *    param).
 */

public class Proxy {

  public Proxy() {
  }

  public static void main(String[] args) {

    String fileDir = System.getProperty("org.cougaar.install.path") + File.separator + "servicediscovery" +
                                        File.separator + "data" + File.separator + "cached";
    String tableFile = fileDir + File.separator + "proxyTable.txt";
    int port = 8082;

    if (args.length > 1) {
      for (int i = 0; i < args.length - 1; i = i + 2) {
        String key = args[i];
        String val = args[i + 1];

        if (key.equalsIgnoreCase("-filedir")) {
          fileDir = val;
        } else if (key.equalsIgnoreCase("-tablefile")) {
          tableFile = val;
        } else if (key.equalsIgnoreCase("-port")) {
          port = Integer.parseInt(val);
        }
      }
    }

    Hashtable knownURLs = new Hashtable();
    setupTable(knownURLs, tableFile);

    try {

      ServerSocket ss = new ServerSocket(port);

      // Single threaded because either we have the file in directory structure or we dont.

      while (true) {
        Socket s = ss.accept();
        InputStream iStream = s.getInputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(iStream));
        OutputStream oStream = s.getOutputStream();
        BufferedWriter socketOut = new BufferedWriter(new OutputStreamWriter(oStream));
        String inStr = in.readLine();
        String getRequest = inStr;
        while (inStr != null) {
          //System.err.println(inStr);
          if (in.ready()) {
            inStr = in.readLine();
          } else
            inStr = null;
        }

        boolean valid = false;
        if ((getRequest != null) &&
            (getRequest.startsWith("GET"))) {

          getRequest = getRequest.substring(4);
          if ((getRequest.indexOf(" ")) > -1)
            getRequest = getRequest.substring(0, getRequest.indexOf(" "));

          String requestedFile = (String) knownURLs.get(getRequest);

//          if (requestedFile == null) {
//            if (getRequest.lastIndexOf("/") > -1) {
//              requestedFile = getRequest.substring(getRequest.lastIndexOf("/") + 1);
//              if (requestedFile.length() > 0 && !(requestedFile.endsWith(".daml"))) {
//                requestedFile = requestedFile + ".daml";
//              }
//            }
//          }

          if (requestedFile != null) {

            BufferedReader fileIn = null;
            try {
              fileIn = new BufferedReader(new FileReader(fileDir + "/" + requestedFile));
            } catch (Exception ex) {
              System.err.println("Error reading file: " + fileDir + "/" + requestedFile);
            }

            if (fileIn != null) {
              System.err.println("GET request for " + getRequest + " is " + requestedFile);
              String inLine = fileIn.readLine();
              while (inLine != null) {
                socketOut.write(inLine + "\n");
                inLine = fileIn.readLine();
                valid = true;
              }
              socketOut.flush();
            }
          } else {
            System.out.println("getRequest = " + getRequest);

            URL myURL = new URL(getRequest);
            URLConnection myConnection = myURL.openConnection();
            System.out.println("opened query connection = "+ myConnection.toString());

            // PASS BACK data ...
            InputStream is = myConnection.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            byte buf[] = new byte[512];
            int sz;
            while ((sz = bis.read(buf, 0, 512)) != -1) {
              oStream.write(buf, 0, sz);
            }
            is.close();
            oStream.close();
            valid = true;
          }
        } else {
          System.out.println("getRequest = " + getRequest);
        }

        if (!valid) {
          System.err.println("GET request for " + getRequest + " File not Found");
          socketOut.write("<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">\n");
          socketOut.write("<HTML><HEAD>\n");
          socketOut.write("<TITLE>404 Not Found</TITLE>\n");
          socketOut.write("</HEAD><BODY>\n");
          socketOut.write("<H1>Not Found</H1>\n");
          socketOut.write("The requested URL " + getRequest + " was not found by the Proxy.<P>");
          socketOut.write("<HR>\n");
          socketOut.write("<ADDRESS>com.bbn.abs.coabs.coax.HttpProxy at localhost:8082</ADDRESS>\n");
          socketOut.write("</BODY></HTML>\n");
          socketOut.flush();
        }

        s.close();
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  static public void setupTable(Hashtable table, String fileName) {
    try {
      File inputFile = new File(fileName);
      BufferedReader bread = new BufferedReader(new FileReader(inputFile));
      String inLine = bread.readLine();
      while (inLine != null) {
        int separator = inLine.indexOf("->");
        if ((separator > 0) &&
            (separator < (inLine.length() - 3))) {
          String key = inLine.substring(0, separator - 1);
          String value = inLine.substring(separator + 3);

          //System.out.println("Key: "+key+" Value: "+value);
          table.put(key, value);
          inLine = bread.readLine();
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}


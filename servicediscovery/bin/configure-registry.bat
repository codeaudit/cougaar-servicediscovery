@echo OFF

REM "<copyright>"
REM " Copyright 2002-2003 BBNT Solutions, LLC"
REM " under sponsorship of the Defense Advanced Research Projects Agency (DARPA)"
REM " and the Defense Logistics Agency (DLA)."
REM ""
REM " This program is free software; you can redistribute it and/or modify"
REM " it under the terms of the Cougaar Open Source License as published by"
REM " DARPA on the Cougaar Open Source Website (www.cougaar.org)."
REM ""
REM " THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS"
REM " PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR"
REM " IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF"
REM " MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT"
REM " ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT"
REM " HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL"
REM " DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,"
REM " TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR"
REM " PERFORMANCE OF THE COUGAAR SOFTWARE."
REM "</copyright>"

REM This batch file configures the WASP UDDI registry by first calling DeleteUsers 
REM followed by PublishTaxonomy.
REM Make sure that COUGAAR_INSTALL_PATH is specified
IF NOT "%COUGAAR_INSTALL_PATH%" == "" GOTO L_2
REM Unable to find cougaar-install-path
ECHO COUGAAR_INSTALL_PATH not set!
GOTO L_END
:L_2

REM To override any of the defaults, set the following properties and add
REM the property to MY_PROPERTIES defined below.
REM set ADMIN_USERNAME="-Dorg.cougaar.servicediscovery.registry.admin.username="
REM set ADMIN_PASSWORD="-Dorg.cougaar.servicediscovery.registry.admin.password="

set SERVER_HOSTNAME="-Dorg.cougaar.servicediscovery.registry.hostname=localhost"
set SERVER_PORT="-Dorg.cougaar.servicediscovery.registry.server.port=8080"

set CIP=-Dorg.cougaar.install.path=%COUGAAR_INSTALL_PATH%

set MY_PROPERTIES= %ADMIN_USERNAME% %SERVER_HOSTNAME% %ADMIN_PASSWORD% %SERVER_PORT% %CIP%

set MY_CLASSPATH=%COUGAAR_INSTALL_PATH%\lib\servicediscovery.jar;%WASP_HOME%\lib\wasp.jar;%WASP_HOME%\dist\uddiclient.jar;%COUGAAR_INSTALL_PATH%\sys\soap.jar;%COUGAAR_INSTALL_PATH%\sys\activation.jar;%COUGAAR_INSTALL_PATH%\sys\uddi4j.jar;%COUGAAR_INSTALL_PATH%\sys\mail.jar

java.exe %MY_PROPERTIES% -classpath %MY_CLASSPATH% org.cougaar.servicediscovery.util.wasp.DeleteUsers

java.exe %MY_PROPERTIES% -classpath %MY_CLASSPATH% org.cougaar.servicediscovery.util.wasp.PublishTaxonomy --all

java.exe %MY_PROPERTIES% -classpath %MY_CLASSPATH% org.cougaar.servicediscovery.util.SaveTModel

:L_END

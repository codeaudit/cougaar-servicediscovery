@echo OFF

REM "<copyright>"
REM " Copyright 2002-2003 BBNT Solutions, LLC"
REM " under sponsorship of the Defense Advanced Research Projects Agency (DARPA)"
REM " and the Defense Logistics Agency (DLA)."

REM " This program is free software; you can redistribute it and/or modify"
REM " it under the terms of the Cougaar Open Source License as published by"
REM " DARPA on the Cougaar Open Source Website (www.cougaar.org)."

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

REM This batch file configures and runs the DeleteRegistryEntries program.
REM Make sure that COUGAAR_INSTALL_PATH is specified
IF NOT "%COUGAAR_INSTALL_PATH%" == "" GOTO L_2

REM Unable to find cougaar-install-path
ECHO COUGAAR_INSTALL_PATH not set!
GOTO L_END
:L_2

REM Some value MUST be assigned to these variables, either by command line args or
REM by a direct assignment below.  The app contains no defaults.
set USER_USERNAME=%1
set USER_PASSWORD=%2

REM Ensure USER_USERNAME and USER_PASSWORD are defined
IF NOT "%USER_USERNAME%" == "" GOTO L_3
ECHO Usage: clear-registry (username) (password)
GOTO L_END
:L_3

IF NOT "%USER_PASSWORD%" == "" GOTO L_4
ECHO Usage: clear-registry (username) (password)
GOTO L_END
:L_4

REM User-configurable section:
set UDDI_HOST=localhost
set UDDI_PORT=8080
set QUERY_URL=http://%UDDI_HOST%:%UDDI_PORT%/uddi/inquiry
set PUBLISH_URL=http://%UDDI_HOST%:%UDDI_PORT%/uddi/publishing

set UDDIPROPERTIES=-Dorg.cougaar.servicediscovery.registry.queryURL=%QUERY_URL% -Dorg.cougaar.servicediscovery.registry.publishURL=%PUBLISH_URL% -Dorg.cougaar.servicediscovery.registry.user.username=%USER_USERNAME% -Dorg.cougaar.servicediscovery.registry.user.password=%USER_PASSWORD%
set UDDICLASSPATH=%COUGAAR_INSTALL_PATH%\lib\servicediscovery.jar;%COUGAAR_INSTALL_PATH%\sys\commons-logging.jar;%COUGAAR_INSTALL_PATH%\sys\castor-0.9.3.9-xml.jar;%COUGAAR_INSTALL_PATH%\sys\xerces.jar;%COUGAAR_INSTALL_PATH%\sys\mail.jar;%COUGAAR_INSTALL_PATH%\sys\dom4j.jar;%COUGAAR_INSTALL_PATH%\sys\activation.jar;%COUGAAR_INSTALL_PATH%\sys\uddi4j.jar;%COUGAAR_INSTALL_PATH%\sys\soap.jar

REM The path to java.exe must be in the PATH environment variable
java %UDDIPROPERTIES% -classpath %UDDICLASSPATH% org.cougaar.servicediscovery.util.UDDI4JDeleteRegistryEntries
:L_END
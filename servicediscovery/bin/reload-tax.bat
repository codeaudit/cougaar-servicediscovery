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

REM This batch file reloads the taxonomy in the WASP UDDI registry by first calling
REM DeleteRegistryEntries, then DeleteTaxonomy, and finally PublishTaxonomy.

REM Make sure that COUGAAR_INSTALL_PATH is specified
IF NOT "%COUGAAR_INSTALL_PATH%" == "" GOTO L_2

REM Unable to find cougaar-install-path
ECHO COUGAAR_INSTALL_PATH not set!
GOTO L_END
:L_2

REM Ensure USER_USERID and USER_PASSWORD are defined
IF NOT "%1" == "" GOTO L_3

ECHO Usage: reload-tax (user username) (user password) [admin username] [admin password]
ECHO Admin username and admin password are optional.
GOTO L_END

:L_3

IF NOT "%2" == "" GOTO L_4

ECHO Usage: reload-tax (user username) (user password) [admin username] [admin password]
ECHO Admin username and admin password are optional.
GOTO L_END

:L_4

REM %1 is USER_USERNAME for registry user account (REQUIRED)
REM %2 is USER_PASSWORD for registry user account (REQUIRED)
REM %3 is ADMIN_USERNAME for registry admin account (OPTIONAL...default provided in app)
REM %4 is ADMIN_PASSWORD for registry admin account (OPTIONAL...default provided in app)
REM Users can edit the values of ADMIN_USERNAME and ADMIN_PASSWORD, below, if the default
REM value in the app is incorrect and providing those values as command line args is 
REM undesirable.

set MY_USERNAME="-Dorg.cougaar.servicediscovery.registry.user.username=%1"
set MY_PASSWORD="-Dorg.cougaar.servicediscovery.registry.user.password=%2"

set ADMIN_USERNAME="-Dorg.cougaar.servicediscovery.registry.admin.username=%3"
set ADMIN_PASSWORD="-Dorg.cougaar.servicediscovery.registry.admin.password=%4"

set UDDI_HOST=localhost
set UDDI_PORT=8080

set SERVER_HOSTNAME="-Dorg.cougaar.servicediscovery.registry.hostname=%UDDI_HOST%"
set SERVER_PORT="-Dorg.cougaar.servicediscovery.registry.server.port=%UDDI_PORT%"
set CIP=-Dorg.cougaar.install.path=%COUGAAR_INSTALL_PATH%

set QUERY_URL="-Dorg.cougaar.servicediscovery.registry.queryURL=http://%UDDI_HOST%:%UDDI_PORT%/uddi/inquiry"
set PUBLISH_URL="-Dorg.cougaar.servicediscovery.registry.publishURL=http://%UDDI_HOST%:%UDDI_PORT%/uddi/publishing"

set MY_PROPERTIES= %MY_USERNAME% %MY_PASSWORD% %ADMIN_USERNAME% %ADMIN_PASSWORD% %SERVER_HOSTNAME% %SERVER_PORT% %CIP% %PUBLISH_URL% %QUERY_URL%

set MY_CLASSPATH=%COUGAAR_INSTALL_PATH%\lib\servicediscovery.jar;%COUGAAR_INSTALL_PATH%\sys\commons-logging.jar;%COUGAAR_INSTALL_PATH%\sys\castor-0.9.3.9-xml.jar;%COUGAAR_INSTALL_PATH%\sys\xerces.jar;%COUGAAR_INSTALL_PATH%\sys\saaj-api.jar;%COUGAAR_INSTALL_PATH%\sys\saaj-ri.jar;%COUGAAR_INSTALL_PATH%\sys\mail.jar;%COUGAAR_INSTALL_PATH%\sys\dom4j.jar;%COUGAAR_INSTALL_PATH%\sys\activation.jar;%COUGAAR_INSTALL_PATH%\lib\servicediscovery.jar;%WASP_HOME%\lib\wasp.jar;%WASP_HOME%\dist\uddiclient.jar;%COUGAAR_INSTALL_PATH%\sys\uddi4j.jar;%COUGAAR_INSTALL_PATH%\sys\soap.jar

java.exe %MY_PROPERTIES% -classpath %MY_CLASSPATH% org.cougaar.servicediscovery.util.UDDI4JDeleteRegistryEntries

java.exe %MY_PROPERTIES% -classpath %MY_CLASSPATH% org.cougaar.servicediscovery.util.wasp.DeleteTaxonomy --all

java.exe %MY_PROPERTIES% -classpath %MY_CLASSPATH% org.cougaar.servicediscovery.util.wasp.PublishTaxonomy --all

java.exe %MY_PROPERTIES% -classpath %MY_CLASSPATH% org.cougaar.servicediscovery.util.SaveTModel

:L_END
@echo OFF

REM "<copyright>"
REM " Copyright 2001 BBNT Solutions, LLC"
REM " under sponsorship of the Defense Advanced Research Projects Agency (DARPA)."
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


rem Domains are now usually defined by the config file LDMDomains.ini
rem But you may still use properties if you wish.
rem set MYDOMAINS=-Dorg.cougaar.domain.alp=org.cougaar.glm.GLMDomain
set MYDOMAINS=
set MYCLASSES=org.cougaar.bootstrap.Bootstrapper org.cougaar.core.node.Node

REM You may use the optional environment variable COUGAAR_DEV_PATH
REM to point to custom developed code that is not in COUGAR_INSTALL_PATH/lib
REM or CIP/sys. This can be one or many semicolon separated 
REM directories/jars/zips, or left undefined


set MYPROPERTIES=-Xbootclasspath/p:%COUGAAR_INSTALL_PATH%\lib\javaiopatch.jar -Dorg.cougaar.system.path=%COUGAAR3RDPARTY% -Dorg.cougaar.install.path=%COUGAAR_INSTALL_PATH% -Duser.timezone=GMT -Dorg.cougaar.core.agent.startTime=08/10/2005 -Dorg.cougaar.class.path=%COUGAAR_DEV_PATH% -Dorg.cougaar.workspace=%COUGAAR_WORKSPACE% -Dorg.cougaar.config.path="%COUGAAR_INSTALL_PATH%\configs\common;%COUGAAR_INSTALL_PATH%\configs\glmtrans;%COUGAAR_INSTALL_PATH%\servicediscovery\data\serviceprofiles\;" -Dorg.cougaar.servicediscovery.registry.username=ulservices -Dorg.cougaar.servicediscovery.registry.password=ulservices -Dorg.cougaar.servicediscovery.registry.queryURL=http://localhost:8080/uddi/inquiry -Dorg.cougaar.servicediscovery.registry.publishURL=http://localhost:8080/uddi/publishing -Dorg.cougaar.core.logging.config.filename=debug.props

set MYMEMORY=-Xms100m -Xmx300m


@echo OFF

REM "<copyright>"
REM " "
REM " Copyright 2001-2004 BBNT Solutions, LLC"
REM " under sponsorship of the Defense Advanced Research Projects"
REM " Agency (DARPA)."
REM ""
REM " You can redistribute this software and/or modify it under the"
REM " terms of the Cougaar Open Source License as published on the"
REM " Cougaar Open Source Website (www.cougaar.org)."
REM ""
REM " THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS"
REM " "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT"
REM " LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR"
REM " A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT"
REM " OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,"
REM " SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT"
REM " LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,"
REM " DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY"
REM " THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT"
REM " (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE"
REM " OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE."
REM " "
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


set MYPROPERTIES=-Xbootclasspath/p:%COUGAAR_INSTALL_PATH%\lib\javaiopatch.jar -Dorg.cougaar.system.path=%COUGAAR3RDPARTY% -Dorg.cougaar.install.path=%COUGAAR_INSTALL_PATH% -Duser.timezone=GMT -Dorg.cougaar.core.agent.startTime=08/10/2005 -Dorg.cougaar.class.path=%COUGAAR_DEV_PATH% -Dorg.cougaar.workspace=%COUGAAR_WORKSPACE% -Dorg.cougaar.config.path="%COUGAAR_INSTALL_PATH%\servicediscovery\configs\common;%COUGAAR_INSTALL_PATH%\configs\common;%COUGAAR_INSTALL_PATH%\servicediscovery\data\serviceprofiles;%COUGAAR_INSTALL_PATH%\servicediscovery\configs\sdtest"  -Dorg.cougaar.yp.ypAgent=3ID

set MYMEMORY=-Xms100m -Xmx300m





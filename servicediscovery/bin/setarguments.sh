#!/bin/sh

# <copyright>
#  
#  Copyright 2001-2004 BBNT Solutions, LLC
#  under sponsorship of the Defense Advanced Research Projects
#  Agency (DARPA).
# 
#  You can redistribute this software and/or modify it under the
#  terms of the Cougaar Open Source License as published on the
#  Cougaar Open Source Website (www.cougaar.org).
# 
#  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
#  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
#  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
#  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
#  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
#  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
#  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
#  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
#  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
#  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
#  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#  
# </copyright>


# This should be sourced by a shell or shell script to define the
# standard contents of an COUGAAR command line.

# Domains are now usually defined by the config file LDMDomains.ini
# But you may still use properties if you wish.
# set MYDOMAINS=-Dorg.cougaar.domain.alp=org.cougaar.glm.GLMDomain

# Ensure the COUGAAR_WORKSPACE is set
if [ "$COUGAAR_WORKSPACE"="" ]; then
    COUGAAR_WORKSPACE="${COUGAAR_INSTALL_PATH}/workspace"
fi

MYDOMAINS=""
BOOTSTRAPPER=org.cougaar.bootstrap.Bootstrapper
MYCLASSES=org.cougaar.core.node.Node
OS=`uname`
# No green threads in jdk 1.3.1
#if [ "$OS" == "Linux" ]; then
#  MYPROPERTIES="-green"
#fi
MYPROPERTIES="$MYPROPERTIES $MYDOMAINS  -Dorg.cougaar.system.path=$COUGAAR3RDPARTY -Dorg.cougaar.install.path=$COUGAAR_INSTALL_PATH -Dorg.cougaar.workspace=$COUGAAR_WORKSPACE -Dorg.cougaar.config.path=%COUGAAR_INSTALL_PATH%/servicediscovery/configs/common;%COUGAAR_INSTALL_PATH%/configs/common;%COUGAAR_INSTALL_PATH%/servicediscovery/data/serviceprofiles;%COUGAAR_INSTALL_PATH%/servicediscovery/configs/sdtest  -Dorg.cougaar.yp.ypAgent=3ID"

MYPROPERTIES="$MYPROPERTIES -Duser.timezone=GMT -Dorg.cougaar.core.useBootstrapper=true "

MYMEMORY="-Xms100m -Xmx300m"



#!/bin/sh

# <copyright>
#  Copyright 2001-2003 BBNT Solutions, LLC
#  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
# 
#  This program is free software; you can redistribute it and/or modify
#  it under the terms of the Cougaar Open Source License as published by
#  DARPA on the Cougaar Open Source Website (www.cougaar.org).
# 
#  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
#  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
#  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
#  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
#  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
#  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
#  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
#  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
#  PERFORMANCE OF THE COUGAAR SOFTWARE.
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
MYPROPERTIES="-Xbootclasspath/p:$COUGAAR_INSTALL_PATH/lib/javaiopatch.jar $MYPROPERTIES $MYDOMAINS  -Dorg.cougaar.system.path=$COUGAAR3RDPARTY -Dorg.cougaar.install.path=$COUGAAR_INSTALL_PATH -Dorg.cougaar.workspace=$COUGAAR_WORKSPACE -Dorg.cougaar.config.path=%COUGAAR_INSTALL_PATH%/servicediscovery/configs/common;%COUGAAR_INSTALL_PATH%/configs/common;%COUGAAR_INSTALL_PATH%/servicediscovery/data/serviceprofiles;%COUGAAR_INSTALL_PATH%/servicediscovery/configs/sdtest  -Dorg.cougaar.yp.ypAgent=3ID"

MYPROPERTIES="$MYPROPERTIES -Duser.timezone=GMT -Dorg.cougaar.core.useBootstrapper=true "

MYMEMORY="-Xms100m -Xmx300m"



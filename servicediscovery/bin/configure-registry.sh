#!/bin/sh

# <copyright>
#  Copyright 2002 BBNT Solutions, LLC
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

# This shell script configures the WASP UDDI registry by first running DeleteUsers 
# followed by PublishTaxonomy.

# Make sure that COUGAAR_INSTALL_PATH is specified
if [ "$COUGAAR_INSTALL_PATH" = "" ]; then
  echo "COUGAAR_INSTALL_PATH not set!"

elif [ "$WASP_HOME" = "" ]; then
  echo "WASP_HOME not set!"

else

  # ADMIN_USERNAME="-Dorg.cougaar.servicediscovery.registry.admin.username="
  # ADMIN_PASSWORD="-Dorg.cougaar.servicediscovery.registry.admin.password="
  SERVER_HOSTNAME="-Dorg.cougaar.servicediscovery.registry.hostname=localhost"
  SERVER_PORT="-Dorg.cougaar.servicediscovery.registry.server.port=8080"
  CIP="-Dorg.cougaar.install.path=$COUGAAR_INSTALL_PATH"

  MY_PROPERTIES=$ADMIN_USERNAME $ADMIN_PASSWORD $SERVER_HOSTNAME $SERVER_PORT $CIP

  MY_CLASSPATH=$COUGAAR_INSTALL_PATH/lib/servicediscovery.jar:$WASP_HOME/lib/wasp.jar:$WASP_HOME/dist/uddiclient.jar:$COUGAAR_INSTALL_PATH/sys/soap.jar:$COUGAAR_INSTALL_PATH/sys/activation.jar:$COUGAAR_INSTALL_PATH/sys/uddi4j.jar:$COUGAAR_INSTALL_PATH/sys/mail.jar

  java.exe $MY_PROPERTIES -classpath $MY_CLASSPATH org.cougaar.servicediscovery.util.wasp.DeleteUsers

  java.exe $MY_PROPERTIES -classpath $MY_CLASSPATH org.cougaar.servicediscovery.util.wasp.PublishTaxonomy --all

  java.exe $MY_PROPERTIES -classpath $MY_CLASSPATH org.cougaar.servicediscovery.util.SaveTModel

fi

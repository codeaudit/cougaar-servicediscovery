#!/bin/sh

# "<copyright>"
# " Copyright 2002-2003 BBNT Solutions, LLC"
# " under sponsorship of the Defense Advanced Research Projects Agency (DARPA)"
# " and the Defense Logistics Agency (DLA)."
# ""
# " This program is free software; you can redistribute it and/or modify"
# " it under the terms of the Cougaar Open Source License as published by"
# " DARPA on the Cougaar Open Source Website (www.cougaar.org)."
# ""
# " THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS"
# " PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR"
# " IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF"
# " MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT"
# " ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT"
# " HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL"
# " DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,"
# " TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR"
# " PERFORMANCE OF THE COUGAAR SOFTWARE."
# "</copyright>"

# This shell script reloads the taxonomy in the WASP UDDI registry by first calling
# DeleteRegistryEntries, then DeleteTaxonomy, then PublishTaxonomy.

# Make sure that COUGAAR_INSTALL_PATH is specified
if [ "$COUGAAR_INSTALL_PATH" = "" ]; then
  echo "COUGAAR_INSTALL_PATH not set!"

# Make sure that WASP_HOME is specified
elif [ "$WASP_HOME" = "" ]; then
  echo "WASP_HOME not set!"

# Ensure USER_USERID and USER_PASSWORD are defined
elif [ "$1" == "" -o "$2" == "" ]; then
  echo "Usage: reload-tax.sh (user username) (user password) [admin username] [admin password]"
  echo "Admin username and admin password are optional."
else
  # Set properties
  MY_USERNAME="-Dorg.cougaar.servicediscovery.registry.user.username=$1"

  MY_PASSWORD="-Dorg.cougaar.servicediscovery.registry.user.password=$2"

  ADMIN_USERNAME="-Dorg.cougaar.servicediscovery.registry.admin.username=$3"

  ADMIN_PASSWORD="-Dorg.cougaar.servicediscovery.registry.admin.password=$4"

  UDDI_HOST=localhost

  UDDI_PORT=1980

  SERVER_HOSTNAME="-Dorg.cougaar.servicediscovery.registry.hostname=$UDDI_HOST"

  SERVER_PORT="-Dorg.cougaar.servicediscovery.registry.server.port=$UDDI_PORT"

  CIP=-Dorg.cougaar.install.path=$COUGAAR_INSTALL_PATH

  QUERY_URL="-Dorg.cougaar.servicediscovery.registry.queryURL=http://$UDDI_HOST:$UDDI_PORT/uddi/inquiry"

  PUBLISH_URL="-Dorg.cougaar.servicediscovery.registry.publishURL=http://$UDDI_HOST:$UDDI_PORT/uddi/publishing"

  MY_PROPERTIES="$MY_USERNAME $MY_PASSWORD $ADMIN_USERNAME $ADMIN_PASSWORD $SERVER_HOSTNAME $SERVER_PORT $CIP $PUBLISH_URL $QUERY_URL"

  MY_CLASSPATH=$COUGAAR_INSTALL_PATH/lib/servicediscovery.jar:$COUGAAR_INSTALL_PATH/sys/commons-logging.jar:$COUGAAR_INSTALL_PATH/sys/castor-0.9.3.9-xml.jar:$COUGAAR_INSTALL_PATH/sys/xerces.jar:$COUGAAR_INSTALL_PATH/sys/saaj-api.jar:$COUGAAR_INSTALL_PATH/sys/saaj-ri.jar:$COUGAAR_INSTALL_PATH/sys/mail.jar:$COUGAAR_INSTALL_PATH/sys/dom4j.jar:$COUGAAR_INSTALL_PATH/sys/activation.jar:$COUGAAR_INSTALL_PATH/sys/uddi4j.jar:$COUGAAR_INSTALL_PATH/sys/soap.jar:$WASP_HOME/dist/uddiclient.jar:$WASP_HOME/lib/wasp.jar

echo $MY_PROPERTIES

  java $MY_PROPERTIES -classpath $MY_CLASSPATH org.cougaar.servicediscovery.util.UDDI4JDeleteRegistryEntries

  java $MY_PROPERTIES -classpath $MY_CLASSPATH org.cougaar.servicediscovery.util.wasp.DeleteTaxonomy --all

  java $MY_PROPERTIES -classpath $MY_CLASSPATH org.cougaar.servicediscovery.util.wasp.PublishTaxonomy --all

  java $MY_PROPERTIES -classpath $MY_CLASSPATH org.cougaar.servicediscovery.util.SaveTModel
fi
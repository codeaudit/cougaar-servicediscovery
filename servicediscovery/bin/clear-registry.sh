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

# This batch file configures and runs the DeleteRegistryEntries program.
# Some value MUST be assigned to these variables, either by command line args or
# by a direct assignment here.  The app contains no defaults.
USER_USERNAME=$1
USER_PASSWORD=$2

# Make sure that COUGAAR_INSTALL_PATH is specified
if [ "$COUGAAR_INSTALL_PATH" = "" ]; then
  echo "COUGAAR_INSTALL_PATH not set!"
# Ensure USER_USERID and USER_PASSWORD are defined
elif [ "$USER_USERNAME" = "" -o "$USER_PASSWORD" = "" ]; then
  echo "Usage: clear-registry.sh (user username) (user password)"
else
  # User-configurable section:
  UDDI_HOST="localhost"
  UDDI_PORT="8080"
  QUERY_URL="http://$UDDI_HOST:$UDDI_PORT/uddi/inquiry"
  PUBLISH_URL="http://$UDDI_HOST:$UDDI_PORT/uddi/publishing"

  UDDIPROPERTIES="-Dorg.cougaar.servicediscovery.registry.queryURL=$QUERY_URL -Dorg.cougaar.servicediscovery.registry.publishURL=$PUBLISH_URL -Dorg.cougaar.servicediscovery.registry.user.username=$USER_USERNAME -Dorg.cougaar.servicediscovery.registry.user.password=$USER_PASSWORD"
  UDDICLASSPATH="$COUGAAR_INSTALL_PATH/lib/servicediscovery.jar:$COUGAAR_INSTALL_PATH/sys/xerces.jar:$COUGAAR_INSTALL_PATH/sys/commons-logging.jar:$COUGAAR_INSTALL_PATH/sys/castor-0.9.3.9-xml.jar:$COUGAAR_INSTALL_PATH/sys/mail.jar:$COUGAAR_INSTALL_PATH/sys/dom4j.jar:$COUGAAR_INSTALL_PATH/sys/activation.jar:$COUGAAR_INSTALL_PATH/sys/uddi4j.jar:$COUGAAR_INSTALL_PATH/sys/soap.jar"

  # The path to java.exe must be in the PATH environment variable
  java $UDDIPROPERTIES -classpath $UDDICLASSPATH org.cougaar.servicediscovery.util.UDDI4JDeleteRegistryEntries
fi
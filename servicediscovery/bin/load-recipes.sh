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

# This shell script loads the Service Discovery CSMART recipes

if [ "x$3" = "x" ]; then
  echo "Usage: load-recipes.sh [Config DB Username] [Password] [MySQL Config DB database name] [Optional: MySQL DB host name]"
  exit
fi


# Make sure that COUGAAR_INSTALL_PATH is specified
if [ "$COUGAAR_INSTALL_PATH" = "" ]; then
  echo "COUGAAR_INSTALL_PATH not set!"
  exit
fi

rm mega.tmp
cat $COUGAAR_INSTALL_PATH/servicediscovery/data/csmart/recipe/*.sql > $COUGAAR_INSTALL_PATH/servicediscovery/data/csmart/recipe/mega.tmp
mysql -f -u$1 -p$2 $3 < $COUGAAR_INSTALL_PATH/servicediscovery/data/csmart/recipe/mega.tmp
rm $COUGAAR_INSTALL_PATH/servicediscovery/data/csmart/recipe/mega.tmp



#!/bin/sh

# "<copyright>"
# " "
# " Copyright 2002-2004 BBNT Solutions, LLC"
# " under sponsorship of the Defense Advanced Research Projects"
# " Agency (DARPA)."
# ""
# " You can redistribute this software and/or modify it under the"
# " terms of the Cougaar Open Source License as published on the"
# " Cougaar Open Source Website (www.cougaar.org)."
# ""
# " THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS"
# " "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT"
# " LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR"
# " A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT"
# " OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,"
# " SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT"
# " LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,"
# " DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY"
# " THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT"
# " (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE"
# " OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE."
# " "
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
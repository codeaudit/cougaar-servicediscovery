Society Configuration -
Note that path specifications use unix conventions. NT users should substitute
as appropriate.

1) See the Database Installation chapter of the CSMART installation document
at CIP/csmart/doc/InstallAndTest.html
 
2) Load the Service Discovery  specific configuration information. This step
must occur after the CSMART configuration database has been installed. (See 
above.)
	(a) cd CIP/servicediscovery/data/database/scripts/mysql
	(b) load_sd_1ad_mysql.[bat][sh] (userid) (password) (CSMARTSD db name)
 
3) Import Service Discovery specific recipes
	(a) cd CIP/servicediscovery/data/csmart/recipe
	(b) mysql -u (userid) -p(password) (CSMART db name) < ServiceDiscoveryRegistration-export.sql
	(c) mysql -u (userid) -p(password) (CSMART db name) < ServiceDiscoveryBootstrapping-export.sql
	(d) cd CIP/yp/data/csmart/recipes
	(e) mysql -u (userid) -p(password) (CSMART db name) < YPSupport-export.sql

            
4) Generate the provider agent DAML files. 

	(a) If Perl is not installed on your computer, you can download it 
	from http://www.activestate.com.
    
	(d) cd CIP/servicediscovery/data/serviceprofiles

	(c) Edit agent-input.txt. 
            Replace  <COUGAAR_INSTALL_PATH> with your cougaar install path.
            

	Note: If running from NT, you must use a capital letter for the the 
	drive specification.
		cougaarInstallPath=C:/cougaar
	
	If running from linux, the cougaar install path specification must not 
	contain any symbolic links.
    
	(d) perl generateDAML.pl agent-input.txt profile-template.txt
 
5) If running sdtest configuration from the command line: add the following 
Service Discovery specific definitions to MYPROPERTIES variable used in 
CIP/bin/Node[.bat]. CIP/servicediscovery/bin/setarguments[.bat][.sh] can be 
used as a template.
       
	(a) Include both %COUGAAR_INSTALL_PATH%\servicediscovery\configs\common
	and %COUGAAR_INSTALL_PATH%\servicediscovery\data\serviceprofiles in 
	the definition of -Dorg.cougaar.config.path. 
	%COUGAAR_INSTALL_PATH%\servicediscovery\configs\common must be 
	referenced before %COUGAAR_INSTALL_PATH\configs\common to ensure that 
	the Service Discovery domain is used.  
           
	Example: -Dorg.cougaar.config.path=%COUGAAR_INSTALL_PATH%\servicediscovery\configs\common;%COUGAAR_INSTALL_PATH%\configs\common;%COUGAAR_INSTALL_PATH%\servicediscovery\data\serviceprofiles;
       
	(b) Set org.cougaar.yp.ypAgent to the Agent running the YPServer - 3ID
	in the CIP/servicediscovery/configs/sdtest configuration.

	Example: -Dorg.cougaar.yp.ypAgent=3ID

	(c) cd CIP/servicediscovery/configs/sdtest
	(d) node MiniNode

6) If running from CSMART, edit CIP/server/bin/server.props.

	(a) org.cougaar.config.path must include both
	(CIP)/servicediscovery/configs/common\; 
	(CIP)/servicediscovery/data/serviceprofile

	Example:
	org.cougaar.config.path="/opt/cougaar/head/servicediscovery/configs/common\;/opt/cougaar/head/configs/common\;/opt/cougaar/head/servicediscovery/data/serviceprofiles;"

        IMPORTANT: (CIP)servicediscovery/configs/common must be listed prior 
	to the entry for(CIP)/configs/common

7) If running from CSMART, edit CSMART.sh[bat] as follows:

   Add the CIP/servicediscovery/configs/common path to MYCONFIGPATH
   
   Example:
   MYCONFIGPATH="-Dorg.cougaar.config.path=$COUGAAR_INSTALL_PATH/servicediscovery/configs/common/\;$COUGAAR_INSTALL_PATH/csmart/data/common/\;"
   
8) Running from CSMART:
 
	(a) Use experiment SD-SMALL-1AD-TRANS-STUB.
 
	(b) Add the following recipes in the following order:
		YPSupport
		ServiceDiscoveryRegistration
		ServiceDiscoveryBootstraping
	IMPORTANT: If recipes are not loaded in the above order, experiment
	will not run.
	
	(c)Add Global Command Line Arguments to the experiment as follows:
		Right click on experiment name, select Build
		Select the Configuration tab
		Right click on the host name in the Hosts pane (left-most pane)
		Click on Global Command Line Arguments
		Click the Read From File button, then select 
		CIP/servicediscovery/bin/ypServer.props. 

        IMPORTANT: When configuring the experiment, you must place NCA agent on
        its own node to ensure that the YP Server is initialized before any other
        agent tries to register.
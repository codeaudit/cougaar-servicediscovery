Society Configuration -
Note that path specifications use unix conventions. NT users should substitute
as appropriate.
1) See the Database Installation chapter of the CSMART installation document
at CIP/csmart/doc/InstallAndTest.html
  
2) Import Service Discovery specific recipes (for use from CSMART)
	(a) cd CIP/servicediscovery/bin
	(b) load-recipes (userid) (password) (CSMART db name)
	(f) cd CIP/yp/data/csmart/recipes
	(g) mysql -u (userid) -p(password) (CSMART db name) < YPSupport-export.sql

            
3) Optional: ServiceDiscovery uses per-agent profiles for each agent that is
registering services. If you modify the services being registered
or add additional such agents, you must regenerate the service profiles.

   Generate the provider agent DAML files. 

	(a) If Perl is not installed on your computer, you can download it 
	from http://www.activestate.com.
    
	(d) cd CIP/servicediscovery/data/serviceprofiles.

	(c) Edit agent-input.txt to express new or modified provider 
	capabilities.
    
	(d) perl generateDAML.pl agent-input.txt profile-template.txt
 
4) If running sdtest configuration from the command line: add the following 
Service Discovery specific definitions to MYPROPERTIES variable used in 
CIP/bin/Node[.bat]. CIP/servicediscovery/bin/setarguments[.bat][.sh] can be 
used as a template.
       
	(a) Include %COUGAAR_INSTALL_PATH%\servicediscovery\configs\common
	in the definition of -Dorg.cougaar.config.path. 
           
	Example: -Dorg.cougaar.config.path=%COUGAAR_INSTALL_PATH%\servicediscovery\configs\common;%COUGAAR_INSTALL_PATH%\configs\common;
       
	(b) Set org.cougaar.yp.ypAgent to the Agent running the YPServer - 3ID
	in the CIP/servicediscovery/configs/sdtest configuration.

	Example: -Dorg.cougaar.yp.ypAgent=3ID

	(c) cd CIP/servicediscovery/configs/sdtest
	(d) node MiniNode

6) If running from CSMART, edit CSMART.sh[bat] as follows:

   Add the CIP/servicediscovery/configs/common path to MYCONFIGPATH
   
   Example:
   MYCONFIGPATH="-Dorg.cougaar.config.path=$COUGAAR_INSTALL_PATH/servicediscovery/configs/common/\;$COUGAAR_INSTALL_PATH/csmart/data/common/\;"
   
   The purpose of this is to find the SD specific recipeQueries.q
   file. You may choose, instead, to copy the queries from the SD
   recipeQueries.q to another copy elsewhere.


7) Running from CSMART:
 
	(a) Add the following recipes in the following order:
		YPSupport
		ServiceDiscoveryRegistration
		ServiceDiscoveryBootstrapping

        (b) If you are running a TRANS-STUB society (only TRANSCOM, no
        GlobalAir, GlobalSea, etc), then add the recipe
		   SDBootstrap-TRANS-STUB-ONLY

	(c) Otherwise, if you are running a -TRANS society (not STUB,
	one which includes the GlobalAir, GlobalSea, etc agents), then
	add the recipe
		 SDBootstrap-TRANS-ONLY-notstub

	IMPORTANT: If recipes are not loaded in the above order, experiment
	may not run.
	



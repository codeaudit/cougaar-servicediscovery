# MySQL dump 8.16
#
# Host: localhost    Database: tempcopy
#--------------------------------------------------------
# Server version	3.23.43-nt

#
# Dumping data for table 'alib_component'
#

LOCK TABLES alib_component WRITE;
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('SDBootstrap-TRANS-ONLY-notstub','SDBootstrap-TRANS-ONLY-notstub','recipe|##RECIPE_CLASS##','recipe',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('SDBootstrap-TRANS-ONLY-notstub|org.cougaar.servicediscovery.plugin.SDClientPlugin','SDBootstrap-TRANS-ONLY-notstub|org.cougaar.servicediscovery.plugin.SDClientPlugin','plugin|org.cougaar.servicediscovery.plugin.SDClientPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('SDBootstrap-TRANS-ONLY-notstub|org.cougaar.servicediscovery.plugin.SDClientPlugin|CONUSGroundTransportationProvider','SDBootstrap-TRANS-ONLY-notstub|org.cougaar.servicediscovery.plugin.SDClientPlugin|CONUSGroundTransportationProvider','plugin|org.cougaar.servicediscovery.plugin.SDClientPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('SDBootstrap-TRANS-ONLY-notstub|org.cougaar.servicediscovery.plugin.SDClientPlugin|CONUSGroundTransportationProvider2','SDBootstrap-TRANS-ONLY-notstub|org.cougaar.servicediscovery.plugin.SDClientPlugin|CONUSGroundTransportationProvider2','plugin|org.cougaar.servicediscovery.plugin.SDClientPlugin','plugin',0.000000000000000000000000000000);
UNLOCK TABLES;

#
# Dumping data for table 'asb_assembly'
#

LOCK TABLES asb_assembly WRITE;
INSERT INTO asb_assembly (ASSEMBLY_ID, ASSEMBLY_TYPE, DESCRIPTION) VALUES ('RCP-0001-SDBootstrap-TRANS-ONLY-notstub','RCP','SDBootstrap-TRANS-ONLY-notstub');
UNLOCK TABLES;

#
# Dumping data for table 'asb_component_arg'
#

LOCK TABLES asb_component_arg WRITE;
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-SDBootstrap-TRANS-ONLY-notstub','SDBootstrap-TRANS-ONLY-notstub|org.cougaar.servicediscovery.plugin.SDClientPlugin','AirTransportationProvider',1.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-SDBootstrap-TRANS-ONLY-notstub','SDBootstrap-TRANS-ONLY-notstub|org.cougaar.servicediscovery.plugin.SDClientPlugin','SeaTransportationProvider',2.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-SDBootstrap-TRANS-ONLY-notstub','SDBootstrap-TRANS-ONLY-notstub|org.cougaar.servicediscovery.plugin.SDClientPlugin|CONUSGroundTransportationProvider','CONUSGroundTransportationProvider',1.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-SDBootstrap-TRANS-ONLY-notstub','SDBootstrap-TRANS-ONLY-notstub|org.cougaar.servicediscovery.plugin.SDClientPlugin|CONUSGroundTransportationProvider','OrganicAirTransportationProvider',2.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-SDBootstrap-TRANS-ONLY-notstub','SDBootstrap-TRANS-ONLY-notstub|org.cougaar.servicediscovery.plugin.SDClientPlugin|CONUSGroundTransportationProvider','TheaterStrategicTransportationProvider',3.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-SDBootstrap-TRANS-ONLY-notstub','SDBootstrap-TRANS-ONLY-notstub|org.cougaar.servicediscovery.plugin.SDClientPlugin|CONUSGroundTransportationProvider2','CONUSGroundTransportationProvider',1.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-SDBootstrap-TRANS-ONLY-notstub','SDBootstrap-TRANS-ONLY-notstub|org.cougaar.servicediscovery.plugin.SDClientPlugin|CONUSGroundTransportationProvider2','ShipPackingTransportationProvider',2.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-SDBootstrap-TRANS-ONLY-notstub','SDBootstrap-TRANS-ONLY-notstub|org.cougaar.servicediscovery.plugin.SDClientPlugin|CONUSGroundTransportationProvider2','TheaterStrategicTransportationProvider',3.000000000000000000000000000000);
UNLOCK TABLES;

#
# Dumping data for table 'asb_component_hierarchy'
#

LOCK TABLES asb_component_hierarchy WRITE;
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0001-SDBootstrap-TRANS-ONLY-notstub','SDBootstrap-TRANS-ONLY-notstub|org.cougaar.servicediscovery.plugin.SDClientPlugin','SDBootstrap-TRANS-ONLY-notstub','LOW',0.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0001-SDBootstrap-TRANS-ONLY-notstub','SDBootstrap-TRANS-ONLY-notstub|org.cougaar.servicediscovery.plugin.SDClientPlugin|CONUSGroundTransportationProvider','SDBootstrap-TRANS-ONLY-notstub','LOW',1.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0001-SDBootstrap-TRANS-ONLY-notstub','SDBootstrap-TRANS-ONLY-notstub|org.cougaar.servicediscovery.plugin.SDClientPlugin|CONUSGroundTransportationProvider2','SDBootstrap-TRANS-ONLY-notstub','LOW',2.000000000000000000000000000000);
UNLOCK TABLES;

#
# Dumping data for table 'lib_component'
#

LOCK TABLES lib_component WRITE;
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('recipe|##RECIPE_CLASS##','recipe','##RECIPE_CLASS##','recipe','Added recipe');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.servicediscovery.plugin.SDClientPlugin','plugin','org.cougaar.servicediscovery.plugin.SDClientPlugin','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
UNLOCK TABLES;

#
# Dumping data for table 'lib_mod_recipe'
#

LOCK TABLES lib_mod_recipe WRITE;
REPLACE INTO lib_mod_recipe (MOD_RECIPE_LIB_ID, NAME, JAVA_CLASS, DESCRIPTION) VALUES ('RECIPE-0002SDBootstrap-TRANS-ONLY-notstub','SDBootstrap-TRANS-ONLY-notstub','org.cougaar.tools.csmart.recipe.ComponentCollectionRecipe','Add SDClientPlugin to TRANSCOM, GlobalAir, and GlobalSea when running with TRANSCOM');
UNLOCK TABLES;

#
# Dumping data for table 'lib_mod_recipe_arg'
#

LOCK TABLES lib_mod_recipe_arg WRITE;
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0002SDBootstrap-TRANS-ONLY-notstub','$$CP=org.cougaar.servicediscovery.plugin.SDClientPlugin-0',2.000000000000000000000000000000,'recipeQueryTRANSCOM');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0002SDBootstrap-TRANS-ONLY-notstub','$$CP=org.cougaar.servicediscovery.plugin.SDClientPlugin-1',4.000000000000000000000000000000,'recipeQueryGlobalAir');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0002SDBootstrap-TRANS-ONLY-notstub','$$CP=org.cougaar.servicediscovery.plugin.SDClientPlugin-2',3.000000000000000000000000000000,'recipeQueryGlobalSea');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0002SDBootstrap-TRANS-ONLY-notstub','Assembly Id',0.000000000000000000000000000000,'RCP-0001-SDBootstrap-TRANS-ONLY-notstub');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0002SDBootstrap-TRANS-ONLY-notstub','Target Component Selection Query',1.000000000000000000000000000000,'recipeQuerySelectNothing');
UNLOCK TABLES;

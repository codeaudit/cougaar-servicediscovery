# MySQL dump 8.14
#
# Host: localhost    Database: tempcopy
#--------------------------------------------------------
# Server version	3.23.39-nt

#
# Dumping data for table 'alib_component'
#

LOCK TABLES alib_component WRITE;
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryBootstrapping','ServiceDiscoveryBootstrapping','recipe|##RECIPE_CLASS##','recipe',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDProviderPlugin','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDProviderPlugin','plugin|org.cougaar.servicediscovery.plugin.SDProviderPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider8','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider8','plugin|org.cougaar.servicediscovery.plugin.SDClientPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.MatchmakerStubPlugin','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.MatchmakerStubPlugin','plugin|org.cougaar.servicediscovery.plugin.MatchmakerStubPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider6','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider6','plugin|org.cougaar.servicediscovery.plugin.SDClientPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider7','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider7','plugin|org.cougaar.servicediscovery.plugin.SDClientPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider5','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider5','plugin|org.cougaar.servicediscovery.plugin.SDClientPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider4','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider4','plugin|org.cougaar.servicediscovery.plugin.SDClientPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider2','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider2','plugin|org.cougaar.servicediscovery.plugin.SDClientPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider3','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider3','plugin|org.cougaar.servicediscovery.plugin.SDClientPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider','plugin|org.cougaar.servicediscovery.plugin.SDClientPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin','plugin|org.cougaar.servicediscovery.plugin.SDClientPlugin','plugin',0.000000000000000000000000000000);
UNLOCK TABLES;

#
# Dumping data for table 'asb_agent'
#

LOCK TABLES asb_agent WRITE;
UNLOCK TABLES;

#
# Dumping data for table 'asb_agent_pg_attr'
#

LOCK TABLES asb_agent_pg_attr WRITE;
UNLOCK TABLES;

#
# Dumping data for table 'asb_agent_relation'
#

LOCK TABLES asb_agent_relation WRITE;
UNLOCK TABLES;

#
# Dumping data for table 'asb_assembly'
#

LOCK TABLES asb_assembly WRITE;
INSERT INTO asb_assembly (ASSEMBLY_ID, ASSEMBLY_TYPE, DESCRIPTION) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','RCP','SDClient [1]');
UNLOCK TABLES;

#
# Dumping data for table 'asb_component_arg'
#

LOCK TABLES asb_component_arg WRITE;
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider7','PackagedPOLSupplyProvider',5.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider8','StrategicTransportationProvider',1.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider8','MaterielTransportProvider',2.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider8','SubsistenceSupplyProvider',3.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider7','FuelSupplyProvider',4.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider6','StrategicTransportationProvider',1.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider6','SparePartsDistributor',2.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider7','StrategicTransportationProvider',1.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider7','FuelTransportProvider',2.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider7','MaterielTransportProvider',3.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider5','StrategicTransportationProvider',1.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider5','AmmunitionProvider',2.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin','StrategicTransportationProvider',1.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin','FuelSupplyProvider',2.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin','PackagedPOLSupplyProvider',3.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin','AmmunitionProvider',4.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin','SubsistenceSupplyProvider',5.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin','SparePartsProvider',6.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin','AircraftMaintenanceProvider',7.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider','StrategicTransportationProvider',1.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider2','StrategicTransportationProvider',1.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider2','FuelSupplyProvider',2.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider2','PackagedPOLSupplyProvider',3.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider2','AmmunitionProvider',4.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider2','SubsistenceSupplyProvider',5.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider2','SparePartsProvider',6.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider3','StrategicTransportationProvider',1.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider3','MaterielTransportProvider',2.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider3','SparePartsProvider',3.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider4','StrategicTransportationProvider',1.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider4','MaterielTransportProvider',2.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider4','AmmunitionProvider',3.000000000000000000000000000000);
UNLOCK TABLES;

#
# Dumping data for table 'asb_component_hierarchy'
#

LOCK TABLES asb_component_hierarchy WRITE;
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDProviderPlugin','ServiceDiscoveryBootstrapping','COMPONENT',10.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider8','ServiceDiscoveryBootstrapping','COMPONENT',8.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.MatchmakerStubPlugin','ServiceDiscoveryBootstrapping','COMPONENT',9.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider6','ServiceDiscoveryBootstrapping','COMPONENT',6.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider7','ServiceDiscoveryBootstrapping','COMPONENT',7.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider5','ServiceDiscoveryBootstrapping','COMPONENT',5.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider4','ServiceDiscoveryBootstrapping','COMPONENT',4.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider2','ServiceDiscoveryBootstrapping','COMPONENT',2.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider3','ServiceDiscoveryBootstrapping','COMPONENT',3.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin|StrategicTransportationProvider','ServiceDiscoveryBootstrapping','COMPONENT',1.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin','ServiceDiscoveryBootstrapping','COMPONENT',0.000000000000000000000000000000);
UNLOCK TABLES;

#
# Dumping data for table 'asb_oplan'
#

LOCK TABLES asb_oplan WRITE;
UNLOCK TABLES;

#
# Dumping data for table 'asb_oplan_agent_attr'
#

LOCK TABLES asb_oplan_agent_attr WRITE;
UNLOCK TABLES;

#
# Dumping data for table 'community_attribute'
#

LOCK TABLES community_attribute WRITE;
UNLOCK TABLES;

#
# Dumping data for table 'community_entity_attribute'
#

LOCK TABLES community_entity_attribute WRITE;
UNLOCK TABLES;

#
# Dumping data for table 'lib_agent_org'
#

LOCK TABLES lib_agent_org WRITE;
UNLOCK TABLES;

#
# Dumping data for table 'lib_component'
#

LOCK TABLES lib_component WRITE;
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('recipe|##RECIPE_CLASS##','recipe','##RECIPE_CLASS##','recipe','Added recipe');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.servicediscovery.plugin.SDProviderPlugin','plugin','org.cougaar.servicediscovery.plugin.SDProviderPlugin','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.servicediscovery.plugin.SDClientPlugin','plugin','org.cougaar.servicediscovery.plugin.SDClientPlugin','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.servicediscovery.plugin.MatchmakerStubPlugin','plugin','org.cougaar.servicediscovery.plugin.MatchmakerStubPlugin','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
UNLOCK TABLES;

#
# Dumping data for table 'lib_mod_recipe'
#

LOCK TABLES lib_mod_recipe WRITE;
REPLACE INTO lib_mod_recipe (MOD_RECIPE_LIB_ID, NAME, JAVA_CLASS, DESCRIPTION) VALUES ('RECIPE-0001ServiceDiscoveryBootstrapping','ServiceDiscoveryBootstrapping','org.cougaar.tools.csmart.recipe.ComponentCollectionRecipe','No description available');
UNLOCK TABLES;

#
# Dumping data for table 'lib_mod_recipe_arg'
#

LOCK TABLES lib_mod_recipe_arg WRITE;
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0001ServiceDiscoveryBootstrapping','$$CP=org.cougaar.servicediscovery.plugin.MatchmakerStubPlugin-9',5.000000000000000000000000000000,'recipeQueryBootstrappingAgent');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0001ServiceDiscoveryBootstrapping','$$CP=org.cougaar.servicediscovery.plugin.SDClientPlugin-0',3.000000000000000000000000000000,'recipeQueryStratTransDefaultSupplyAndMaint');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0001ServiceDiscoveryBootstrapping','$$CP=org.cougaar.servicediscovery.plugin.SDClientPlugin-1',2.000000000000000000000000000000,'recipeQueryStratTransOnly');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0001ServiceDiscoveryBootstrapping','$$CP=org.cougaar.servicediscovery.plugin.SDClientPlugin-2',12.000000000000000000000000000000,'recipeQueryStratTransAndDefaultSupply');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0001ServiceDiscoveryBootstrapping','$$CP=org.cougaar.servicediscovery.plugin.SDClientPlugin-3',11.000000000000000000000000000000,'recipeQueryStratMatTransAndSpareParts');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0001ServiceDiscoveryBootstrapping','$$CP=org.cougaar.servicediscovery.plugin.SDClientPlugin-4',10.000000000000000000000000000000,'recipeQueryStratMatTransAndAmmo');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0001ServiceDiscoveryBootstrapping','$$CP=org.cougaar.servicediscovery.plugin.SDClientPlugin-5',9.000000000000000000000000000000,'recipeQueryStratTransAndAmmo');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0001ServiceDiscoveryBootstrapping','$$CP=org.cougaar.servicediscovery.plugin.SDClientPlugin-6',8.000000000000000000000000000000,'recipeQueryStratTransAndSpareParts');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0001ServiceDiscoveryBootstrapping','$$CP=org.cougaar.servicediscovery.plugin.SDClientPlugin-7',7.000000000000000000000000000000,'recipeQueryAllTransAndFuelPOL');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0001ServiceDiscoveryBootstrapping','$$CP=org.cougaar.servicediscovery.plugin.SDClientPlugin-8',6.000000000000000000000000000000,'recipeQueryStratMatTransAndSubsistence');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0001ServiceDiscoveryBootstrapping','$$CP=org.cougaar.servicediscovery.plugin.SDProviderPlugin-10',4.000000000000000000000000000000,'recipeQueryBootstrappingAgent');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0001ServiceDiscoveryBootstrapping','Assembly Id',0.000000000000000000000000000000,'RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0001ServiceDiscoveryBootstrapping','Target Component Selection Query',1.000000000000000000000000000000,'recipeQuerySelectNothing');
UNLOCK TABLES;

#
# Dumping data for table 'lib_pg_attribute'
#

LOCK TABLES lib_pg_attribute WRITE;
UNLOCK TABLES;


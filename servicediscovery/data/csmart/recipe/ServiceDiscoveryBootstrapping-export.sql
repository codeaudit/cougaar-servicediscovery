# MySQL dump 8.16
#
# Host: localhost    Database: tempcopy
#--------------------------------------------------------
# Server version	3.23.43-nt

#
# Dumping data for table 'alib_component'
#

LOCK TABLES alib_component WRITE;
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryBootstrapping','ServiceDiscoveryBootstrapping','recipe|##RECIPE_CLASS##','recipe',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin3','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin3','plugin|org.cougaar.servicediscovery.plugin.SDClientPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin2','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin2','plugin|org.cougaar.servicediscovery.plugin.SDClientPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDProviderPlugin','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDProviderPlugin','plugin|org.cougaar.servicediscovery.plugin.SDProviderPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.MatchmakerStubPlugin','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.MatchmakerStubPlugin','plugin|org.cougaar.servicediscovery.plugin.MatchmakerStubPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin','plugin|org.cougaar.servicediscovery.plugin.SDClientPlugin','plugin',0.000000000000000000000000000000);
UNLOCK TABLES;

#
# Dumping data for table 'asb_assembly'
#

LOCK TABLES asb_assembly WRITE;
REPLACE INTO asb_assembly (ASSEMBLY_ID, ASSEMBLY_TYPE, DESCRIPTION) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','RCP','ServiceDiscoveryBootstrapping');
UNLOCK TABLES;

#
# Dumping data for table 'asb_component_arg'
#

LOCK TABLES asb_component_arg WRITE;
REPLACE INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin3','StrategicTransportationProvider',1.000000000000000000000000000000);
REPLACE INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin','SparePartsProvider',6.000000000000000000000000000000);
REPLACE INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin','SubsistenceSupplyProvider',5.000000000000000000000000000000);
REPLACE INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin','AmmunitionProvider',4.000000000000000000000000000000);
REPLACE INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin','PackagedPOLSupplyProvider',3.000000000000000000000000000000);
REPLACE INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin','FuelSupplyProvider',2.000000000000000000000000000000);
REPLACE INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin','StrategicTransportationProvider',1.000000000000000000000000000000);
UNLOCK TABLES;

#
# Dumping data for table 'asb_component_hierarchy'
#

LOCK TABLES asb_component_hierarchy WRITE;
REPLACE INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin3','ServiceDiscoveryBootstrapping','LOW',4.000000000000000000000000000000);
REPLACE INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin2','ServiceDiscoveryBootstrapping','LOW',3.000000000000000000000000000000);
REPLACE INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDProviderPlugin','ServiceDiscoveryBootstrapping','COMPONENT',2.000000000000000000000000000000);
REPLACE INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.MatchmakerStubPlugin','ServiceDiscoveryBootstrapping','COMPONENT',1.000000000000000000000000000000);
REPLACE INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc','ServiceDiscoveryBootstrapping|org.cougaar.servicediscovery.plugin.SDClientPlugin','ServiceDiscoveryBootstrapping','LOW',0.000000000000000000000000000000);
UNLOCK TABLES;

#
# Dumping data for table 'lib_component'
#

LOCK TABLES lib_component WRITE;
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('recipe|##RECIPE_CLASS##','recipe','##RECIPE_CLASS##','recipe','Added recipe');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.servicediscovery.plugin.SDClientPlugin','plugin','org.cougaar.servicediscovery.plugin.SDClientPlugin','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.servicediscovery.plugin.SDProviderPlugin','plugin','org.cougaar.servicediscovery.plugin.SDProviderPlugin','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.servicediscovery.plugin.MatchmakerStubPlugin','plugin','org.cougaar.servicediscovery.plugin.MatchmakerStubPlugin','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
UNLOCK TABLES;

#
# Dumping data for table 'lib_mod_recipe'
#

LOCK TABLES lib_mod_recipe WRITE;
REPLACE INTO lib_mod_recipe (MOD_RECIPE_LIB_ID, NAME, JAVA_CLASS, DESCRIPTION) VALUES ('RECIPE-0001ServiceDiscoveryBootstrapping','ServiceDiscoveryBootstrapping','org.cougaar.tools.csmart.recipe.ComponentCollectionRecipe','Add basic ServiceDiscovery Plugins');
UNLOCK TABLES;

#
# Dumping data for table 'lib_mod_recipe_arg'
#

LOCK TABLES lib_mod_recipe_arg WRITE;
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0001ServiceDiscoveryBootstrapping','$$CP=org.cougaar.servicediscovery.plugin.SDClientPlugin-0',2.000000000000000000000000000000,'recipeQueryAllClientAgents');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0001ServiceDiscoveryBootstrapping','$$CP=org.cougaar.servicediscovery.plugin.SDClientPlugin-3',3.000000000000000000000000000000,'recipeQueryAllNonClientAgents');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0001ServiceDiscoveryBootstrapping','$$CP=org.cougaar.servicediscovery.plugin.SDClientPlugin-4',4.000000000000000000000000000000,'recipeQueryOSCAgent');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0001ServiceDiscoveryBootstrapping','Assembly Id',0.000000000000000000000000000000,'RCP-0001-ServiceDiscoveryBootstrapping-ServiceDisc');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0001ServiceDiscoveryBootstrapping','Target Component Selection Query',1.000000000000000000000000000000,'recipeQueryAllAgents');
UNLOCK TABLES;



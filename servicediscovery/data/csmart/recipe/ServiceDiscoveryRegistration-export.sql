# MySQL dump 8.14
#
# Host: localhost    Database: tempcopy
#--------------------------------------------------------
# Server version	3.23.39-nt

#
# Dumping data for table 'alib_component'
#

LOCK TABLES alib_component WRITE;
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryRegistration','ServiceDiscoveryRegistration','recipe|##RECIPE_CLASS##','recipe',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryRegistration|org.cougaar.servicediscovery.plugin.SDRegistrationPlugin','ServiceDiscoveryRegistration|org.cougaar.servicediscovery.plugin.SDRegistrationPlugin','plugin|org.cougaar.servicediscovery.plugin.SDRegistrationPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryRegistration|org.cougaar.servicediscovery.servlet.MatchMakerQueryServletComponent','ServiceDiscoveryRegistration|org.cougaar.servicediscovery.servlet.MatchMakerQueryServletComponent','plugin|org.cougaar.servicediscovery.servlet.MatchMakerQueryServletComponent','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryRegistration|org.cougaar.servicediscovery.service.UDDI4JRegistrationServiceComponent','ServiceDiscoveryRegistration|org.cougaar.servicediscovery.service.UDDI4JRegistrationServiceComponent','plugin|org.cougaar.servicediscovery.service.UDDI4JRegistrationServiceComponent','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryRegistration|org.cougaar.servicediscovery.service.UDDI4JRegistryQueryServiceComponent','ServiceDiscoveryRegistration|org.cougaar.servicediscovery.service.UDDI4JRegistryQueryServiceComponent','plugin|org.cougaar.servicediscovery.service.UDDI4JRegistryQueryServiceComponent','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryRegistration|org.cougaar.servicediscovery.plugin.LineagePlugin','ServiceDiscoveryRegistration|org.cougaar.servicediscovery.plugin.LineagePlugin','plugin|org.cougaar.servicediscovery.plugin.LineagePlugin','plugin',0.000000000000000000000000000000);
UNLOCK TABLES;

#
# Dumping data for table 'asb_assembly'
#

LOCK TABLES asb_assembly WRITE;
REPLACE INTO asb_assembly (ASSEMBLY_ID, ASSEMBLY_TYPE, DESCRIPTION) VALUES ('RCP-0001-ServiceDiscoveryRegistration-ServiceDisco','RCP','ServiceDiscoveryRegistration');
UNLOCK TABLES;

#
# Dumping data for table 'asb_component_arg'
#

LOCK TABLES asb_component_arg WRITE;
REPLACE INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryRegistration-ServiceDisco','ServiceDiscoveryRegistration|org.cougaar.servicediscovery.servlet.MatchMakerQueryServletComponent','org.cougaar.servicediscovery.servlet.MatchMakerQueryServlet',1.000000000000000000000000000000);
REPLACE INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryRegistration-ServiceDisco','ServiceDiscoveryRegistration|org.cougaar.servicediscovery.servlet.MatchMakerQueryServletComponent','/matchmaker_query',2.000000000000000000000000000000);
UNLOCK TABLES;

#
# Dumping data for table 'asb_component_hierarchy'
#

LOCK TABLES asb_component_hierarchy WRITE;
REPLACE INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0001-ServiceDiscoveryRegistration-ServiceDisco','ServiceDiscoveryRegistration|org.cougaar.servicediscovery.plugin.SDRegistrationPlugin','ServiceDiscoveryRegistration','COMPONENT',3.000000000000000000000000000000);
REPLACE INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0001-ServiceDiscoveryRegistration-ServiceDisco','ServiceDiscoveryRegistration|org.cougaar.servicediscovery.servlet.MatchMakerQueryServletComponent','ServiceDiscoveryRegistration','COMPONENT',4.000000000000000000000000000000);
REPLACE INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0001-ServiceDiscoveryRegistration-ServiceDisco','ServiceDiscoveryRegistration|org.cougaar.servicediscovery.service.UDDI4JRegistrationServiceComponent','ServiceDiscoveryRegistration','COMPONENT',0.000000000000000000000000000000);
REPLACE INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0001-ServiceDiscoveryRegistration-ServiceDisco','ServiceDiscoveryRegistration|org.cougaar.servicediscovery.service.UDDI4JRegistryQueryServiceComponent','ServiceDiscoveryRegistration','COMPONENT',1.000000000000000000000000000000);
REPLACE INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0001-ServiceDiscoveryRegistration-ServiceDisco','ServiceDiscoveryRegistration|org.cougaar.servicediscovery.plugin.LineagePlugin','ServiceDiscoveryRegistration','COMPONENT',2.000000000000000000000000000000);
UNLOCK TABLES;

#
# Dumping data for table 'lib_component'
#

LOCK TABLES lib_component WRITE;
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('recipe|##RECIPE_CLASS##','recipe','##RECIPE_CLASS##','recipe','Added recipe');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.servicediscovery.plugin.SDRegistrationPlugin','plugin','org.cougaar.servicediscovery.plugin.SDRegistrationPlugin','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.servicediscovery.servlet.MatchMakerQueryServletComponent','plugin','org.cougaar.servicediscovery.servlet.MatchMakerQueryServletComponent','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.servicediscovery.service.UDDI4JRegistrationServiceComponent','plugin','org.cougaar.servicediscovery.service.UDDI4JRegistrationServiceComponent','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.servicediscovery.service.UDDI4JRegistryQueryServiceComponent','plugin','org.cougaar.servicediscovery.service.UDDI4JRegistryQueryServiceComponent','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.servicediscovery.plugin.LineagePlugin','plugin','org.cougaar.servicediscovery.plugin.LineagePlugin','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
UNLOCK TABLES;

#
# Dumping data for table 'lib_mod_recipe'
#

LOCK TABLES lib_mod_recipe WRITE;
REPLACE INTO lib_mod_recipe (MOD_RECIPE_LIB_ID, NAME, JAVA_CLASS, DESCRIPTION) VALUES ('RECIPE-0006ServiceDiscoveryRegistration','ServiceDiscoveryRegistration','org.cougaar.tools.csmart.recipe.ComponentCollectionRecipe','Add SD Plugins to register roles');
UNLOCK TABLES;

#
# Dumping data for table 'lib_mod_recipe_arg'
#

LOCK TABLES lib_mod_recipe_arg WRITE;
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0006ServiceDiscoveryRegistration','$$CP=org.cougaar.servicediscovery.plugin.LineagePlugin-2',3.000000000000000000000000000000,'recipeQueryAllAgents');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0006ServiceDiscoveryRegistration','$$CP=org.cougaar.servicediscovery.plugin.SDRegistrationPlugin-3',2.000000000000000000000000000000,'recipeQueryAllAgents');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0006ServiceDiscoveryRegistration','$$CP=org.cougaar.servicediscovery.service.UDDI4JRegistrationServiceComponent-0',4.000000000000000000000000000000,'recipeQueryAllAgents');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0006ServiceDiscoveryRegistration','$$CP=org.cougaar.servicediscovery.service.UDDI4JRegistryQueryServiceComponent-1',5.000000000000000000000000000000,'recipeQueryAllAgents');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0006ServiceDiscoveryRegistration','$$CP=org.cougaar.servicediscovery.servlet.MatchMakerQueryServletComponent-4',6.000000000000000000000000000000,'recipeQueryAllAgents');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0006ServiceDiscoveryRegistration','Assembly Id',1.000000000000000000000000000000,'RCP-0001-ServiceDiscoveryRegistration-ServiceDisco');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0006ServiceDiscoveryRegistration','Target Component Selection Query',0.000000000000000000000000000000,'recipeQuerySelectNothing');
UNLOCK TABLES;



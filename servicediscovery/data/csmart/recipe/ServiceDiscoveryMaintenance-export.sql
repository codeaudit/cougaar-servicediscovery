# MySQL dump 8.14
#
# Host: localhost    Database: tempcopy
#--------------------------------------------------------
# Server version	3.23.39-nt

#
# Dumping data for table 'alib_component'
#

LOCK TABLES alib_component WRITE;
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryMaintenance','ServiceDiscoveryMaintenance','recipe|##RECIPE_CLASS##','recipe',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryMaintenance|org.cougaar.mlm.plugin.generic.GenericTablePlugin','ServiceDiscoveryMaintenance|org.cougaar.mlm.plugin.generic.GenericTablePlugin','plugin|org.cougaar.mlm.plugin.generic.GenericTablePlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryMaintenance|org.cougaar.mlm.plugin.generic.GenericTablePlugin|supply_generic_table.xml','ServiceDiscoveryMaintenance|org.cougaar.mlm.plugin.generic.GenericTablePlugin|supply_generic_table.xml','plugin|org.cougaar.mlm.plugin.generic.GenericTablePlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryMaintenance|org.cougaar.servicediscovery.plugin.ExpandMaintainTaskPlugin','ServiceDiscoveryMaintenance|org.cougaar.servicediscovery.plugin.ExpandMaintainTaskPlugin','plugin|org.cougaar.servicediscovery.plugin.ExpandMaintainTaskPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryMaintenance|org.cougaar.servicediscovery.plugin.GenerateMaintainTaskPlugin','ServiceDiscoveryMaintenance|org.cougaar.servicediscovery.plugin.GenerateMaintainTaskPlugin','plugin|org.cougaar.servicediscovery.plugin.GenerateMaintainTaskPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryMaintenance|org.cougaar.servicediscovery.servlet.TaskInitiationServlet','ServiceDiscoveryMaintenance|org.cougaar.servicediscovery.servlet.TaskInitiationServlet','plugin|org.cougaar.servicediscovery.servlet.TaskInitiationServlet','plugin',0.000000000000000000000000000000);
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
INSERT INTO asb_assembly (ASSEMBLY_ID, ASSEMBLY_TYPE, DESCRIPTION) VALUES ('RCP-0001-ServiceDiscoveryMaintenance-ServiceDisc','RCP','ServiceDiscoveryAllProviderMMAdditions [1]');
UNLOCK TABLES;

#
# Dumping data for table 'asb_component_arg'
#

LOCK TABLES asb_component_arg WRITE;
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryMaintenance-ServiceDisc','ServiceDiscoveryMaintenance|org.cougaar.mlm.plugin.generic.GenericTablePlugin','maintenance_generic_table.xml',1.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0001-ServiceDiscoveryMaintenance-ServiceDisc','ServiceDiscoveryMaintenance|org.cougaar.mlm.plugin.generic.GenericTablePlugin|supply_generic_table.xml','supply_generic_table.xml',1.000000000000000000000000000000);
UNLOCK TABLES;

#
# Dumping data for table 'asb_component_hierarchy'
#

LOCK TABLES asb_component_hierarchy WRITE;
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0001-ServiceDiscoveryMaintenance-ServiceDisc','ServiceDiscoveryMaintenance|org.cougaar.mlm.plugin.generic.GenericTablePlugin','ServiceDiscoveryMaintenance','COMPONENT',3.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0001-ServiceDiscoveryMaintenance-ServiceDisc','ServiceDiscoveryMaintenance|org.cougaar.mlm.plugin.generic.GenericTablePlugin|supply_generic_table.xml','ServiceDiscoveryMaintenance','COMPONENT',4.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0001-ServiceDiscoveryMaintenance-ServiceDisc','ServiceDiscoveryMaintenance|org.cougaar.servicediscovery.plugin.ExpandMaintainTaskPlugin','ServiceDiscoveryMaintenance','COMPONENT',2.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0001-ServiceDiscoveryMaintenance-ServiceDisc','ServiceDiscoveryMaintenance|org.cougaar.servicediscovery.plugin.GenerateMaintainTaskPlugin','ServiceDiscoveryMaintenance','COMPONENT',0.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0001-ServiceDiscoveryMaintenance-ServiceDisc','ServiceDiscoveryMaintenance|org.cougaar.servicediscovery.servlet.TaskInitiationServlet','ServiceDiscoveryMaintenance','COMPONENT',1.000000000000000000000000000000);
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
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.mlm.plugin.generic.GenericTablePlugin','plugin','org.cougaar.mlm.plugin.generic.GenericTablePlugin','Node.AgentManager.Agent.PluginManager.Plugin','Rule based allocator Plugin. Uses %ALLOCATION_RULE%');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.servicediscovery.plugin.ExpandMaintainTaskPlugin','plugin','org.cougaar.servicediscovery.plugin.ExpandMaintainTaskPlugin','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.servicediscovery.plugin.GenerateMaintainTaskPlugin','plugin','org.cougaar.servicediscovery.plugin.GenerateMaintainTaskPlugin','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.servicediscovery.servlet.TaskInitiationServlet','plugin','org.cougaar.servicediscovery.servlet.TaskInitiationServlet','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
UNLOCK TABLES;

#
# Dumping data for table 'lib_mod_recipe'
#

LOCK TABLES lib_mod_recipe WRITE;
REPLACE INTO lib_mod_recipe (MOD_RECIPE_LIB_ID, NAME, JAVA_CLASS, DESCRIPTION) VALUES ('RECIPE-0001ServiceDiscoveryMaintenanceServiceDiscoveryMaintenance','ServiceDiscoveryMaintenance','org.cougaar.tools.csmart.recipe.ComponentCollectionRecipe','No description available');
UNLOCK TABLES;

#
# Dumping data for table 'lib_mod_recipe_arg'
#

LOCK TABLES lib_mod_recipe_arg WRITE;
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0001ServiceDiscoveryMaintenanceServiceDiscoveryMaintenance','$$CP=org.cougaar.mlm.plugin.generic.GenericTablePlugin-3',3.000000000000000000000000000000,'recipeQueryMaintPassThroughAgent');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0001ServiceDiscoveryMaintenanceServiceDiscoveryMaintenance','$$CP=org.cougaar.mlm.plugin.generic.GenericTablePlugin-4',2.000000000000000000000000000000,'recipeQuery18MAINTBNAgent');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0001ServiceDiscoveryMaintenanceServiceDiscoveryMaintenance','$$CP=org.cougaar.servicediscovery.plugin.ExpandMaintainTaskPlugin-2',4.000000000000000000000000000000,'recipeQuery18MAINTBNAgent');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0001ServiceDiscoveryMaintenanceServiceDiscoveryMaintenance','$$CP=org.cougaar.servicediscovery.plugin.GenerateMaintainTaskPlugin-0',5.000000000000000000000000000000,'recipeQuery2AVNBNAgent');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0001ServiceDiscoveryMaintenanceServiceDiscoveryMaintenance','$$CP=org.cougaar.servicediscovery.servlet.TaskInitiationServlet-1',6.000000000000000000000000000000,'recipeQuery2AVNBNAgent');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0001ServiceDiscoveryMaintenanceServiceDiscoveryMaintenance','Assembly Id',0.000000000000000000000000000000,'RCP-0001-ServiceDiscoveryMaintenance-ServiceDisc');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0001ServiceDiscoveryMaintenanceServiceDiscoveryMaintenance','Target Component Selection Query',1.000000000000000000000000000000,'recipeQuerySelectNothing');
UNLOCK TABLES;

#
# Dumping data for table 'lib_pg_attribute'
#

LOCK TABLES lib_pg_attribute WRITE;
UNLOCK TABLES;


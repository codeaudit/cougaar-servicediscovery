# MySQL dump 8.14
#
# Host: localhost    Database: tempcopy
#--------------------------------------------------------
# Server version	3.23.39-nt

#
# Dumping data for table 'alib_component'
#

LOCK TABLES alib_component WRITE;
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryMatchmaking','ServiceDiscoveryMatchmaking','recipe|##RECIPE_CLASS##','recipe',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryMatchmaking|org.cougaar.servicediscovery.matchmaker.MatchMakerPlugin','ServiceDiscoveryMatchmaking|org.cougaar.servicediscovery.matchmaker.MatchMakerPlugin','plugin|org.cougaar.servicediscovery.matchmaker.MatchMakerPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryMatchmaking|org.cougaar.servicediscovery.matchmaker.MatchMakerServiceProviderComponent','ServiceDiscoveryMatchmaking|org.cougaar.servicediscovery.matchmaker.MatchMakerServiceProviderComponent','plugin|org.cougaar.servicediscovery.matchmaker.MatchMakerServiceProviderComponent','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryMatchmaking|org.cougaar.servicediscovery.plugin.CCADPlugin','ServiceDiscoveryMatchmaking|org.cougaar.servicediscovery.plugin.CCADPlugin','plugin|org.cougaar.servicediscovery.plugin.CCADPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryMatchmaking|org.cougaar.servicediscovery.plugin.DLAPlugin','ServiceDiscoveryMatchmaking|org.cougaar.servicediscovery.plugin.DLAPlugin','plugin|org.cougaar.servicediscovery.plugin.DLAPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryMatchmaking|org.cougaar.servicediscovery.plugin.ODMPlugin','ServiceDiscoveryMatchmaking|org.cougaar.servicediscovery.plugin.ODMPlugin','plugin|org.cougaar.servicediscovery.plugin.ODMPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryMatchmaking|org.cougaar.servicediscovery.plugin.SDProviderPlugin','ServiceDiscoveryMatchmaking|org.cougaar.servicediscovery.plugin.SDProviderPlugin','plugin|org.cougaar.servicediscovery.plugin.SDProviderPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryMatchmaking|org.cougaar.servicediscovery.servlet.MyTaskServlet','ServiceDiscoveryMatchmaking|org.cougaar.servicediscovery.servlet.MyTaskServlet','plugin|org.cougaar.servicediscovery.servlet.MyTaskServlet','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryMatchmaking|org.cougaar.servicediscovery.servlet.SupplyAuxQueryServletComponent','ServiceDiscoveryMatchmaking|org.cougaar.servicediscovery.servlet.SupplyAuxQueryServletComponent','plugin|org.cougaar.servicediscovery.servlet.SupplyAuxQueryServletComponent','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('ServiceDiscoveryMatchmaking|org.cougaar.servicediscovery.servlet.SwitchServlet','ServiceDiscoveryMatchmaking|org.cougaar.servicediscovery.servlet.SwitchServlet','plugin|org.cougaar.servicediscovery.servlet.SwitchServlet','plugin',0.000000000000000000000000000000);
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
INSERT INTO asb_assembly (ASSEMBLY_ID, ASSEMBLY_TYPE, DESCRIPTION) VALUES ('RCP-0003-ServiceDiscoveryMatchmaking','RCP','ServiceDiscoveryMatchmaking');
UNLOCK TABLES;

#
# Dumping data for table 'asb_component_arg'
#

LOCK TABLES asb_component_arg WRITE;
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0003-ServiceDiscoveryMatchmaking','ServiceDiscoveryMatchmaking|org.cougaar.servicediscovery.servlet.SupplyAuxQueryServletComponent','/supplytask_auxquery',2.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0003-ServiceDiscoveryMatchmaking','ServiceDiscoveryMatchmaking|org.cougaar.servicediscovery.servlet.SupplyAuxQueryServletComponent','org.cougaar.servicediscovery.servlet.SupplyAuxQueryServlet',1.000000000000000000000000000000);
UNLOCK TABLES;

#
# Dumping data for table 'asb_component_hierarchy'
#

LOCK TABLES asb_component_hierarchy WRITE;
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0003-ServiceDiscoveryMatchmaking','ServiceDiscoveryMatchmaking|org.cougaar.servicediscovery.matchmaker.MatchMakerPlugin','ServiceDiscoveryMatchmaking','COMPONENT',5.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0003-ServiceDiscoveryMatchmaking','ServiceDiscoveryMatchmaking|org.cougaar.servicediscovery.matchmaker.MatchMakerServiceProviderComponent','ServiceDiscoveryMatchmaking','COMPONENT',4.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0003-ServiceDiscoveryMatchmaking','ServiceDiscoveryMatchmaking|org.cougaar.servicediscovery.plugin.CCADPlugin','ServiceDiscoveryMatchmaking','COMPONENT',1.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0003-ServiceDiscoveryMatchmaking','ServiceDiscoveryMatchmaking|org.cougaar.servicediscovery.plugin.DLAPlugin','ServiceDiscoveryMatchmaking','COMPONENT',0.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0003-ServiceDiscoveryMatchmaking','ServiceDiscoveryMatchmaking|org.cougaar.servicediscovery.plugin.ODMPlugin','ServiceDiscoveryMatchmaking','COMPONENT',6.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0003-ServiceDiscoveryMatchmaking','ServiceDiscoveryMatchmaking|org.cougaar.servicediscovery.plugin.SDProviderPlugin','ServiceDiscoveryMatchmaking','COMPONENT',7.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0003-ServiceDiscoveryMatchmaking','ServiceDiscoveryMatchmaking|org.cougaar.servicediscovery.servlet.MyTaskServlet','ServiceDiscoveryMatchmaking','COMPONENT',2.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0003-ServiceDiscoveryMatchmaking','ServiceDiscoveryMatchmaking|org.cougaar.servicediscovery.servlet.SupplyAuxQueryServletComponent','ServiceDiscoveryMatchmaking','COMPONENT',8.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0003-ServiceDiscoveryMatchmaking','ServiceDiscoveryMatchmaking|org.cougaar.servicediscovery.servlet.SwitchServlet','ServiceDiscoveryMatchmaking','COMPONENT',3.000000000000000000000000000000);
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
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.servicediscovery.matchmaker.MatchMakerPlugin','plugin','org.cougaar.servicediscovery.matchmaker.MatchMakerPlugin','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.servicediscovery.matchmaker.MatchMakerServiceProviderComponent','plugin','org.cougaar.servicediscovery.matchmaker.MatchMakerServiceProviderComponent','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.servicediscovery.plugin.CCADPlugin','plugin','org.cougaar.servicediscovery.plugin.CCADPlugin','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.servicediscovery.plugin.DLAPlugin','plugin','org.cougaar.servicediscovery.plugin.DLAPlugin','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.servicediscovery.plugin.ODMPlugin','plugin','org.cougaar.servicediscovery.plugin.ODMPlugin','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.servicediscovery.plugin.SDProviderPlugin','plugin','org.cougaar.servicediscovery.plugin.SDProviderPlugin','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.servicediscovery.servlet.MyTaskServlet','plugin','org.cougaar.servicediscovery.servlet.MyTaskServlet','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.servicediscovery.servlet.SupplyAuxQueryServletComponent','plugin','org.cougaar.servicediscovery.servlet.SupplyAuxQueryServletComponent','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.servicediscovery.servlet.SwitchServlet','plugin','org.cougaar.servicediscovery.servlet.SwitchServlet','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
UNLOCK TABLES;

#
# Dumping data for table 'lib_mod_recipe'
#

LOCK TABLES lib_mod_recipe WRITE;
REPLACE INTO lib_mod_recipe (MOD_RECIPE_LIB_ID, NAME, JAVA_CLASS, DESCRIPTION) VALUES ('RECIPE-0003ServiceDiscoveryMatchmaking','ServiceDiscoveryMatchmaking','org.cougaar.tools.csmart.recipe.ComponentCollectionRecipe','No description available');
UNLOCK TABLES;

#
# Dumping data for table 'lib_mod_recipe_arg'
#

LOCK TABLES lib_mod_recipe_arg WRITE;
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0003ServiceDiscoveryMatchmaking','$$CP=org.cougaar.servicediscovery.matchmaker.MatchMakerPlugin-5',6.000000000000000000000000000000,'recipeQueryXSBAgent');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0003ServiceDiscoveryMatchmaking','$$CP=org.cougaar.servicediscovery.matchmaker.MatchMakerServiceProviderComponent-4',4.000000000000000000000000000000,'recipeQueryAllNodes');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0003ServiceDiscoveryMatchmaking','$$CP=org.cougaar.servicediscovery.plugin.CCADPlugin-1',8.000000000000000000000000000000,'recipeQueryCCADAgent');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0003ServiceDiscoveryMatchmaking','$$CP=org.cougaar.servicediscovery.plugin.DLAPlugin-0',9.000000000000000000000000000000,'recipeQueryDLAAgent');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0003ServiceDiscoveryMatchmaking','$$CP=org.cougaar.servicediscovery.plugin.ODMPlugin-6',7.000000000000000000000000000000,'recipeQueryODMAgent');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0003ServiceDiscoveryMatchmaking','$$CP=org.cougaar.servicediscovery.plugin.SDProviderPlugin-7',5.000000000000000000000000000000,'recipeQueryXSBAgent');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0003ServiceDiscoveryMatchmaking','$$CP=org.cougaar.servicediscovery.servlet.MyTaskServlet-2',10.000000000000000000000000000000,'recipeQueryARBNAgent');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0003ServiceDiscoveryMatchmaking','$$CP=org.cougaar.servicediscovery.servlet.SupplyAuxQueryServletComponent-8',3.000000000000000000000000000000,'recipeQuerySelectNothing');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0003ServiceDiscoveryMatchmaking','$$CP=org.cougaar.servicediscovery.servlet.SwitchServlet-3',2.000000000000000000000000000000,'recipeQueryODMAgentandCCAD');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0003ServiceDiscoveryMatchmaking','Assembly Id',0.000000000000000000000000000000,'RCP-0003-ServiceDiscoveryMatchmaking');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0003ServiceDiscoveryMatchmaking','Target Component Selection Query',1.000000000000000000000000000000,'recipeQuerySelectNothing');
UNLOCK TABLES;

#
# Dumping data for table 'lib_pg_attribute'
#

LOCK TABLES lib_pg_attribute WRITE;
UNLOCK TABLES;


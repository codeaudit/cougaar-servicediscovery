# MySQL dump 8.16
#
# Host: localhost    Database: tempcopy
#--------------------------------------------------------
# Server version	3.23.43-nt

#
# Dumping data for table 'lib_mod_recipe'
#

LOCK TABLES lib_mod_recipe WRITE;
REPLACE INTO lib_mod_recipe (MOD_RECIPE_LIB_ID, NAME, JAVA_CLASS, DESCRIPTION) VALUES ('RECIPE-0001SDBootstrap-TRANS-STUB-ONLY','SDBootstrap-TRANS-STUB-ONLY','org.cougaar.tools.csmart.recipe.SpecificInsertionRecipe','Add SDClientPlugin to TRANSCOM when running -STUB');
UNLOCK TABLES;

#
# Dumping data for table 'lib_mod_recipe_arg'
#

LOCK TABLES lib_mod_recipe_arg WRITE;
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0001SDBootstrap-TRANS-STUB-ONLY','Class Name',4.000000000000000000000000000000,'org.cougaar.servicediscovery.plugin.SDClientPlugin');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0001SDBootstrap-TRANS-STUB-ONLY','Component Name',2.000000000000000000000000000000,'org.cougaar.servicediscovery.plugin.SDClientPlugin');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0001SDBootstrap-TRANS-STUB-ONLY','Component Priority',5.000000000000000000000000000000,'LOW');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0001SDBootstrap-TRANS-STUB-ONLY','Number of Arguments',0.000000000000000000000000000000,'0');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0001SDBootstrap-TRANS-STUB-ONLY','Target Component Selection Query',1.000000000000000000000000000000,'recipeQueryTRANSCOM');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0001SDBootstrap-TRANS-STUB-ONLY','Type of Insertion',3.000000000000000000000000000000,'plugin');
UNLOCK TABLES;



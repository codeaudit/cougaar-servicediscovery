## INDEX: reflib_clone_set38 
##

CREATE INDEX reflib_clone_set38 ON alib_component(CLONE_SET_ID);
;
## 
## INDEX: reflib_component13 
##

CREATE INDEX reflib_component13 ON alib_component(COMPONENT_LIB_ID);
;

CREATE INDEX reflib_plugin_arg43 ON cfw_context_plugin_arg(PLUGIN_ARG_ID);
;
## 
## INDEX: refcfw_instance44 
##

CREATE INDEX refcfw_instance44 ON cfw_context_plugin_arg(CFW_ID);
;
## 
## INDEX: refcfw_group2 
##

CREATE INDEX refcfw_group2 ON cfw_group_member(CFW_GROUP_ID);
;
## 
## INDEX: refcfw_instance3 
##

CREATE INDEX refcfw_instance3 ON cfw_group_member(CFW_ID);
;
## 
## INDEX: refcfw_group32 
##

CREATE INDEX refcfw_group32 ON cfw_group_org(CFW_GROUP_ID);
;
## 
## INDEX: reflib_organization33 
##

CREATE INDEX reflib_organization33 ON cfw_group_org(ORG_ID);
;

CREATE INDEX reflib_org_group11 ON cfw_org_group_org_member(ORG_GROUP_ID);
;
## 
## INDEX: refcfw_instance60 
##

CREATE INDEX refcfw_instance60 ON cfw_org_group_org_member(CFW_ID);
;
## 
## INDEX: reflib_organization61 
##

CREATE INDEX reflib_organization61 ON cfw_org_group_org_member(ORG_ID);
;
## 
## INDEX: refcfw_instance56 
##

CREATE INDEX refcfw_instance56 ON cfw_org_hierarchy(CFW_ID);
;
## 
## INDEX: reflib_organization57 
##

CREATE INDEX reflib_organization57 ON cfw_org_hierarchy(ORG_ID);
;
## 
## INDEX: reflib_organization59 
##

CREATE INDEX reflib_organization59 ON cfw_org_hierarchy(SUPERIOR_ORG_ID);
;
## 
## INDEX: reflib_organization16 
##

CREATE INDEX reflib_organization16 ON cfw_org_list(ORG_ID);
;
## 
## INDEX: refcfw_instance17 
##

CREATE INDEX refcfw_instance17 ON cfw_org_list(CFW_ID);
;
## 
## INDEX: reflib_org_group20 
##

CREATE INDEX reflib_org_group20 ON cfw_org_og_relation(ORG_GROUP_ID);
;
## 
## INDEX: reflib_role_ref31 
##

CREATE INDEX reflib_role_ref31 ON cfw_org_og_relation(ROLE);
;
## 
## INDEX: reflib_organization52 
##

CREATE INDEX reflib_organization52 ON cfw_org_og_relation(ORG_ID);
;
## 
## INDEX: refcfw_instance53 
##

CREATE INDEX refcfw_instance53 ON cfw_org_og_relation(CFW_ID);
;
## 
## INDEX: refcfw_org_list47 
##

CREATE INDEX refcfw_org_list47 ON cfw_org_orgtype(ORG_ID, CFW_ID);
;
## 
## INDEX: reflib_orgtype_ref48 
##

CREATE INDEX reflib_orgtype_ref48 ON cfw_org_orgtype(ORGTYPE_ID);
;
## 
## INDEX: refcfw_org_list37 
##

CREATE INDEX refcfw_org_list37 ON cfw_org_pg_attr(ORG_ID, CFW_ID);
;
## 
## INDEX: reflib_orgtype_ref21 
##

CREATE INDEX reflib_orgtype_ref21 ON cfw_orgtype_plugin_grp(ORGTYPE_ID);
;
## 
## INDEX: refcfw_instance22 
##

CREATE INDEX refcfw_instance22 ON cfw_orgtype_plugin_grp(CFW_ID);
;
## 
## INDEX: reflib_plugin_group23 
##

CREATE INDEX reflib_plugin_group23 ON cfw_orgtype_plugin_grp(PLUGIN_GROUP_ID);
;
## 
## INDEX: reflib_plugin_ref26 
##

CREATE INDEX reflib_plugin_ref26 ON cfw_plugin_group_member(PLUGIN_ID);
;
## 
## INDEX: reflib_plugin_group27 
##

CREATE INDEX reflib_plugin_group27 ON cfw_plugin_group_member(PLUGIN_GROUP_ID);
;
## 
## INDEX: refcfw_instance28 
##

CREATE INDEX refcfw_instance28 ON cfw_plugin_group_member(CFW_ID);
;

## 
## INDEX: refexpt_experiment26 
##

CREATE INDEX refexpt_experiment26 ON expt_trial(EXPT_ID);
;
## 
## INDEX: Ref1278 
##

CREATE INDEX Ref1278 ON expt_trial_config_assembly(TRIAL_ID);
;
## 
## INDEX: Ref1179 
##

CREATE INDEX Ref1179 ON expt_trial_config_assembly(EXPT_ID);
;
## 
## INDEX: Ref680 
##

CREATE INDEX Ref680 ON expt_trial_config_assembly(ASSEMBLY_ID);
;
## 
## INDEX: refexpt_trial47 
##

CREATE INDEX refexpt_trial47 ON expt_trial_mod_recipe(TRIAL_ID);
;
## 
## INDEX: refexpt_experiment51 
##

CREATE INDEX refexpt_experiment51 ON expt_trial_mod_recipe(EXPT_ID);
;
## 
## INDEX: reflib_mod_recipe56 
##

CREATE INDEX reflib_mod_recipe56 ON expt_trial_mod_recipe(MOD_RECIPE_LIB_ID);
;

## 
## INDEX: reflib_component45 
##

CREATE INDEX reflib_component45 ON lib_agent_org(COMPONENT_LIB_ID);
;

CREATE INDEX reflib_role_ref49 ON lib_role_thread(ROLE);
;
## 
## INDEX: reflib_thread50 
##

CREATE INDEX reflib_thread50 ON lib_role_thread(THREAD_ID);
;
## 
## TABLE: alib_component 
##


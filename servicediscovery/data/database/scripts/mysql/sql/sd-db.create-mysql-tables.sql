##
##ER/Studio 5.1 SQL Code Generation
## Company :      BBNT
## Project :      CSMART Database
## Author :       M. Kappler & J. Berliner
##
## Date Created : Tuesday, July 09, 2002 16:57:59
## Target DBMS : Oracle 8
##


## 
## TABLE: alib_component 
##

CREATE TABLE alib_component(
    COMPONENT_ALIB_ID    VARCHAR(150)    BINARY NOT NULL DEFAULT '',
    COMPONENT_NAME       VARCHAR(150)    BINARY DEFAULT NULL,
    COMPONENT_LIB_ID     VARCHAR(150)    BINARY NOT NULL DEFAULT '',
    COMPONENT_TYPE       VARCHAR(50)    BINARY DEFAULT NULL,
    CLONE_SET_ID         DECIMAL(68,30)          NOT NULL DEFAULT '0.000000000000000000000000000000',
    UNIQUE KEY pk_alib_component (COMPONENT_ALIB_ID)
) TYPE=MyISAM 
;

## 
## TABLE: asb_assembly 
##

CREATE TABLE asb_assembly(
    ASSEMBLY_ID      VARCHAR(50)     BINARY NOT NULL DEFAULT '',
    ASSEMBLY_TYPE    VARCHAR(50)    BINARY DEFAULT NULL,
    DESCRIPTION      VARCHAR(200)    BINARY DEFAULT NULL,
    UNIQUE KEY pk_asb_assembly (ASSEMBLY_ID)
) TYPE=MyISAM 
;



## 
## TABLE: cfw_context_plugin_arg 
##

CREATE TABLE cfw_context_plugin_arg(
    CFW_ID           VARCHAR(50)     BINARY NOT NULL DEFAULT '',
    ORG_CONTEXT      VARCHAR(50)     BINARY NOT NULL DEFAULT '',
    PLUGIN_ARG_ID    VARCHAR(150)    BINARY NOT NULL DEFAULT '',
    UNIQUE KEY pk_cfw_context_plugin_arg (CFW_ID, ORG_CONTEXT, PLUGIN_ARG_ID)
) TYPE=MyISAM 
;


## 
## TABLE: cfw_group 
##

CREATE TABLE cfw_group(
    CFW_GROUP_ID    VARCHAR(50)     BINARY NOT NULL DEFAULT '',
    DESCRIPTION     VARCHAR(100)    BINARY DEFAULT NULL,
    UNIQUE KEY pk_cfw_group (CFW_GROUP_ID)
) TYPE=MyISAM 
;


## COMMENT ON TABLE cfw_group IS 'CFW_GROUP_ID defines a "Society Template"'

## 
## TABLE: cfw_group_member 
##

CREATE TABLE cfw_group_member(
    CFW_ID          VARCHAR(50)    BINARY NOT NULL DEFAULT '',
    CFW_GROUP_ID    VARCHAR(50)    BINARY NOT NULL DEFAULT '',
    UNIQUE KEY pk_cfw_group_member (CFW_ID, CFW_GROUP_ID)
) TYPE=MyISAM 
;


## 
## TABLE: cfw_group_org 
##

CREATE TABLE cfw_group_org(
    CFW_GROUP_ID    VARCHAR(50)    BINARY NOT NULL DEFAULT '',
    ORG_ID          VARCHAR(50)    BINARY NOT NULL DEFAULT '',
    UNIQUE KEY pk_cfw_group_org (CFW_GROUP_ID, ORG_ID)
) TYPE=MyISAM 
;


## 
## TABLE: cfw_instance 
##

CREATE TABLE cfw_instance(
    CFW_ID         VARCHAR(50)     BINARY NOT NULL DEFAULT '',
    DESCRIPTION    VARCHAR(200)    BINARY DEFAULT NULL,
    UNIQUE KEY pk_cfw_instance (CFW_ID)
) TYPE=MyISAM 
;

## 
## TABLE: cfw_org_group_org_member 
##

CREATE TABLE cfw_org_group_org_member(
    CFW_ID          VARCHAR(50)    BINARY NOT NULL DEFAULT '',
    ORG_GROUP_ID    VARCHAR(50)    BINARY NOT NULL DEFAULT '',
    ORG_ID          VARCHAR(50)    BINARY NOT NULL DEFAULT '',
    UNIQUE KEY pk_cfw_org_group_org_member (CFW_ID, ORG_GROUP_ID, ORG_ID)
) TYPE=MyISAM 
;


## 
## TABLE: cfw_org_hierarchy 
##

CREATE TABLE cfw_org_hierarchy(
    CFW_ID             VARCHAR(50)    BINARY NOT NULL DEFAULT '',
    ORG_ID             VARCHAR(50)    BINARY NOT NULL DEFAULT '',
    SUPERIOR_ORG_ID    VARCHAR(50)    BINARY NOT NULL DEFAULT '',
    UNIQUE KEY pk_cfw_org_hierarchy (CFW_ID, ORG_ID)
) TYPE=MyISAM 
;


## 
## TABLE: cfw_org_list 
##

CREATE TABLE cfw_org_list(
    CFW_ID    VARCHAR(50)    BINARY NOT NULL DEFAULT '',
    ORG_ID    VARCHAR(50)    BINARY NOT NULL DEFAULT '',
    UNIQUE KEY pk_cfw_org_list (CFW_ID, ORG_ID)
) TYPE=MyISAM 
;


## 
## TABLE: cfw_org_og_relation 
##

CREATE TABLE cfw_org_og_relation(
    CFW_ID            VARCHAR(50)    BINARY NOT NULL DEFAULT '',
    ROLE              VARCHAR(50)    BINARY NOT NULL DEFAULT '',
    ORG_ID            VARCHAR(50)    BINARY NOT NULL DEFAULT '',
    ORG_GROUP_ID      VARCHAR(50)    BINARY NOT NULL DEFAULT '',
    START_DATE        DATETIME            NOT NULL DEFAULT '0000-00-00 00:00:00',
    END_DATE          DATETIME             DEFAULT NULL,
    RELATION_ORDER    DECIMAL(68,30)         NOT NULL DEFAULT '0.000000000000000000000000000000',
    UNIQUE KEY pk_cfw_org_og_relation (CFW_ID, ROLE, ORG_ID, ORG_GROUP_ID, START_DATE)
) TYPE=MyISAM 
;


## 
## TABLE: cfw_org_orgtype 
##

CREATE TABLE cfw_org_orgtype(
    CFW_ID        VARCHAR(50)    BINARY NOT NULL DEFAULT '',
    ORG_ID        VARCHAR(50)    BINARY NOT NULL DEFAULT '',
    ORGTYPE_ID    VARCHAR(50)    BINARY NOT NULL DEFAULT '',
    UNIQUE KEY pk_cfw_org_orgtype (CFW_ID, ORG_ID, ORGTYPE_ID)
) TYPE=MyISAM 
;


## 
## TABLE: cfw_org_pg_attr 
##

CREATE TABLE cfw_org_pg_attr(
    CFW_ID                 VARCHAR(50)     BINARY NOT NULL DEFAULT '',
    ORG_ID                 VARCHAR(50)     BINARY NOT NULL DEFAULT '',
    PG_ATTRIBUTE_LIB_ID    VARCHAR(100)    BINARY NOT NULL DEFAULT '',
    ATTRIBUTE_VALUE        VARCHAR(50)     BINARY NOT NULL DEFAULT '',
    ATTRIBUTE_ORDER        DECIMAL(68,30)          NOT NULL DEFAULT '0.000000000000000000000000000000',
    START_DATE             DATETIME             NOT NULL DEFAULT '0000-00-00 00:00:00',
    END_DATE               DATETIME             DEFAULT NULL,
    UNIQUE KEY pk_cfw_org_pg_attr (CFW_ID, ORG_ID, PG_ATTRIBUTE_LIB_ID, ATTRIBUTE_VALUE, ATTRIBUTE_ORDER, START_DATE)
) TYPE=MyISAM 
;


## 
## TABLE: cfw_orgtype_plugin_grp 
##

CREATE TABLE cfw_orgtype_plugin_grp(
    CFW_ID             VARCHAR(50)    BINARY NOT NULL DEFAULT '',
    ORGTYPE_ID         VARCHAR(50)    BINARY NOT NULL DEFAULT '',
    PLUGIN_GROUP_ID    VARCHAR(50)    BINARY NOT NULL DEFAULT '',
    UNIQUE KEY pk_cfw_orgtype_plugin_grp (CFW_ID, ORGTYPE_ID, PLUGIN_GROUP_ID)
) TYPE=MyISAM 
;


## 
## TABLE: cfw_plugin_group_member 
##

CREATE TABLE cfw_plugin_group_member(
    CFW_ID                VARCHAR(50)     BINARY NOT NULL DEFAULT '',
    PLUGIN_GROUP_ID       VARCHAR(50)     BINARY NOT NULL DEFAULT '',
    PLUGIN_ID             VARCHAR(100)    BINARY NOT NULL DEFAULT '',
    PLUGIN_CLASS_ORDER    DECIMAL(68,30) DEFAULT NULL,
    UNIQUE KEY pk_cfw_plugin_group_member (CFW_ID, PLUGIN_GROUP_ID, PLUGIN_ID)
) TYPE=MyISAM 
;


## 
## TABLE: expt_experiment 
##

CREATE TABLE expt_experiment(
    EXPT_ID         VARCHAR(50)     BINARY NOT NULL DEFAULT '',
    DESCRIPTION     VARCHAR(200)    BINARY DEFAULT NULL,
    NAME            VARCHAR(50)     BINARY NOT NULL DEFAULT '',
    CFW_GROUP_ID    VARCHAR(50)    BINARY DEFAULT NULL,
    UNIQUE KEY pk_expt_experiment (EXPT_ID)
) TYPE=MyISAM 
;


## 
## TABLE: expt_trial 
##

CREATE TABLE expt_trial(
    TRIAL_ID       VARCHAR(50)     BINARY NOT NULL DEFAULT '',
    EXPT_ID        VARCHAR(50)    BINARY DEFAULT NULL,
    DESCRIPTION    VARCHAR(100)    BINARY DEFAULT NULL,
    NAME           VARCHAR(50)    BINARY DEFAULT NULL,
    UNIQUE KEY pk_expt_trial (TRIAL_ID)
) TYPE=MyISAM 
;

## 
## TABLE: expt_trial_config_assembly 
##

CREATE TABLE expt_trial_config_assembly(
    TRIAL_ID       VARCHAR(50)     BINARY NOT NULL DEFAULT '',
    ASSEMBLY_ID    VARCHAR(50)     BINARY NOT NULL DEFAULT '',
    EXPT_ID        VARCHAR(50)     BINARY NOT NULL DEFAULT '',
    DESCRIPTION    VARCHAR(200)    BINARY DEFAULT NULL,
    UNIQUE KEY pk_expt_trial_config_assembly (TRIAL_ID, ASSEMBLY_ID)
) TYPE=MyISAM 
;


## 
## TABLE: expt_trial_mod_recipe 
##

CREATE TABLE expt_trial_mod_recipe(
    TRIAL_ID             VARCHAR(50)     BINARY NOT NULL DEFAULT '',
    MOD_RECIPE_LIB_ID    VARCHAR(100)    BINARY NOT NULL DEFAULT '',
    RECIPE_ORDER         DECIMAL(68,30)          NOT NULL DEFAULT '0.000000000000000000000000000000',
    EXPT_ID              VARCHAR(50)    BINARY DEFAULT NULL,
    UNIQUE KEY pk_expt_trial_mod_recipe (TRIAL_ID, MOD_RECIPE_LIB_ID, RECIPE_ORDER)
) TYPE=MyISAM 
;


## 
## TABLE: lib_agent_org 
##

CREATE TABLE lib_agent_org(
    COMPONENT_LIB_ID    VARCHAR(150)    BINARY NOT NULL DEFAULT '',
    AGENT_LIB_NAME      VARCHAR(50)    BINARY DEFAULT NULL,
    AGENT_ORG_CLASS     VARCHAR(50)    BINARY DEFAULT NULL,
    UNIQUE KEY pk_lib_agent_org (COMPONENT_LIB_ID)
) TYPE=MyISAM 
;

## 
## TABLE: lib_component 
##

CREATE TABLE lib_component(
    COMPONENT_LIB_ID    VARCHAR(150)    BINARY NOT NULL DEFAULT '',
    COMPONENT_TYPE      VARCHAR(50)    BINARY DEFAULT NULL,
    COMPONENT_CLASS     VARCHAR(100)    BINARY DEFAULT NULL,
    INSERTION_POINT     VARCHAR(50)    BINARY DEFAULT NULL,
    DESCRIPTION         VARCHAR(100)    BINARY DEFAULT NULL,
    UNIQUE KEY pk_lib_component (COMPONENT_LIB_ID)
) TYPE=MyISAM 
;

## 
## TABLE: lib_org_group 
##

CREATE TABLE lib_org_group(
    ORG_GROUP_ID    VARCHAR(50)    BINARY NOT NULL DEFAULT '',
    DESCRIPTION     VARCHAR(50)    BINARY DEFAULT NULL,
    UNIQUE KEY pk_lib_org_group (ORG_GROUP_ID)
) TYPE=MyISAM 
;


## 
## TABLE: lib_organization 
##

CREATE TABLE lib_organization(
    ORG_ID      VARCHAR(50)    BINARY NOT NULL DEFAULT '',
    ORG_NAME    VARCHAR(50)    BINARY DEFAULT NULL,
    UIC         VARCHAR(50)    BINARY DEFAULT NULL,
    UNIQUE KEY pk_lib_organization (ORG_ID)
) TYPE=MyISAM 
;


## 
## TABLE: lib_orgtype_ref 
##

CREATE TABLE lib_orgtype_ref(
    ORGTYPE_ID     VARCHAR(50)     BINARY NOT NULL DEFAULT '',
    DESCRIPTION    VARCHAR(100)    BINARY DEFAULT NULL,
    UNIQUE KEY pk_lib_orgtype_ref (ORGTYPE_ID)
) TYPE=MyISAM 
;

## 
## TABLE: lib_role_ref 
##

CREATE TABLE lib_role_ref(
    ROLE           VARCHAR(50)     BINARY NOT NULL DEFAULT '',
    DESCRIPTION    VARCHAR(100)    BINARY DEFAULT NULL,
    UNIQUE KEY pk_lib_role_ref (ROLE)
) TYPE=MyISAM 
;


## 
## TABLE: lib_role_thread 
##

CREATE TABLE lib_role_thread(
    ROLE           VARCHAR(50)     BINARY NOT NULL DEFAULT '',
    THREAD_ID      VARCHAR(50)     BINARY NOT NULL DEFAULT '',
    DESCRIPTION    VARCHAR(100)    BINARY DEFAULT NULL,
    UNIQUE KEY pk_lib_role_thread (ROLE, THREAD_ID)
) TYPE=MyISAM 
;


## 
## TABLE: lib_thread 
##

CREATE TABLE lib_thread(
    THREAD_ID      VARCHAR(50)     BINARY NOT NULL DEFAULT '',
    DESCRIPTION    VARCHAR(100)    BINARY DEFAULT NULL,
    UNIQUE KEY pk_lib_thread (THREAD_ID)
) TYPE=MyISAM 
;


## 

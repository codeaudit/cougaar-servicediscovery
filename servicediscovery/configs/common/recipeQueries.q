# for sd client plugin parameters
recipeQueryAllClientAgents=\
 SELECT COMPONENT_ALIB_ID FROM alib_component WHERE COMPONENT_TYPE = 'agent' AND COMPONENT_NAME NOT IN ('NCA','USEUCOM','USAEUR','FORSCOM','TRANSCOM','OSC','HNS', 'DLAHQ', 'GlobalAir', 'GlobalSea')

# Do not include TRANSCOM, GlobalAir, or GlobalSea - handled separately
recipeQueryAllNonClientAgents=\
 SELECT COMPONENT_ALIB_ID FROM alib_component WHERE COMPONENT_TYPE = 'agent' AND COMPONENT_NAME IN ('NCA','USEUCOM','USAEUR','FORSCOM','HNS', 'DLAHQ')

recipeQueryDAMLAgent=\
 SELECT COMPONENT_ALIB_ID FROM alib_component WHERE COMPONENT_TYPE = 'agent' AND COMPONENT_NAME='DAML'

# OSC gets exactly one relationship - StrategicTransportation
recipeQueryOSCAgent=\
 SELECT COMPONENT_ALIB_ID FROM alib_component WHERE COMPONENT_TYPE = 'agent' AND COMPONENT_NAME='OSC'

# Now the TRANSCOM queries
recipeQueryGlobalAir=\
 SELECT COMPONENT_ALIB_ID FROM alib_component WHERE COMPONENT_NAME='GlobalAir'

recipeQueryGlobalSea=\
 SELECT COMPONENT_ALIB_ID FROM alib_component WHERE COMPONENT_NAME='GlobalSea'

# FIXME: Should return only if this is -TRANS not -STUB...
recipeQueryTRANSCOM=\
 SELECT COMPONENT_ALIB_ID FROM alib_component WHERE COMPONENT_NAME='TRANSCOM'

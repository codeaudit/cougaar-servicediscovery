# for sd client plugin parameters
recipeQueryAllClientAgents=\
 SELECT COMPONENT_ALIB_ID FROM alib_component WHERE COMPONENT_TYPE = 'agent' AND COMPONENT_NAME NOT IN ('NCA','USEUCOM','USAEUR','FORSCOM','TRANSCOM','OSC','HNS')

recipeQueryDAMLAgent=\
 SELECT COMPONENT_ALIB_ID FROM alib_component WHERE COMPONENT_TYPE = 'agent' AND COMPONENT_NAME='DAML'

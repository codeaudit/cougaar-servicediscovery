Readme for sdtest

Note that old INI based configurations for sdtest have been
removed.

sdtest is a simple 4 agent application used as a basic
plumbing test. It is identical to 
$COUGAAR_INSTALL_PATH/configs/minitestconfig/MiniNode except 
that the agents use service discovery to find their 
StrategicTransportProvider.The version in minitestconfig uses 
relationships defined at initialization by SimpleOrgDataParamPlugin.

sdtest also offers an example of distributed, hierarchical YP 
Communities. communities.xml establishes 2 YP Communities - 
3ID-YPCommunity and 1BDE-YPCommunity which match the command chain 
of the agents, ie. 3-69-ARBN reports to 1BDE which reports to 3ID.
3ID-YPCommunity contains 3ID and 1BDE-YPCommunity. 1BDE-YPCommunity
contains 1BDE and 3-69-ARBN. MCCGlobalMode is not included in the
command chain and hence does not belong to any YP Community. 

Agents looking for providers use their YP Community structure to find
the appropriate YPServers. For example, 3-69-ARBN would look first in
the YPServer for 1BDE-YPCommunity. If it could not find a provider
which met its needs, it would expand the search to 3ID-YPCommunity.

The provider Agents register use their SupportSubordinate
relationships to find the appropriate YPServers with which to 
register. For example, MCCGlobalMode is defined an 3ID's 
SupportSubordinate. (Look at the arguments to 
SimpleOrgDataParamPlugin for the MCCGlobalModeAgent.) The 
SupportSubordinate relationship informs MCCGlobalMode that it 
should register its provider services with the YPServer for 3ID's
YP Commummunity. 

sdtest is useful to verify that your installation is
working, or that recent modifications you have made to
core/util/planning or glm have not seriously broken anything.

It is _not_ a good example of developing plugins or interfaces. It is
based largely on deprecated base classes and development patterns.

To run sdtest, use the Cougaar scripts, as in:

     $COUGAAR_INSTALL_PATH/bin/Cougaar MiniNode.xml MiniNode

This runs the single node named "MiniNode" which is definied in
MiniNode.xml. You can split the agents across multiple Nodes by
editing this XML file. This is useful for a more complete test of the
infrastructure.






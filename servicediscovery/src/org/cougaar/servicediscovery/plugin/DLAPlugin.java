/*
 * <copyright>
 *  Copyright 2003 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA)
 *  and the Defense Logistics Agency (DLA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.servicediscovery.plugin;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.cougaar.core.blackboard.IncrementalSubscription;

import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.mts.MessageAddress;

import org.cougaar.glm.ldm.asset.Organization;

import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.planning.ldm.plan.*;

import org.cougaar.servicediscovery.SDDomain;
import org.cougaar.servicediscovery.SDFactory;

import org.cougaar.servicediscovery.description.ScoredServiceDescription;
import org.cougaar.servicediscovery.description.ServiceBinding;
import org.cougaar.servicediscovery.description.ServiceClassification;
import org.cougaar.servicediscovery.description.ServiceRequest;

import org.cougaar.servicediscovery.matchmaker.MatchMakerQuery;
import org.cougaar.servicediscovery.matchmaker.MatchMakerQueryGenerator;

import org.cougaar.servicediscovery.transaction.MMQueryRequest;
import org.cougaar.servicediscovery.transaction.MMQueryRequestImpl;
import org.cougaar.servicediscovery.transaction.ServiceContractRelay;

import org.cougaar.servicediscovery.util.SoapStub;
import org.cougaar.servicediscovery.util.UDDIConstants;
import org.cougaar.servicediscovery.util.WSDLObject;
import org.cougaar.servicediscovery.util.WSDLParser;

import org.cougaar.util.UnaryPredicate;

/**
 * DLA Plugin: Contains business logic for Defense Logistics Agency. Listens for
 * supply tasks. Query for nsn providers is structured to give preference to
 * C-OTS over C-ODM and C-ODM over M-ODM.<br>
 * Decision Tree:
 * <ul>
 *   <li> If nsn is in stock publishes successful disposition</li>
 *   <li> Else if dla has a long term agreement for nsn</li>
 *   <ul>
 *     <li> If lta-provider speaks cougaar allocate task to provider</li>
 *     <ul>
 *       <li> If allocation is unsuccessful publish query to match maker for nsn
 *       provider
 *       <li>
 *     </ul>
 *
 *     <li> Else if lta-provider speaks soap check for avialability</li>
 *     <ul>
 *       <li> If lta-provider has part in stock publish successful disposition.
 *       </li>
 *       <li> Else publish query to match maker for nsn provider</li>
 *     </ul>
 *
 *   </ul>
 *
 *   <li> Else publish query to match maker for nsn provider</li>
 *   <ul>
 *     <li> If provider speaks cougaar allocate task to provider</li>
 *     <ul>
 *       <li> If allocation is unsuccessful and no more results publish
 *       unsuccessful disposition
 *       <li>
 *     </ul>
 *
 *     <li> Else if provider speaks soap check for avialability</li>
 *     <ul>
 *       <li> If provider has part in stock publish successful disposition.</li>
 *
 *       <li> Else if no more results publish unsuccessful disposition.</li>
 *
 *     </ul>
 *
 *     <li> Publish unsuccessful disposition.</li>
 *   </ul>
 *
 *
 *@author    HSingh
 *@version   $Id: DLAPlugin.java,v 1.3 2003-01-23 20:01:13 mthome Exp $
 */
public class DLAPlugin extends SimplePlugin {
	private IncrementalSubscription mySupplyTaskSubscription;
	private IncrementalSubscription mySupplyAllocSubscription;
	private IncrementalSubscription myMMRequestSubscription;
	private IncrementalSubscription mySelfOrgSubscription;
	private IncrementalSubscription myServiceContractRelaySubscription;

	private LoggingService log;
	private UIDService myUIDService;

	private Hashtable dlaStock;
	private Hashtable dlaMappingNSNPNo;
	private Hashtable dlaMappingNSNCage;
	private Hashtable dlaLTA;
	private Hashtable requestTracker;

	private final static String RELAXED_NSN_QUERY = "Query_for_nsn_provier";
	private final static String PROVIDER_SERVICE_QUERY = "Query_for_provider_info";

	private SDFactory mySDFactory;

	private static Calendar myCalendar = Calendar.getInstance();

	private static long DEFAULT_START_TIME = -1;
	private static long DEFAULT_END_TIME = -1;

	static {
		myCalendar.set(1990, 0, 1, 0, 0, 0);
		DEFAULT_START_TIME = myCalendar.getTime().getTime();

		myCalendar.set(2010, 0, 1, 0, 0, 0);
		DEFAULT_END_TIME = myCalendar.getTime().getTime();
	}

	//Supply allocations made by DLA-AVIATION
	private UnaryPredicate mySupplyAllocPred =
		new UnaryPredicate() {
			public boolean execute(Object o) {
				if(o instanceof Allocation) {
					Task task = ((Allocation) o).getTask();
					return task.getVerb().equals(org.cougaar.glm.ldm.Constants.Verb.SUPPLY);
				}
				return false;
			}
		};

	//Supply tasks allocated to DLA-AVIATION
	private UnaryPredicate mySupplyTaskPred =
		new UnaryPredicate() {
			public boolean execute(Object o) {
				if(o instanceof Task) {
					Task task = (Task) o;
					return task.getVerb().equals(org.cougaar.glm.ldm.Constants.Verb.SUPPLY);
				}
				return false;
			}
		};

	//Matchmaker query requests
	private UnaryPredicate myMMRequestPred =
		new UnaryPredicate() {
			public boolean execute(Object o) {
				return (o instanceof MMQueryRequest);
			}
		};

	//Myself
	private UnaryPredicate mySelfOrgPred =
		new UnaryPredicate() {
			public boolean execute(Object o) {
				if(o instanceof Organization) {
					Organization org = (Organization) o;
					if(org.isLocal()) {
						return true;
					}
				}
				return false;
			}
		};

	//Service contract relays sent by DLA-AVIATION
	private UnaryPredicate myServiceContractRelayPred =
		new UnaryPredicate() {
			public boolean execute(Object o) {
				if(o instanceof ServiceContractRelay) {
					ServiceContractRelay relay = (ServiceContractRelay) o;
					return !(relay.getProviderName().equals(getAgentIdentifier().toString()));
				} else {
					return false;
				}
			}
		};

	/**
	 * Set up blackboard subscriptions. Listens for:
	 * <ul>
	 *   <li> ServiceContractRelays: service contract requests sent out by DLA.
	 *   </li>
	 *   <li> Tasks: Supply tasks allocated to DLA.</li>
	 *   <li> Allocations: allocations made by DLA.</li>
	 *   <li> MatchMaker Query Requests: responses to match maker queries publised
	 *   by DLA.</li>
	 * </ul>
	 *
	 */
	protected void setupSubscriptions() {
		init_dlaDatabases();

		mySupplyTaskSubscription =
			(IncrementalSubscription) subscribe(mySupplyTaskPred);
		myMMRequestSubscription =
			(IncrementalSubscription) subscribe(myMMRequestPred);
		mySupplyAllocSubscription =
			(IncrementalSubscription) subscribe(mySupplyAllocPred);
		mySelfOrgSubscription =
			(IncrementalSubscription) subscribe(mySelfOrgPred);
		myServiceContractRelaySubscription =
			(IncrementalSubscription) subscribe(myServiceContractRelayPred);

		myUIDService =
			(UIDService) getBindingSite().getServiceBroker().getService(this, UIDService.class, null);

		log =
			(LoggingService) getBindingSite().getServiceBroker().getService(this, LoggingService.class, null);

		mySDFactory = (SDFactory) getFactory(SDDomain.SD_NAME);
		if(log.isDebugEnabled()) {
			log.debug("setupSubscriptions: finished setting up subscriptions. ");
		}
	}

	public void execute() {
		Enumeration task_enum = mySupplyTaskSubscription.elements();
		while(task_enum.hasMoreElements()) {
			Task task = (Task) task_enum.nextElement();
			if(task.getPlanElement() != null || requestTracker.containsKey(task)) {
				continue;
			} else {
				handleSupplyTaskSubscription(task);
			}
		}

		if(myMMRequestSubscription.hasChanged()) {
			handleMMRequestSubscription(myMMRequestSubscription.getChangedCollection());
		}

		if(mySupplyAllocSubscription.hasChanged()) {
			handleSupplyAllocSubscription(mySupplyAllocSubscription.getChangedCollection());
		}

		if(myServiceContractRelaySubscription.hasChanged()) {
			handleServiceContractRelaySubscription(myServiceContractRelaySubscription.getChangedCollection());
		}
	}

	/**
	 * If the requested nsn is in stock publishes successful disposition. Next
	 * checks if a long term agreement exists for nsn, if so publishes a query to
	 * match maker for more information about provider. If both checks fail
	 * publishes a query to match maker for nsn providers.
	 *
	 *@param task  Supply task.
	 */
	protected void handleSupplyTaskSubscription(Task task) {
		String nsn =
			task.getDirectObject().getTypeIdentificationPG().getTypeIdentification();
		Info info = new Info(task, nsn);
		requestTracker.put(task, info);

		if(log.isDebugEnabled()) {
			log.debug("handleSupplyTaskSubscription: SupplyTask for : " + nsn);
		}

		if(dlaStock.containsKey(nsn)) {

			if(log.isDebugEnabled()) {
				log.debug("handleSupplyTaskSubscription: found NSN in dla stock : "
					 + (String) dlaStock.get(nsn));
			}

			if(!publishDispos(task, true)) {
				if(log.isDebugEnabled()) {
					log.debug("handleSupplyTaskSubscription: unable to publish "
						 + "successful disposition : NSN in stock.");
				}
			}

			requestTracker.remove(task);

		} else if(dlaLTA.containsKey(nsn)) {
			if(log.isDebugEnabled()) {
				log.debug("handleSupplyTaskSubscription: found LTA for NSN with : "
					 + (String) dlaLTA.get(nsn));
			}

			String providerName = (String) dlaLTA.get(nsn);
			String serviceName = "A_" + providerName + "_Profile";
			info.setProviderName(providerName);
			info.setServiceName(serviceName);
			info.setQueryType(PROVIDER_SERVICE_QUERY);

			requestTracker.put(task, info);
			publishMMQuery(task, info);

		} else {

			if(log.isDebugEnabled()) {
				log.debug("handleSupplyTaskSubscription: NSN not in stock "
					 + "creating mmquery : " + nsn);
			}

			info.setQueryType(RELAXED_NSN_QUERY);

			requestTracker.put(task, info);
			publishMMQuery(task, info);
		}
	}

	/**
	 * Tries to assign task to the first result returned by match maker. If is
	 * unable to find suitable provider tries to relax query. If query cannot be
	 * relaxed further publishes an unsuccessful disposition.
	 * publishRemove(mmRequest) commented out to allow demo visualization to pick
	 * it up.
	 *
	 *@param mmQueryResults  Match Maker query results.
	 */
	protected void handleMMRequestSubscription(Collection mmQueryResults) {
		for(Iterator i = mmQueryResults.iterator(); i.hasNext(); ) {
			MMQueryRequest mmRequest = (MMQueryRequest) i.next();
			boolean assigned = false;
			Task task = (Task) requestTracker.get(mmRequest.getUID());
			Info info = (Info) requestTracker.get(task);

			if(mmRequest.getResult() != null) {
				Collection services = mmRequest.getResult();
				//publishRemove(mmRequest);

				info.setMMServices(services);

				requestTracker.put(task, info);
				for(Iterator j = services.iterator(); j.hasNext() && !assigned; ) {
					ScoredServiceDescription serviceDescription =
						(ScoredServiceDescription) j.next();

					Vector used_services = info.getServices();

					if(used_services == null) {
						used_services = new Vector();
					}
					if(serviceDescription.getProviderName().equals(getAgentIdentifier().toString()) ||
						used_services.contains(serviceDescription)) {
						;
					} else {
						if(log.isDebugEnabled()) {
							log.debug("handleMMRequestSubscription: response for"
								 + " MMQuery:" + serviceDescription.getProviderName());
							log.debug("handleMMRequestSubscription: response for"
								 + " MMQuery:" + serviceDescription.getScore());
						}
						if(assignTask(serviceDescription, task)) {
							assigned = true;
						}
					}
					info.addService(serviceDescription);

					requestTracker.put(task, info);
				}
			}
			if(!assigned) {
				if(info.getQueryType().equals(PROVIDER_SERVICE_QUERY)) {

					if(log.isDebugEnabled()) {
						log.debug("handleMMRequestSubscription: " +
							"PROVIDER_SERVICE_QUERY failed, trying " +
							"RELAXED_NSN_QUERY");
					}

					info.setQueryType(RELAXED_NSN_QUERY);
					info.setServiceName(null);
					info.setProviderName(null);
					info.clearServices();
					info.setMMServices(null);

					requestTracker.put(task, info);
					publishMMQuery(task, info);
				} else {
					//NO MORE RELAXATION POSSIBLE
					if(!publishDispos(task, false)) {

						if(log.isDebugEnabled()) {
							log.debug("handleMMRequestSubscription: unable to "
								 + "publish unsuccessful disposition : no more "
								 + "relaxation.");
						}
					}
				}
			}
		}
	}

	/**
	 * If allocation was successful updates all listeners, otherwise tries one of
	 * the other providers returned by match maker. If no more providers are
	 * avialable tries to generalize query, if query cannot be generalized publihes
	 * an unsuccessful disposition.
	 *
	 *@param SupplyAllocs  Supply allocations.
	 */
	protected void handleSupplyAllocSubscription(Collection SupplyAllocs) {
		for(Iterator i = SupplyAllocs.iterator(); i.hasNext(); ) {
			Allocation alloc = (Allocation) i.next();
			Task task = alloc.getTask();

			if(alloc.getEstimatedResult().isSuccess()) {
				PluginHelper.updateAllocationResult(mySupplyAllocSubscription);
			} else {
				//TRY ONE OF THE OTHER SERVICES RETURNED BY MM

				Info info = (Info) requestTracker.get(task);

				boolean assigned = false;

				for(Iterator j = info.getMMServices().iterator(); j.hasNext() && !assigned; ) {
					ScoredServiceDescription serviceDescription =
						(ScoredServiceDescription) j.next();

					Vector used_services = info.getServices();
					if(serviceDescription.getProviderName().equals(getAgentIdentifier().toString()) ||
						used_services.contains(serviceDescription)) {
						;
					} else {
						if(log.isDebugEnabled()) {
							log.debug("handleSupplyAllocSubscription: response for"
								 + " MMQuery:" + serviceDescription.getProviderName());
							log.debug("handleSupplyAllocSubscription: response for"
								 + " MMQuery:" + serviceDescription.getScore());
						}
						if(assignTask(serviceDescription, task)) {
							assigned = true;
						}
					}
					info.addService(serviceDescription);

					requestTracker.put(task, info);
				}
				if(!assigned) {
					if(info.getQueryType().equals(PROVIDER_SERVICE_QUERY)) {

						if(log.isDebugEnabled()) {
							log.debug("handleSupplyAllocSubscription: " +
								"PROVIDER_SERVICE_QUERY failed, trying " +
								"RELAXED_NSN_QUERY");
						}
						info.setQueryType(RELAXED_NSN_QUERY);
						info.setServiceName(null);
						info.setProviderName(null);
						info.clearServices();
						info.setMMServices(null);

						requestTracker.put(task, info);
						publishMMQuery(task, info);
					} else {
						//NO MORE RELAXATION POSSIBLE

						if(!publishDispos(task, false)) {

							if(log.isDebugEnabled()) {
								log.debug("handleSupplyAllocSubscription: unable"
									 + " to publish unsuccessful disposition : "
									 + "no more relaxation.");
							}
						}
					}
				}
			}
		}
	}

	/**
	 * If it receives a response for service contract publishes allocation to
	 * provider.
	 *
	 *@param serviceRelays  Service Contract Relays.
	 */
	protected void handleServiceContractRelaySubscription(Collection serviceRelays) {
		if(serviceRelays.isEmpty()) {
			return;
		}

		for(Iterator i = serviceRelays.iterator(); i.hasNext(); ) {
			ServiceContractRelay relay = (ServiceContractRelay) i.next();
			Asset providerOrg = relay.getServiceContract().getProvider();
			Task task = (Task) requestTracker.get(relay.getUID());
			String providerName = relay.getProviderName().toLowerCase();

			if(task != null) {
				if(publishAlloc(task, providerOrg)) {

					if(log.isDebugEnabled()) {
						log.debug("handleServiceContractRelaySubscription:task "
							 + "allocated to:" + providerName);
						log.debug("handleServiceContractRelaySubscription:task "
							 + "planelement :" + task.getPlanElement());
					}
				} else {

					if(log.isDebugEnabled()) {
						log.debug("handleServiceContractRelaySubscription:error "
							 + "assigning task to:" + providerName);
						log.debug("handleServiceContractRelaySubscription:task "
							 + "planelement       :" + task.getPlanElement());
					}
				}
				requestTracker.remove(relay.getUID());
			} else {

				if(log.isDebugEnabled()) {
					log.debug("handleServiceContractRelaySubscription: no record"
						 + " of service contract relay to:" + providerName);
				}
			}
			publishRemove(relay);
		}
	}

	/**
	 * Checks if the service has a soap binding, if so checks if part is available.
	 * Returns true if part is available, false otherwise. If service has cougaar
	 * binding assigns task to provider agent returns the result of
	 * publishServiceContractRelay
	 *
	 *@param serviceDescription  ScoredServiceDescription for the service.
	 *@param task                Cougaar Task
	 *@return                    Returns if provider has soap bindings, returns
	 *      true if part is available. If provider has cougaar bindings, returns
	 *      result of publishAdd(serviceContract).
	 */
	protected boolean assignTask(ScoredServiceDescription serviceDescription,
		Task task) {
		String providerName = serviceDescription.getProviderName();
		Collection bindings = serviceDescription.getServiceBindings();
		if(bindings == null || bindings.isEmpty()) {
			if(log.isDebugEnabled()) {
				log.debug("assignTask:Service bindings are == null");
			}

			return false;
		}

		ServiceBinding serviceBinding = (ServiceBinding) bindings.iterator().next();

		if(serviceBinding.getBindingType().equals(ServiceBinding.SOAP_BINDING)) {

			if(isAvailable(task, serviceBinding)) {
				if(!publishDispos(task, true)) {
					if(log.isDebugEnabled()) {
						log.debug("assignTask: unable to publish successful "
							 + "disposition for:" + providerName);
					}
				} else {
					if(log.isDebugEnabled()) {
						log.debug("assignTask: published successful disposition "
							 + "for:" + providerName);
					}
				}
				requestTracker.remove(task);
				return true;
			} else {
				return false;
			}

		} else {
			//GET SERVICE CONTRACT
			if(!publishServiceContractRelay(serviceDescription, task)) {
				if(log.isDebugEnabled()) {
					log.debug("assignTask: unable to publish service contract "
						 + "relay to: " + providerName);
				}
				return false;
			}
			return true;
		}
	}

	/**
	 * Publihes an allocation to an organizational asset.
	 *
	 *@param task   Supply nsn task
	 *@param asset  Organizational asset
	 *@return       Returns result of publishAdd
	 */
	protected boolean publishAlloc(Task task, Asset asset) {
		Info info = (Info) requestTracker.get(task);
		if(info.getAllocation() != null) {
			publishRemove(info.getAllocation());
			info.setAllocation(null);
		}

		boolean isSuccessful = true;
    AllocationResult estAR = PluginHelper.createEstimatedAllocationResult(task, theLDMF, 1.0, isSuccessful);
    estAR.addAuxiliaryQueryInfo(AuxiliaryQueryType.PORT_NAME,
			"Allocating task to:" + asset);

		Allocation allocation = getFactory().createAllocation(task.getPlan(), task,
			asset, estAR, Role.ASSIGNED);
		info.setAllocation(allocation);

		requestTracker.put(task, info);
		try {
			publishAdd(allocation);
		} catch(Exception e) {
			if(log.isDebugEnabled()) {
				log.debug("publishAlloc: exception occurred while trying to "
					 + "publish allocation.");
				e.printStackTrace();
			}
			return false;
		}
		return true;
	}

	/**
	 * Publishes a disposition with specified result.
	 *
	 *@param task          Supply task
	 *@param isSuccessful  Task outcome
	 *@return              Returns result of publishAdd
	 */
	protected boolean publishDispos(Task task, boolean isSuccessful) {
    AllocationResult estAR = PluginHelper.createEstimatedAllocationResult(task, theLDMF, 1.0, isSuccessful);
		estAR.addAuxiliaryQueryInfo(AuxiliaryQueryType.PORT_NAME,
			"Publishing disposition:" + isSuccessful);

		Disposition dispos = getFactory().createDisposition(task.getPlan(), task, estAR);

		try {
			publishAdd(dispos);
		} catch(Exception e) {
			if(log.isDebugEnabled()) {
				log.debug("publishDispos: exception occurred while trying to " +
					"publish disposition.");
				e.printStackTrace();
			}
			return false;
		}
		return true;
	}

	/**
	 * Publishes query to match maker.
	 *
	 *@param task  Supply nsn task.
	 *@param info  Information about the type of query.
	 *@return      Returns the result of publishAdd.
	 */
	protected boolean publishMMQuery(Task task, Info info) {
		String query = null;
		MMQueryRequest mmRequest = null;
		float defaultCutoff = 31;

		if(info.getQueryType().equals(PROVIDER_SERVICE_QUERY)) {
			query =
				MatchMakerQueryGenerator.queryForServiceByName(info.getProviderName(),
				info.getServiceName());
			mmRequest = new MMQueryRequestImpl(new MatchMakerQuery(query));

		} else {
			query = MatchMakerQueryGenerator.dlaNsnQuery(info.getNSN().substring(4));
			mmRequest = new MMQueryRequestImpl(new MatchMakerQuery(query, defaultCutoff));

		}

		mmRequest.setUID(myUIDService.nextUID());
		requestTracker.put(mmRequest.getUID(), task);
		try {
			publishAdd(mmRequest);
		} catch(Exception e) {
			if(log.isDebugEnabled()) {
				log.debug("publishMMQuery: exception occurred while trying to "
					 + "publish query.");
				e.printStackTrace();
			}
			return false;
		}
		return true;
	}

	/**
	 * Publishes a service contract request. If provider does not have a military
	 * classification assumes PartProvider classification.
	 *
	 *@param serviceDescription  Provider serviceDescrition.
	 *@param task                Supply nsn task
	 *@return                    Returns result of publishAdd
	 */
	protected boolean publishServiceContractRelay(ScoredServiceDescription serviceDescription,
		Task task) {
		String providerName = serviceDescription.getProviderName();
		if(log.isDebugEnabled()) {
			log.debug("publishServiceContractRelay: creating service contract "
				 + "relay to:" + providerName);
		}

		for(Iterator iterator = serviceDescription.getServiceClassifications().iterator();
			iterator.hasNext(); ) {
			ServiceClassification serviceClassification =
				(ServiceClassification) iterator.next();

			if(serviceClassification.getClassificationSchemeName().equals(UDDIConstants.MILITARY_SERVICE_SCHEME)) {
				Role role = Role.getRole(serviceClassification.getClassificationName());
				ServiceRequest request =
					mySDFactory.newServiceRequest(getSelfOrg(), role,
					getDefaultPreferences(DEFAULT_START_TIME, DEFAULT_END_TIME));

				ServiceContractRelay relay =
					mySDFactory.newServiceContractRelay(MessageAddress.getMessageAddress(providerName),
					request);

				if(relay.getUID() == null) {
					relay.setUID(myUIDService.nextUID());
				}
				requestTracker.put(relay.getUID(), task);
				try {
					publishAdd(relay);
				} catch(Exception e) {
					if(log.isDebugEnabled()) {
						log.debug("publishServiceContractRelay: exception occurred "
							 + "while trying to publish service contract relay.");
						e.printStackTrace();
					}
					return false;
				}
				return true;
			}
		}
		//assume commercial entity use PartsProvider role
		Role role = Role.getRole("PartProvider");
		ServiceRequest request =
			mySDFactory.newServiceRequest(getSelfOrg(), role,
			getDefaultPreferences(DEFAULT_START_TIME, DEFAULT_END_TIME));
		ServiceContractRelay relay =
			mySDFactory.newServiceContractRelay(MessageAddress.getMessageAddress(providerName), request);
		if(relay.getUID() == null) {
			relay.setUID(myUIDService.nextUID());
		}
		requestTracker.put(relay.getUID(), task);

		try {
			publishAdd(relay);
		} catch(Exception e) {
			if(log.isDebugEnabled()) {
				log.debug("publishServiceContractRelay: exception occurred while"
					 + " trying to publish task.");
				e.printStackTrace();
			}
			return false;
		}
		return true;
	}

	/**
	 * Start and end times for service contract. CCAD does not enforce these
	 * conditions on receipt of a service contract.
	 *
	 *@param startTime  Plugins default start time.
	 *@param endTime    Plugins default end time.
	 *@return           Preferences for service contract.
	 */
	Collection getDefaultPreferences(long startTime, long endTime) {
		ArrayList preferences = new ArrayList(2);

		AspectValue startTAV = TimeAspectValue.create(AspectType.START_TIME, startTime);
		ScoringFunction startScoreFunc =
			ScoringFunction.createStrictlyAtValue(startTAV);
		Preference startPreference =
			getFactory().newPreference(AspectType.START_TIME, startScoreFunc);

		AspectValue endTAV = TimeAspectValue.create(AspectType.END_TIME, endTime);
		ScoringFunction endScoreFunc =
			ScoringFunction.createStrictlyAtValue(endTAV);
		Preference endPreference =
			getFactory().newPreference(AspectType.END_TIME, endScoreFunc);

		preferences.add(startPreference);
		preferences.add(endPreference);

		return preferences;
	}

	/**
	 * Checks if a provider has the nsn stock by invoking the providers SOAP
	 * enabled service.
	 *
	 *@param task            Cougaar task
	 *@param serviceBinding  Location of the service provider wsdl file
	 *@return                True if part is available through the provider web
	 *      site.
	 */
	protected boolean isAvailable(Task task, ServiceBinding serviceBinding) {
		WSDLParser wsdlParser = new WSDLParser();
		WSDLObject wsdlObj = wsdlParser.parse(serviceBinding.getURI());

		if(wsdlObj.hasSoapBinding()) {
			Info info = (Info) requestTracker.get(task);
			String manufacturer = (String) dlaMappingNSNCage.get(info.getNSN());
			String partNumber = (String) dlaMappingNSNPNo.get(info.getNSN());
			SoapStub ss = new SoapStub();

			if(log.isDebugEnabled()) {
				log.debug("isAvailable: checking for availability "
					 + partNumber + " " + manufacturer);
			}

			try {
				return ss.checkAvailability(partNumber, manufacturer, wsdlObj);
			} catch(Exception e) {
				if(log.isDebugEnabled()) {
					log.debug("isAvailable: unable to contact provider.");
				}
				return false;
			}

		} else {
			if(log.isDebugEnabled()) {
				log.debug("isAvailable: unable to parse wsdl document.");
			}

			return false;
		}

	}

	/**
	 * Initializes the information known to the DLA agent:
	 * <ul>
	 *   <li> Inventory</li>
	 *   <li> NSN to Part Number mapping</li>
	 *   <li> NSN to Cage mapping</li>
	 *   <li> Long term agreements</li>
	 * </ul>
	 *
	 */
	protected void init_dlaDatabases() {
		//init stock database
		dlaStock = new Hashtable();
		dlaStock.put("NSN/5930008432366", "Pressure Switch");
		dlaStock.put("NSN/3110005656233", "Bearing, Roller, Rod End");

		//init NSN to pno database
		dlaMappingNSNPNo = new Hashtable();
		dlaMappingNSNPNo.put("NSN/4320012017527", "1p775");
		dlaMappingNSNPNo.put("NSN/5945002010273", "csj-38-70010");
		dlaMappingNSNPNo.put("NSN/4310004145989", "2z203");
		dlaMappingNSNPNo.put("NSN/4710007606205", "LTCT20564-01");
		dlaMappingNSNPNo.put("NSN/1730007603370", "114E5985-10");
		dlaMappingNSNPNo.put("NSN/5930011951836", "1SM1");
		dlaMappingNSNPNo.put("NSN/6105007262754", "3M458");

		//init NSN to cage database
		dlaMappingNSNCage = new Hashtable();
		dlaMappingNSNCage.put("NSN/4320012017527", "GRAINGER");
		dlaMappingNSNCage.put("NSN/5945002010273", "Tyco Electronics");
		dlaMappingNSNCage.put("NSN/4310004145989", "GRAINGER");
		dlaMappingNSNCage.put("NSN/4710007606205", "unknown");
		dlaMappingNSNCage.put("NSN/1730007603370", "unknown");
		dlaMappingNSNCage.put("NSN/5930011951836", "Micro Switch");
		dlaMappingNSNCage.put("NSN/6105007262754", "GRAINGER");

		//init DLAdb of known long term agreements
		dlaLTA = new Hashtable();
		dlaLTA.put("NSN/4320012017527", "GRAINGER");
		dlaLTA.put("NSN/4310004145989", "GRAINGER");

		//init request tracker
		requestTracker = new Hashtable();
	}

	/**
	 *@return   The selfOrg value
	 */
	protected Organization getSelfOrg() {
		for(Iterator iterator = mySelfOrgSubscription.iterator();
			iterator.hasNext(); ) {
			return (Organization) iterator.next();
		}

		return null;
	}

	/**
	 * Bean for holding information associated with a task.
	 *
	 *@author    HSingh
	 *@version   $Id: DLAPlugin.java,v 1.3 2003-01-23 20:01:13 mthome Exp $
	 */
	public class Info {
		Task task;
		String mmQuery;
		Vector services;
		String nsn;
		String queryType;
		String providerName;
		String serviceName;
		Allocation allocation;
		Collection mmServices;

		public Info(Task t, String nsn) {
			this.task = t;
			this.nsn = nsn;
			this.allocation = null;
		}

		public String getNSN() {
			return this.nsn;
		}

		public void setNSN(String nsn) {
			this.nsn = nsn;
		}

		public String getMMQuery() {
			return this.mmQuery;
		}

		public void setMMQuery(String mmQuery) {
			this.mmQuery = mmQuery;
		}

		public void clearServices() {
			this.services = new Vector();
		}

		public Vector getServices() {
			return this.services;
		}

		public void addService(ScoredServiceDescription sd) {
			if(this.services == null) {
				this.services = new Vector();
			}
			this.services.add(sd);
		}

		public Task getTask() {
			return this.task;
		}

		public void setTask(Task task) {
			this.task = task;
		}

		public void setQueryType(String queryType) {
			this.queryType = queryType;
		}

		public String getQueryType() {
			return this.queryType;
		}


		public String getProviderName() {
			return this.providerName;
		}

		public void setProviderName(String providerName) {
			this.providerName = providerName;
		}

		public String getServiceName() {
			return this.serviceName;
		}

		public void setServiceName(String serviceName) {
			this.serviceName = serviceName;
		}

		public void setAllocation(Allocation alloc) {
			this.allocation = alloc;
		}

		public Allocation getAllocation() {
			return this.allocation;
		}

		public void setMMServices(Collection services) {
			this.mmServices = services;
		}

		public Collection getMMServices() {
			return this.mmServices;
		}

		public String toString() {
			String servicesString = "";
			if(this.services != null) {
				for(Iterator i = this.services.iterator(); i.hasNext(); ) {
					servicesString += "Service: "
						 + ((ScoredServiceDescription) i.next()).getProviderName()
						 + "\n";
				}
			}

			return "Task     : " + this.task + "\n"
				 + "Query    : " + this.mmQuery + "\n"
				 + "Services : " + servicesString;
		}
	}

}


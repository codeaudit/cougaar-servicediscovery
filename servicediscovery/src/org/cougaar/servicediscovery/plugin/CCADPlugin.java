package org.cougaar.servicediscovery.plugin;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;


import org.cougaar.core.blackboard.IncrementalSubscription;

import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.planning.plugin.legacy.SimplePlugin;

import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.mts.MessageAddress;

import org.cougaar.glm.ldm.asset.MilitaryOrgPG;
import org.cougaar.glm.ldm.asset.Organization;

import org.cougaar.planning.ldm.asset.Asset;

import org.cougaar.planning.ldm.plan.*;
import org.cougaar.servicediscovery.Constants;

import org.cougaar.servicediscovery.SDDomain;
import org.cougaar.servicediscovery.SDFactory;

import org.cougaar.servicediscovery.description.ScoredServiceDescription;
import org.cougaar.servicediscovery.description.ServiceClassification;
import org.cougaar.servicediscovery.description.ServiceRequest;

import org.cougaar.servicediscovery.matchmaker.MatchMakerQuery;
import org.cougaar.servicediscovery.matchmaker.MatchMakerQueryGenerator;

import org.cougaar.servicediscovery.transaction.MMQueryRequest;
import org.cougaar.servicediscovery.transaction.MMQueryRequestImpl;
import org.cougaar.servicediscovery.transaction.ServiceContractRelay;

import org.cougaar.servicediscovery.util.Switch;
import org.cougaar.servicediscovery.util.UDDIConstants;

import org.cougaar.util.UnaryPredicate;

/**
 * CCAD Plugin contains business logic for the Corpus Cristi Army Depot. The
 * inventory managed by the plugin can be set to zero by using the switch servlet
 * and publishing a switch object with state OFF. Toggling the state back to
 * ON replenishes the inventory.<br>
 * http://&lt;hostname&gt;:&lt;port&gt;/$CCAD/switchServlet<br>
 * Decision Tree:
 * <ul>
 *   <li> If nsn is inventory publishes disposition with successful result.
 *   </li>
 *   <li> Else publishes match maker query to find provider based on support
 *   command assignment. </li>
 *   <ul>
 *     <li> If provider is found requests service contract</li>
 *     <ul>
 *       <li> If service contract is established allocates task to provider.
 *       </li>
 *       <li> Else publishes disposition with unsuccessful result.</li>
 *     </ul>
 *
 *     <li> Else publishes dispotsition with unsuccessful result.</li>
 *   </ul>
 * </ul>
 *
 *
 *@author    HSingh
 *@version   $Id: CCADPlugin.java,v 1.2 2003-01-22 15:50:29 lgoldsto Exp $
 */
public class CCADPlugin extends SimplePlugin {

	private Hashtable ccadStock;
	private Hashtable requestTracker;

	private LoggingService log;

	private IncrementalSubscription myMMRequestSubscription;
	private IncrementalSubscription mySelfOrgSubscription;
	private IncrementalSubscription myServiceContractRelaySubscription;
	private IncrementalSubscription mySupplyAllocSubscription;
	private IncrementalSubscription mySupplyTaskSubscription;
	private IncrementalSubscription mySwitchSubscription;

	private SDFactory mySDFactory;

	private UIDService myUIDService;

	private final static String SUPPORT_TYPE = "SupplyProvider";
	private final static String SERVICE_SCHEME = UDDIConstants.MILITARY_SERVICE_SCHEME;
	private final static float PENALTY = 10;
	private final static float CUTOFF = 22;

	private static Calendar myCalendar = Calendar.getInstance();
	private static long DEFAULT_START_TIME = -1;
	private static long DEFAULT_END_TIME = -1;

	static {
		myCalendar.set(1990, 0, 1, 0, 0, 0);
		DEFAULT_START_TIME = myCalendar.getTime().getTime();

		myCalendar.set(2010, 0, 1, 0, 0, 0);
		DEFAULT_END_TIME = myCalendar.getTime().getTime();
	}

	//Supply allocations made by CCAD
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

	//Supply tasks assigned to CCAD
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

	//My self
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

	//Service contract relays sent by CCAD
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

	// Listens for org.cougaar.servicediscovery.util.Switch objects.
	private UnaryPredicate mySwitchPred =
		new UnaryPredicate() {
			public boolean execute(Object o) {
				return o instanceof Switch;
			}
		};

	/**
	 * Set up blackboard subscriptions. Listens for:
	 * <ul>
	 *   <li> ServiceContractRelays: Service contract requests sent out by CCAD.
	 *   </li>
	 *   <li> Tasks: Supply tasks allocated to CCAD.</li>
	 *   <li> Allocations: Allocations made by CCAD.</li>
	 *   <li> Switch: Switch objects added to the CCAD blackboard.</li>
	 *   <li> MatchMaker Query Requests: Responses to mm queries. </li>
	 * </ul>
	 *
	 */
	protected void setupSubscriptions() {
		init_ccadDatabase();

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
		mySwitchSubscription =
			(IncrementalSubscription) subscribe(mySwitchPred);
		myUIDService =
			(UIDService) getBindingSite().getServiceBroker().getService(this, UIDService.class, null);

		log =
			(LoggingService) getBindingSite().getServiceBroker().getService(this, LoggingService.class, null);

		mySDFactory = (SDFactory) getFactory(SDDomain.SD_NAME);

		if(log.isDebugEnabled()) {
			log.debug("setupSubscriptions: Finished setting up subscriptions.");
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
		if(mySwitchSubscription.hasChanged()) {
			for(Iterator i = mySwitchSubscription.getChangedCollection().iterator(); i.hasNext(); ) {
				Switch sw = (Switch) i.next();
				change_ccadDatabase(sw.getState());
				break;
			}
		}
	}

	/**
	 * If requested nsn is in stock publishes succeessful disposition. If not
	 * publishes query to match maker.
	 *
	 *@param task  Supply task
	 */
	protected void handleSupplyTaskSubscription(Task task) {
		String nsn =
			task.getDirectObject().getTypeIdentificationPG().getTypeIdentification();
		requestTracker.put(task, nsn);

		if(log.isDebugEnabled()) {
			log.debug("handleSupplyTaskSubscription: Supplytask for : " + nsn);
		}

		if(ccadStock.containsKey(nsn)) {
			if(log.isDebugEnabled()) {
				log.debug("handleSupplyTaskSubscription: found NSN in CCAD stock:"
					 + nsn);
			}

      boolean isSuccess = true;
      AllocationResult estAR = PluginHelper.createEstimatedAllocationResult(task, theLDMF, 1.0, isSuccess);
      estAR.addAuxiliaryQueryInfo(AuxiliaryQueryType.PORT_NAME,
                                  "Found NSN in CCAD stock.");

			Disposition dis = getFactory().createDisposition(task.getPlan(), task, estAR);
			requestTracker.remove(task);
			publishAdd(dis);
		} else {
			//PUBLISH MATCHMAKER QUERY
			if(log.isDebugEnabled()) {
				log.debug("handleSupplyTaskSubscription: NSN not in stock "
					 + "creating mmquery : " + nsn);
			}
			publishMMQuery(task);
		}
	}

	/**
	 * Tries to establish service contract with one of the returned results. If no
	 * results are returned or unable to create service contract publishes
	 * unsuccessful disposition. publishRemove(mmRequest) commented out to allow
	 * demo visualization to pick them up.
	 *
	 *@param mmQueryResults  Match Maker query responses
	 */
	protected void handleMMRequestSubscription(Collection mmQueryResults) {
		for(Iterator i = mmQueryResults.iterator(); i.hasNext(); ) {
			MMQueryRequest mmRequest = (MMQueryRequest) i.next();
			boolean assigned = false;

			if(mmRequest.getResult() != null) {
				Collection services = mmRequest.getResult();

				for(Iterator j = services.iterator(); j.hasNext() && !assigned; ) {
					ScoredServiceDescription serviceDescription =
						(ScoredServiceDescription) j.next();

					if(log.isDebugEnabled()) {
						log.debug("handleMMRequestSubscription: response for MMQuery:"
							 + serviceDescription.getProviderName());
						log.debug("handleMMRequestSubscription: repsonse for MMQUery:"
							 + serviceDescription.getScore());
					}

					if(serviceDescription.getProviderName().equals(getAgentIdentifier().toString())) {
						continue;
					}
					if(getServiceContract(serviceDescription,
						(Task) requestTracker.get(mmRequest.getUID()))) {
						assigned = true;
						requestTracker.remove(mmRequest.getUID());
						//publishRemove(mmRequest);
					}
				}
			}
			if(!assigned) {
				Task task = (Task) requestTracker.get(mmRequest.getUID());
				boolean isSuccess = false;
        AllocationResult estAR = PluginHelper.createEstimatedAllocationResult(task, theLDMF, 1.0, isSuccess);
				estAR.addAuxiliaryQueryInfo(AuxiliaryQueryType.FAILURE_REASON,
					"Unable to find viable provider publishing unsuccessful disposition.");

				Disposition dis = getFactory().createDisposition(task.getPlan(), task, estAR);
				publishAdd(dis);

				if(log.isDebugEnabled()) {
					log.debug("handleMMRequestSubscription:Unable to find viable provider" +
						" publishing unsuccessful disposition.");
				}
				//publishRemove(mmRequest);
			}
		}
	}

	/**
	 * Checks for response from service contract requests, assign task to first
	 * provider who successfully responds to service contract request.
	 *
	 *@param serviceRelays  Service contract relays.
	 */
	protected void handleServiceContractRelaySubscription(Collection serviceRelays) {
		if(serviceRelays.isEmpty()) {
			return;
		}

		for(Iterator i = serviceRelays.iterator(); i.hasNext(); ) {
			ServiceContractRelay relay = (ServiceContractRelay) i.next();
			Asset providerOrg = relay.getServiceContract().getProvider();
			Task task = (Task) requestTracker.get(relay.getUID());
			String providerName = relay.getProviderName();

			if(task != null) {
				if(log.isDebugEnabled()) {
					log.debug("handleServiceContractRelaySubscription:established" +
						" service contract with: " + providerName);
				}
				if(assignTask(providerOrg, task)) {
					if(log.isDebugEnabled()) {
						log.debug("handleServiceContractRelaySub:task "
							 + "allocated to:" + providerName);
						log.debug("handleServiceContractRelaySub: plan element:"
							 + task.getPlanElement());
					}
				} else {
					if(log.isDebugEnabled()) {
						log.debug("handleServiceContractRelaySub: unable to "
							 + "allocate task to:" + providerName);
						log.debug("handleServiceContractRelaySub: plan element:"
							 + task.getPlanElement());
					}
				}
				requestTracker.remove(relay.getUID());
			}
			publishRemove(relay);
		}
	}

	/**
	 * Listens for task allocations made by CCAD updates listeners when results are
	 * received.
	 *
	 *@param supplyAllocs  supply task allocations made by ccad.
	 */
	protected void handleSupplyAllocSubscription(Collection supplyAllocs) {
		PluginHelper.updateAllocationResult(mySupplyAllocSubscription);
	}

	/**
	 * Allocates the supply task to the provided organizational asset.
	 *
	 *@param providerOrg  Organizational asset to which task will be assigned.
	 *@param task         Supply task.
	 *@return             Returns true if it was successfully able to publish the
	 *      allocation.
	 */
	protected boolean assignTask(Asset providerOrg, Task task) {
    boolean isSuccess = true;
    AllocationResult estAR = PluginHelper.createEstimatedAllocationResult(task, theLDMF, 0.5, isSuccess);
		estAR.addAuxiliaryQueryInfo(AuxiliaryQueryType.PORT_NAME,
			"Allocating task to ");
		Allocation allocation = getFactory().createAllocation(task.getPlan(), task,
			providerOrg, estAR, Role.ASSIGNED);

		try {
			publishAdd(allocation);
		} catch(Exception e) {
			if(log.isDebugEnabled()) {
				log.debug("assignTask: exception occurred while trying to "
					 + "publish task.");
				e.printStackTrace();
			}
			return false;
		}
		return true;
	}

	/**
	 * Publihes a militaryServiceQuery.
	 *
	 *@param task  Supply task.
	 */
	protected void publishMMQuery(Task task) {
		String myAgentName = getAgentIdentifier().toString();
		String supportLevel = getRequestedSupportEchelon().toUpperCase();

		String query =
			MatchMakerQueryGenerator.militaryServiceQuery(myAgentName,
			supportLevel, SUPPORT_TYPE, SERVICE_SCHEME, PENALTY);
		MMQueryRequest mmRequest = new MMQueryRequestImpl(
			new MatchMakerQuery(query, CUTOFF));

		mmRequest.setUID(myUIDService.nextUID());
		requestTracker.put(mmRequest.getUID(), task);

		if(log.isDebugEnabled()) {
			log.debug("Publishing mmquery, requestedSupportEchelon:"
				 + supportLevel);
		}
		publishAdd(mmRequest);
	}

	/**
	 * Initializes inventory database. Inventory state is binary, parts are either
	 * in stock or out of stock.
	 */
	protected void init_ccadDatabase() {
		ccadStock = new Hashtable();
		ccadStock.put("NSN/5930011951836", "Sensitive Switch");
		ccadStock.put("NSN/6105007262754", "AC Motor");

		requestTracker = new Hashtable();
	}

	protected void change_ccadDatabase(boolean dbswitch) {
		if(dbswitch) {
			ccadStock.put("NSN/5930011951836", "Sensitive Switch");
			ccadStock.put("NSN/6105007262754", "AC Motor");
		} else {
			ccadStock.remove("NSN/5930011951836");
			ccadStock.remove("NSN/6105007262754");
		}
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
	 * Can only establish service contract requests with military organizations or
	 * organizations that have a military classification.
	 *
	 *@param serviceDescription  ScoredServiceDescription for provider
	 *@param task                Supply nsn task
	 *@return                    Returns true if it was able to publish service
	 *      contract request.
	 */
	protected boolean getServiceContract(ScoredServiceDescription serviceDescription, Task task) {
		String providerName = serviceDescription.getProviderName();

		for(Iterator iterator = serviceDescription.getServiceClassifications().iterator();
			iterator.hasNext(); ) {
			ServiceClassification serviceClassification =
				(ServiceClassification) iterator.next();
			if(serviceClassification.getClassificationSchemeName().equals(SERVICE_SCHEME)) {

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

				if(log.isDebugEnabled()) {
					log.debug("getServiceContract: publishing service contract "
						 + "request to : " + providerName);
				}

				requestTracker.put(relay.getUID(), task);
				try {
					publishAdd(relay);
				} catch(Exception e) {
					if(log.isDebugEnabled()) {
						log.debug("getServiceContract: exception occurred while " +
							"trying to publish task.");
						e.printStackTrace();
					}
					return false;
				}
				return true;
			}
		}

		if(log.isDebugEnabled()) {
			log.debug("getServiceContract: unable to decipher role, request failed");
		}
		return false;
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
	 * Checks what type of support the plugin should ask for, returns US-ARMY by
	 * default.
	 *
	 *@return   The requestedSupportEchelon value
	 */
	protected String getRequestedSupportEchelon() {
		Organization selfOrg = getSelfOrg();

		if(selfOrg == null) {
			if(log.isDebugEnabled()) {
				log.debug("getRequestedSupportEchelon(): Agent does not " +
					" have a local Organization Asset.");
			}
			return "";
		} else {
			MilitaryOrgPG militaryOrgPG = selfOrg.getMilitaryOrgPG();
			if(militaryOrgPG != null) {
				String echelon = militaryOrgPG.getRequestedEchelonOfSupport();

				if((echelon != null) &&
					(echelon != "")) {
					return echelon;
				}
			}
			return Constants.MilitaryEchelon.USARMY;
		}
	}

}



/*
 * <copyright>
 *  Copyright 2002 BBNT Solutions, LLC
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
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.servicediscovery;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.domain.Factory;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.HasRelationships;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.LDMServesPlugin;
import org.cougaar.servicediscovery.description.LineageList;
import org.cougaar.servicediscovery.description.LineageListImpl;
import org.cougaar.servicediscovery.description.MMQuery;
import org.cougaar.servicediscovery.description.ServiceContract;
import org.cougaar.servicediscovery.description.ServiceContractImpl;
import org.cougaar.servicediscovery.description.ServiceContractRelationship;
import org.cougaar.servicediscovery.description.ServiceContractRelationshipImpl;
import org.cougaar.servicediscovery.description.ServiceRequest;
import org.cougaar.servicediscovery.description.ServiceRequestImpl;
import org.cougaar.servicediscovery.description.SupportLineageList;
import org.cougaar.servicediscovery.description.SupportLineageListImpl;
import org.cougaar.servicediscovery.transaction.LineageListRelay;
import org.cougaar.servicediscovery.transaction.MMQueryRequest;
import org.cougaar.servicediscovery.transaction.MMQueryRequestImpl;
import org.cougaar.servicediscovery.transaction.ServiceContractRelay;
import org.cougaar.servicediscovery.transaction.DAMLReadyRelay;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;
import org.cougaar.util.TimeSpan;

import java.util.Collection;
import java.util.Iterator;

/**
 * Service discovery factory Domain package definition.
 **/

public class SDFactory implements Factory {
  private static Logger myLogger = Logging.getLogger(SDFactory.class);

  LDMServesPlugin myLDM;

  public SDFactory(LDMServesPlugin ldm) {
    myLDM = ldm;

    /**
     * Don't currently have service discovery specific assets or property
     * groups.
    RootFactory rf = ldm.getFactory();
    rf.addAssetFactory(new org.cougaar.servicediscovery.asset.AssetFactory());
    rf.addPropertyGroupFactory(new org.cougaar.servicediscovery.PropertyGroupFactory());
    */
  }


  /** Generate a new MMQueryRequest
    *@param query - MMQuery to be executed
    * @return MMQueryRequest
    **/
  public MMQueryRequest newMMQueryRequest(MMQuery query) {
    MMQueryRequest mmRequest = new MMQueryRequestImpl(query);
    mmRequest.setUID(myLDM.getUIDServer().nextUID());
    return mmRequest;
  }

  /** Generate a new LineageList
    * @return LineageList
    **/
  public LineageList newLineageList(int type) {
    if (!validLineageListType(type)) {
      myLogger.error("Invalid lineage type: " + type);
      return null;
    } else {
      if (type == LineageList.SUPPORT) {
	return newSupportLineageList();
      } else {
	LineageList lineageList = new LineageListImpl(type);
	lineageList.setUID(myLDM.getUIDServer().nextUID());
	return lineageList;
      }
    }
  }

  /** Generate a new LineageList
    * @return LineageList
    **/
  public LineageList newLineageList(int type, Collection lineage) {
    if (!validLineageListType(type)) {
      myLogger.error("Invalid lineage type: " + type);
      return null;
    } else {
      LineageList lineageList = newLineageList(type);
      lineageList.addAll(lineage);
      return lineageList;
    }
  }

  /** Generate a new SupportLineageList
    * @return SupportLineageList
    **/
  public SupportLineageList newSupportLineageList() {
    SupportLineageList supportLineageList = new SupportLineageListImpl();
    supportLineageList.setUID(myLDM.getUIDServer().nextUID());
    return supportLineageList;
  }

  /** Generate a new SupportLineageList
    * @return SupportLineageList
    **/
  public SupportLineageList newSupportLineageList(String echelon) {
    if (!validMilitaryEchelon(echelon)) {
      myLogger.error("Invalid military echelon: " + echelon);
      return null;
    }
    SupportLineageList supportLineageList = newSupportLineageList();
    supportLineageList.setEchelonOfSupport(echelon);
    return supportLineageList;
  }

  /** Generate a new SupportLineageList
    * @return SupportLineageList
    **/
  public SupportLineageList newSupportLineageList(String echelon,
						  Collection lineage) {
    SupportLineageList supportLineageList = newSupportLineageList(echelon);
    supportLineageList.addAll(lineage);
    return supportLineageList;
  }

  /** Copy an existing LineageList or SupportLineageList
    * @return LineageList
    **/
  public LineageList copyLineageList(LineageList list) {
    LineageList lineageList;

    if (list instanceof SupportLineageList) {
	lineageList = new SupportLineageListImpl((SupportLineageList) list);
      } else {
	lineageList = new LineageListImpl(list);
      }
    lineageList.setUID(list.getUID());
    return lineageList;
  }

  /** Generate a new LineageListRelay
    * @return LineageListRelay
    **/
  public LineageListRelay newLineageListRelay(MessageAddress superior) {
    LineageListRelay lineageListRelay = new LineageListRelay();
    lineageListRelay.setUID(myLDM.getUIDServer().nextUID());
    lineageListRelay.addTarget(superior);
    return lineageListRelay;
  }

  /**
   * validate specified lineage type
   */
  public static boolean validLineageListType(int lineageType) {
    return LineageListImpl.validType(lineageType);
  }

  /**
   * validate specified military echelon
   */
  public static boolean validMilitaryEchelon(String echelon) {
    return Constants.MilitaryEchelon.validMilitaryEchelon(echelon);
  }

  /** Generate a new ServiceRequest
    * @return a ServiceRequest
    **/
  public ServiceRequest newServiceRequest(Asset client, Role serviceRole,
					  Collection servicePreferences) {
    ServiceRequest serviceRequest =
      new ServiceRequestImpl(myLDM.getFactory().cloneInstance(client),
			     serviceRole, servicePreferences);
    return serviceRequest;
  }

  /** Generate a new ServiceContract
    * @return a ServiceContract
    **/
  public ServiceContract newServiceContract(Asset provider, Role serviceRole,
					    Collection servicePreferences) {
    ServiceContract serviceContract =
      new ServiceContractImpl(myLDM.getFactory().cloneInstance(provider),
			     serviceRole, servicePreferences);
    return serviceContract;
  }

  /**
   * revoke a service contract
   */
  public void revokeServiceContract(ServiceContract contract) {
    ServiceContractImpl revokedContract = (ServiceContractImpl) contract;
    revokedContract.revoke();
  }

  /** Generate a new ServiceContractRelay
    * @return ServiceContractRelay
    **/
  public ServiceContractRelay newServiceContractRelay(MessageAddress provider,
						      ServiceRequest request) {
    ServiceContractRelay serviceContractRelay =
      new ServiceContractRelay(request);
    serviceContractRelay.setUID(myLDM.getUIDServer().nextUID());
    serviceContractRelay.addTarget(provider);
    return serviceContractRelay;
  }

  public DAMLReadyRelay newDAMLReadyRelay(MessageAddress damlAgent) {
    DAMLReadyRelay damlReadyRelay = new DAMLReadyRelay();
    damlReadyRelay.setUID(myLDM.getUIDServer().nextUID());
    damlReadyRelay.addTarget(damlAgent);
    return damlReadyRelay;
  }

  /** Generate a new ServiceContractRelationship
    * @return ServiceContractRelationship
    **/
  static public ServiceContractRelationship newServiceContractRelationship(ServiceContractRelay relay,
									   HasRelationships provider,
									   HasRelationships client) {
    Collection contractPreferences = relay.getServiceContract().getServicePreferences();
    long start = (long) getPreference(contractPreferences, AspectType.START_TIME);
    // Use beginning of time as default
    if (start == -1) {
      start = TimeSpan.MIN_VALUE;
    }

    long end = (long) getPreference(contractPreferences, AspectType.END_TIME);
    // Use end of time as defaults
    if (end == -1) {
      end = TimeSpan.MAX_VALUE;
    }

    return new ServiceContractRelationshipImpl(start, end,
					       relay.getServiceContract().getServiceRole(),
					       provider, client, relay);
  }

  /** Return the value associated with a Preference with the
   *  specified AspectType
   *
   * @param preferences Collection of Preferences (will ignore in the Collection
   * which are not Preferences
   * @param aspectType int specifying the AspectType of the Preference
   */
  static public double getPreference(Collection preferences, int aspectType) {
    double result = -1;
    Preference preference = null;

    for (Iterator iterator = preferences.iterator(); iterator.hasNext();) {
      Object next = iterator.next();

      if (next instanceof Preference) {
	Preference testPreference = (Preference) iterator.next();
	if (testPreference.getAspectType() == aspectType) {
	  preference = testPreference;
	  break;
	}
      }
    }

    if (preference != null) {
      result = preference.getScoringFunction().getBest().getValue();
    }
    return result;
  }

}



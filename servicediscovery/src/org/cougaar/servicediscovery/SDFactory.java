/*
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.servicediscovery;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.cougaar.core.domain.Factory;
import org.cougaar.core.mts.MessageAddress;

import org.cougaar.planning.ldm.LDMServesPlugin;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.HasRelationships;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.ScheduleElementImpl;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.planning.ldm.plan.ScheduleImpl;
import org.cougaar.planning.ldm.plan.ScoringFunction;
import org.cougaar.planning.ldm.plan.TimeAspectValue;

import org.cougaar.servicediscovery.description.Lineage;
import org.cougaar.servicediscovery.description.LineageImpl;
import org.cougaar.servicediscovery.description.LineageScheduleElement;
import org.cougaar.servicediscovery.description.LineageScheduleElementImpl;
import org.cougaar.servicediscovery.description.MMQuery;
import org.cougaar.servicediscovery.description.ProviderCapabilities;
import org.cougaar.servicediscovery.description.ProviderCapabilitiesImpl;
import org.cougaar.servicediscovery.description.ServiceContract;
import org.cougaar.servicediscovery.description.ServiceContractImpl;
import org.cougaar.servicediscovery.description.ServiceContractRelationship;
import org.cougaar.servicediscovery.description.ServiceContractRelationshipImpl;
import org.cougaar.servicediscovery.description.ServiceRequest;
import org.cougaar.servicediscovery.description.ServiceRequestImpl;
import org.cougaar.servicediscovery.transaction.LineageRelay;
import org.cougaar.servicediscovery.transaction.MMQueryRequest;
import org.cougaar.servicediscovery.transaction.MMQueryRequestImpl;
import org.cougaar.servicediscovery.transaction.ServiceContractRelay;
import org.cougaar.servicediscovery.transaction.ServiceContractRelayImpl;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;
import org.cougaar.util.MutableTimeSpan;
import org.cougaar.util.TimeSpan;

/**
 * Service discovery factory Domain package definition.
 **/

public class SDFactory implements Factory {
  private static Logger myLogger = Logging.getLogger(SDFactory.class);

  private static Calendar myCalendar = Calendar.getInstance();

  public static final long DEFAULT_START_TIME;
  public static final long DEFAULT_END_TIME;


  static {
    myCalendar.set(2000, 0, 1, 0, 0, 0);
    myCalendar.set(Calendar.MILLISECOND, 0);
    DEFAULT_START_TIME = myCalendar.getTime().getTime();

    myCalendar.set(2010, 0, 1, 0, 0, 0);
    myCalendar.set(Calendar.MILLISECOND, 0);
    DEFAULT_END_TIME = myCalendar.getTime().getTime();
  }

  private LDMServesPlugin myLDM;

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


  /** Generate a new Lineage
    * @return Lineage
    **/
  public Lineage newLineage(int type) {
    Lineage lineage = null;

    if (!validLineageType(type)) {
      myLogger.error("Invalid lineage type: " + type);
    } else {
      lineage = new LineageImpl(type);
      lineage.setUID(myLDM.getUIDServer().nextUID());

      ScheduleImpl schedule = (ScheduleImpl) newLineageSchedule();
      ((LineageImpl) lineage).setSchedule(schedule);
      
    }

    return lineage;
  }


  /** Generate a new Lineage
    * @return Lineage
    **/
  public Lineage newLineage(int type, List list) {
    Lineage lineage = newLineage(type);

    if (lineage != null) {
      ((LineageImpl) lineage).setList(list);
    }

    return lineage;
  }


  /** Generate a new Lineage
    * @return Lineage
    **/
  public Lineage newLineage(int type, List list, TimeSpan timeSpan) {
    Lineage lineage = newLineage(type, list);

    if (lineage != null) {
      ScheduleImpl schedule = (ScheduleImpl) lineage.getSchedule();
      LineageScheduleElement scheduleElement = 
	newLineageScheduleElement(timeSpan);
      schedule.add(scheduleElement);
    }

    return lineage;
  }

  /** Copy an existing Lineage - does not create new UID
    * @return Lineage
    **/
  public Lineage copyLineage(Lineage original){
    Lineage lineage = new LineageImpl(original);
    return lineage;
  }

  /** Generate a new LineageRelay
    * @return LineageRelay
    **/
  public LineageRelay newLineageRelay(MessageAddress superior) {
    LineageRelay lineageRelay = new LineageRelay();
    lineageRelay.setUID(myLDM.getUIDServer().nextUID());
    lineageRelay.addTarget(superior);
    return lineageRelay;
  }

  /**
   * validate specified lineage type
   */
  public static boolean validLineageType(int lineageType) {
    return Lineage.validType(lineageType);
  }

  /** Generate a new Lineage
    * @return Lineage
    **/
  public static Schedule newLineageSchedule() {
    ScheduleImpl lineageSchedule = new ScheduleImpl();

    lineageSchedule.setScheduleElementType(Constants.SDScheduleElementType.LINEAGE);

    return lineageSchedule;
  }

  /** Generate a new LineageScheduleElement
    * @return LineageScheduleElement
    **/
  public static LineageScheduleElement newLineageScheduleElement(TimeSpan timeSpan) {
    return new LineageScheduleElementImpl(timeSpan);
  }

  /** Generate a new LineageScheduleElement
    * @return LineageScheduleElement
    **/
  public static LineageScheduleElement newLineageScheduleElement(long startTime,
								 long endTime) {
    return new LineageScheduleElementImpl(startTime, endTime);
  }

  /**
   * validate specified military echelon
   */
  public static boolean validMilitaryEchelon(String echelon) {
    return Constants.MilitaryEchelon.validMilitaryEchelon(echelon);
  }

  /** Generate a new ProviderCapabilities()
    * @return a ProviderCapabilities
    **/
  public ProviderCapabilities newProviderCapabilities(String providerName) {
    ProviderCapabilities providerCapabilities =
      new ProviderCapabilitiesImpl(providerName);
    providerCapabilities.setUID(myLDM.getUIDServer().nextUID());
    return providerCapabilities;
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
  public static void revokeServiceContract(ServiceContract contract) {
    ServiceContractImpl revokedContract = (ServiceContractImpl) contract;
    revokedContract.revoke();
  }

  /** Generate a new ServiceContractRelay
    * @return ServiceContractRelay
    **/
  public ServiceContractRelay newServiceContractRelay(MessageAddress provider,
						      ServiceRequest request) {
    ServiceContractRelayImpl serviceContractRelay =
      new ServiceContractRelayImpl(request);
    serviceContractRelay.setUID(myLDM.getUIDServer().nextUID());
    serviceContractRelay.addTarget(provider);
    return serviceContractRelay;
  }

  /** Generate a new ServiceContractRelationship
    * @return ServiceContractRelationship
    **/
  static public ServiceContractRelationship newServiceContractRelationship(ServiceContractRelay relay,
									   HasRelationships provider,
									   HasRelationships client) {
    Collection contractPreferences = relay.getServiceContract().getServicePreferences();

    TimeSpan timeSpan = getTimeSpanFromPreferences(contractPreferences);

    return new ServiceContractRelationshipImpl(timeSpan.getStartTime(),
					       timeSpan.getEndTime(),
					       relay.getServiceContract().getServiceRole(),
					       provider, client, relay);
  }

  /** Return the value associated with a Preference with the
   *  specified AspectType
   *
   * @param preferences Collection of Preferences (will ignore in the Collection
   * which are not Preferences
   * @param aspectType int specifying the AspectType of the Preference
   * @return value, -1 if matching Preference not found.
   */
  static public double getPreference(Collection preferences, int aspectType) {
    double result = -1;
    Preference preference = null;

    for (Iterator iterator = preferences.iterator(); iterator.hasNext();) {
      Object next = iterator.next();

      if (next instanceof Preference) {
	Preference testPreference = (Preference) next;
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

  /** 
   * Return the (long) value associated with a Time Preference with the
   * specified AspectType, avoiding long->double->long type conversion.
   *
   * @param preferences Collection of Preferences (will ignore any in the Collection
   * which are not Preferences)
   * @param aspectType int specifying the AspectType of the Preference
   * @return value, -1 if matching Preference not found.
   */
  static public long getTimePreference(Collection preferences, int aspectType) {
    long result = -1;
    Preference preference = null;

    for (Iterator iterator = preferences.iterator(); iterator.hasNext();) {
      Object next = iterator.next();

      if (next instanceof Preference) {
	Preference testPreference = (Preference) next;
	if (testPreference.getAspectType() == aspectType) {
	  preference = testPreference;
	  break;
	}
      }
    }

    if (preference != null) {
      // Because we know this is a time, treat the value as a long
      result = preference.getScoringFunction().getBest().getAspectValue().longValue();
    }
    return result;
  }

  static public Preference findPreference(Collection preferences, int aspectType){
    Preference preference = null;

    for (Iterator iterator = preferences.iterator(); iterator.hasNext();) {
      Object next = iterator.next();

      if (next instanceof Preference) {
        Preference testPreference = (Preference) next;
        if (testPreference.getAspectType() == aspectType) {
          preference = testPreference;
          break;
        }
      }
    }

    return preference;
  }

  public static TimeSpan getTimeSpanFromPreferences(Collection preferences) {
    long preferenceStart = getTimePreference(preferences, Preference.START_TIME);
    long preferenceEnd = getTimePreference(preferences, Preference.END_TIME);
    
    if ((preferenceEnd == -1) ||
	(preferenceStart == -1)) {
      myLogger.error("getTimeSpanFromPreferences: " + 
		     " unable to handle start and/or end time " +
		     " preferences from " + preferences);
      return null;
    }
    

    if (preferenceStart >= preferenceEnd) {
	myLogger.error("getTimeSpanFromPreferences: preferences  - " +
		       preferences + 
		       " - do specify a valid time span.\n" +
		       "Start - " + preferenceStart + 
		       "- >= End - " + preferenceEnd +
		       " Returning null."); 
	return null;
    }

    MutableTimeSpan timeSpan = new MutableTimeSpan();
    timeSpan.setTimeSpan(preferenceStart, preferenceEnd);
    
    return timeSpan;
  }

  public Collection createTimeSpanPreferences(TimeSpan timeSpan) {
    ArrayList preferences = new ArrayList(2);

    AspectValue startTAV = TimeAspectValue.create(AspectType.START_TIME, 
						  timeSpan.getStartTime());
    ScoringFunction startScoreFunc =
      ScoringFunction.createStrictlyAtValue(startTAV);
    Preference startPreference =
      myLDM.getFactory().newPreference(AspectType.START_TIME, startScoreFunc);

    AspectValue endTAV = TimeAspectValue.create(AspectType.END_TIME, 
						timeSpan.getEndTime());
    ScoringFunction endScoreFunc =
      ScoringFunction.createStrictlyAtValue(endTAV);
    Preference endPreference =
      myLDM.getFactory().newPreference(AspectType.END_TIME, endScoreFunc );
    
    preferences.add(startPreference);
    preferences.add(endPreference);

    return preferences;
  }
}




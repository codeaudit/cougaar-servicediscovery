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

package org.cougaar.servicediscovery.plugin;

import java.io.File;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.QuiescenceReportService;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.Disposition;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.planning.ldm.plan.ScheduleElement;
import org.cougaar.planning.ldm.plan.ScheduleElementImpl;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.servicediscovery.SDDomain;
import org.cougaar.servicediscovery.SDFactory;
import org.cougaar.servicediscovery.Constants;
import org.cougaar.servicediscovery.description.AvailabilityChangeMessage;
import org.cougaar.servicediscovery.description.Lineage;
import org.cougaar.servicediscovery.description.ProviderCapabilities;
import org.cougaar.servicediscovery.description.ProviderCapability;
import org.cougaar.servicediscovery.description.ProviderDescription;
import org.cougaar.servicediscovery.description.ProviderDescriptionImpl;
import org.cougaar.servicediscovery.description.ServiceCategory;
import org.cougaar.servicediscovery.description.ServiceClassification;
import org.cougaar.servicediscovery.description.ServiceClassificationImpl;
import org.cougaar.servicediscovery.description.ServiceProfile;
import org.cougaar.servicediscovery.service.RegistrationService;
import org.cougaar.servicediscovery.util.UDDIConstants;
import org.cougaar.util.TimeSpan;
import org.cougaar.util.UnaryPredicate;

/**
 * Read local agent OWL profile file. Use the listed roles and register this agent with those
 * roles in the YP.
 **/
public abstract class SDRegistrationPluginBase extends ComponentPlugin {

  protected static final long DEFAULT_START = TimeSpan.MIN_VALUE;
  protected static final long DEFAULT_END = TimeSpan.MAX_VALUE;

  protected static final String OWL_IDENTIFIER = ".profile.owl";

  protected LoggingService log;
  protected RegistrationService registrationService = null;
  protected DomainService domainService = null;
  protected QuiescenceReportService quiescenceReportService = null;

  protected IncrementalSubscription supportLineageSubscription;
  protected IncrementalSubscription availabilityChangeSubscription;
  protected IncrementalSubscription registerTaskSubscription;

  protected static int WARNING_SUPPRESSION_INTERVAL = 5;
  protected long warningCutoffTime = 0;
  protected static final String REGISTRATION_GRACE_PERIOD_PROPERTY = 
                "org.cougaar.servicediscovery.plugin.RegistrationGracePeriod";
  private static final long DATE_ERROR = Long.MIN_VALUE;
  private static final long HOUR_IN_MILLIS = 3600000;
  private static final long DAY_IN_MILLIS = 86400000;

  protected Alarm retryAlarm;
  protected int knownSCAs = 0;
  long initialTime = parseInitialTime(); //should be 8/10/05 00:05:00
  long parsedAvailStart = DEFAULT_START;
  long parsedAvailEnd = DEFAULT_END;

  protected boolean rehydrated;

  protected ProviderDescription provD = null;
  protected boolean publishProviderCapabilities;


  private UnaryPredicate supportLineagePredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return ((o instanceof Lineage) &&
	      (((Lineage) o).getType() == Lineage.SUPPORT));
    }
  };

  private UnaryPredicate availabilityChangePredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof AvailabilityChangeMessage);
    }
  };

  private UnaryPredicate registerTaskPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return ((o instanceof Task) &&
	      (((Task) o).getVerb().equals(Constants.Verbs.RegisterServices)));
    }
  };

  private UnaryPredicate providerCapabilitiesPredicate = 
  new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof ProviderCapabilities) {
	ProviderCapabilities providerCapabilities = (ProviderCapabilities) o;
	return (providerCapabilities.getProviderName().equals(getAgentIdentifier().toString()));
      } else {
	return false;
      }
    }
  };

  public void setDomainService(DomainService ds) {
    domainService = ds;
  }


  public void setLoggingService(LoggingService log) {
    this.log = log;
  }

  public void setQuiescenceReportService(QuiescenceReportService qrs) {
    quiescenceReportService = qrs;

  }

  public void setRegistrationService(RegistrationService rs) {
    registrationService = rs;
  }

  public AgentIdentificationService getAgentIdentificationService() {
    // Service established down in BlackboardClientComponent
    return agentIdentificationService;
  }


  public void suspend() {
    if (log.isInfoEnabled()) {
      log.info(getAgentIdentifier() + " suspend.");
    }
    super.suspend();

    if (retryAlarm != null) {
      if (log.isInfoEnabled()) {
	log.info(getAgentIdentifier() + " cancelling retryAlarm.");
      }
      retryAlarm.cancel();
    }
  }

  public void load() {
    super.load();

    if (getAgentIdentificationService() != null) {
      quiescenceReportService.setAgentIdentificationService(getAgentIdentificationService());
    }
  }

  public void unload() {
    if (registrationService != null) {
      getBindingSite().getServiceBroker().releaseService(this,
                                                         RegistrationService.class,
                                                         registrationService);
      registrationService = null;
    }

    /* Quiescence reporting support should we decide we need it */
    if (quiescenceReportService != null) {
      getBindingSite().getServiceBroker().releaseService(this,
							 QuiescenceReportService.class,
							 quiescenceReportService);
	quiescenceReportService = null;
    }

    if ((log != null) && (log != LoggingService.NULL)) {
      getBindingSite().getServiceBroker().releaseService(this, LoggingService.class, log);
      log = null;
    }
    super.unload();
  }


  protected void setupSubscriptions() {
    supportLineageSubscription =
        (IncrementalSubscription) getBlackboardService().subscribe(supportLineagePredicate);
    availabilityChangeSubscription =
        (IncrementalSubscription) getBlackboardService().subscribe(availabilityChangePredicate);
    registerTaskSubscription =
        (IncrementalSubscription) getBlackboardService().subscribe(registerTaskPredicate);

    Collection params = getParameters();
    // Optional parameters to this plugin are the number of known sca's - i.e. an int
    // and the start and end time representing the availability of a provider
    //   i.e. AvailabilityStart:0   and AvailabilityEnd:14
    // where the value is an offset from C0 (i.e. 8/15/00 00:00:00) in even hour increments.
    // Currently does NOT handle non even hour offsets.

    if (params.size() > 0) {
      Iterator it = params.iterator();
      while (it.hasNext()) {
        String nextParam =(String) it.next();
        //String numStr = (String) params.iterator().next();
        if (!nextParam.startsWith("Availability")) {
          try {
            knownSCAs = Integer.parseInt(nextParam);
          } catch (NumberFormatException nfe) {
            knownSCAs = 0;
            log.error(getAgentIdentifier() + " invalid SCA count parameter - " + nextParam,
                nfe);
          }
        }
        else {
          int tokenIndex = nextParam.indexOf(":");
          String availabilityParam = nextParam.substring(tokenIndex+1);
          long parsedAvailability =  Long.parseLong(availabilityParam);
          if (nextParam.startsWith("AvailabilityStart")) {
            parsedAvailStart = parsedAvailability;
          }
          else {
            parsedAvailEnd = parsedAvailability;
          }
        }
      }
    } else {
      knownSCAs = 0;
    }

    rehydrated = getBlackboardService().didRehydrate();

    if (rehydrated) {
      Collection pcCollection =
          getBlackboardService().query(providerCapabilitiesPredicate);

      publishProviderCapabilities = (pcCollection.isEmpty());
    } else {
      publishProviderCapabilities = true;
    }
  }

  protected void execute () {
    if (isProvider()) {

      if (publishProviderCapabilities) {
	ProviderCapabilities providerCapabilities = 
	  createProviderCapabilities();

	if (providerCapabilities != null) {
	  getBlackboardService().publishAdd(providerCapabilities);
	  publishProviderCapabilities = false;
	} else {
	  retryErrorLog("Problem getting ProviderDescription, try again later.");
	}
      }
      
      if (availabilityChangeSubscription.hasChanged()) {
	Collection adds = availabilityChangeSubscription.getAddedCollection();
	handleAvailabilityChange(adds);
      }
    }
  }
  
  /* ProviderDescription is big - release resources if we don't need it
   * anymore.
   */
  protected  void clearPD() {
    if ((provD != null) && 
	(log.isDebugEnabled())) {
      log.debug(getAgentIdentifier() + ": clearPD()");
    }
    
    
    provD = null;
  }

  /* Returns null if unable to parse the provider description */
  protected  ProviderDescription getPD() {
    if (provD == null) {
      if (log.isDebugEnabled()) {
	log.debug(getAgentIdentifier() + ": getPD() parsing OWL.");
      }
      
      ProviderDescription pd = new ProviderDescriptionImpl();
      try {
	boolean ok = pd.parseOWL(getAgentIdentifier() + OWL_IDENTIFIER);
	
	if (ok && (pd.getProviderName() != null)) {
	  if (log.isDebugEnabled()) {
	    log.debug(getAgentIdentifier() + 
		      ": getPD() successfully parsed OWL.");
	  }
	  
	  provD = pd;
	} else {
	  if (log.isDebugEnabled()) {
	    log.debug(getAgentIdentifier() + 
		      ": getPD() unable to parse OWL." +
		      " ok = " + ok);
	  }
	}
      } catch (java.util.ConcurrentModificationException cme) {
	// Jena can do a concurrent mod exception. See bug 3052
	// Leave provD uninitialized
	if (log.isDebugEnabled()) {
	  log.debug(getAgentIdentifier() + 
		    ": getPD() ConcurrentModificationException - " +
		    cme);
	}
      }
    }
    return provD;
  }
  
  protected long getWarningCutOffTime() {
    if (warningCutoffTime == 0) {
      WARNING_SUPPRESSION_INTERVAL = Integer.getInteger(REGISTRATION_GRACE_PERIOD_PROPERTY,
							WARNING_SUPPRESSION_INTERVAL).intValue();
      warningCutoffTime = System.currentTimeMillis() + WARNING_SUPPRESSION_INTERVAL*60000;
    }
    
    return warningCutoffTime;
  }
  
  
  protected void resetWarningCutoffTime() {
    warningCutoffTime = -1;
  }

  protected void retryErrorLog(String message) {
    retryErrorLog(message, null);
  }
  
  // When an error occurs, but we'll be retrying later, treat it as a DEBUG
  // at first. After a while it becomes an error.
  protected void retryErrorLog(String message, Throwable e) {
    
    long absTime = getAlarmService().currentTimeMillis()+ 
      (int)(Math.random()*10000) + 1000;
    
    retryAlarm = new RetryAlarm(absTime);
    getAlarmService().addAlarm(retryAlarm);
    
    if(System.currentTimeMillis() > getWarningCutOffTime()) {
      if (e == null)
	log.error(getAgentIdentifier() + message);
      else
	log.error(getAgentIdentifier() + message, e);
    } else if (log.isDebugEnabled()) {
      if (e == null)
	log.debug(getAgentIdentifier() + message);
      else
	log.debug(getAgentIdentifier() + message, e);
    }
  }
  
  protected Collection scaServiceClassifications(Collection supportLineageCollection) {
    Collection serviceClassifications =
      new ArrayList(supportLineageCollection.size());
    for (Iterator iterator = supportLineageCollection.iterator();
	 iterator.hasNext();) {
      Lineage lineage = (Lineage) iterator.next();
      ServiceClassification sca =
	new ServiceClassificationImpl(lineage.getRoot(),
				      lineage.getRoot(),
				      UDDIConstants.SUPPORT_COMMAND_ASSIGNMENT);
      serviceClassifications.add(sca);
    }
    return serviceClassifications;
  }
  
  protected abstract  boolean registrationComplete();

  /* Returns initial version of ProviderCapabilities created from the 
   * provider OWL file.
   */
  protected ProviderCapabilities createProviderCapabilities(){
    ProviderDescription pd = getPD();

    if (pd == null) {
      return null;
    }

    long localC0 = initialTime + (5 * DAY_IN_MILLIS) - 300000;  //take off 5 minutes to get to midnight

    long availabilityStart = localC0 + (parsedAvailStart * HOUR_IN_MILLIS);
    long availabilityEnd =  localC0 + (parsedAvailEnd * HOUR_IN_MILLIS);

    Collection serviceProfiles = pd.getServiceProfiles();

    PlanningFactory planningFactory = 
	(PlanningFactory) domainService.getFactory("planning");
    SDFactory sdFactory = 
      (SDFactory) domainService.getFactory(SDDomain.SD_NAME);
    ProviderCapabilities providerCapabilities = 
      sdFactory.newProviderCapabilities(getAgentIdentifier().toString());

    for (Iterator iterator = serviceProfiles.iterator();
	 iterator.hasNext();) {
      ServiceProfile serviceProfile = (ServiceProfile) iterator.next();
      
      Collection serviceCategories = serviceProfile.getServiceCategories();

      Role role = null;
      String echelon = null;

      for (Iterator scIterator = serviceCategories.iterator();
	   scIterator.hasNext();) {
	ServiceCategory serviceCategory = (ServiceCategory) scIterator.next();
	
	String scheme = serviceCategory.getCategorySchemeName();
	if (scheme.equals(UDDIConstants.MILITARY_ECHELON_SCHEME)) {
	  echelon = serviceCategory.getCategoryName();
	} else if (scheme.equals(UDDIConstants.MILITARY_SERVICE_SCHEME)) {
	  role = Role.getRole(serviceCategory.getCategoryName());
	}

	if ((role != null) &&
	    (echelon != null)) {
          Schedule defaultSchedule = null;
          if (parsedAvailStart == DEFAULT_START) {
            defaultSchedule = planningFactory.newSimpleSchedule(DEFAULT_START, DEFAULT_END);
          }
          else {
            if (log.isInfoEnabled()) {
              log.info("availabilityStart is " + new Date(availabilityStart)
                       + " and availabilityEnd is " +new Date(availabilityEnd)
                       + "for agent "+getAgentIdentifier());
            }
            defaultSchedule = planningFactory.newSimpleSchedule(availabilityStart, availabilityEnd);
          }
          
          providerCapabilities.addCapability(role, echelon, defaultSchedule);
          break;
	}
      }
    }

    return providerCapabilities;
  }

  private boolean reregistrationKludgeNeeded(Task task) {
    PlanElement pe = task.getPlanElement();
    // BOZO - special rehydration kludge for quiesence monitor.
    // Plugin will reregister because it doesn't know whether the previous
    // registration exists but we don't want to perturb quiescence.
    return ((rehydrated) && 
	    (pe != null) &&
	    (pe.getEstimatedResult().getConfidenceRating() == 1.0));
  }

  protected void updateQuiescenceService() {
    if (quiescenceReportService != null) {
      if (registrationComplete()) {
	// Tell the Q Service I'm quiescent
	quiescenceReportService.setQuiescentState();
	if (log.isInfoEnabled()) {
	  log.info(getAgentIdentifier() + 
		   " done with SDRegistration. Now quiescent.");
	}
      } else if (registerTaskSubscription.isEmpty()) {
	// no point in checking for the reregistration kludge
	quiescenceReportService.clearQuiescentState();
	if (log.isInfoEnabled()) {
	  log.info(getAgentIdentifier() + 
		   " waiting to complete registration - not quiescent.");
	}
	return;
      } else {
	for (Iterator iterator = registerTaskSubscription.iterator();
	     iterator.hasNext();) {
	  Task task = (Task) iterator.next();

	  if (!reregistrationKludgeNeeded(task)) {
	    // May be waiting on a callback or a community or an SCA. Say not Q
	    quiescenceReportService.clearQuiescentState();
	    if (log.isInfoEnabled()) {
	      log.info(getAgentIdentifier() + 
		       " waiting to complete registration - not quiescent.");
	    }
	    return;
	  }
	}
	
	// Getting here means that all the registerTasks require the 
	// reregistation kludge -  special 'fix' for quiesence
	// monitor. Plugin will reregister on rehydration because it doesn't
	// know whether the previous registration still exists but we don't 
	// want to perturb quiescence state.
	quiescenceReportService.setQuiescentState();
	if (log.isInfoEnabled()) {
	  log.info(getAgentIdentifier() + 
		   ": updateQuiescenceService() " +
		   " setting quiescent state even though reregistration " +
		   " after rehydration is not complete\n" +
		   " rehydrated = " + rehydrated + 
		   " register tasks = " + registerTaskSubscription);
	}
      }
    }
  }


  protected void updateRegisterTaskDispositions() {
    PlanningFactory planningFactory = 
      (PlanningFactory) domainService.getFactory("planning");
    
    for (Iterator iterator = registerTaskSubscription.iterator();
	 iterator.hasNext();) {
      Task task = (Task) iterator.next();
      PlanElement pe = task.getPlanElement();
      double conf;
      if (!registrationComplete()) {
	// BOZO - special rehydration kludge for quiesence monitor.
	// Plugin will reregister because it doesn't know whether the previous
	// registration exists but does not downgrade the confidence as the  
	// change would propagate via GLSExpander back to NCA. 
	if (reregistrationKludgeNeeded(task)) {
	  if (log.isWarnEnabled()) {
	    log.warn(getAgentIdentifier() + 
		     ": updateRegisterTaskDisposition() " +
		     "leaving confidence at 1.0 after rehydration even though " +
		     "reregistration is not complete.");
	  }
	  conf = 1.0;
	} else {
	  if ((pe != null) &&
	      (pe.getEstimatedResult().getConfidenceRating() == 1.0)) {
	    if (log.isDebugEnabled()) {
	      log.debug(getAgentIdentifier() + 
		       ": updateRegisterTaskDisposition() " +
		       "changing confidence back to 0.0." +
		       " rehydrated == " + rehydrated);
	    }
	  }
	  conf = 0.0;
	}
      } else {
	// ProviderDescription is big - release resources since we're
	// done with registration
	clearPD();

	conf = 1.0;
      }

      AllocationResult estResult =
	PluginHelper.createEstimatedAllocationResult(task,
						     planningFactory,
						     conf,
						     true);

      if (pe == null) {
	if (log.isInfoEnabled()) {
	  log.info(getAgentIdentifier() +
		   " adding a disposition to RegisterServices task, confidence rating is  " +
		   conf);
	}
	Disposition disposition =
	  planningFactory.createDisposition(task.getPlan(), task, estResult);
	getBlackboardService().publishAdd(disposition);
      } else if (pe.getEstimatedResult().getConfidenceRating() != conf) {
	double previousConf = pe.getEstimatedResult().getConfidenceRating();
	
	// If we're backing up from a Confidence of 1.0 to lower, like at rehydration
	// when we don't know whether the YP lost our registration,
	// be a little more verbose.
        if (previousConf == 1.0) {
	  log.warn(getAgentIdentifier() + " SDRegistrationPlugin is " +
		   " changing RegisterServices confidence rating from " +
		   previousConf + " to " +
		   conf + ". Rehydrated?");
	} else if (log.isInfoEnabled()) {
	  log.info(getAgentIdentifier() +
		   " changing RegisterServices confidence rating from " +
		   previousConf + " to " +
		   conf);
	}
	pe.setEstimatedResult(estResult);
	getBlackboardService().publishChange(pe);
      }
      
      if (conf == 1.0) {
	resetWarningCutoffTime();
      }
    }
  }

  private long parseInitialTime() {
    String propertyName = "org.cougaar.initTime";
    long date = DATE_ERROR;
    long time = DATE_ERROR;
    String value = System.getProperty(propertyName);
    if (value != null) {
      try {
        DateFormat f = (new SimpleDateFormat("MM/dd/yyy H:mm:ss"));
        f.setTimeZone(TimeZone.getTimeZone("GMT"));
        time = f.parse(value).getTime();
        // get midnight of specified date
        Calendar c = f.getCalendar();
        c.setTimeInMillis(time);
        c.set(Calendar.HOUR, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        date = c.getTimeInMillis();
      } catch (ParseException e) {
        // try with just the date
        try {
          DateFormat f = (new SimpleDateFormat("MM/dd/yyy"));
          f.setTimeZone(TimeZone.getTimeZone("GMT"));
          time = f.parse(value).getTime();
        } catch (ParseException e1) {
          if (log.isDebugEnabled())
            log.debug("Failed to parse property " + propertyName + " as date+time or just time: " + value, e1);
        }
      }
    }

   return time;
  }


  // Handle a change to our registration status
  protected void handleAvailabilityChange(AvailabilityChangeMessage availabilityChange) {
    synchronized (availabilityChange) {
      if (!isProvider()) {
	if (log.isDebugEnabled()) {
	  log.debug(getAgentIdentifier() + " not a provider. " +
		    "Ignoring AvailabiltyChangeMessage - " + 
		    availabilityChange);
	}
	return;
      }

      if (log.isDebugEnabled()) {
	log.debug(getAgentIdentifier() + 
		  " handling AvailabiltyChangeMessage. Status = " + 
		  availabilityChange.getStatus());
      }

      switch (availabilityChange.getStatus()) {
      case AvailabilityChangeMessage.REQUESTED:
	  updateProviderCapability(availabilityChange);
	  availabilityChange.setStatus(AvailabilityChangeMessage.COMPLETED);
	  getBlackboardService().publishChange(availabilityChange);
	  break;
      case AvailabilityChangeMessage.PENDING:
        // let it go.  might want to check to see if it takes a very long time...
        break;
      case AvailabilityChangeMessage.COMPLETED:
	if (!availabilityChange.isRegistryUpdated()) {
	  availabilityChange.setRegistryUpdated(true);
	  getBlackboardService().publishChange(availabilityChange);
	}
        break;
      case AvailabilityChangeMessage.DONE:
        // should drop it from the list;
        break;
      case AvailabilityChangeMessage.ERROR:
        // retry, perhaps?
        break;
      }
    }
  }

  protected void handleAvailabilityChange(Collection availabilityChanges) {
    for (Iterator iterator = availabilityChanges.iterator(); iterator.hasNext();) {
      AvailabilityChangeMessage availabilityChange = (AvailabilityChangeMessage) iterator.next();
      handleAvailabilityChange(availabilityChange);
    }
    
    return;
  }
  
  protected void updateProviderCapability(AvailabilityChangeMessage availabilityChange) {
    Collection pcCollection = 
      getBlackboardService().query(providerCapabilitiesPredicate);

    if (log.isDebugEnabled()) {
      log.debug(getAgentIdentifier() + ": updateProviderCapability handling " +
		availabilityChange);
    }


    for (Iterator iterator = pcCollection.iterator();
	 iterator.hasNext();) {
      ProviderCapabilities capabilities = 
	(ProviderCapabilities) iterator.next();

      ProviderCapability capability = 
	  capabilities.getCapability(availabilityChange.getRole());

      if (capability != null) {
	TimeSpan timeSpan = availabilityChange.getTimeSpan();
	Schedule currentAvailability = 
	  capability.getAvailableSchedule();

	if (log.isDebugEnabled()) {
	  log.debug(getAgentIdentifier() + 
		    ": found matching ProviderCapability handling " +
		    capability + 
		    " with current availability " + currentAvailability);
	}
	
	PlanningFactory planningFactory = 
	  (PlanningFactory) domainService.getFactory("planning");
	Schedule newAvailability = 
	  planningFactory.newSchedule(currentAvailability.getAllScheduleElements());
	
	Collection overlaps = 
	  currentAvailability.getOverlappingScheduleElements(timeSpan.getStartTime(),
							     timeSpan.getEndTime());

	if (log.isDebugEnabled()) {
	  log.debug(getAgentIdentifier() + 
		    ": overlaps found - " + overlaps);
	}
	
	boolean change = false;
	if (overlaps.size() == 0) {
	  if (availabilityChange.isAvailable()) {
	    change = true;
	    // Construct ScheduleElement to fill the entire period.
	    ScheduleElementImpl newScheduleElement = 
	      new ScheduleElementImpl(timeSpan.getStartTime(),
				      timeSpan.getEndTime());
	    newAvailability.add(newScheduleElement);
	  }
	} else {
	  change = true;

	  ScheduleElement earliest = null;
	  ScheduleElement latest = null;

	  for (Iterator overlap = overlaps.iterator();
	       overlap.hasNext();) {
	    latest = (ScheduleElement) overlap.next();
	    if (earliest == null) {
	      earliest = latest;
	    }
	    newAvailability.remove(latest);
	  }
	 
	  if (availabilityChange.isAvailable()) {
	    // Construct ScheduleElement to fill the entire period.
	    long newStart = Math.min(earliest.getStartTime(), 
				     timeSpan.getStartTime());
	    long newEnd = Math.max(latest.getEndTime(), 
				   timeSpan.getEndTime());
	    ScheduleElementImpl newScheduleElement = 
	      new ScheduleElementImpl(newStart, newEnd);
	    newAvailability.add(newScheduleElement);
	  } else {
	    // Construct ScheduleElements to bracket the unavailable time
	    
	    if (earliest.getStartTime() < timeSpan.getStartTime()) {
	      ScheduleElementImpl newEarliest = 
		new ScheduleElementImpl(earliest.getStartTime(), 
					timeSpan.getStartTime());
	      newAvailability.add(newEarliest);
	    }

	    if (latest.getEndTime() > timeSpan.getEndTime()) {
	      ScheduleElementImpl newLatest = 
		new ScheduleElementImpl(timeSpan.getEndTime(), 
					latest.getEndTime());
	      newAvailability.add(newLatest);
	    }
	  }
	}


	if (change) {
	  if (log.isDebugEnabled()) {
	    log.debug(getAgentIdentifier() + 
		      " provider availability after change " + 
		      newAvailability);
	  }


	  capability.setAvailableSchedule(newAvailability);
	  getBlackboardService().publishChange(capabilities);

	  
	  if (newAvailability.size() == 0) {
	    // Provider never available so remove registration
	    removeRegisteredRole(availabilityChange);
	  } else if (currentAvailability.size() == 0) {
	    // Provider changing from never available so add registration
	    addRegisteredRole(availabilityChange);	
	  }

	} else {
	  if (log.isDebugEnabled()) {
	    log.debug(getAgentIdentifier() + 
		      " ignoring AvailabilityChangeMessage " + 
		      availabilityChange +
		      " information already included in ProviderCapability " +
		      capability);
	  }
	}
      }
    }
  }

  protected abstract void addRegisteredRole(final AvailabilityChangeMessage availabilityChange);

  protected abstract void removeRegisteredRole(final AvailabilityChangeMessage availabilityChange);

  // Is this Agent a service provider?
  protected boolean isProvider() {
    return getProviderFile().exists();
  }
  
  // Get the OWL service provider file
  protected File getProviderFile() {
    String owlFileName = getAgentIdentifier().toString() + OWL_IDENTIFIER;
    return new File(org.cougaar.servicediscovery.Constants.getServiceProfileURL().getFile() +
		    owlFileName);
  }

  public class RetryAlarm implements Alarm {
    private long expiresAt;
    private boolean expired = false;

    public RetryAlarm (long expirationTime) {
      expiresAt = expirationTime;
    }

    public long getExpirationTime() { return expiresAt; }
    public synchronized void expire() {
      if (!expired) {
        expired = true;
        getBlackboardService().signalClientActivity();
      }
    }
    public boolean hasExpired() { return expired; }
    public synchronized boolean cancel() {
      boolean was = expired;
      expired=true;
      return was;
    }
    public String toString() {
      return "<RetryAlarm "+expiresAt+
        (expired?"(Expired) ":" ")+
        "for SDCommunityBasedRegistrationPlugin at " + 
	getAgentIdentifier() + ">";
    }
  }
}



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

import java.util.ArrayList;
import java.util.Collection;

import org.cougaar.core.service.LoggingService;
import org.cougaar.servicediscovery.description.AvailabilityChangeMessage;
import org.cougaar.servicediscovery.description.ProviderDescription;
import org.cougaar.servicediscovery.description.ServiceClassification;
import org.cougaar.servicediscovery.description.ServiceClassificationImpl;
import org.cougaar.servicediscovery.service.RegistrationService;
import org.cougaar.servicediscovery.util.UDDIConstants;

/**
 * Read local agent OWL profile file. Use the listed roles and register this agent with those
 * roles in the YP.
 **/
public class SDRegistrationPlugin extends SDRegistrationPluginBase {
  private static String ypAgent = 
  System.getProperty("org.cougaar.yp.ypAgent", "OSD.GOV");

  private int outstandingSCAUpdates = 0;

  private boolean isRegistered = false;

  /** true iff we have submitted a registration request which hasn't yet been observed to complete **/
  private boolean inProgress = false;
  /** true iff we've got the callback successfully, but need to notify everyone else **/
  private boolean isPending = false;

  public void load() {
    super.load();
  }

  public void unload() {
    super.unload();
  }

  protected void setupSubscriptions() {
    super.setupSubscriptions();

    // For now re-register every time.
    isRegistered = false;
  }

  protected void execute () {
    super.execute();

    if (isProvider()) {
      boolean registrationInitiated = false;

      if (isPending && !inProgress) {
        isRegistered = true;
        isPending = false;

	if (log.isInfoEnabled()) {
	  log.info("Completed initial registration of "+getAgentIdentifier());
	}

	if (outstandingSCAUpdates != 0) {
	  Collection scas = supportLineageSubscription.getCollection();
	  if (log.isInfoEnabled()) {
	    log.info(getAgentIdentifier() + " post pending added new SCAs - " + scas);
	  }

	  // Set outstanding count to 1 because we're adding all known SCAs
	  outstandingSCAUpdates = 1;
	  updateRegistration(scas);
	}
      } else if (!isRegistered) {
	if (supportLineageSubscription.size() >= knownSCAs) {
	  if (log.isDebugEnabled()) {
	    log.debug("Registering: " + getAgentIdentifier() +
		      " with SCAs " + supportLineageSubscription);
	  }
	  registrationInitiated = true;
	  initialRegister();
	} else {
	  if (log.isDebugEnabled()) {
	    log.debug("Waiting to register: " + getAgentIdentifier() +
		      " need " + knownSCAs + " SCAs, have " +
		      supportLineageSubscription.size());
	  }
	}
      }

      if (supportLineageSubscription.hasChanged()) {
	Collection adds = supportLineageSubscription.getAddedCollection();

	if (!adds.isEmpty()) {

	  if ((log.isInfoEnabled()) &&
	      (supportLineageSubscription.size() > knownSCAs)) {
	      log.info(getAgentIdentifier() + " expected " + knownSCAs +
		       " received " + supportLineageSubscription.size() + " " +
		       supportLineageSubscription);
	  }

	  if(isRegistered){
	    outstandingSCAUpdates++;
	    if (log.isInfoEnabled()) {
	      log.info(getAgentIdentifier() + " added new SCAs - " + adds);
	    }
	    updateRegistration(adds);
	  } else if (inProgress && !registrationInitiated) {
	    outstandingSCAUpdates++;
	    if (log.isInfoEnabled()) {
	      log.info(getAgentIdentifier() + " skip adding new SCAs - " + adds);
	    }
	  }
	}
      }
    }

    // No harm since publish change only occurs if the conf rating has changed.
    updateRegisterTaskDispositions();

    // This PI is capable of exiting the execute method while still having
    // work to do -- hence it must tell the QuiescenceReportService
    // that it has outstanding work
    updateQuiescenceService();
  }

  private void updateRegistration(Collection scas) {
    final Collection adds = scas;

    // IMPORTANT - currently no code to retry. Failed transactions == lost SCAs
    registrationService.updateServiceDescription(ypAgent,
					         getAgentIdentifier().toString(),
                                                 scaServiceClassifications(adds),
                                                 new RegistrationService.Callback() {
      public void invoke(Object o) {
	// Success or not, we've completed
	// handling this transaction.
	outstandingSCAUpdates--;
	boolean success = ((Boolean)o).booleanValue();
	if (!success) {
	  log.error(getAgentIdentifier() +
		    " unable to update registry with new SCAs - " + adds);
	} else {
	  
	  if (log.isInfoEnabled()) {
	    log.info(getAgentIdentifier() +
		     " updated registry with new SCAs - " +
		     adds);
	  }
	}
	getBlackboardService().signalClientActivity();
      }
      public void handle(Exception e) {
	// Success or not, we've completed
	// handling this transaction.
	outstandingSCAUpdates--;
	retryErrorLog("updateRegistration", e);
      }
    });
    
  }

  private void initialRegister() {
    if (isPending) return;      // skip if we're just waiting to notify - of course, then we shouldn't be here

    if (isProvider()) {
      final ProviderDescription pd = getPD();
      
      if (pd == null) {
	// Unable to get provider description
	retryErrorLog("Problem getting ProviderDescription now, try again later.");
	return;
      } 
      
      if (inProgress) {
	if (log.isInfoEnabled()) {
	  log.info(getAgentIdentifier() + "Still waiting for register");
	}
      } else {
	inProgress = true;      // don't allow another one to start
	outstandingSCAUpdates = 0;    // will pick up all known SCAs
	
	// callback will be executed by another thread
	RegistrationService.Callback cb =
	  new RegistrationService.Callback() {
	  public void invoke(Object o) {
	    boolean success = ((Boolean) o).booleanValue();
	    if (log.isInfoEnabled()) {
	      log.info(pd.getProviderName()+ " initialRegister success = "+success);
	    }
	    
	    isPending = true; // let the plugin set isRegistered
	    retryAlarm = null;   // probably not safe
	    inProgress = false; // ok to update
	    getBlackboardService().signalClientActivity();
	  }
	  public void handle(Exception e) {
	    inProgress = false; // ok to update
	    retryErrorLog("initialRegister", e);
	  }
	};
	
	// actually submit the request.
	if (log.isInfoEnabled()) {
	  log.info(getAgentIdentifier() + " initial registration with SCAs " +
		   supportLineageSubscription);
	}
	registrationService.addProviderDescription(ypAgent,
						   pd,
						   scaServiceClassifications(supportLineageSubscription),
						   cb);
      }
    } else {
      if (log.isDebugEnabled()) {
	log.debug("Agent " + getAgentIdentifier() + " Not Registering, no owl file.");
      }
    }
  }

  protected boolean registrationComplete() {
    //return ((outstandingSCAUpdates == 0) && (isRegistered));
    return (!isProvider()) || (isRegistered);
  }
  
  protected void removeRegisteredRole(final AvailabilityChangeMessage availabilityChange) {
    Collection serviceClassifications = new ArrayList(1);
    ServiceClassification sca =
      new ServiceClassificationImpl(availabilityChange.getRole().toString(),
				    availabilityChange.getRole().toString(),
				    UDDIConstants.MILITARY_SERVICE_SCHEME);
    serviceClassifications.add(sca);
    
    RegistrationService.Callback cb =
      new RegistrationService.Callback() {
      public void invoke(Object o) {
	if (log.isDebugEnabled()) {
	  log.debug(getAgentIdentifier() + 
		    " removed yp registration for role " +
		    availabilityChange.getRole());
	}
        getBlackboardService().signalClientActivity();
      }
      public void handle(Exception e) {
	synchronized(availabilityChange) {
	  availabilityChange.setStatus(AvailabilityChangeMessage.ERROR);
	}
	retryErrorLog("removeRegisteredRole", e);
      }
    };
    registrationService.deleteServiceDescription(ypAgent,
						 getAgentIdentifier().toString(),
						 serviceClassifications,
						 cb);
  }

  protected void addRegisteredRole(final AvailabilityChangeMessage availabilityChange) {
    Collection serviceClassifications = new ArrayList(1);
    ServiceClassification sca =
      new ServiceClassificationImpl(availabilityChange.getRole().toString(),
				    availabilityChange.getRole().toString(),
				    UDDIConstants.MILITARY_SERVICE_SCHEME);
    serviceClassifications.add(sca);
    
    RegistrationService.Callback cb =
      new RegistrationService.Callback() {
      public void invoke(Object o) {
	if (log.isDebugEnabled()) {
	  log.debug(getAgentIdentifier() + " added yp registration for role " +
		    availabilityChange.getRole());
	}
	getBlackboardService().signalClientActivity();
      }
      public void handle(Exception e) {
	synchronized(availabilityChange) {
	  availabilityChange.setStatus(AvailabilityChangeMessage.ERROR);
	}
	retryErrorLog("addRegisteredRole", e);
      }
    };
    registrationService.updateServiceDescription(ypAgent,
						 getAgentIdentifier().toString(),
						 serviceClassifications,
						 cb);
  }
}



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
import java.net.URL;
import java.util.Collection;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.service.community.Community;
import org.cougaar.core.service.community.CommunityChangeEvent;
import org.cougaar.core.service.community.CommunityChangeListener;
import org.cougaar.core.service.community.CommunityResponse;
import org.cougaar.core.service.community.CommunityResponseListener;
import org.cougaar.core.service.community.CommunityService;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.servicediscovery.description.ProviderDescription;
import org.cougaar.servicediscovery.description.ProviderDescriptionImpl;
import org.cougaar.servicediscovery.service.RegistrationService;
import org.cougaar.util.Configuration;


/**
 * Simplified version of SDRegistrationPlugin that registers this agent
 * using the <agent name>-profile.owl file if any in the plugin parameter-named YP agent.
 * <p>
 * This version of the plugin is somewhat simplified -- fewer error checks for example. It 
 * does not create {@link org.cougaar.servicediscovery.description.ProviderCapabilities} objects (used by more complex SDProviderPlugins).
 * <p>
 * First plugin argument is the name of the agent hosting the YP that we will register with.
 *<p>
 * The {@link SDRegistrationPluginBase} and extensions are more complex; they handle more errors,
 * dynamic changes to the services provided, deal with society quiescence issues, create 
 * ProviderCapabilities, etc.  
 * Complex applications will likely want to use those plugins, but most users will prefer
 * to extend this plugin. Extensions should over-rider the getServiceProfileURL() method,
 * to specify where their application's service profiles are located. For an example, 
 * see the pizza application.
 *
 * @property org.cougaar.servicediscovery.plugin.SimpleRegistrationGracePeriod is the number 
 * of minutes after startup, during which we ignore SD registration Warnings, to allow the YP 
 * to start up. After this we complain more loudly. Default is 5 minutes.
 *
 * @see org.cougaar.pizza.plugin.SDRegistrationPlugin
 **/
public class SimpleSDRegistrationPlugin extends ComponentPlugin {

  private static final String REGISTRATION_GRACE_PERIOD_PROPERTY = 
    "org.cougaar.servicediscovery.plugin.SimpleRegistrationGracePeriod";

  private static final int DEFAULT_WARNING_SUPPRESSION_INTERVAL = 5; // in minutes

  private static final int WARNING_SUPPRESSION_INTERVAL;

  static {
    WARNING_SUPPRESSION_INTERVAL = Integer.getInteger(REGISTRATION_GRACE_PERIOD_PROPERTY,
						      DEFAULT_WARNING_SUPPRESSION_INTERVAL).intValue();
  }

  private long warningCutoffTime = 0;

  protected static final String OWL_IDENTIFIER = ".profile.owl";

  private Alarm retryAlarm;

  // Make log service protected, so extensions can use it.
  protected LoggingService log;

  private RegistrationService registrationService = null;
  private CommunityService communityService = null;

  private YPInfo ypInfo;

  private ProviderDescription provD = null;


  public void setCommunityService(CommunityService cs) { 
    this.communityService = cs; 
  }

  public void setLoggingService(LoggingService log) {
    this.log = log;
  }

  public void setRegistrationService(RegistrationService rs) {
    registrationService = rs;
  }

  /**
   * When the agent moves, we don't want dangling callbacks - so clear them. On resume, we'll just re-register
   * from scratch, since we can't otherwise recover where we'd gotten to.
   */
  public void suspend() {
    super.suspend();

    if (ypInfo != null) {
      // Remove all community change notifications
      if (log.isInfoEnabled()) {
	log.info("removing community change listeners.");
      }
      
      ypInfo.clearCommunity();
    }
  }

  /**
   * This plugin has no subscriptions. It will execute exactly once, since the 
   * infrastructure calls the execute() method once at plugin startup.
   */
  protected void setupSubscriptions() {
  }

  /**
   * If this agent has a -profile.owl file, then ask for a handle on the named (in the only plugin parameter)
   * YP agent's YP community. Once we have the community (may be
   * a subsequent execute when our CommunityListener tells us
   * we found the YP community), we call initialRegister
   * to register this agent in the YP.
   */
  protected void execute () {
    // Does this agent have a service profile
    if (isProvider()) {
      // If we haven't already gotten the YP info
      if (ypInfo == null) {
	// get the yp info
	initYPInfo();
	// Find our YP community
	findYPCommunity();
      }

      if (ypInfo.readyToRegister()) {
	if (log.isDebugEnabled()) {
	  log.debug("Registering: " + getAgentIdentifier() + " with " +
		    ypInfo.getCommunity().getName());
	}
	initialRegister();
      }
    }
  }

  private void initialRegister() {
    // FIXME: Is this check necessary?
    if (!ypInfo.readyToRegister()) {
      if (log.isDebugEnabled()) {
	log.debug("Exiting initialRegister early - " +
		  " ypInfo not ready - " + 
		  " community " + ypInfo.getCommunity().getName() +
		  " isRegistered " + ypInfo.getIsRegistered() +
		  " pendingRegistration " + ypInfo.getPendingRegistration());
      }
      return;
    } // end block to handle ypInfo not ready to register.

    // Wrap whole call to YP in a try/catch....
    try {
      // Get our ProviderDescription (what we're registering)
      final ProviderDescription pd = getPD();
      
      if (pd == null) {
	ypInfo.setIsRegistered(false);	
	ypInfo.setPendingRegistration(false); // okay to try again
	
	retryErrorLog("Problem getting ProviderDescription -- transient Jena error?" + 
		      " Unable to add registration to " +
		      ypInfo.getCommunity().getName() + 
		      ", try again later.");
	
	return;
      }
      
      ypInfo.setPendingRegistration(true);
      
      // Create the callback - by which the YP will tell us when it finishes
      RegistrationService.Callback cb =
	new RegistrationService.Callback() {
	    
	    /** 
	     * YP Calls when the registration call completes. Note that the
	     * Object argument is only useful for debugging.
	     */ 
	    public void invoke(Object o) {
	      if (log.isInfoEnabled()) {
		boolean success = ((Boolean) o).booleanValue();
		log.info(pd.getProviderName()+ " initialRegister success = " + 
			 success + " with " + ypInfo.getCommunity().getName());
	      }
	      
	      ypInfo.setIsRegistered(true);
	      ypInfo.setPendingRegistration(false);
	      ypInfo.clearCommunity();
	      
	      retryAlarm = null;
	      
	      getBlackboardService().signalClientActivity();
	    } // end of invoke()
	    
	    /**
	     * YP Calls when there was an error trying to register the provider.
	     */
	    public void handle(Exception e) {
	      ypInfo.setPendingRegistration(false); // okay to try again
	      ypInfo.setIsRegistered(false);
	      
	      retryErrorLog("Problem adding ProviderDescription to " + 
			    ypInfo.getCommunity().getName() + 
			    ", try again later: " +
			    getAgentIdentifier(), e);
	    }
	  }; // end of Callback definition
      
      // actually submit the request: register at the given community,
      // with the given Provider information, calling back to us using 
      // the given callback
      registrationService.addProviderDescription(ypInfo.getCommunity(),
						 pd,
						 cb);
    } catch (RuntimeException e) {
      ypInfo.setIsRegistered(false);	
      ypInfo.setPendingRegistration(false); // okay to try again
      
      retryErrorLog("Problem adding ProviderDescription to " + 
		    ypInfo.getCommunity().getName() + 
		    ", try again later: " +
		    getAgentIdentifier(), e);
    } // end of try/catch that actually does the registration (via callback)
  } // end of initialRegister

  //
  private void findYPCommunity() {
    Community ypCommunity = 
      communityService.getCommunity(getYPCommunityName(ypInfo.getAgentName()),
				    new YPCommunityResponseListener(ypInfo));
    
    if (ypCommunity != null) {
      ypInfo.setCommunity(ypCommunity);
      if (log.isDebugEnabled()) {
	log.debug("Registering: " + getAgentIdentifier() + " with " +
		  ypInfo.getCommunity().getName());
      }
      initialRegister();
    } else if (log.isDebugEnabled()) {
      log.debug("waiting on community info " +
		getYPCommunityName(ypInfo.getAgentName()));
    }
  }

  /** 
   * Create the YPInfo object for this instance.
   * Takes the first plugin parameter as the name of the agent hosting the YP that
   * we will register with.
   */
  private void initYPInfo() {
    Collection params = getParameters();
    
    if (params.isEmpty()) {
      IllegalArgumentException iae = new IllegalArgumentException();
      log.error("SDRegistrationPlugin: no YP agent parameter" +
		" - unable to register.", iae);
    } else {
      ypInfo = new YPInfo((String) params.iterator().next(),
			  null, false, false);
    }

    if (log.isDebugEnabled())
      log.debug(": ypInfo = " + ypInfo);
  }

  private String getYPCommunityName(String ypAgentName) {
    // For now assume every YP represented by a YPCommunity called
    // <yp agent name>-YPCOMMUNITY
    return ypAgentName + "-YPCOMMUNITY";
  }
 
  
  /** @return null if unable to parse the provider description */
  private ProviderDescription getPD() {
    if (provD == null) {
      if (log.isDebugEnabled()) {
	log.debug(": getPD() parsing OWL.");
      }
      
      ProviderDescription pd = new ProviderDescriptionImpl();
      try {
	URL serviceProfileURL = Configuration.urlify(getServiceProfileURL().toString()); 
	boolean ok = pd.parseOWL(serviceProfileURL, 
				 getAgentIdentifier() + OWL_IDENTIFIER);
	
	// We have to check the return status because
	// occasionally Jena seems to hiccup on the parse...
	if (ok && (pd.getProviderName() != null)) {
	  if (log.isDebugEnabled()) {
	    log.debug(": getPD() successfully parsed OWL.");
	  }
	  
	  provD = pd;
	} else {
	  if (log.isDebugEnabled()) {
	    log.debug(": getPD() unable to parse OWL." +
		      " ok = " + ok);
	  }
	}
      } catch (java.util.ConcurrentModificationException cme) {
	// Jena can do a concurrent mod exception. See bug 3052
	// Leave provD uninitialized
	if (log.isDebugEnabled()) {
	  log.debug(": getPD() ConcurrentModificationException - " +
		    cme);
	}
      } catch (java.net.MalformedURLException mue) {
	log.error("getPD() couldn't find directory for service profiles, starting from " + getServiceProfileURL(), mue);
      }
    }
    return provD;
  }
  
  private long getWarningCutOffTime() {
    if (warningCutoffTime == 0) {
      warningCutoffTime = System.currentTimeMillis() + WARNING_SUPPRESSION_INTERVAL*60000;
    }
    
    return warningCutoffTime;
  }
  
  
  private void retryErrorLog(String message) {
    retryErrorLog(message, null);
  }
  
  /**
   * When an error occurs, but we'll be retrying later, treat it as a DEBUG
   * at first. After a while it becomes an error.
   */
  private void retryErrorLog(String message, Throwable e) {
    
    // Note that we want this to be random because.. FIXME!!!!!
    long absTime = getAlarmService().currentTimeMillis()+ 
      (int)(Math.random()*10000) + 1000;
    
    retryAlarm = new RetryAlarm(absTime);
    getAlarmService().addAlarm(retryAlarm);
    
    if(System.currentTimeMillis() > getWarningCutOffTime()) {
      if (e == null)
	log.error(message);
      else
	log.error(message, e);
    } else if (log.isDebugEnabled()) {
      if (e == null)
	log.debug(message);
      else
	log.debug(message, e);
    }
  }

  /** This agent is a provider if there is a provider file for it */
  private boolean isProvider() {
    return getProviderFile().exists();
  }
  
  /** Get the OWL service provider file named after this agent, if any */
  private File getProviderFile() {
    String owlFileName = getAgentIdentifier().toString() + OWL_IDENTIFIER;
    return new File(getServiceProfileURL().getFile() +
		    owlFileName);
  }

  /** 
   * Get the URL for the service profiles directory for this application. 
   * This is the only method application specific versions of this plugin
   * will likely over-ride.
   */
  protected URL getServiceProfileURL() {
    try {
      return new URL(Configuration.getInstallURL() + File.separator + "servicediscovery" + File.separator + "data" + File.separator + "serviceprofiles" + File.separator);
    } catch (java.net.MalformedURLException mue) {
      log.error("Exception constructing service profile URL" , mue);
      return null;
    }
  }
  
  private class RetryAlarm implements Alarm {
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

  private class YPInfo {
    private String myYPAgentName;
    private Community myYPCommunity;
    private YPCommunityChangeListener myCommunityListener;
    private boolean myIsRegistered;
    private boolean myPendingRegistration;
    
    public YPInfo(String ypAgentName, Community ypCommunity, 
		  boolean isRegistered, boolean pendingRegistration) {
      myYPAgentName = ypAgentName;
      myYPCommunity = ypCommunity;
      myIsRegistered = isRegistered;
      myPendingRegistration = pendingRegistration;
    }

    public String getAgentName(){
      return myYPAgentName;
    }

    public void setAgentName(String ypAgentName){
      if (ypAgentName == null) {
	clearCommunity();
      } else {
	myYPAgentName = ypAgentName;
      }
    }

    public Community getCommunity(){
      return myYPCommunity;
    }

    public void setCommunity(Community ypCommunity){
      if (ypCommunity == null) {
	clearCommunity();
      } else {
	if (myYPCommunity == null) {
	  if (log.isDebugEnabled()) {
	    log.debug("adding listener for " + 
		      ypCommunity);
	  }
	  myYPCommunity = ypCommunity;
	
	  // First time so set up change listener
	  myCommunityListener = new YPCommunityChangeListener(this);
	  communityService.addListener(myCommunityListener);
	} else {
	  myYPCommunity = ypCommunity;
	}
      }
    }


    public void clearCommunity() {
      if (log.isDebugEnabled()) {
	log.debug("removing listener for " + myYPCommunity);
      }
      myYPCommunity = null;
      if (myCommunityListener != null) {
	communityService.removeListener(myCommunityListener);
      }
    }

    public boolean getIsRegistered() {
      return myIsRegistered;
    }

    public void setIsRegistered(boolean isRegistered){ 
      if ((myIsRegistered) && (!isRegistered) &&
	  (log.isDebugEnabled())) {
	RuntimeException re  = new RuntimeException();
	log.debug("setIsRegistered() going from true to false.", re);
      }
      myIsRegistered = isRegistered;
    }

    public boolean getPendingRegistration() {
      return myPendingRegistration;
    }

    public void setPendingRegistration(boolean pendingRegistration){ 
      myPendingRegistration = pendingRegistration;
    }

    public boolean readyToRegister() {
      return ((getCommunity() != null) &&
	      (!getIsRegistered()) &&
	      (!getPendingRegistration()));
    }
  }

  private class YPCommunityResponseListener 
    implements CommunityResponseListener {
    private YPInfo ypInfo;

    public YPCommunityResponseListener(YPInfo info) {
      ypInfo = info;
    }

    public void getResponse(CommunityResponse resp){
      if (log.isDebugEnabled()) {
	log.debug("got Community info for " +
		  (Community) resp.getContent());
      }

      Community ypCommunity = (Community) resp.getContent();
      
      ypInfo.setCommunity(ypCommunity);
      getBlackboardService().signalClientActivity();
    }
  }

  private class YPCommunityChangeListener 
    implements CommunityChangeListener {
    private YPInfo ypInfo;
    String communityName;

    public YPCommunityChangeListener(YPInfo info) {
      ypInfo = info;
      communityName = ypInfo.getCommunity().getName();
    }

    public void communityChanged(CommunityChangeEvent event){
      Community ypCommunity = event.getCommunity();

      // Paranoia code - bug in community code seems to lead to
      // notifications with null communities.
      // FIXME: This could be a Java assert
      if (ypCommunity == null) {
	if (log.isDebugEnabled()) {
	  log.debug("received Community change info for a null community");
	}
	return;
      }

      if (log.isDebugEnabled()) {
	log.debug("got Community change info for " +
		  ypCommunity);
      }

      if (ypCommunity.getName().equals(getCommunityName())) {
	ypInfo.setCommunity(ypCommunity);

	if (ypInfo.readyToRegister()) {
	  if (log.isDebugEnabled()) {
	    log.debug("signalClientActivity for " + ypCommunity);
	  }
	  
	  if (getBlackboardService() == null) {
	    if (log.isWarnEnabled())
	      log.warn("ignoring change notification " +
		       " - getBlackboardService() returned null");
	    ypInfo.clearCommunity();
	  } else {
            getBlackboardService().signalClientActivity();
	  }
	}
      } else if (log.isDebugEnabled()) {
	log.debug("ignoring CommunityChangeEvent  for " + 
		  ypCommunity.getName() + 
		  " - listening for - " + getCommunityName());
      }

    }

    public String getCommunityName() {
      return communityName;
    }
  }
}



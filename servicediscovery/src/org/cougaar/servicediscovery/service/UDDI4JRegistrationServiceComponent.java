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

package org.cougaar.servicediscovery.service;

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.service.community.Community;
import org.cougaar.servicediscovery.description.AdditionalQualificationRecord;
import org.cougaar.servicediscovery.description.BusinessCategory;
import org.cougaar.servicediscovery.description.ProviderDescription;
import org.cougaar.servicediscovery.description.ServiceCategory;
import org.cougaar.servicediscovery.description.ServiceClassification;
import org.cougaar.servicediscovery.description.ServiceProfile;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;
import org.cougaar.yp.YPFuture;
import org.cougaar.yp.YPProxy;
import org.cougaar.yp.YPService;
import org.cougaar.yp.YPStateMachine;
import org.uddi4j.datatype.Name;
import org.uddi4j.datatype.binding.BindingTemplates;
import org.uddi4j.datatype.binding.TModelInstanceDetails;
import org.uddi4j.datatype.business.BusinessEntity;
import org.uddi4j.datatype.service.BusinessService;
import org.uddi4j.datatype.service.BusinessServices;
import org.uddi4j.response.BusinessInfo;
import org.uddi4j.response.BusinessList;
import org.uddi4j.response.ServiceDetail;
import org.uddi4j.response.ServiceInfo;
import org.uddi4j.util.CategoryBag;
import org.uddi4j.util.FindQualifier;
import org.uddi4j.util.FindQualifiers;
import org.uddi4j.util.KeyedReference;

/**
 * This component provides access to the Registration Service
 * which allows a service provider agent to register
 * the services it provides in the registry.
 *
 * Registered Services are kept both locally and on the Blackboard.
 * The redundant storage is to provide quick access by having the information stored
 * locally within the Agent, but also have the ability to re-hydrate if the agent gets killed.
 *
 * @see RegistrationService
 */

// why is this a plugin?? it doesn't use the blackboard!
public final class UDDI4JRegistrationServiceComponent
  extends ComponentPlugin
{
  private static Logger staticLogger = 
        Logging.getLogger(UDDI4JRegistrationServiceComponent.class);


  /**  Cougaar service used for logging **/
  private LoggingService log;
  public void setLoggingService(LoggingService ls) {
    log = (log==null)?LoggingService.NULL:log; 
  }

  private YPService ypService;
  public void setYPService(YPService yp) { ypService = yp; }
  private ThreadService threads;
  public void setThreadService(ThreadService t) { threads=t; }

  protected String agentName = "anonymous";

  private RegistrationServiceProviderImpl mySP;

  public void load() {
    super.load();

    // create and advertise our service
    mySP = new RegistrationServiceProviderImpl();
    getServiceBroker().addService(RegistrationService.class, mySP);

    agentName = agentIdentificationService.getName();
  }

  public void unload() {
    // revoke our service
    if (mySP != null) {
      getServiceBroker().revokeService(RegistrationService.class, mySP);
      mySP = null;
    }
    super.unload();
  }

  /**
   * Populate the Agent will all known ProviderDescriptions.
   * The only reason ProviderDescriptions would exist is if the
   * agent is re-hydrating.
   */
  protected void setupSubscriptions() {}

  protected void execute() {}

  private class RegistrationServiceProviderImpl implements ServiceProvider {

    public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
      if (serviceClass == RegistrationService.class) {
        return new RegistrationServiceImpl();
      } else {
        return null;
      }
    }

    public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object service) {}
  }

  //The following uddi registry parameters are set via System properties.
  private static String username = null;
  private static String password = null;

  static {
    username = System.getProperty("org.cougaar.yp.juddi-users.username", YPProxy.DEFAULT_UDDI_USERNAME);
    password = System.getProperty("org.cougaar.yp.juddi-users.password", YPProxy.DEFAULT_UDDI_PASSWORD);
  }

  private class RegistrationServiceImpl extends UDDI4JUtility implements RegistrationService {

    private YPProxy makeProxy(Object ypContext) {
      YPProxy proxy = null;

      if (ypContext == null) {
	proxy = ypService.getYP();
      } else if (ypContext instanceof String) {
	proxy = ypService.getYP((String) ypContext);
      } else if (ypContext instanceof MessageAddress) {
	proxy = ypService.getYP((MessageAddress) ypContext);
      } else if (ypContext instanceof Community) {
	proxy = ypService.getYP((Community) ypContext);
      } else {
	throw new IllegalArgumentException("Invalid datatype for ypContext - " +
					   ypContext.getClass() +
					   " - must be String, MessageAddress, or Community.");
      }

      return proxy;
    }

    public RegistrationServiceImpl() { 
      super(UDDI4JRegistrationServiceComponent.this.log, 
            UDDI4JRegistrationServiceComponent.this.ypService,
            UDDI4JRegistrationServiceComponent.this.threads);
    }

    //
    // Implement the registrationservice api
    //
    public void addProviderDescription(Object ypContext, 
				       ProviderDescription pd, 
				       Callback callback) {
      addProviderDescription(ypContext,
			     pd, 
			     Collections.EMPTY_LIST, 
			     callback);
    }


    private class SMBase extends YPStateMachine {
      protected final Object ypContext;
      protected final Callback callback;

      public SMBase(Object ypContext, Callback callback) {
        super(
          ypService,
          makeProxy(ypContext),
          UDDI4JRegistrationServiceComponent.this.threads);
	this.ypContext = ypContext;
        this.callback = callback;
      }
      protected void handleException(Exception e) {
        staticLogger.error("Caught unexpected Exception.  StateMachine will exit", e);
        callback.handle(e);
      }
    

      public void transit(State s0, State s1) {
	if (staticLogger.isInfoEnabled()) {
	  staticLogger.info(this.toString()+" StateMachine transit: "+s0+" to "+s1);
	}
	super.transit(s0, s1);
      }
      protected void kick() {
	if (staticLogger.isInfoEnabled()) {
	  staticLogger.info(this.toString()+" kicked"/*, new Throwable()*/); // sigh
	}
	super.kick();
      }
      protected Frame popFrame() {
	Frame f = super.popFrame();
	if (staticLogger.isInfoEnabled()) {
	  staticLogger.info(this.toString()+
		      " StateMachine pop("+stackSize()+") backto "+
                            f.getReturnTag() /*, new Throwable()*/ );
	}
	return f;
      }
      
      protected void pushFrame(Frame f) {
	if (staticLogger.isInfoEnabled()) {
	  staticLogger.info(this.toString()+" StateMachine push("+stackSize()+")");
	}
	super.pushFrame(f);
      }
      public  void set(State s) {
	super.set(s);
	if (staticLogger.isInfoEnabled()) {
	  staticLogger.info(this.toString()+" StateMachine set to "+s);
	}
      }
      public boolean step() {
	State s1= getState();
	boolean b = super.step();
	State s2 = getState();
	if (staticLogger.isInfoEnabled()) {
	  staticLogger.info(this.toString()+" StateMachine stepped from "+s1+" to "+s2+
		   " progress="+b );
	}
	return b;
      }
      
      public void go() {
	super.go();
	if (staticLogger.isInfoEnabled()) {
	  staticLogger.info(this.toString()+" StateMachine stopped in state "+getState(
));
	}
      }
    }
    

    /**
     * Adds a new ProviderDescription object.
     *
     * @param pd ProviderDescription for this provider.
     * @param callback Callback.invoke(Boolean) called with true on success
     */
    public void addProviderDescription(Object ypContext,
				       ProviderDescription pd, 
				       Collection additionalServiceClassifications, 
				       Callback callback) {
      (new PublishSM(ypContext, pd, additionalServiceClassifications, 
		     callback)).start();
    }

    private class PublishSM extends SMBase {
      private ProviderDescription pd;
      private Collection additionalServiceClassifications;

      public String toString() { return (pd!=null)?(pd.getProviderName()):"anonymous"; }
      public PublishSM(Object ypContext, ProviderDescription pd, Collection additionalServiceClassifications, Callback callback) {
	super(ypContext, callback);
        if (pd.getProviderName() == null) {
          throw new IllegalArgumentException("Provider name is null, unable to register. ");
        }
        this.pd = pd;
        this.additionalServiceClassifications = additionalServiceClassifications;
      }

      BusinessEntity be;
      CategoryBag bzBag = new CategoryBag();
      Iterator iter;

      Vector entities = new Vector();
      Vector services = new Vector();
      Collection serviceDescriptions;
      BusinessServices businessServices = new BusinessServices ();

      protected void init() {
        super.init();


      addLink("YPError", "handleYPError");
      add(new SState("handleYPError") {
	public void invoke() {
	  callback.handle((Exception) getVar("YPErrorException"));
	}
      });

        addLink("START", "getToken");
        add(new SState("getToken") {
            public void invoke() { 
	      call("getAuthToken", null, "gotToken");
	    }
	});

        addLink("gotToken","initBag");

        // initBag - fills up the new BusinessEntity with keyedrefs to the business categories
        add(new SState("initBag") { public void invoke() {
          be = new BusinessEntity("", pd.getProviderName());
          setVar("be", be);
          setVar("bzBag", bzBag);
          iterate(pd.getBusinessCategories(), "initBag.Loop", "initBagDone");
        }});

        add(new SState("initBag.Loop") { public void invoke() {
          final BusinessCategory bc = (BusinessCategory) getArgument();
          Callback cb = new Callback() {
            public void invoke(Object o) {
              KeyedReference kr = (KeyedReference) o;
              bzBag.getKeyedReferenceVector().add(kr);
              callReturn(null);
              kick();
            }
            public void handle(Exception e) {
              log.error("initBag.Loop.getKeyedReference(" + bc + ")",
                e);
              //transit("ERROR");
              callback.handle(e);
            }
          };
          getKeyedReference(getYPProxy(),
			    bc.getCategorySchemeName(),
                            bc.getCategoryName(), 
                            bc.getCategoryCode(),
                            cb);
        }});
        add(new SState("initBagDone") {
            public void invoke() {
              be.setCategoryBag(bzBag);
              transit("initBS");
            }});


        // initBS - inits the be's Services, iterate over the SDs
        add(new SState("initBS") { public void invoke() {
          serviceDescriptions = pd.getServiceProfiles();
          iterate(serviceDescriptions, "ibsl", "initBSDone");
        }});
        // foreach service description, loop over service categories
        add(new SState("ibsl") { public void invoke() {
          ServiceProfile sd = (ServiceProfile) getArgument();
          setVar("sd", sd);
          BusinessService bSvc = new BusinessService("");
          bSvc.setDefaultName(new Name(sd.getServiceProfileID()));
          setVar("bSvc", bSvc);
          Collection serviceCategories = sd.getServiceCategories();
          CategoryBag categoryBag = new CategoryBag();
          setVar("categoryBag", categoryBag);
          iterate(serviceCategories, "ibsl_sub1", "ibsl_1b");
        }});
        // foreach service category, get the SC ref and add to the bag
        add(new SState("ibsl_sub1") { public void invoke() {
          final ServiceCategory sc = (ServiceCategory) getArgument();
          setVar("sc", sc);
          final CategoryBag categoryBag = (CategoryBag) getVar("categoryBag");
          Callback cb = new Callback() {
            public void invoke(Object o) {
              KeyedReference kr = (KeyedReference) o;
              categoryBag.getKeyedReferenceVector().add(kr);
              transit("ibsl_sub1a");
              kick();
            }
            public void handle(Exception e) {
              log.error("ibsl_sub1.getKeyedReference(" + sc + ")",
                e);
              //transit("ERROR");
              callback.handle(e);
            }
          };
          getKeyedReference(getYPProxy(),
			    sc.getCategorySchemeName(), 
                            sc.getCategoryName(),
                            sc.getCategoryCode(),
                            cb);
        }});
        // foreach Service category, loop over the additional qualifications
        add(new SState("ibsl_sub1a") { public void invoke() {
          ServiceCategory sc = (ServiceCategory) getVar("sc");
          iterate(sc.getAdditionalQualifications(), "ibsl_sub2", "ibsl_sub1b");
        }});

        // foreach AQ, collect the keyed refs and return.
        add(new SState("ibsl_sub2") { public void invoke() {
          AdditionalQualificationRecord aqr = (AdditionalQualificationRecord) getArgument();
          final ServiceCategory sc = (ServiceCategory) getVar("sc");
          final CategoryBag categoryBag = (CategoryBag) getVar("categoryBag");
          Callback cb = new Callback() {
                              public void invoke(Object o) {
                                KeyedReference kr = (KeyedReference) o;
                                categoryBag.getKeyedReferenceVector().add(kr);
                                callReturn(null);
                                kick();
                              }
                              public void handle(Exception e) {
                                log.error("ibsl_sub2.getKeyedReference("+sc+")", e);
                                //transit("ERROR");
                                callback.handle(e);
                              }};
          getKeyedReference(getYPProxy(),
			    sc.getCategorySchemeName(),
                            aqr.getQualificationName(),
                            aqr.getQualificationValue(),
                            cb);
        }});

        // endpoint of loop started in ibsl_sub1a, returns up to loop started in ibsl which may 
        // progress to ibsl_1
        add(new SState("ibsl_sub1b") { public void invoke() {
          callReturn(null);
        }});

        // ibsl_1b, iterate over additionalServiceClassifications
        add(new SState("ibsl_1b") { public void invoke() {
          iterate(additionalServiceClassifications, "ibsl_sub3", "ibsl_1c");
        }});

        // foreach ASQ, collect the keyed refs and return.
        add(new SState("ibsl_sub3") { public void invoke() {
          final ServiceClassification sc = (ServiceClassification) getArgument();
          final CategoryBag categoryBag = (CategoryBag) getVar("categoryBag");
          Callback cb = new Callback() {
            public void invoke(Object o) {
              KeyedReference kr = (KeyedReference) o;
              categoryBag.getKeyedReferenceVector().add(kr);
              callReturn(null);
              kick();
            }
            public void handle(Exception e) {
              log.error("ibsl_sub3.getKeyedReference(" + sc + ")", e);
              //transit("ERROR");
              callback.handle(e);
            }
          };
          getKeyedReference(getYPProxy(),
			    sc.getClassificationSchemeName(),
                            sc.getClassificationName(),
                            sc.getClassificationCode(),
                            cb);
        }});

        // ibsl_1c, collect results
        add(new SState("ibsl_1c") { public void invoke() { 
          final CategoryBag categoryBag = (CategoryBag) getVar("categoryBag");
          final BusinessService bSvc = (BusinessService)getVar("bSvc");
          bSvc.setCategoryBag(categoryBag);
          bSvc.setBusinessKey("");

          final ServiceProfile sd = (ServiceProfile) getVar("sd");
          if(sd.getTextDescription().trim().length() != 0) {
            bSvc.setDefaultDescriptionString(sd.getTextDescription());
          }
          Callback cb = new Callback() {
            public void invoke(Object o) {
              TModelInstanceDetails tModelInstanceDetails =
                (TModelInstanceDetails) o;
              BindingTemplates bindings =
                createBindingTemplates(
                  sd.getServiceGroundingURI(),
                  tModelInstanceDetails);
              bSvc.setBindingTemplates(bindings);
              services.add(bSvc);
              callReturn(null); // return back to initBS
              kick();
            }
            public void handle(Exception e) {
              log.error("ibsl_1c.createTModelInstance", e);
              //transit("ERROR");
              callback.handle(e);
            }
          };
          createTModelInstance(getYPProxy(),
			       sd.getServiceGroundingBindingType(),
                               pd.getProviderName(),
                               cb);
        }});



        add(new SState("initBSDone") { public void invoke() {
          businessServices.setBusinessServiceVector (services);
          transit("saveBusiness");
        }});


        addYPQ("saveBusiness", "saveBusinessDone", new YPQ() {
            public YPFuture get(Frame f) {
              be.setBusinessServices (businessServices);
              entities.add(be);
              return getYPProxy().save_business(getAuthToken().getAuthInfoString(), entities);
            }
            public void set(Frame f, Object result) {
              // ignore the result.
            }
            public void handle(Frame f, Exception e) {
              log.error("saveBusiness exception", e);
              callback.handle(e);
            }
          });
        addLink("saveBusinessDone", "finish");
        add(new SState("finish") {
            public void invoke() { 
              callback.invoke(Boolean.TRUE); // let complete while discarding token
              call("discardAuthToken", null, "DONE");
            }
          });
      }
    }


    /**
     * @param callback Callback.invoke(Boolean) true on success, Callback.handle(Exception) on failure
     */
    public void updateServiceDescription(Object ypContext, 
					 String providerName, 
					 Collection serviceCategories, 
					 Callback callback){
      (new UpdateSM(ypContext, 
		    providerName, 
		    serviceCategories, 
		    callback)).start();
    }
    private class UpdateSM extends SMBase {
      final String providerName;
      final Collection serviceCategories;

      private UpdateSM(Object ypContext,String providerName, Collection serviceCategories, Callback callback) {
	super(ypContext, callback);
        this.providerName = providerName;
        this.serviceCategories = serviceCategories;

        // do some pre-machine initialization
        namePatterns.add(new Name(providerName));
      }

      BusinessList businessList;
      Iterator iter;
      CategoryBag updateBag = new CategoryBag();
      Vector namePatterns = new Vector();
      Vector services = new Vector();
      Vector serviceKeys = new Vector();

      protected void init() {
        super.init();
        addLink("START", "getToken");
        add(new SState("getToken") {
            public void invoke() { 
              call("getAuthToken", null, "gotToken");
            }
          });
        addLink("gotToken","findBusiness");

        addYPQ("findBusiness", "findBusinessDone", new YPQ() {
            public YPFuture get(Frame f) {
              FindQualifiers findQualifiers = new FindQualifiers();
              Vector qualifier = new Vector();
              qualifier.add(new FindQualifier("caseSensitiveMatch"));
              findQualifiers.setFindQualifierVector(qualifier);
              return getYPProxy().find_business(namePatterns, null, null, null, null, findQualifiers, 5);
            }
            public void set(Frame f, Object r) {
              businessList = (BusinessList) r;
            }
            public void handle(Frame f, Exception e) {
              log.error("updateServiceDescription.findBusiness", e);
              callback.handle(e);
              transit("ERROR");
            }
          });
        addLink("findBusinessDone", "ScanBusinesses");
        add(new SState("ScanBusinesses") {
            public void invoke() {
              Iterator it = businessList.getBusinessInfos().getBusinessInfoVector().iterator();
              if (!it.hasNext()) {
                if (log.isDebugEnabled()) {
                  log.debug("updateServiceDescription, cannot find registration for: " + 
			    providerName);
                }
                callback.invoke(Boolean.TRUE);
                transit("DONE");
                return;
              } 
              while (it.hasNext()) {
                BusinessInfo businessInfo = (BusinessInfo) it.next();
                for (Iterator kit = businessInfo.getServiceInfos().getServiceInfoVector().iterator(); kit.hasNext(); ) {
                  serviceKeys.add(((ServiceInfo) kit.next()).getServiceKey());
                }
              }
              transit("SBDone");
            } });

        addLink("SBDone", "initBag");

        add(new SState("initBag") {
            public void invoke() {
              iter = serviceCategories.iterator();
              transit("initBagLoop");
            }});
        add(new SState("initBagLoop") {
            public void invoke() {
              if (iter.hasNext()) {
                final ServiceClassification sc = (ServiceClassification) iter.next();
                Callback cb = new Callback() {
                  public void invoke(Object o) {
                    KeyedReference kr = (KeyedReference) o;
                    updateBag.getKeyedReferenceVector().add(kr);
                    transit("initBagLoop");
                    kick();
                  }
                  public void handle(Exception e) {
                    log.error("initBagLoop.getKeyedReference(" + sc + ")", e);
                    //transit("ERROR");
                    callback.handle(e);
                  }
                };
                getKeyedReference(getYPProxy(),
				  sc.getClassificationSchemeName(),
                                  sc.getClassificationName(),
                                  sc.getClassificationCode(),
                                  cb);
              } else {
                transit("initBagDone");
              }
            }});
        addLink("initBagDone", "getServiceDetail");

        addYPQ("getServiceDetail", "getServiceDetailDone", new YPQ() {
            public YPFuture get(Frame f) {
              return getYPProxy().get_serviceDetail(serviceKeys);
            }
            public void set(Frame f, Object r) {
              ServiceDetail sd = (ServiceDetail) r;
              Enumeration e = sd.getBusinessServiceVector().elements();
              while (e.hasMoreElements()) {
                BusinessService bs  = (BusinessService)e.nextElement();
                CategoryBag thisBag = bs.getCategoryBag();
                thisBag.getKeyedReferenceVector().addAll(updateBag.getKeyedReferenceVector());
                services.add(bs);
              }
            }
            public void handle(Frame f, Exception e) {
              log.error("getServiceDetail", e);
              callback.handle(e);
            }
          });

        addLink("getServiceDetailDone", "saveService");
        addYPQ("saveService", "saveServiceDone", new YPQ() {
            public YPFuture get(Frame f) {
              return getYPProxy().save_service(getAuthToken().getAuthInfoString(), services);
            }
            public void set(Frame f, Object r) {
              // copacetic
            }
            public void handle(Frame f, Exception e) {
              log.error("saveService", e);
              callback.handle(e);
            }
          });
        addLink("saveServiceDone", "finish");
        add(new SState("finish") {
            public void invoke() { 
              callback.invoke(Boolean.TRUE); // let complete while discarding token
              call("discardAuthToken", null, "DONE");
            }
          });

      }
    }

    /**
     * @param callback Callback.invoke(Boolean) true IFF success.
     **/
    public void deleteServiceDescription(Object ypContext,
					 String providerName, 
					 Collection serviceCategories, 
					 Callback callback) {
      (new DeleteSM(ypContext, providerName, serviceCategories, callback)).start();
    }      
    
    private class DeleteSM extends SMBase {
      final String providerName;
      final Collection serviceCategories;

      ServiceInfo service = null;
      Vector namePatterns = new Vector();
      FindQualifiers findQualifiers = new FindQualifiers();
      Vector qualifier = new Vector();
      CategoryBag bag = new CategoryBag();
      Iterator iter;            // used by initBag
      BusinessList businessList; // set by findBusiness
      BusinessInfo businessInfo; // set by findBusinessDone
      Vector serviceKeys;       // used in dbLoop

      private DeleteSM(Object ypContext, String providerName, Collection serviceCategories, Callback callback) {
	super(ypContext, callback);
        this.providerName = providerName;
        this.serviceCategories = serviceCategories;

        namePatterns.add(new Name(providerName));
        qualifier.add(new FindQualifier(FindQualifier.serviceSubset));
        findQualifiers.setFindQualifierVector(qualifier);
      }

      protected void init() {
        super.init();
        addLink("START", "getToken");
        add(new SState("getToken") {
            public void invoke() { 
              call("getAuthToken", null, "gotToken");
            }
          });
        addLink("gotToken","initBag");

        add(new SState("initBag") {
            public void invoke() {
              iter = serviceCategories.iterator();
              transit("initBagLoop");
            }});
        add(new SState("initBagLoop") {
            public void invoke() {
              if (iter.hasNext()) {
                final ServiceClassification sc = (ServiceClassification) iter.next();
                Callback cb = new Callback() {
                  public void invoke(Object o) {
                    KeyedReference kr = (KeyedReference) o;
                    bag.getKeyedReferenceVector().add(kr);
                    transit("initBagLoop");
                    kick();
                  }
                  public void handle(Exception e) {
                    log.error("initBagLoop.getKeyedReference(" + sc + ")", e);
                    //transit("ERROR");
                    callback.handle(e);
                  }
                };
                getKeyedReference(getYPProxy(),
				  sc.getClassificationSchemeName(),
                                  sc.getClassificationName(),
                                  sc.getClassificationCode(),
                                  cb);
              } else {
                transit("initBagDone");
              }
            }});
        addLink("initBagDone", "findBusiness");

        addYPQ("findBusiness", "findBusinessDone", new YPQ() {
            public YPFuture get(Frame f) {
              return getYPProxy().find_business(namePatterns, null, null, bag, null, findQualifiers, 1);
            }
            public void set(Frame f, Object result) {
              businessList = (BusinessList) result;
            }
            public void handle(Frame f, Exception e) {
              log.error("findBusiness", e);
              callback.handle(e);
            }
          });

        add(new SState("findBusinessDone") {
            public void invoke() {
              if (businessList == null) {
                callback.invoke(Boolean.TRUE); // easy - nothing to delete
                transit(DONE);
              }
              transit("deleteBusiness");
            } 
          });
        add(new SState("deleteBusiness") {
            public void invoke() {
              iter = businessList.getBusinessInfos().getBusinessInfoVector().iterator();
              transit("dbLoop");
            }});
        add(new SState("dbLoop") {
            public void invoke() {
              if (iter.hasNext()) {
                businessInfo = (BusinessInfo) iter.next();
                if (businessInfo == null) {
                  if (log.isDebugEnabled()) {
                    log.debug("deleteServiceDescription, cannot find registration for: " + 
			      providerName);
                  }
                } else {
                  transit("dbLoop1");
                }
              } else {
                transit("dbDone");
              }
            }
          });
        addYPQ("dbLoop1", "dbLoop", new YPQ() {
            public YPFuture get(Frame f) {
              serviceKeys = new Vector();
              for (Enumeration enum = businessInfo.getServiceInfos().getServiceInfoVector().elements(); 
                   enum.hasMoreElements();
                   ) {
                serviceKeys.add(((ServiceInfo) enum.nextElement()).getServiceKey());
              }
              return getYPProxy().delete_service(getAuthToken().getAuthInfoString(), serviceKeys);
            }
            public void set(Frame f, Object r) {
              // dont care
            }
            public void handle(Frame f, Exception e) {
              log.error("deleteServiceDescription exception", e);
              transit("dbLoop"); // continue to next
            }
          });
        add(new SState("dbDone") {
            public void invoke() { 
              callback.invoke(Boolean.TRUE); 
	      // let it complete while we're discarding the token
              call("discardAuthToken", null, "DONE");
            }
          });
      }
    }
  }
}





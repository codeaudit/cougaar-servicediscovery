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

package org.cougaar.servicediscovery.util.yp;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Vector;

import org.apache.xerces.parsers.DOMParser;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.component.ComponentSupport;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.AlarmService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.servicediscovery.util.UDDIConstants;
import org.cougaar.util.StateMachine;
import org.cougaar.yp.YPProxy;
import org.cougaar.yp.YPService;
import org.cougaar.yp.YPStateMachine;

import org.uddi4j.datatype.tmodel.TModel;
import org.uddi4j.response.AuthToken;
import org.uddi4j.response.TModelDetail;
import org.uddi4j.util.CategoryBag;
import org.uddi4j.util.KeyedReference;
import org.xml.sax.InputSource;


/**
 *
 */
public class PublishTaxonomy extends ComponentSupport {
  private static String UDDI_USERID = "cougaar";
  private static String UDDI_PASSWORD = "cougaarPass";

  static {
    UDDI_USERID = System.getProperty("org.cougaar.yp.juddi-users.username", YPProxy.DEFAULT_UDDI_USERNAME);
    UDDI_PASSWORD = System.getProperty("org.cougaar.yp.juddi-users.password", YPProxy.DEFAULT_UDDI_PASSWORD);
  }

  private static int WARNING_SUPPRESSION_INTERVAL = 2;
  private static final String TAXONOMY_GRACE_PERIOD = 
    "org.cougaar.servicediscovery.util.yp.TaxonomyGracePeriod"; 
  private long myWarningCutoffTime = -1;

  private YPProxy myYPProxy;

  private PublishTaxonomyMachine myStateMachine;

  private AlarmService myAlarmService;
  private AgentIdentificationService myAgentIdentificationService;
  private LoggingService myLoggingService;
  private ThreadService myThreadService;
  private YPService myYPService;

  private Alarm myAlarm;
  
  private ArrayList myTModelNames;

  private static String []TMODELNAMES = 
     { UDDIConstants.MILITARY_SERVICE_SCHEME,
       UDDIConstants.MILITARY_ECHELON_SCHEME,
       UDDIConstants.ORGANIZATION_TYPES,
       UDDIConstants.SOURCING_CAPABILITY_SCHEME,
       UDDIConstants.SUPPORT_COMMAND_ASSIGNMENT };

  public void setAgentIdentificationService(AgentIdentificationService ais) {
    myAgentIdentificationService = ais;
  }

  protected final AgentIdentificationService getAgentIdentificationService() {
    return myAgentIdentificationService;
  }

  public void setAlarmService(AlarmService as) {
    myAlarmService = as;
  }

  protected final AlarmService getAlarmService() {
    return myAlarmService;
  }

  public void setLoggingService(LoggingService ls) { 
    myLoggingService = ls; 
  }

  protected final LoggingService getLoggingService() {
    return myLoggingService;
  }


  public void setThreadService(ThreadService ts) { 
    myThreadService = ts; 
  }

  protected final ThreadService getThreadService() {
    return myThreadService;
  }

  public void setYPService(YPService yps) { 
    myYPService = yps; 
  }

  protected final YPService getYPService() {
    return myYPService;
  }

  public void load() {
    super.load();

    myTModelNames = new ArrayList(TMODELNAMES.length);
    for (int index = 0; index < TMODELNAMES.length; index++) {
      addTModelName(TMODELNAMES[index]);
    }

    // Don't mess around with community based lookup. 
    // Assume component loaded in the same agent as the YPServer
    myYPProxy = getYPService().getYP(getAgentIdentificationService().getMessageAddress());

    myStateMachine = new PublishTaxonomyMachine(new PublishTaxonomyCallback());

    // start the ball rolling
    myStateMachine.start();
  }

  public long getWarningCutoffTime() {
    if (myWarningCutoffTime == -1) {
      WARNING_SUPPRESSION_INTERVAL = 
	Integer.getInteger(TAXONOMY_GRACE_PERIOD, 
			   WARNING_SUPPRESSION_INTERVAL).intValue();
      myWarningCutoffTime = System.currentTimeMillis() + 
	(WARNING_SUPPRESSION_INTERVAL * 60000);
    }
    
    return myWarningCutoffTime;
  }

  public void addTModelName(String tModelName) {
    // Don't add if we've already started.
    if (myStateMachine != null) {
      StateMachine.State current = myStateMachine.getState();
      if (!current.equals(StateMachine.UNINITIALIZED) && 
	  !current.getKey().equals("START")) {
	getLoggingService().warn("addTModelName: TModelName " + tModelName +
				 " specified after the process of loading " +
				 " taxonomies has started. Will not be added " + 
				 " to the registry.");
	return;
      }
    }

    myTModelNames.add(tModelName);
  }
  

  private class PublishTaxonomyMachine extends YPStateMachine {
    protected final PublishTaxonomyCallback myCallback;

    PublishTaxonomyMachine(PublishTaxonomyCallback callback) {
      super(myYPService, myYPProxy, myThreadService);
      myCallback = callback;
    }

    public void transit(State s0, State s1) {
      if (getLoggingService().isInfoEnabled()) {
        getLoggingService().info("StateMachine transit: "+s0+" to "+s1);
      }
      super.transit(s0, s1);
    }
    protected void kick() {
      if (getLoggingService().isInfoEnabled()) {
        getLoggingService().info("kicked");      
      }
      super.kick();
    }

    private int gentaxi = 0;

    protected void init() {
      super.init();

      addLink("YPError", "handleYPError");
      add(new SState("handleYPError") {
	public void invoke() {
	  myStateMachine.getCallback().handle((Exception) getVar("YPErrorException"),
					      (String ) getVar("YPErrorText"));
	}
      });
	  
      addLink("START", "getToken");
      add(new SState("getToken") {
          public void invoke() { 
	    try {
	      call("getAuthToken", null, "gotToken");
	    } catch (Exception e) {
	      setVar("YPErrorException", e);
	      transit("YPError");      
	    }
          }
        });
      addLink("gotToken","startTaxonomy");

      addLink("startTaxonomy", "genTax");
      add(new SState("genTax") {
          public void invoke() { 
            gentaxi = 0;
            transit("genTax0");
          }
        });
      add(new SState("genTax0") {
          public void invoke() { 
            if (gentaxi < myTModelNames.size()) {
              call("genTaxonomy", myTModelNames.get(gentaxi), "genTax1");
            } else {
              transit("doneTaxonomy");
            }
          }
        });

      add(new SState("genTax1") {
          public void invoke() {
            gentaxi++;
            transit("genTax0");
          }});

      addLink("doneTaxonomy","startTModels");

      addLink("startTModels", "tm0");
      add(new SState("tm0") {
          public void invoke() { 
            call("createBindingTModels", null, "doneTModels");
          }
        });
      add(new SState("doneTModels") {
          public void invoke() { 
            call("discardAuthToken", null, "DONE");
          }
        });

      // subroutines

      // genTaxonomy
      add(new SState("genTaxonomy") {
          public void invoke() {
            String name = (String) getArgument();
            String file_ext = "-yp.xml";

            String basePath = System.getProperty("org.cougaar.install.path") + File.separator +
              "servicediscovery" + File.separator + "data" + File.separator + "taxonomies" + File.separator;

            if(validPath(basePath + name + file_ext)) {
              call("createTaxonomy", new String[] {name,  basePath + name + file_ext}, "POP");
            } else {
              getLoggingService().error("Invalid Path: " + basePath + name + file_ext);
              transit("POP");
            }
          }
        });

      // createTaxonomy
      addTModelPush("createTaxonomy", "POP",
                    new TModelThunk() {
                      public TModel make(Frame f) {
                        String[] args = (String[]) f.getArgument();
                        String name = args[0];
                        TModel tModel = new TModel();
                        tModel.setName(name);
                        return tModel;
                      }
                      public TModel update(Frame f, TModelDetail tModelDetail) {
                        // get the args again
                        String[] args = (String[]) f.getArgument();
                        String name = args[0];
                        String file = args[1];
                        
                        String tModelKey = ((TModel) (tModelDetail.getTModelVector().elementAt(0))).getTModelKey();

			TModel tmodel = null;

			try {
			  tmodel = createTaxonomy(name, file, tModelKey);
		
			} catch (Exception e) {
			  f.setVar("YPErrorException", e);
			  transit("YPERROR");
			}
			return tmodel;
                      }});

      //
      // createBindingTModels
      addLink("createBindingTModels", "createCougaarBinding");
      addTModelPush("createCougaarBinding", "createSoapBinding",
                    new TModelThunk() {
                      public TModel make(Frame f) {
                        TModel cougaarTModel = new TModel("", "COUGAAR:Binding");
                        cougaarTModel.setDefaultDescriptionString("Protocol for COUGAAR services");
                        return cougaarTModel;
                      }
                      public TModel update(Frame f, TModelDetail tModelDetail) {
                        Vector tModels = tModelDetail.getTModelVector();
                        return addCougaarBinding((TModel) tModels.elementAt(0));
                      }});
      addTModelPush("createSoapBinding", "doneCBT",
                    new TModelThunk() {
                      public TModel make(Frame f) {
                        TModel soapTModel = new TModel("", "SOAP:Binding");
                        soapTModel.setDefaultDescriptionString("SOAP binding for non-COUGAAR services");
                        return soapTModel;
                      }
                      public TModel update(Frame f, TModelDetail tModelDetail) {
                        Vector tModels = tModelDetail.getTModelVector();
                        return addSoapBinding((TModel) tModels.elementAt(0));
                      }});
      addLink("doneCBT", "POP");
    }

    protected PublishTaxonomyCallback getCallback() {
      return myCallback;
    }
  }

  TModel createTaxonomy(String name, String file, String tModelKey) throws java.io.IOException, org.xml.sax.SAXException, org.uddi4j.UDDIException {
    FileInputStream fis = new FileInputStream(file);
    DOMParser parser = new DOMParser();
    parser.parse(new InputSource(fis));
    TModel tModel = new TModel(parser.getDocument().getDocumentElement());
    
    tModel.setName(name);
    tModel.setTModelKey(tModelKey);
    
    // Add TModelKey to KeyedReferences
    CategoryBag categoryBag = tModel.getCategoryBag();
    for (int index = 0; index < categoryBag.size(); index++) {
      KeyedReference keyedReference = categoryBag.get(index);
      keyedReference.setTModelKey(tModelKey);
    }
    return tModel;
  }    

  TModel addCougaarBinding(TModel tm) {
    CategoryBag categoryBag = new CategoryBag();
    KeyedReference wsdlKr = new KeyedReference("uddi-org:types", "wsdlSpec");
    wsdlKr.setTModelKey(tm.getTModelKey());
    Vector krList = new Vector();
    krList.add(wsdlKr);
    categoryBag.setKeyedReferenceVector(krList);
    tm.setCategoryBag(categoryBag);
    return tm;
  }

  TModel addSoapBinding(TModel tm) {
    CategoryBag categoryBag = new CategoryBag();
    KeyedReference soapKr = new KeyedReference("uddi-org:types", "soapSpec");
    soapKr.setTModelKey(tm.getTModelKey());
    // described by WSDL
    Vector krList = new Vector();
    krList.add(soapKr);
    KeyedReference wsdlKr = new KeyedReference("uddi-org:types", "wsdlSpec");
    wsdlKr.setTModelKey(tm.getTModelKey());
    krList.add(wsdlKr);
    categoryBag.setKeyedReferenceVector(krList);
    tm.setCategoryBag(categoryBag);
    return tm;
  }

  private static boolean validPath(String path) {
    return (new File(path)).exists();
  }

  private class PublishTaxonomyCallback {
    void handle(Exception e, String exceptionText) {
      // Don't try to keep state - just restart from scratch
      int rand = (int)(Math.random()*10000) + 1000;
      
      if(System.currentTimeMillis() > getWarningCutoffTime()) {
	if (exceptionText == null) {
	  getLoggingService().error("Problem adding service discovery taxonomies, try again later.", e); 
	} else {
	  getLoggingService().error(exceptionText, e);
	  getLoggingService().error("Problem adding service discovery taxonomies, try again later."); 
	}
      } else if (getLoggingService().isDebugEnabled()) {
	if (exceptionText == null) {
	  getLoggingService().debug("Problem adding service discovery taxonomies, try again later." , e);
	} else {
	  getLoggingService().debug(exceptionText, e);
	  getLoggingService().debug("Problem adding service discovery taxonomies, try again later."); 
	}
      }
      
      myAlarm = new PublishAlarm(getAlarmService().currentTimeMillis() + rand);
      getAlarmService().addAlarm(myAlarm);      
    }
  }

  public class PublishAlarm implements Alarm {
    private long expiresAt;
    private boolean expired = false;
    public PublishAlarm (long expirationTime) {
      expiresAt = expirationTime;
    }
    public long getExpirationTime() { return expiresAt; }
    public synchronized void expire() {
      if (!expired) {
        expired = true;
	myStateMachine.reset();
	myStateMachine.start();
      }
    }

    public boolean hasExpired() { return expired; }
    public synchronized boolean cancel() {
      boolean was = expired;
      expired = true;
      return was;
    }
    public String toString() {
      return "<PublishAlarm " + expiresAt +
        (expired ? "(Expired) " : " ") +
        "for PublishTaxonomy at "  + 
	getAgentIdentificationService().getName() + ">";
    }
  }
}






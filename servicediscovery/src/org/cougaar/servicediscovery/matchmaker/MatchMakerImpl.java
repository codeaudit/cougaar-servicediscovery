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
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.servicediscovery.matchmaker;

import com.declarativa.interprolog.ObjectExamplePair;
import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.SubprocessEngine;
import com.declarativa.interprolog.util.BasicTypeWrapper;
import com.declarativa.interprolog.util.InvisibleObject;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.LoggingService;
import org.cougaar.servicediscovery.description.BusinessClassification;
import org.cougaar.servicediscovery.description.BusinessClassificationImpl;
import org.cougaar.servicediscovery.description.MMQuery;
import org.cougaar.servicediscovery.description.ProviderInfo;
import org.cougaar.servicediscovery.description.ServiceClassification;
import org.cougaar.servicediscovery.description.ServiceClassificationImpl;
import org.cougaar.servicediscovery.description.ServiceInfo;
import org.cougaar.servicediscovery.service.RegistryQueryService;
import org.cougaar.servicediscovery.transaction.RegistryQuery;
import org.cougaar.servicediscovery.transaction.RegistryQueryImpl;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * <code>MatchMakerImpl</code> directly communicates with XSB Prolog to find answers
 * to queries.
 */
public class MatchMakerImpl implements MatchMakerService {

    private PrologEngine engine; // interprolog engine
    //private SubprocessEngine engine; // interprolog engine
    private LoggingService log;
    private RegistryQueryService registryQueryService;
    private String[] lineage;

    /** Creates a new instance of MatchMaker. Initializes XSB engine. */
    public MatchMakerImpl() {

        String xsbLocation;
        String mmCodeLocation;

        String CIP = ".";
        CIP = System.getProperty("org.cougaar.install.path");

        if(CIP != null){
            // Figure out xsb.exe (or other binary xsb file) location
            xsbLocation = getXSBLocation(CIP);

            mmCodeLocation = CIP + File.separator + "servicediscovery" +
            File.separator + "data" + File.separator + "matchmaker"
            + File.separator + "matchmaker.P";
        } else {
            throw new RuntimeException("[MatchMakerImpl]: COUGAAR install path not found");
        }

        // Use Logging Service
        log = LoggingService.NULL;

        synchronized(MatchMakerImpl.class){
            if(engine == null){  // not nessecary for Subprocess Engine??
                printDebugInfo("Starting XSB Engine");
                // this.engine = new NativeEngine("C:\\xsbsys\\XSBENV\\xsb",false);
                this.engine = new SubprocessEngine(xsbLocation);
                printDebugInfo("XSB Engine successfully started");
                ObjectExamplePair[] examples = {
                    new ObjectExamplePair("ArrayOfInvisibleObject",
                    new InvisibleObject[0],
                    new InvisibleObject[1]),
                };
                if (!this.engine.teachMoreObjects(examples)) {
                    throw new RuntimeException("Unable to teach InterProlog objects");
                }

                boolean consultResult =
                engine.deterministicGoal("ensure_loaded('" + mmCodeLocation + "')");
                if(consultResult != true) {
                    throw new RuntimeException("Failure consulting matchmaker");
                }

                consultResult =
                engine.deterministicGoal("matchmaker_initialize('" + CIP + "')");
                if(consultResult != true) {
                    if(log.isShoutEnabled()) {
                        log.shout("Failure initializing matchmaker");
                    }
                }
            }
        }
    }

    /**
     * Get location of xsb executable file
     */
    protected String getXSBLocation(String CIP){
        String xsbLocation;
        if(isWindowsOS()){ // We are in Windows
            xsbLocation = CIP + File.separator + "servicediscovery" +
            File.separator + "bin" + File.separator + "XSB" +
            //File.separator + "config" + File.separator + "x86-pc-windows" +
            File.separator + "config" + File.separator + "i686-pc-cygwin" +
            File.separator + "bin" + File.separator + "xsb.exe";
        } else if(isLinuxOS()){
            xsbLocation = CIP + File.separator + "servicediscovery" +
            File.separator + "bin" + File.separator + "XSB" +
            File.separator + "config" + File.separator + "i686-pc-linux-gnu" +
            File.separator + "bin" + File.separator + "xsb";
        } else {
            xsbLocation = System.getProperty("org.cougaar.servicediscovery.xsb.location");
            if(xsbLocation == null){
                throw new RuntimeException("[MatchMakerImpl]: XSB not found," +
                " please specify in org.cougaar.servicediscovery.xsb.location property");
            }
        }
        return xsbLocation;
    }

    protected static boolean isWindowsOS(){ // if we're under Windows
        return (System.getProperty("os.name").toLowerCase().indexOf("windows")!=-1);
    }

    protected static boolean isLinuxOS(){ // if we're under Windows
        return (System.getProperty("os.name").toLowerCase().indexOf("linux")!=-1);
    }

    /**
     * Set service broker that will be used to obtain LoggingService and RegistryQueryService.
     */
    public void setServiceBroker(ServiceBroker sb){

        // Use Logging Service
        this.log = (LoggingService) sb.getService(this, LoggingService.class, null);
        if(log == null){
            log = LoggingService.NULL;
        }

        // get RegistryQueryService
        this.registryQueryService = (RegistryQueryService)sb.getService(this,
        RegistryQueryService.class, null);
        if (registryQueryService == null){
            throw new RuntimeException("[MatchMakerImpl]: Unable to obtain RegistryQuery service");
        }

    }

    /**
     * Method for implementation of MatchMakerService interface. Finds services for a given <code>query</code>.
     */
    public MatchMakerQueryResultImpl findService(MMQuery query, String[] commandLineage, ServiceBroker sb) {
        setServiceBroker(sb);
        if (registryQueryService == null){
            throw new RuntimeException("No RegistryQuery service");
        }

        if(query != null){
            if(engine != null){
                printDebugInfo("MatchMakerImpl: findService");
                Object[] bindings = null;
                synchronized(MatchMakerImpl.class){
                    clearCacheForClass("org.cougaar.servicediscovery.description.ProviderInfo");
                    clearCacheForClass("org.cougaar.servicediscovery.description.ServiceInfo");
                    clearCacheForClass("org.cougaar.servicediscovery.matchmaker.ScoredServiceInfo");
                    clearCacheForClass("org.cougaar.servicediscovery.description.ServiceClassificationImpl");
                    clearCacheForClass("org.cougaar.servicediscovery.description.BusinessClassificationImpl");
                    engine.unregisterJavaObjects((new InvisibleObject[0]).getClass());
                    this.lineage = commandLineage;
                    // Service is a name of variable,
                    // all queries should use it to stand for service provider
                    bindings = engine.deterministicGoal(
                    "matchmaker(Query,Cutoff,JavaRef,ScoredServiceList,ExceptionString)",
                    "[Query,Cutoff, JavaRef]",
                    new Object[]{"query("+((MatchMakerQuery)query).getQueryString()+", Service).",
                    new BasicTypeWrapper(new Float(((MatchMakerQuery)query).getCutoff())),
                    engine.makeInvisible(this)},
                    "[ScoredServiceList,ExceptionString]");

                    if(bindings != null){ //received array of invisible objects representing ScoredServiceInfo
                        InvisibleObject [] serviceBindings = ((InvisibleObject [])bindings[0]);
                        printDebugInfo("MatchMakerImpl: findService, services found: "+serviceBindings.length);
                        // There are answers to the query
                        ArrayList answers = new ArrayList();
                        for(int i = 0; i < serviceBindings.length; i++){
                            answers.add(engine.getRealJavaObject(serviceBindings[i]));
                        }
                        if(bindings[1] != null){
                            if(!((String)bindings[1]).equals("")){
                                if(log.isWarnEnabled()){
                                    log.warn("Exception in prolog matchmaker code" + bindings[1]);
                                }
                            }
                        }
                        return new MatchMakerQueryResultImpl(1,answers);

                    } else {
                        printDebugInfo("MatchMakerImpl: findService, no services found");
                        // Query failed by some reason
                        return new MatchMakerQueryResultImpl(2,null);
                    }
                }

            } else {
                throw new RuntimeException("[MatchMakerImpl]: XSB Prolog engine is null");
            }

        }
        return null;
    }
    
    protected void clearCacheForClass(String className){
        try{
            engine.unregisterJavaObjects(Class.forName(className));
        } catch(ClassNotFoundException e) {
            if(log.isWarnEnabled()) {
                log.warn("[MMService:] " + className + " class is not found");
            }
        }
    }
    
    /**
     * Method called from XSB Prolog. Calls findServices method of the
     * RegistryQueryService to find services that are either classified
     * to a class with name className, code classCode
     * and taxonomy schema corresponding to classSource, or to find services that
     * have a value for one of attributes with name className, code classCode
     * and name-space taxonomyScheme. Makes the conversion
     * between data structures returned by the RegistryQueryService and those easily understandable
     * by XSB Prolog.
     */
    public InvisibleObject[] findServices(String className, String classCode, String taxonomyScheme){
        InvisibleObject[] servicesArray;
        printDebugInfo("MM Call to RegistryQueryService: findServiceAndBinding " + classCode+" "+ className +" "+ taxonomyScheme);

        RegistryQuery rq = new RegistryQueryImpl();
        ServiceClassification sc = new ServiceClassificationImpl(classCode, className, taxonomyScheme);
        rq.addServiceClassification(sc);
        Collection services = registryQueryService.findServiceAndBinding(rq);
        
        if(services != null){
            
            printDebugInfo("MM Call to RegistryQueryService: findServiceAndBinding - found "+services.size()+" services");
            servicesArray = new InvisibleObject[services.size()];
            int i=0;
            for (Iterator iter = services.iterator(); iter.hasNext(); ) {
                ServiceInfo key = (ServiceInfo) iter.next();
                servicesArray[i] = (InvisibleObject)engine.makeInvisible(key);
                i++;
            }
            return servicesArray;
        } else return null;
    }

    /**
     * Method called from XSB Prolog. <code>classifications</code> input argument
     * is a string array of the form className1, classCode1, taxonomyScheme1,
     * className2, classCode2, taxonomyScheme2, ...
     * Calls findServices method of the
     * RegistryQueryService to find services that are either classified
     * to any of classes with name classNameN, code classCodeN
     * and taxonomy schema corresponding to classSource, or to find services that
     * have a value for one of attributes with name classNameN, code classCodeN
     * and name-space taxonomySchemeN. It uses OR connector for the classifications and attributes.
     * Makes the conversion between data structures returned by
     * the RegistryQueryService and those easily understandable by XSB Prolog.
     */
    public InvisibleObject[] findServices(String[] classifications){
        InvisibleObject[] servicesArray;

        RegistryQuery rq = new RegistryQueryImpl();
        printDebugInfo("MM Call to RegistryQueryService: findServiceAndBinding ");
        for(int j = 0; j < classifications.length; j = j + 3){
            String className = classifications[j];
            String classCode = classifications[j+1];
            String taxonomyScheme = classifications[j+2];
            ServiceClassification sc = new ServiceClassificationImpl(classCode, className, taxonomyScheme);
            rq.addServiceClassification(sc);
            printDebugInfo("for " + classCode+" "+ className +" "+ taxonomyScheme +
                          ((j < classifications.length - 3)?" OR ":""));
       }
       rq.orLikeKeysQualifier();
       
        Collection services = registryQueryService.findServiceAndBinding(rq);
        
        if(services != null){
            printDebugInfo("MM Call to RegistryQueryService: findServiceAndBinding - found "+services.size()+" services");
            servicesArray = new InvisibleObject[services.size()];
            int i=0;
            for (Iterator iter = services.iterator(); iter.hasNext(); ) {
                ServiceInfo key = (ServiceInfo) iter.next();
                servicesArray[i] = (InvisibleObject)engine.makeInvisible(key);
                i++;
            }
            return servicesArray;
        } else return null;
    }


    /**
     * Method called from XSB Prolog. Calls findProviders method of the
     * RegistryQueryService to find providers who are either classified
     * to a class with name className, code classCode
     * and taxonomy schema corresponding to classSource, or to find providers who
     * have a value for one of attributes with name className, code classCode
     * and name-space taxonomyScheme. Makes the conversion
     * between data structures returned by the RegistryQueryService and those easily understandable
     * by XSB Prolog.
     */

    public InvisibleObject[] findProviders(String className, String classCode, String taxonomyScheme){
        InvisibleObject[] providerArray;
        printDebugInfo("MM Call to RegistryQueryService: findProviders " + classCode + " " + className +" "+ taxonomyScheme);

        RegistryQuery rq = new RegistryQueryImpl();
        BusinessClassification sc = new BusinessClassificationImpl(classCode, className, taxonomyScheme);
        rq.addBusinessClassification(sc);
        Collection providers = registryQueryService.findProviders(rq);
        
        if(providers != null){
            printDebugInfo("MM Call to RegistryQueryService: findProviders - found "+providers.size()+" providers");
            
            providerArray = new InvisibleObject[providers.size()];
            int i=0;
            for (Iterator iter = providers.iterator(); iter.hasNext(); ) {
                ProviderInfo key = (ProviderInfo) iter.next();
                providerArray[i] = (InvisibleObject)engine.makeInvisible(key);
                i++;
            }
            
            return providerArray;
        } else return null;
    }

    /**
     * Method called from XSB Prolog. For a given service returns array of
     * pointers to classification objects. Classification objects contain
     * information of name, code and namespace for classes that are either
     * values of attributes or classifications of services.
     */

    public InvisibleObject[] getServiceCategories(ServiceInfo service){
        InvisibleObject [] classificationObjs;
        Collection classifications = service.getServiceClassifications();
        if(classifications != null){
            classificationObjs = new InvisibleObject[classifications.size()];
            int i=0;
            for (Iterator iter = classifications.iterator(); iter.hasNext(); ) {
                ServiceClassification classification = (ServiceClassification) iter.next();
                classificationObjs[i] = (InvisibleObject)engine.makeInvisible(classification);
                i++;
            }
            return classificationObjs;
        } else return null;
    }

    /**
     * Method called from XSB Prolog. For a given provider returns array of
     * pointers to classification objects. Classification objects contain
     * information of name, code and namespace for classes that are either
     * values of attributes of the provider or classifications of the provider.
     */
    /*public InvisibleObject[] getBusinessCategories(ServiceInfo service){
        InvisibleObject [] classificationObjs;
        Collection classifications = service.getBusinessClassifications();
        classificationObjs = new InvisibleObject[classifications.size()];
        int i=0;
        for (Iterator iter = classifications.iterator(); iter.hasNext(); ) {
            BusinessClassification classification = (BusinessClassification) iter.next();
            classificationObjs[i] = (InvisibleObject)engine.makeInvisible(classification);
            i++;
        }
        return classificationObjs;
    }*/


    /**
     * Method called from XSB Prolog. For a given provider returns array of
     * pointers to classification objects. Classification objects contain
     * information of name, code and namespace for classes that are either
     * values of attributes of the provider or classifications of the provider.
     */
    public InvisibleObject[] getBusinessCategories(ProviderInfo provider){
        InvisibleObject [] classificationObjs;
        Collection classifications = provider.getBusinessClassifications();

        if(classifications != null){
            classificationObjs = new InvisibleObject[classifications.size()];
            int i=0;
            for (Iterator iter = classifications.iterator(); iter.hasNext(); ) {
                BusinessClassification classification = (BusinessClassification) iter.next();
                classificationObjs[i] = (InvisibleObject)engine.makeInvisible(classification);
                i++;
            }
            return classificationObjs;
        } else return null;
    }

    /**
     * Method called from XSB Prolog. For a given service returns true if it is
     * either classified to a class with native id valueCode
     * in namingScheme or has attributes with values (valueCode, namingScheme).
     */
    public boolean hasServiceCategory(ServiceInfo service, String valueCode, String namingScheme){
        Collection classifications = service.getServiceClassifications();
        if(classifications != null){
            for (Iterator iter = classifications.iterator(); iter.hasNext(); ) {
                ServiceClassification classification = (ServiceClassification) iter.next();
                if(classification.getClassificationCode().equals(valueCode)
                && classification.getClassificationSchemeName().equals(namingScheme)){
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Method called from XSB Prolog. For a given provider returns true if the
     * provider is either classified to a class with native id valueCode in
     * namingScheme or has attributes with values (valueCode, namingScheme).
     */
   /* public boolean hasBusinessCategory(ServiceInfo service, String attributeCode, String namingScheme){
        Collection classifications = service.getBusinessClassifications();
        for (Iterator iter = classifications.iterator(); iter.hasNext(); ) {
            BusinessClassification classification = (BusinessClassification) iter.next();
            if(classification.getClassificationCode().equals(attributeCode)
            && classification.getClassificationSchemeName().equals(namingScheme))
                return true;
        }
        return false;
    } */


    /**
     * Method called from XSB Prolog. For a given provider returns true if the
     * provider is either classified to a class with native id valueCode in
     * namingScheme or has attributes with values (valueCode, namingScheme).
     */
    public boolean hasBusinessCategory(ProviderInfo provider, String attributeCode, String namingScheme){
        Collection classifications = provider.getBusinessClassifications();

        if(classifications != null){
            for (Iterator iter = classifications.iterator(); iter.hasNext(); ) {
                BusinessClassification classification = (BusinessClassification) iter.next();
                if(classification.getClassificationCode().equals(attributeCode)
                && classification.getClassificationSchemeName().equals(namingScheme))
                    return true;
            }
        }
        return false;
    }

    /**
     * Method called from XSB Prolog. Calls findProviders method of the
     * RegistryQueryService to find providers with name providerName.
     * Makes the conversion between data structures returned by
     * the RegistryQueryService and those easily understandable
     * by XSB Prolog.
     */
    public InvisibleObject[] findProvidersByName(String providerName){
        InvisibleObject[] providerArray;
        printDebugInfo("MM Call to RegistryQueryService: findProvidersByName " + providerName);

        RegistryQuery rq = new RegistryQueryImpl();
        rq.setProviderName(providerName);
        Collection providers = registryQueryService.findProviders(rq);

        if(providers != null){
            printDebugInfo("MM Call to RegistryQueryService: findProvidersByName - found "+providers.size()+" providers");
            
            providerArray = new InvisibleObject[providers.size()];
            int i=0;
            for (Iterator iter = providers.iterator(); iter.hasNext(); ) {
                ProviderInfo key = (ProviderInfo) iter.next();
                providerArray[i] = (InvisibleObject)engine.makeInvisible(key);
                i++;
            }
            
            return providerArray;
        } else return null;
    }

    /**
     * Method called from XSB Prolog. Calls findServices method of the
     * RegistryQueryService to find services with name serviceName.
     * Makes the conversion between data structures returned by
     * the RegistryQueryService and those easily understandable
     * by XSB Prolog.
     */
    public InvisibleObject[] findServicesByName(String serviceName){
        InvisibleObject[] servicesArray;
        printDebugInfo("MM Call to RegistryQueryService: findServicesByName " + serviceName);

        RegistryQuery rq = new RegistryQueryImpl();
        rq.setServiceName(serviceName);
        Collection services = registryQueryService.findServiceAndBinding(rq);

        if(services != null){
            printDebugInfo("MM Call to RegistryQueryService: findServicesByName - found " + services.size() + " services");
            servicesArray = new InvisibleObject[services.size()];
            int i=0;
            for (Iterator iter = services.iterator(); iter.hasNext(); ) {
                ServiceInfo key = (ServiceInfo) iter.next();
                servicesArray[i] = (InvisibleObject)engine.makeInvisible(key);
                i++;
            }
            return servicesArray;
        } else return null;
    }

    /**
     * Method called from XSB Prolog. For a given provider returns array of
     * pointers to ServiceInfo objects.
     */
    public InvisibleObject[] getServicesOfProvider(ProviderInfo provider){
        InvisibleObject[] serviceObjs;
        Collection services = provider.getServiceInfos();
        if(services != null){
            serviceObjs = new InvisibleObject[services.size()];
            int i=0;
            for (Iterator iter = services.iterator(); iter.hasNext(); ) {
                ServiceInfo serviceInfo = (ServiceInfo) iter.next();
                serviceObjs[i] = (InvisibleObject)engine.makeInvisible(serviceInfo);
                i++;
            }
            return serviceObjs;
        } else return null;
    }

    /**
     * Method called from XSB Prolog. Returns command lineage of the client.
     */
    public String[] getCommandLineage(){
        return lineage;
    }

    /**
     * Method called from XSB Prolog. Returns command superior of the client.
     */
    public String getCommandSuperior(String forEntity){
        if((forEntity != null) && (lineage != null)){
            for(int i=0; i<lineage.length; i++){
                if(forEntity.equals(lineage[i]) && (i < lineage.length-1)){
                    return lineage[i+1];
                }
            }
        }
        return null;
    }

    /**
     * Method called from XSB Prolog. Returns true if object is an element in command lineage.
     */
    public boolean isMilitaryEntity(String object){
        if((object != null) && (lineage != null)){
            for(int i=0; i<lineage.length; i++){
                if(object.equals(lineage[i])){
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * If debug mode is enabled, print <code>outputString</code>
     */
    public void printDebugInfo(String outputString){
        if(log.isDebugEnabled()) {
            log.debug(outputString);
        }
    }
}

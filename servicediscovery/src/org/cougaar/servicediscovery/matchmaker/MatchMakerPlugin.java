/*
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
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

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedEvent;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;
import org.cougaar.servicediscovery.description.LineageList;
import org.cougaar.servicediscovery.description.ScoredServiceDescriptionImpl;
import org.cougaar.servicediscovery.service.RegistryQueryService;
import org.cougaar.servicediscovery.transaction.NewMMQueryRequest;
import org.cougaar.util.UnaryPredicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * MatchMakerPlugin is a part of the MatchMaker architecture, it subscribes for
 * NewMMQueryRequest posted by plugins outside the MatchMaker architecture. It uses
 * MatchMakerService to answer those queries and posts results to the Blackboard.
 */
public class MatchMakerPlugin extends ComponentPlugin {
    
    private LoggingService log;
    private RegistryQueryService registryQueryService;
    private UIDService uidService;
    
    private MatchMakerService matchMakerService = null;
    
    private IncrementalSubscription queries;
    private IncrementalSubscription lineage;
    
    private UnaryPredicate matchMakerRequestPredicate = new UnaryPredicate() {
        public boolean execute(Object o) {
            return (o instanceof NewMMQueryRequest);
        }};

    private UnaryPredicate lineageListPredicate = new UnaryPredicate() {
        public boolean execute(Object o) {
            if(o instanceof LineageList){
                if(((LineageList)o).getType() == LineageList.COMMAND){
                    return true;
                }
            }
            return false;
        }};

        /**
         * Adds MatchMakerServiceProvider as a provider of MatchMakerService
         */
        /*public void initialize(){
            super.initialize();
            ServiceBroker sb = getBindingSite().getServiceBroker();
            MatchMakerServiceProvider mmsp = new MatchMakerServiceProvider(sb);
            sb.addService(MatchMakerService.class, mmsp);
        }*/
        
        /**
         * Gets logging, uid and registry services
         */
        public void load(){
            super.load();
            
            // setup logging service
            this.log = (LoggingService) getBindingSite().getServiceBroker().getService(this, LoggingService.class, null);
            if(log == null){
                log = LoggingService.NULL;
            }
            
            this.uidService = (UIDService)
            getBindingSite().getServiceBroker().getService(this, UIDService.class,
            null);
            if (uidService == null) {
                throw new RuntimeException("Unable to obtain UID service");
            }
            
            // get RegistryQueryService
            // RegistryQueryService is not used currently by MatchMakerPlugin
            // (used by MatchMakerService) so the code might be removed later
            // Now it just makes sure that MatchMakerService will be able to 
            // access RegistryQueryService
            this.registryQueryService = (RegistryQueryService)
            getBindingSite().getServiceBroker().getService(this,
            RegistryQueryService.class,
            null);
            if (registryQueryService == null){
                throw new RuntimeException("Unable to obtain RegistryQuery service");
            }
        }
        
        /**
         * Release UIDService, LoggingService and RegistryQueryService.
         */
        public void unload() {
            if (registryQueryService != null) {
                getBindingSite().getServiceBroker().releaseService(this,
                RegistryQueryService.class,
                registryQueryService);
                registryQueryService = null;
            }
            
            if (uidService != null) {
                getBindingSite().getServiceBroker().releaseService(this, UIDService.class, uidService);
                uidService = null;
            }
            
            if ((log != null) && (log != LoggingService.NULL)) {
                getBindingSite().getServiceBroker().releaseService(this, LoggingService.class, log);
                log = null;
            }
            super.unload();
        }
        
        protected void setupSubscriptions() {
            log = (LoggingService) getBindingSite().getServiceBroker().getService(this, LoggingService.class, null);
            matchMakerService = getMatchMakerService();
            
            // subscribe to lineage
            lineage = (IncrementalSubscription)getBlackboardService().subscribe(lineageListPredicate);

            // subscribe to new queries
            queries = (IncrementalSubscription)getBlackboardService().subscribe(matchMakerRequestPredicate);
            
            // process existing queries
            processMatchMakerQueries(getBlackboardService().query(matchMakerRequestPredicate));
        }
        
        /**
         * Gets reference to MatchMakerService.
         * MatchMakerService is a node level service
         */
        private MatchMakerService getMatchMakerService() {
            ServiceBroker sb = getBindingSite().getServiceBroker();
            if (sb.hasService(MatchMakerService.class)) {
                return (MatchMakerService)sb.getService(this, MatchMakerService.class,
                new ServiceRevokedListener() {
                    public void serviceRevoked(ServiceRevokedEvent re) {}
                });
            } else {
                log.error("[MatchMakerPlugin]: MatchMakerService not available");
                return null;
            }
        }
        
        protected void execute() {
            // Process new MatchMakerRequests
            if(queries.hasChanged()){
                processMatchMakerQueries(queries.getAddedCollection());
            }
        }
        
        /*
         * Process collection of new MatchMakerRequests
         **/
        private void processMatchMakerQueries(Collection collection) {
            Iterator it = collection.iterator();
            while (it.hasNext()) {
                NewMMQueryRequest query = (NewMMQueryRequest)it.next();
                if ((query.getResult() == null) &&  // Haven't responded to this yet
                (query.getQuery() != null)) {
                    handleNewQuery(query);
                }
            }
        }
        
        /*
         * Process a query
         **/
        private void handleNewQuery(NewMMQueryRequest query) {
            if(log.isDebugEnabled()) {
                log.debug("MatchMakerPlugin: about to invoke service for query "+query.getQuery().toString());
            }
            
            String [] lineageArray=null;
            Iterator lineageIterator = lineage.iterator();
            if(lineageIterator.hasNext()){
                LineageList lineageCollection = (LineageList)lineageIterator.next();
                lineageArray = new String [lineageCollection.size()];
                int i = 0;
                //System.out.println("Lineage:");
                for(Iterator it = lineageCollection.iterator(); it.hasNext();){
                    lineageArray[i] = (String)it.next();
                    //System.out.println("--"+i+":"+lineageArray[i]);
                    i++;
                }
            }
            
            // Invoke MatchMakerService
            MatchMakerQueryResultImpl result = 
                matchMakerService.findService(query.getQuery(), lineageArray, getBindingSite().getServiceBroker());
            
            if(result != null){
            // Process MatchMakerService results
            if(result.getResultCode() == 1){ // success, there are matching services
                Collection services = result.getResults(); // Collection of ScoredServiceInfo
                if(services != null){ // there are matching services
                    
                    if(log.isDebugEnabled()) {
                        log.debug("MatchMakerPlugin: received responce with services "+services.size());
                    }
                    
                    // Convert scored service info into scored service descriptions
                    ArrayList scoredServiceDescriptions = new ArrayList();
                    for (Iterator iter = services.iterator(); iter.hasNext(); ) {
                        ScoredServiceInfo key = (ScoredServiceInfo) iter.next();
                        scoredServiceDescriptions.add(new ScoredServiceDescriptionImpl(key.getScore(), key.getServiceKey()));
                    }
                            
                    // post results
                   query.setResult(scoredServiceDescriptions);
                   query.setResultCode(1);
                                    
                    getBlackboardService().publishChange(query);
                    if(log.isDebugEnabled()) {
                        log.debug("MM execute: publishChanged query");
                    }

                } else { //providers == null
                    publishEmptyResponce(query, result.getResultCode());
                }
            } else { // either there are no results or there is a problem in a query
                publishEmptyResponce(query, result.getResultCode());
            }
            }
        }
        
        /**
         * Publishes MMQueryRequest that has result with result code  - resultCode
         * and empty Collection of matching services
         */
        private void publishEmptyResponce(NewMMQueryRequest query, int resultCode){
            if(log.isDebugEnabled()) {
                    log.debug("MatchMakerPlugin: no services found");
                }
                
                query.setResult(new ArrayList());
                query.setResultCode(resultCode);
                getBlackboardService().publishChange(query);
        }
        
}

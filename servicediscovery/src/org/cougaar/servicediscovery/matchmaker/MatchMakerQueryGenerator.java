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

import org.cougaar.servicediscovery.description.ServiceClassification;
import org.cougaar.servicediscovery.util.UDDIConstants;

/**
 * Class whose static methods are MatchMaker query templates.
 */
public class MatchMakerQueryGenerator {
    
    /**
     * Generates a query to find services of a service category <code>category</code>
     */
    public static String providerOfServiceCategory(ServiceClassification category){
        return "','(isa(CategoryObj, id('" + category.getClassificationCode()
                    + "', '" + category.getClassificationSchemeName() + "')), "+
                    "','(attr(ProfileObj, id(serviceCategory,'http://www.daml.org/services/daml-s/0.7/Profile.daml'), CategoryObj), "
                    +"attr(id(Service, _), id(presents,'http://www.daml.org/services/daml-s/0.7/Service.daml'), ProfileObj)))"; 
    }

    /**
     * Generates a query to find services of a service category with <code>code</code> in 
     * naming scheme <code>scheme</code>
     */
    public static String providerOfServiceCategory(String code, String scheme){
        return "','(isa(CategoryObj, id('" + code + "', '" + scheme + "')), "+
                    "','(attr(ProfileObj, id(serviceCategory,'http://www.daml.org/services/daml-s/0.7/Profile.daml'), CategoryObj), "
                    +"attr(id(Service, _), id(presents,'http://www.daml.org/services/daml-s/0.7/Service.daml'), ProfileObj)))"; 
    }
    
    /**
     * Generates a query to find services of possible providers of NSN
     */
    public static String providerForNSN(String nsn){
        return "(attr(id('"+nsn+"',ultralog), id('hasNAICSCategory',ultralog), NaicsCat)," +
	 "(isa(PartProvCat,NaicsCat)" +
	  ";" +
	  " attr(NaicsCat,id('coveredBy',ultralog),DistNaicsCat)," +
	  " isa(PartProvCat,DistNaicsCat)" +
	  ")," +
	  "attr(ProfObj,id('serviceCategory','http://www.daml.org/services/daml-s/0.7/Profile.daml'),PartProvCat)," +
	  "attr(id(Service, _),id('presents','http://www.daml.org/services/daml-s/0.7/Service.daml'),ProfObj))";
    }

    /**
     * Generates a query to find services classified to <code>category</code>
     * for some <code>echelon</code>. For example, 
     * <code> providerOfEchelonOfSupport("BRIGADE", 
     *                                    new ServiceClassificationImpl("3329",
     *                                    "Other Fabricated Metal Product Manufacturing",
     *                                    UDDIConstants.NAICS_TAXONOMY))</code>
     */
    public static String providerOfEchelonOfSupport(String echelon, ServiceClassification category){
        return "','(isa(CategoryObj, id('" + category.getClassificationCode()
                    + "', '" + category.getClassificationSchemeName() + "')), "
                    + "','(attr(CategoryObj,id('" + UDDIConstants.MILITARY_ECHELON_SCHEME 
                    + "', ultralog),id('" + echelon+"', '" + UDDIConstants.MILITARY_ECHELON_SCHEME+"')),"
                    + "','(attr(ProfileObj, id(serviceCategory,'http://www.daml.org/services/daml-s/0.7/Profile.daml'), CategoryObj), "
                    +"attr(id(Service, _), id(presents,'http://www.daml.org/services/daml-s/0.7/Service.daml'), ProfileObj))))"; 
    }

    /**
     * Generates a query to find services classified with <code>code</code> 
     * in a naming <code>scheme</code> for some <code>echelon</code>. For example, 
     * <code> providerOfEchelonOfSupport("BRIGADE", "3329", UDDIConstants.NAICS_TAXONOMY)</code>
     */
    public static String providerOfEchelonOfSupport(String echelon, String code, String scheme){
        return "','(isa(CategoryObj, id('" + code + "', '" + scheme + "')), "
                    + "','(attr(CategoryObj,id('" + UDDIConstants.MILITARY_ECHELON_SCHEME 
                    + "', ultralog),id('" + echelon+"', '" + UDDIConstants.MILITARY_ECHELON_SCHEME+"')),"
                    + "','(attr(ProfileObj, id(serviceCategory,'http://www.daml.org/services/daml-s/0.7/Profile.daml'), CategoryObj), "
                    +"attr(id(Service, _), id(presents,'http://www.daml.org/services/daml-s/0.7/Service.daml'), ProfileObj))))"; 
    }
    
    /**
     * Generates a query to find services classified with <code>code</code> in a naming <code>scheme</code>
     * that provide support for client <code>clientName</code> for some <code>echelon</code>, 
     * allow relaxation up command lineage with penalty <code>lineageRelaxationPenalty</code>. For example, 
     * <code> militaryServiceQuery("3-69-ARBN", "BRIGADE", "3329", UDDIConstants.NAICS_TAXONOMY, 10)</code>
     */

    public static String militaryServiceQuery(String clientName, String echelon, String code, String scheme, float lineageRelaxationPenalty){
        return "(attr(id('mil_entity(''" + clientName + "'')',ultralog),id('SupportedBy',ultralog),Support)::widen(1," + lineageRelaxationPenalty + ",_X),"+
               "isa(id('" + echelon + "','" + UDDIConstants.MILITARY_ECHELON_SCHEME +"'),id('"
                          + echelon + "','" + UDDIConstants.MILITARY_ECHELON_SCHEME +"'))::widen(1,RealEch)," +
               "attr(Support,id('" + UDDIConstants.MILITARY_ECHELON_SCHEME + "',ultralog),RealEch)," +
               "isa(Support, id('" + code + "', '" + scheme + "')),"+
               "attr(ProfileObj, id(serviceCategory,'http://www.daml.org/services/daml-s/0.7/Profile.daml'), Support)," +
               "attr(id(Service, _), id(presents,'http://www.daml.org/services/daml-s/0.7/Service.daml'), ProfileObj))";
    }
    
    /**
     * Find services with name <code>serviceName</code> for a provider with name 
     * <code>providerName</code>.
     */
    public static String queryForServiceByName(String providerName, String serviceName){
        return "(object(_,'" + providerName + "', ProviderObjId, ultralog), " + 
                "attr(id(Service,ServiceSource),id('providedBy','http://www.daml.org/services/daml-s/0.7/Service.daml'),id(ProviderObjId, ultralog))," + 
                "object(_,'" + serviceName + "', Service,ServiceSource))";
    }
    
    /**
     * Query for services that can either distribute or manufacture <code>nsn</code>.
     * Prefers commercial providers of those services over military. Prefers
     * Commercial-Off-The-Shelf services over On-Demand-Manufacturing services.
     */
    public static String dlaNsnQuery(String nsn){
       return "(attr(id('" + nsn + "', ultralog), id('hasNAICSCategory',ultralog), NaicsCat),"+
       "(isa(PartProvCat,NaicsCat)" +
	";" +
	"attr(NaicsCat,id('coveredBy',ultralog),DistNaicsCat)," +
	"isa(PartProvCat,DistNaicsCat)" +
       ")," +
       "attr(ProfObj,id('serviceCategory','http://www.daml.org/services/daml-s/0.7/Profile.daml'),PartProvCat),"+
       "attr(id(Service,ServiceSource),id('presents','http://www.daml.org/services/daml-s/0.7/Service.daml'),ProfObj),"+
       "attr(id(Service,ServiceSource),id('providedBy','http://www.daml.org/services/daml-s/0.7/Service.daml'),ServiceProvider),"+
       "isa(ServiceProvider,id('Commercial','" + UDDIConstants.ORGANIZATION_TYPES + "'))::widen(10,ActType),"+
       "isa(ActType,id('OrganizationTypes','" + UDDIConstants.ORGANIZATION_TYPES + "')),"+
       "attr(PartProvCat,id('" + UDDIConstants.SOURCING_CAPABILITY_SCHEME + "',ultralog),CapType),"+
       "isa(CapType,id('COTS','" + UDDIConstants.SOURCING_CAPABILITY_SCHEME + "'))::widen(5,ActCType),"+
       "isa(ActCType,id('SourcingCapability','" + UDDIConstants.SOURCING_CAPABILITY_SCHEME + "'))," +
       "(isa(CapType,id('ODM','" + UDDIConstants.SOURCING_CAPABILITY_SCHEME + "'))" +
	"->     attr(PartProvCat,id('hasNSNProductionHistory',ultralog),id('" + nsn + "',ultralog))::opt(15)" +
	";      true" +
       "))";
    }    
}

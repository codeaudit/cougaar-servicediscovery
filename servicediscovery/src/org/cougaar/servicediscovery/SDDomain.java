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

package org.cougaar.servicediscovery;

import java.util.ArrayList;
import java.util.Collection;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.domain.DomainAdapter;
import org.cougaar.core.domain.RootPlan;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.DomainService;
import org.cougaar.planning.ldm.LDMServesPlugin;
import org.cougaar.planning.ldm.LogPlan;
import org.cougaar.planning.ldm.LogPlanImpl;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.service.LDMService;
import org.cougaar.servicediscovery.lp.ServiceContractLP;


/**
 * Service discovery Domain package definition.
 **/

public class SDDomain extends DomainAdapter {
  public static final String SD_NAME = "servicediscovery".intern();

  private AgentIdentificationService agentIdService;
  private MessageAddress self;
  private LDMService ldmService;
  private DomainService domainService;

  public String getDomainName() {
    return SD_NAME;
  }

  public SDDomain() {
    super();
  }

  public void setAgentIdentificationService(AgentIdentificationService ais) {
    this.agentIdService = ais;
    if (ais == null) {
      // Revocation
    } else {
      this.self = ais.getMessageAddress();
    }
  }

  public void setLDMService(LDMService ldmService) {
    this.ldmService = ldmService;
  }

  public void setDomainService(DomainService domainService) {
    this.domainService = domainService;
  }

  public void initialize() {
    super.initialize();
    Constants.Role.init();    // Insure that our Role constants are initted
  }

  public void unload() {
    ServiceBroker sb = getBindingSite().getServiceBroker();
    if (agentIdService != null) {
      sb.releaseService(this, AgentIdentificationService.class, agentIdService);
      agentIdService = null;
    }
    if (ldmService != null) {
      sb.releaseService(this, LDMService.class, ldmService);
      ldmService = null;
    }
    if (domainService != null) {
      sb.releaseService(
        this, DomainService.class, domainService);
      domainService = null;
    }
    super.unload();
  }
  public Collection getAliases() {
    ArrayList l = new ArrayList(1);
    l.add("sd");
    return l;
  }

  protected void loadFactory() {
    LDMServesPlugin ldm = ldmService.getLDM();
    setFactory(new SDFactory(ldm));
  }

  protected void loadXPlan() {
    LogPlan logplan = new LogPlanImpl();
    setXPlan(logplan);
  }

  protected void loadLPs() {
    RootPlan rootplan = (RootPlan) getXPlanForDomain("root");
    if (rootplan == null) {
      throw new RuntimeException("Missing \"root\" plan!");
    }
    PlanningFactory ldmf = (PlanningFactory)
      domainService.getFactory("planning");
    LogPlan logPlan = (LogPlan) getXPlan();

    addLogicProvider(new ServiceContractLP(logPlan, rootplan, self, ldmf));
  }

}












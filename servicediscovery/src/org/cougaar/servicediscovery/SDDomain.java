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












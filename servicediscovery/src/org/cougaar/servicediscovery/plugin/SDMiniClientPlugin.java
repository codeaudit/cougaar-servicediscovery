/*
 * <copyright>
 *
 *  Copyright 1997-2004 BBNT Solutions, LLC
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

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.DomainService;
import org.cougaar.glm.ldm.Constants;
import org.cougaar.planning.ldm.PlanningDomain;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.Verb;

/**
 * Self publishes the FindProviders task. Uses a timer to defer publication so that
 * yp queries occur after registration has completed.
 * Intended for use only with the MiniNode configuration.
 */
public class SDMiniClientPlugin extends SDClientPlugin {
  protected boolean publishedFindProviders = false;
  
  public void load() {
    super.load();

    setExecutionDelay(60000L, 120000L);
  }

  boolean woken = false;

  public void execute() {
    if (!woken) {
      wake();
      woken = false;
    }

    if (!publishedFindProviders) {
      DomainService domainService = (DomainService) getBindingSite().getServiceBroker().getService(this, DomainService.class, null);

      PlanningFactory planningFactory = 
	(PlanningFactory) getDomainService().getFactory(PlanningDomain.PLANNING_NAME);
      NewTask task = planningFactory.newTask();
      task.setVerb(Constants.Verb.FindProviders);
      getBlackboardService().publishAdd(task);
      publishedFindProviders = true;
    }

    super.execute();
  }
}

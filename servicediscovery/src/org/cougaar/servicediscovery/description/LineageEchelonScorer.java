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

package org.cougaar.servicediscovery.description;

import java.util.Iterator;

import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.servicediscovery.Constants;
import org.cougaar.servicediscovery.description.Lineage;
import org.cougaar.servicediscovery.description.ProviderCapabilities;
import org.cougaar.servicediscovery.description.ProviderCapability;
import org.cougaar.servicediscovery.description.ServiceInfo;
import org.cougaar.servicediscovery.util.UDDIConstants;

import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

public class LineageEchelonScorer implements ServiceInfoScorer, java.io.Serializable {
  private static Logger logger = Logging.getLogger(LineageEchelonScorer.class);
  String myMinimumEchelon = null;
  Role myRole;
  Lineage myCommandLineage;
  String myAgentName = null;

  public static String getMinimumEchelonOfSupport(ProviderCapabilities capabilities,
						  Role role) {
    int providedEchelonIndex = -1;

    if (capabilities !=null) {
      ProviderCapability providerCapability =  
	capabilities.getCapability(role);
      
      if (providerCapability != null) {
	providedEchelonIndex = 
	  Constants.MilitaryEchelon.echelonOrder(providerCapability.getEchelon());
      }
    }
    
    return Constants.MilitaryEchelon.ECHELON_ORDER[providedEchelonIndex + 1];
  }
  
  public LineageEchelonScorer(Lineage lineage,
			      String minimumEchelon,
			      Role role) {
    setLineage(lineage);
    myMinimumEchelon = minimumEchelon;
    myRole = role;
  }

  public LineageEchelonScorer() {
    myCommandLineage = null;
    myRole = null;
    myMinimumEchelon = null;
  }

  public void setLineage(Lineage lineage) {
    if (myCommandLineage != null) {
      logger.warn("setLineage: ignoring attempt to change lineage from " + 
		  myCommandLineage + " to " + lineage);
    } else if (lineage.getType() == Lineage.OPCON) {
      myCommandLineage = lineage;
      myAgentName = myCommandLineage.getLeaf();
    } else {
      logger.warn("setLineage: ignoring " + lineage +
		  ". Must be OPCON lineage.");
    }
  }

  public Lineage getLineage() {
    return myCommandLineage;
  }

  public void setRole(Role role) {
    if (myRole != null) {
      logger.warn("setRole: ignoring attempt to change Role from " + 
		  myRole + " to " + role);
    } else {
      myRole = role;
    }
  }

  public Role getRole() {
    return myRole;
  }
  

  public void setMinimumEchelon(String echelon) {
    if (myMinimumEchelon != null) {
      logger.warn("setMinimum: ignoring attempt to change minimum echelon from " + 
		  myMinimumEchelon + " to " + echelon);
    } else {
      String verifiedEchelon = 
	Constants.MilitaryEchelon.mapToMilitaryEchelon(echelon);
      
      if (verifiedEchelon == Constants.MilitaryEchelon.UNDEFINED) {
	logger.warn("setMinimumEchelonOfSupport: unrecognized echelon " + 
		    echelon + 
		    ". Ignoring attempt to change minimum echelon from " + 
		    myMinimumEchelon + " to " + echelon);
      } else {
	myMinimumEchelon = verifiedEchelon;
      }
    }
  }

  public String getMinimumEchelon() {
    return myMinimumEchelon;
  }


  /**
   * Will be called by Matchmaker for each ServiceInfo. Returned score will
   * be added to the ScoredServiceDescription associated with the Service.
   * 
   * @return int representing score. Client responsible for 
   * understanding the precise value. Current usage assumes lowest value >= 0
   * is the best. Values less than 0 are not suitable.
   * 
   */
  public int scoreServiceInfo(ServiceInfo serviceInfo) {
    int echelonScore = getEchelonScore(serviceInfo, getMinimumEchelon());

    if (logger.isDebugEnabled()) {
      logger.debug("scoreServiceProvider: echelon score " + echelonScore);
    }
    if (echelonScore < 0) {
      return -1;
    }

    int lineageScore = getLineageScore(serviceInfo);
    if (logger.isDebugEnabled()) {
      logger.debug("scoreServiceProvider: lineage score " + lineageScore);
    }
    if (lineageScore < 0) {
      return -1;
    } else {
      lineageScore = 100 * lineageScore;
    }

    return echelonScore + lineageScore;
  }

  protected int getEchelonScore(ServiceInfo serviceInfo,
				String requestedEchelonOfSupport) {
    int requestedEchelonOrder =
      Constants.MilitaryEchelon.echelonOrder(requestedEchelonOfSupport);

    if (requestedEchelonOrder == -1) {
      if (logger.isWarnEnabled())
	logger.warn(myAgentName + " getEchelonScore() - invalid echelon " + requestedEchelonOfSupport);
      return 0;
    }

    int serviceEchelonOrder = -1;

    for (Iterator iterator = serviceInfo.getServiceClassifications().iterator();
	 iterator.hasNext();) {
      ServiceClassification classification =
	(ServiceClassification) iterator.next();
      if (classification.getClassificationSchemeName().equals(UDDIConstants.MILITARY_ECHELON_SCHEME)) {

	String serviceEchelon = classification.getClassificationCode();
	serviceEchelonOrder =
	  Constants.MilitaryEchelon.echelonOrder(serviceEchelon);
	break;
      }
    }

    if (serviceEchelonOrder == -1) {
      if (logger.isInfoEnabled()) {
	logger.info(myAgentName + ": Ignoring service with a bad echelon of support: " +
		  serviceEchelonOrder + " for provider: " + serviceInfo.getProviderName());
      }
      return -1;
    } if (serviceEchelonOrder < requestedEchelonOrder) {
      if (logger.isInfoEnabled()) {
	logger.info(myAgentName + ": Ignoring service with a lower echelon of support: " +
		  serviceEchelonOrder + " for provider: " + serviceInfo.getProviderName());
      }
      return -1;
    } else {
      return (serviceEchelonOrder - requestedEchelonOrder);
    }
  }

  protected int getLineageScore(ServiceInfo serviceInfo) {
    if (myCommandLineage == null) {
      if (logger.isWarnEnabled()) {
        logger.warn(myAgentName + ": in getLineageScore, has no command lineage");
      }
      return -1;
    }

    //if there are multiple SCAs, return the minimum distance
    //among them
    int minHops = Integer.MAX_VALUE;

    for (Iterator iterator = serviceInfo.getServiceClassifications().iterator();
	 iterator.hasNext();) {
      ServiceClassification classification =
	(ServiceClassification) iterator.next();
      if (classification.getClassificationSchemeName().equals(UDDIConstants.SUPPORT_COMMAND_ASSIGNMENT)) {
	int hops = 
	  myCommandLineage.countHops(myAgentName,
				     classification.getClassificationName());
	if (hops != -1) {
	  minHops = Math.min(minHops, hops);
	}
      }
    }

    if(minHops == Integer.MAX_VALUE) {
      if (logger.isInfoEnabled()) {
        logger.info(myAgentName + ": in getLineageScore, local lineage " + 
		    myCommandLineage + 
		    " does not intersect with provider's SCA " +
		    " for provider " + serviceInfo.getProviderName());
      }
      return -1;
    }
    else
      return minHops;
  }

  public boolean equals(Object o) {
    if (o instanceof LineageEchelonScorer) {
      LineageEchelonScorer scorer = (LineageEchelonScorer) o;

      return ((scorer.getMinimumEchelon().equals(getMinimumEchelon())) &&
	      (scorer.getRole().equals(getRole())) &&
	      (scorer.getLineage().equals(getLineage())));
    } else {
      return false;
    }
  }

  public String toString() {
    return "Role: " + myRole + " MinimumEchelon: " + myMinimumEchelon +
      " CommandLineage: " + myCommandLineage;
  }

}







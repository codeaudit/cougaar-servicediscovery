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

package org.cougaar.servicediscovery.description;

import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;


/**
 * The ScoredServiceDescription associates a float score with a 
 * ServiceDescription.
 * Uses Float.MIN_VALUE to indicate an uninitialized score.
 */

public class ScoredServiceDescriptionImpl extends ServiceDescriptionImpl 
implements ScoredServiceDescription {
  private static float UNDEFINED = Float.MIN_VALUE;
  private static Logger logger = 
    Logging.getLogger(ScoredServiceDescriptionImpl.class);  

  private float myScore = Float.MIN_VALUE;

  public ScoredServiceDescriptionImpl() {
    super();
  }

  public ScoredServiceDescriptionImpl(float score, 
				      ServiceInfo serviceInfo) {
    super(serviceInfo);
    
    myScore = score;
  }

  /**
   * returns the score (presumably set by the MatchMaker) associated with
   * this ServiceDescription
   */
  public float getScore() {
    return myScore;
  }

  /**
   * sets the score associated with this ServiceDescription.
   * Score can only be set once.
   */
  public void setScore(float score) {
    if (myScore > UNDEFINED) {
      logger.error("Attempt to reset score.");
    } else {
      myScore = score;
    }
  }

  public int compareTo(Object o) { 
    if (!(o instanceof ScoredServiceDescription)) {
      return 1;
    } else {
      ScoredServiceDescription otherSSD = (ScoredServiceDescription) o;
      if (getScore() > otherSSD.getScore()) {
	return 1;
      } else if (getScore() == otherSSD.getScore()) {
	return 0;
      } else {
	return -1;
      }
    }
  }
}




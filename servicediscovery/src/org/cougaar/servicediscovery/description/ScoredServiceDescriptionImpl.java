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

import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;


/**
 * The ScoredServiceDescription associates an int score with a 
 * ServiceDescription.
 * Uses Integer.MIN_VALUE to indicate an uninitialized score.
 */

public class ScoredServiceDescriptionImpl extends ServiceDescriptionImpl 
implements ScoredServiceDescription {
  private static int UNDEFINED = Integer.MIN_VALUE;
  private static Logger logger = 
    Logging.getLogger(ScoredServiceDescriptionImpl.class);  

  private int myScore = UNDEFINED;

  public ScoredServiceDescriptionImpl() {
    super();
  }

  public ScoredServiceDescriptionImpl(int score, 
				      ServiceInfo serviceInfo) {
    super(serviceInfo);
    
    myScore = score;
  }

  /**
   * returns the score (presumably set by the MatchMaker) associated with
   * this ServiceDescription
   */
  public int getScore() {
    return myScore;
  }

  /**
   * sets the score associated with this ServiceDescription.
   * Score can only be set once.
   */
  public void setScore(int score) {
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




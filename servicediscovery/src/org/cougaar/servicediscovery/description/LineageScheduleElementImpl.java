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

package org.cougaar.servicediscovery.description;


import org.cougaar.planning.ldm.plan.ScheduleElementImpl;
import org.cougaar.util.TimeSpan;

/**
 * A LocationScheduleElement is an encapsulation of temporal relationships
 * and a location over that time interval.
 *
 **/

public class LineageScheduleElementImpl 
  extends ScheduleElementImpl
  implements LineageScheduleElement
{
  /** no-arg constructor */
  public LineageScheduleElementImpl () {
    super();
  }
	
  /** constructor for factory use that takes the start and end dates and a location*/
  public LineageScheduleElementImpl(TimeSpan timeSpan) {
    super(timeSpan.getStartTime(), timeSpan.getEndTime());
  }

  /** constructor for factory use that takes the start and end times and a location*/
  public LineageScheduleElementImpl(long start, long end) {
    super(start, end);
  }
} 

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

package org.cougaar.servicediscovery.util;

import org.cougaar.servicediscovery.description.Lineage;
import org.cougaar.util.MutableTimeSpan;
import org.cougaar.util.TimeSpan;

import java.util.Date;

public class LineageTimeSpan extends MutableTimeSpan {
  Lineage myLineage = null;

  public LineageTimeSpan(Lineage lineage, TimeSpan timeSpan) {
    this(lineage, timeSpan.getStartTime(), timeSpan.getEndTime());
  }

  public LineageTimeSpan(Lineage lineage, long startTime, long endTime) {
    super();

    setTimeSpan(startTime, endTime);
    myLineage = lineage;
  }

  public Lineage getLineage() {
    return myLineage;
  }

  public boolean equals(Object o) {
    if (o instanceof LineageTimeSpan) {
      LineageTimeSpan lineageTimeSpan = (LineageTimeSpan) o;
      return ((lineageTimeSpan.getStartTime() == getStartTime()) &&
              (lineageTimeSpan.getEndTime() == getEndTime()) &&
              (lineageTimeSpan.getLineage().equals(getLineage())));
    } else {
      return false;
    }
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("start=" + new Date(getStartTime()) +
               ", end=" + new Date(getEndTime()));
      
    buf.append(", lineage=" + myLineage);
    buf.append("]");
      
    return buf.toString();
  }
}

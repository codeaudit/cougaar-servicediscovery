package org.cougaar.servicediscovery.description;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.cougaar.util.MutableTimeSpan;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 */

public class TimeInterval extends MutableTimeSpan {
  private static Logger logger = Logging.getLogger(TimeInterval.class);

  public TimeInterval(long start, long end) {
    super();
    setTimeSpan(start, end);
  }
  
  
  public static Collection removeInterval(TimeInterval interval, Collection desiredCoverageIntervals) {

    if (interval == null) {
      return desiredCoverageIntervals;
    }

    ArrayList ret = new ArrayList();
    for (Iterator it = desiredCoverageIntervals.iterator(); 
	 it.hasNext();) {
      TimeInterval current = (TimeInterval) it.next();
      ret.addAll(current.subtractInterval(interval));
    }
    return ret;
  }
  
  public Collection subtractInterval(TimeInterval interval) {
    ArrayList ret = new ArrayList();

    if (interval == null) {
      ret.add(this);
      return ret;
    }

    //intervals are not overlapping
    //this      ******
    //interval           *****
    if(this.getStartTime() < interval.getStartTime() &&
       this.getEndTime() < interval.getEndTime() &&
       this.getEndTime() <= interval.getStartTime()) {
      ret.add(this);
      return ret;
    }

    //intervals are not overlapping
    //this             ******
    //interval *****
    if(interval.getStartTime() < this.getStartTime() &&
       interval.getEndTime() < this.getEndTime() &&
       interval.getEndTime() <= this.getStartTime()) {
      ret.add(this);
      return ret;
    }
    
    //this is completely contained
    //return empty
    //this       ****
    //interval  *******
    if(interval.getStartTime() <= this.getStartTime() &&
	    interval.getEndTime() >= this.getEndTime()) {
      return ret;
    }
    
    //interval is completely contained
    //return 0 or 1 or 2 time intervals
    //this     *********
    //interval    ****
    if(this.getStartTime() <= interval.getStartTime() &&
       this.getEndTime() >= interval.getEndTime()) {
      
      if(this.getStartTime() < interval.getStartTime()) {
	ret.add(new TimeInterval(this.getStartTime(), interval.getStartTime()));
      }
      if(this.getEndTime() > interval.getEndTime()) {
	ret.add(new TimeInterval(interval.getEndTime(), this.getEndTime()));
      }
      return ret;
    }
    
    //overlap w/o containing
    //return 1 time interval
    //this   ******
    //inteval    *****
    if(this.getStartTime() <= interval.getStartTime() &&
       this.getEndTime() <= interval.getEndTime() &&
       interval.getStartTime() < this.getEndTime()) {
      ret.add(new TimeInterval(this.getStartTime(), interval.getStartTime()));
      return ret;
    }

    //overlap w/o containing
    //return 1 time interval
    //this       ******
    //inteval  *****
    if(interval.getStartTime() <= this.getStartTime() &&
       interval.getEndTime() <= this.getEndTime() &&
       this.getStartTime() < interval.getEndTime()) {
      ret.add(new TimeInterval(interval.getEndTime(), this.getEndTime()));
      return ret;
    }

    logger.warn("subtractInterval: unable to subtract Start:" + 
		interval.getStartTime() + " - End:" +
		interval.getEndTime() + 
		" from Start:" + this.getStartTime() +
		" - End:" + this.getEndTime());
    return null;
  }
}













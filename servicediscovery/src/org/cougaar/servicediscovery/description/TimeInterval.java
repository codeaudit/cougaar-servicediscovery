package org.cougaar.servicediscovery.description;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.cougaar.util.TimeSpan;
import org.cougaar.util.TimeSpans;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 */

/** Collection of utility methods for manipulation and 
 * construction of TimeSpan objects.
 **/
public abstract class TimeInterval {

  /** make uninstantiable **/
  private TimeInterval() {}
  
  /** Removes specified TimeSpan interval from a Collection of TimeSpans **/
  public static Collection removeInterval(TimeSpan interval, 
					  Collection desiredCoverageIntervals) {

    if (interval == null) {
      return desiredCoverageIntervals;
    }

    ArrayList ret = new ArrayList();
    for (Iterator it = desiredCoverageIntervals.iterator(); 
	 it.hasNext();) {
      TimeSpan current = (TimeSpan) it.next();
      ret.addAll(removeInterval(interval, current));
    }
    return ret;
  }
  
  /** Removes specified TimeSpan interval from a TimeSpan **/
  public static Collection removeInterval(TimeSpan interval, TimeSpan current) {
    ArrayList ret = new ArrayList();

    if (interval == null) {
      ret.add(current);
      return ret;
    }

    //intervals are not overlapping
    //this      ******
    //interval           *****
    if(current.getStartTime() < interval.getStartTime() &&
       current.getEndTime() < interval.getEndTime() &&
       current.getEndTime() <= interval.getStartTime()) {
      ret.add(current);
      return ret;
    }

    //intervals are not overlapping
    //this             ******
    //interval *****
    if(interval.getStartTime() < current.getStartTime() &&
       interval.getEndTime() < current.getEndTime() &&
       interval.getEndTime() <= current.getStartTime()) {
      ret.add(current);
      return ret;
    }
    
    //this is completely contained
    //return empty
    //this       ****
    //interval  *******
    if(interval.getStartTime() <= current.getStartTime() &&
	    interval.getEndTime() >= current.getEndTime()) {
      return ret;
    }
    
    //interval is completely contained
    //return 0 or 1 or 2 time intervals
    //this     *********
    //interval    ****
    if(current.getStartTime() <= interval.getStartTime() &&
       current.getEndTime() >= interval.getEndTime()) {
      
      if(current.getStartTime() < interval.getStartTime()) {
	ret.add(TimeSpans.getSpan(current.getStartTime(), interval.getStartTime()));
      }
      if(current.getEndTime() > interval.getEndTime()) {
	ret.add(TimeSpans.getSpan(interval.getEndTime(), current.getEndTime()));
      }
      return ret;
    }
    
    //overlap w/o containing
    //return 1 time interval
    //this   ******
    //inteval    *****
    if(current.getStartTime() <= interval.getStartTime() &&
       current.getEndTime() <= interval.getEndTime() &&
       interval.getStartTime() < current.getEndTime()) {
      ret.add(TimeSpans.getSpan(current.getStartTime(), interval.getStartTime()));
      return ret;
    }

    //overlap w/o containing
    //return 1 time interval
    //this       ******
    //inteval  *****
    if(interval.getStartTime() <= current.getStartTime() &&
       interval.getEndTime() <= current.getEndTime() &&
       current.getStartTime() < interval.getEndTime()) {
      ret.add(TimeSpans.getSpan(interval.getEndTime(), current.getEndTime()));
      return ret;
    }

    return null;
  }
}













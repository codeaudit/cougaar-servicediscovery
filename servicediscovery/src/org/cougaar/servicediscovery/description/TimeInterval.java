package org.cougaar.servicediscovery.description;

import org.cougaar.util.TimeSpan;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

public class TimeInterval implements TimeSpan {

    private long startTime;
    private long endTime;

    public TimeInterval(long start, long end) {
      startTime = start;
      endTime = end;
    }

    public long getEndTime() {
      return endTime;
    }

    public long getStartTime() {
      return startTime;
    }

    public static Collection removeInterval(TimeInterval interval, Collection desiredCoverageIntervals) {
      ArrayList ret = new ArrayList();
      Iterator it = desiredCoverageIntervals.iterator();
      while(it.hasNext()) {
        TimeInterval current = (TimeInterval) it.next();
        ret.addAll(current.subtractInterval(interval));
      }
      return ret;
    }

    public Collection subtractInterval(TimeInterval interval) {
      ArrayList ret = new ArrayList();

      //intervals are not overlapping
      //return this
      //this      ******
      //interval           *****
      if(this.getStartTime() < interval.getStartTime() &&
         this.getEndTime() < interval.getEndTime() &&
         this.getEndTime() <= interval.getStartTime()) {
        ret.add(this);
        return ret;
      }
      //this             ******
      //interval *****
      else if(interval.getStartTime() < this.getStartTime() &&
              interval.getEndTime() < this.getEndTime() &&
              interval.getEndTime() <= this.getStartTime()) {
        ret.add(this);
        return ret;
      }

      //this is completely contained
      //return empty
      //this       ****
      //interval  *******
      else if(interval.getStartTime() <= this.getStartTime() &&
         interval.getEndTime() >= this.getEndTime()) {
        return ret;
      }

      //interval is completely contained
      //return 0 or 1 or 2 time intervals
      //this     *********
      //interval    ****
      else if(this.getStartTime() <= interval.getStartTime() &&
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
      else if(this.getStartTime() <= interval.getStartTime() &&
              this.getEndTime() <= interval.getEndTime() &&
              interval.getStartTime() < this.getEndTime()) {
        ret.add(new TimeInterval(this.getStartTime(), interval.getStartTime()));
        return ret;

      }
      //this       ******
      //inteval  *****
      else if(interval.getStartTime() <= this.getStartTime() &&
              interval.getEndTime() <= this.getEndTime() &&
              this.getStartTime() < interval.getEndTime()) {
        ret.add(new TimeInterval(interval.getEndTime(), this.getEndTime()));
        return ret;
      }

      else {
        //this should never happen
        return ret;
      }
    }

}






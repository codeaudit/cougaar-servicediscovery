/*
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
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
package org.cougaar.servicediscovery.util;

import java.util.HashSet;
import java.util.Iterator;
import org.cougaar.util.LockFlag;

/*
 * @property org.cougaar.servicediscovery.util.LockPool.maxLocks number
 * of locks in the pool. Defaults to 5.
 */

public class LockPool implements org.cougaar.core.persist.NotPersistable {
  private static LockPool CURRENT_POOL = null;

  /*
   * A hash set holding LockTokens
   */
  private static HashSet LOCK_TOKENS;
  private static int MAX_LOCKS = 5;

  static {
    String prefix = "org.cougaar.servicediscovery.util.LockPool.";

    MAX_LOCKS = 
      (Integer.valueOf(System.getProperty(prefix+"maxLocks", 
					  String.valueOf(MAX_LOCKS)))).intValue();
  }

  public static synchronized LockPool getCurrentPool() {
    if (CURRENT_POOL == null) {
      CURRENT_POOL = new LockPool();
    }

    return CURRENT_POOL;
  }

  private LockPool() {
    if (MAX_LOCKS > 0) {
      LOCK_TOKENS = new HashSet(MAX_LOCKS);
    } 

    for (int index = 0; index < MAX_LOCKS; index++) {
      LOCK_TOKENS.add(new LockToken(new LockFlag()));
    }
  }

  public synchronized Object getLock() {
    while(true) {
      // int lockCount = 0;
      for (Iterator iterator = LOCK_TOKENS.iterator();
           iterator.hasNext();) {
        LockToken token = (LockToken) iterator.next();
        LockFlag lockFlag = token.getLockFlag();

        if (lockFlag.getBusyCount() == 0) {
          lockFlag.getBusyFlag();
          // System.out.println("LockPool: found " + lockCount + " locks.");
          return token;
        }
        // lockCount++;
      }
      // System.out.println("LockPool: all " + lockCount + " locks taken.");
      try {
        // No free locks
        wait();
      } catch (Exception e) {}
    }
  }

  /** @return true on success.
   **/
  public synchronized boolean freeLock(Object token) {
    LockToken lockToken = (LockToken) token;
    LockFlag lockFlag = lockToken.getLockFlag();

    if (lockFlag.freeBusyFlag()) {
      notify();
      return true;
    } else {
      return false;
    }
  }

  private static class LockToken {
    private LockFlag myLock;

    public LockToken(LockFlag lock) {
      myLock = lock;
    }

    public LockFlag getLockFlag() {
      return myLock;
    }
  }
}




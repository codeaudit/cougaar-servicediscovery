package org.cougaar.servicediscovery.transaction;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.relay.Relay;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

public class DAMLReadyRelay extends RelayAdapter {

  boolean isReady;

  public DAMLReadyRelay() {
    isReady = false;
  }

  public void setReady() {
    isReady = true;
  }

  public boolean isReady() {
    return isReady;
  }

  public Object getResponse() {
    return this;
  }

  public int updateResponse(MessageAddress target, Object response) {
    DAMLReadyRelay damlReadyRelay =
      (DAMLReadyRelay) response;
    if (damlReadyRelay.isReady()) {
      setReady();
    }
    return Relay.RESPONSE_CHANGE;
  }
}
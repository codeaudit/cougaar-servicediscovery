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

package org.cougaar.servicediscovery.service;

import java.util.Collection;

import org.cougaar.core.component.Service;
import org.cougaar.servicediscovery.description.ProviderDescription;

/** A RegistrationService is an API which may be supplied by a
 * ServiceProvider registered in a ServiceBroker that provides
 * the ability to register agent services with the Service Discovery
 * Mechanism.
 */
public interface RegistrationService extends Service, YPServiceAdapter {

  /**
   * Registers a Service Provider with the Name Service.
   * @return   True if operation was successful
   */
  void addProviderDescription(Object ypContext, ProviderDescription pd, Callback callback);
  void addProviderDescription(Object ypContext, ProviderDescription pd, Collection additionalServiceClassifications, Callback callback);

  void updateServiceDescription(Object ypContext, String providerName, Collection serviceCategories, Callback callback);

  //perhaps second parameter should correspond to service name directly?
  void deleteServiceDescription(Object ypContext, String providerName, Collection serviceCategories, Callback callback);
}





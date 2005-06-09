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

package org.cougaar.servicediscovery;

import java.net.URL;

import org.cougaar.servicediscovery.description.LineageScheduleElement;

import org.cougaar.glm.ldm.Constants.RelationshipType;
import org.cougaar.util.Configuration;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

public class Constants implements org.cougaar.planning.Constants {
  private static Logger logger = Logging.getLogger(Constants.class);

  private Constants() {}

  public interface SDScheduleElementType {
  
    Class LINEAGE = LineageScheduleElement.class;
  }

  public static class Role {
    /**
     * Insure that Role constants are initialized. Actually does
     * nothing, but the classloader insures that all static
     * initializers have been run before executing any code in this
     * class.
     **/
    public static void init() {
    }

    static {
      org.cougaar.planning.ldm.plan.Role.create("Support", RelationshipType.SUPERIOR);
      org.cougaar.planning.ldm.plan.Role.create("AircraftMaintenance", RelationshipType.PROVIDER);
      org.cougaar.planning.ldm.plan.Role.create("FixedWingAircraftMaintenance", RelationshipType.PROVIDER);
      org.cougaar.planning.ldm.plan.Role.create("RotaryWingAircraftMaintenance", RelationshipType.PROVIDER);
      org.cougaar.planning.ldm.plan.Role.create("GroundVehicleMaintenance", RelationshipType.PROVIDER);
      org.cougaar.planning.ldm.plan.Role.create("WheeledVehicleMaintenance", RelationshipType.PROVIDER);
      org.cougaar.planning.ldm.plan.Role.create("TrackedVehicleMaintenance", RelationshipType.PROVIDER);
      org.cougaar.planning.ldm.plan.Role.create("DryCargoTransport", RelationshipType.PROVIDER);
      org.cougaar.planning.ldm.plan.Role.create("Part", RelationshipType.PROVIDER);
    }


    // Support Command Hierarchy
    public static final org.cougaar.planning.ldm.plan.Role SUPPORTSUPERIOR =
      org.cougaar.planning.ldm.plan.Role.getRole("Support" +
                                RelationshipType.SUPERIOR_SUFFIX);
    public static final org.cougaar.planning.ldm.plan.Role SUPPORTSUBORDINATE =
      org.cougaar.planning.ldm.plan.Role.getRole("Support" +
                                RelationshipType.SUBORDINATE_SUFFIX);

    public static final org.cougaar.planning.ldm.plan.Role AIRCRAFTMAINTENANCEPROVIDER =
      org.cougaar.planning.ldm.plan.Role.getRole("AircraftMaintenance" +
                                                 RelationshipType.PROVIDER_SUFFIX);
    public static final org.cougaar.planning.ldm.plan.Role AIRCRAFTMAINTENANCECUSTOMER =
      org.cougaar.planning.ldm.plan.Role.getRole("AircraftMaintenance" +
                                                 RelationshipType.CUSTOMER_SUFFIX);

    public static final org.cougaar.planning.ldm.plan.Role FIXEDWINGAIRCRAFTMAINTNENANCEPROVIDER =
      org.cougaar.planning.ldm.plan.Role.getRole("FixedWingAircraftMaintenance" +
                                                 RelationshipType.PROVIDER_SUFFIX);
    public static final org.cougaar.planning.ldm.plan.Role FIXEDWINGAIRCRAFTMAINTENANCECUSTOMER =
      org.cougaar.planning.ldm.plan.Role.getRole("FixedWingAircraftMaintenance" +
                                                 RelationshipType.CUSTOMER_SUFFIX);

    public static final org.cougaar.planning.ldm.plan.Role ROTARTYWINGAIRCRAFTMAINTNENANCEPROVIDER =
      org.cougaar.planning.ldm.plan.Role.getRole("RotaryWingAircraftMaintenance" +
                                                 RelationshipType.PROVIDER_SUFFIX);
    public static final org.cougaar.planning.ldm.plan.Role ROTARYWINGAIRCRAFTMAINTENANCECUSTOMER =
      org.cougaar.planning.ldm.plan.Role.getRole("RotaryWingAircraftMaintenance" +
                                                 RelationshipType.CUSTOMER_SUFFIX);

    public static final org.cougaar.planning.ldm.plan.Role GROUNDVEHICLEMAINTNENANCEPROVIDER =
      org.cougaar.planning.ldm.plan.Role.getRole("GroundVehicleMaintenance" +
                                                 RelationshipType.PROVIDER_SUFFIX);
    public static final org.cougaar.planning.ldm.plan.Role GROUNDVEHICLEMAINTENANCECUSTOMER =
      org.cougaar.planning.ldm.plan.Role.getRole("GroundVehicleMaintenance" +
                                                 RelationshipType.CUSTOMER_SUFFIX);

    public static final org.cougaar.planning.ldm.plan.Role WHEELEDVEHICLEMAINTNENANCEPROVIDER =
      org.cougaar.planning.ldm.plan.Role.getRole("WheeledVehicleMaintenance" +
                                                 RelationshipType.PROVIDER_SUFFIX);
    public static final org.cougaar.planning.ldm.plan.Role WHEELEDVEHICLEMAINTENANCECUSTOMER =
      org.cougaar.planning.ldm.plan.Role.getRole("WheeledVehicleMaintenance" +
                                                 RelationshipType.CUSTOMER_SUFFIX);

    public static final org.cougaar.planning.ldm.plan.Role DRYCARGOTRANSPORTPROVIDER =
      org.cougaar.planning.ldm.plan.Role.getRole("DryCargoTransport" +
                                                 RelationshipType.PROVIDER_SUFFIX);
    public static final org.cougaar.planning.ldm.plan.Role DRYCARGOTRANSPORTCUSTOMER =
      org.cougaar.planning.ldm.plan.Role.getRole("DryCargoTransport" +
                                                 RelationshipType.CUSTOMER_SUFFIX);


    public static final org.cougaar.planning.ldm.plan.Role PARTPROVIDER =
      org.cougaar.planning.ldm.plan.Role.getRole("Part" +
                                                 RelationshipType.PROVIDER_SUFFIX);
    public static final org.cougaar.planning.ldm.plan.Role PARTCUSTOMER =
      org.cougaar.planning.ldm.plan.Role.getRole("Part" +
                                                 RelationshipType.CUSTOMER_SUFFIX);

  }


  public static class MilitaryEchelon {
    public static final String UNDEFINED = "UNDEFINED";
    public static final String BATTALION = "BATTALION";
    public static final String BRIGADE = "BRIGADE";
    public static final String DIVISION = "DIVISION";
    public static final String CORPS = "CORPS";
    public static final String THEATER = "THEATER";
    public static final String USARMY = "US-ARMY";
    public static final String JOINT = "JOINT";
    
    public static final String[] ECHELON_ORDER = 
      {BATTALION, BRIGADE, DIVISION, CORPS, THEATER, USARMY, JOINT};
    public static final int MAX_ECHELON_INDEX = ECHELON_ORDER.length - 1;

    public static boolean validMilitaryEchelon(String echelon) {
      return ((echelon.equals(BATTALION)) ||
	      (echelon.equals(BRIGADE)) ||
	      (echelon.equals(DIVISION)) ||
	      (echelon.equals(CORPS)) ||
	      (echelon.equals(THEATER)) ||
	      (echelon.equals(USARMY)) ||
	      (echelon.equals(JOINT)) ||
	      (echelon.equals(UNDEFINED)));
    }

    public static String mapToMilitaryEchelon(String echelonValue) {
      // Upcase for comparison
      String upCase = echelonValue.toUpperCase();
	  
      if (validMilitaryEchelon(upCase)) {
	return upCase;
      } else {
	return UNDEFINED;
      }
    }

    public static int echelonOrder(String echelonValue) {
      // Upcase for comparison
      String upCase = echelonValue.toUpperCase();

      for (int index = 0; index < ECHELON_ORDER.length; index++) {
	if (upCase.equals(ECHELON_ORDER[index])) {
	  return index;
	}
      }
      
      return -1;
    }
  }

  
  public static URL getServiceProfileURL() {
    try {
      return new URL(Configuration.getInstallURL(), 
		     "servicediscovery/data/serviceprofiles/");
    } catch (java.net.MalformedURLException mue) {
      logger.error("Exception constructing service profile URL: " , mue);
      return null;
    }
  }
    
}










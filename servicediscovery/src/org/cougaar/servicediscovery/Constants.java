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

package org.cougaar.servicediscovery;


import org.cougaar.glm.ldm.Constants.RelationshipType;

public class Constants implements org.cougaar.planning.Constants {
  private Constants() {}

  public interface Verb extends org.cougaar.glm.ldm.Constants.Verb {
    String REQUESTFORSUPPORT = "RequestForSupport";

    org.cougaar.planning.ldm.plan.Verb RequestForSupport = org.cougaar.planning.ldm.plan.Verb.getVerb(REQUESTFORSUPPORT);
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
      org.cougaar.planning.ldm.plan.Role.create("BrigadeSupport", RelationshipType.SUPERIOR);
      org.cougaar.planning.ldm.plan.Role.create("DivisionSupport", RelationshipType.SUPERIOR);
      org.cougaar.planning.ldm.plan.Role.create("CorpsSupport", RelationshipType.SUPERIOR);
      org.cougaar.planning.ldm.plan.Role.create("TheaterSupport", RelationshipType.SUPERIOR);
      org.cougaar.planning.ldm.plan.Role.create("USArmySupport", RelationshipType.SUPERIOR);
      org.cougaar.planning.ldm.plan.Role.create("JointSupport", RelationshipType.SUPERIOR);
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

    public static final org.cougaar.planning.ldm.plan.Role BRIGADESUPPORTSUPERIOR =
      org.cougaar.planning.ldm.plan.Role.getRole("BrigadeSupport" +
                                RelationshipType.SUPERIOR_SUFFIX);
    public static final org.cougaar.planning.ldm.plan.Role BRIGADESUPPORTSUBORDINATE =
      org.cougaar.planning.ldm.plan.Role.getRole("BrigadeSupport" +
                                RelationshipType.SUBORDINATE_SUFFIX);
    public static final org.cougaar.planning.ldm.plan.Role DIVISIONSUPPORTSUPERIOR =
      org.cougaar.planning.ldm.plan.Role.getRole("DivisionSupport" +
                                RelationshipType.SUPERIOR_SUFFIX);
    public static final org.cougaar.planning.ldm.plan.Role DIVISIONSUPPORTSUBORDINATE =
      org.cougaar.planning.ldm.plan.Role.getRole("DivisionSupport" +
                                RelationshipType.SUBORDINATE_SUFFIX);

    public static final org.cougaar.planning.ldm.plan.Role CORPSSUPPORTSUPERIOR =
      org.cougaar.planning.ldm.plan.Role.getRole("CorpsSupport" +
                                RelationshipType.SUPERIOR_SUFFIX);
    public static final org.cougaar.planning.ldm.plan.Role CORPSSUPPORTSUBORDINATE =
      org.cougaar.planning.ldm.plan.Role.getRole("CorpsSupport" +
                                RelationshipType.SUBORDINATE_SUFFIX);

    public static final org.cougaar.planning.ldm.plan.Role THEATERSUPPORTSUPERIOR =
      org.cougaar.planning.ldm.plan.Role.getRole("TheaterSupport" +
                                RelationshipType.SUPERIOR_SUFFIX);
    public static final org.cougaar.planning.ldm.plan.Role THEATERSUPPORTSUBORDINATE =
      org.cougaar.planning.ldm.plan.Role.getRole("TheaterSupport" +
                                RelationshipType.SUBORDINATE_SUFFIX);

    public static final org.cougaar.planning.ldm.plan.Role USARMYSUPPORTSUPERIOR =
      org.cougaar.planning.ldm.plan.Role.getRole("USArmySupport" +
                                RelationshipType.SUPERIOR_SUFFIX);
    public static final org.cougaar.planning.ldm.plan.Role USARMYSUPPORTSUBORDINATE =
      org.cougaar.planning.ldm.plan.Role.getRole("USArmySupport" +
                                RelationshipType.SUBORDINATE_SUFFIX);

    public static final org.cougaar.planning.ldm.plan.Role JOINTSUPPORTSUPERIOR =
      org.cougaar.planning.ldm.plan.Role.getRole("JointSupport" +
                                RelationshipType.SUPERIOR_SUFFIX);
    public static final org.cougaar.planning.ldm.plan.Role JOINTSUPPORTSUBORDINATE =
      org.cougaar.planning.ldm.plan.Role.getRole("JointSupport" +
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
    public static final String BRIGADE = "BRIGADE";
    public static final String DIVISION = "DIVISION";
    public static final String CORPS = "CORPS";
    public static final String THEATER = "THEATER";
    public static final String USARMY = "US-ARMY";
    public static final String JOINT = "JOINT";
    
    private static final String[] ECHELON_ORDER = 
      {BRIGADE, DIVISION, CORPS, THEATER, USARMY, JOINT};

    public static boolean validMilitaryEchelon(String echelon) {
      return ((echelon.equals(BRIGADE)) ||
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
 
  private static Object [][]SupportRolesWithEchelon = {
    {Role.BRIGADESUPPORTSUBORDINATE, MilitaryEchelon.BRIGADE},
    {Role.DIVISIONSUPPORTSUBORDINATE, MilitaryEchelon.DIVISION},
    {Role.CORPSSUPPORTSUBORDINATE, MilitaryEchelon.CORPS},
    {Role.THEATERSUPPORTSUBORDINATE, MilitaryEchelon.THEATER},
    {Role.USARMYSUPPORTSUBORDINATE, MilitaryEchelon.USARMY},
    {Role.JOINTSUPPORTSUBORDINATE, MilitaryEchelon.JOINT}
  };


  public static String getMilitaryEchelon(org.cougaar.planning.ldm.plan.Role role) {
    for (int index = 0; index < SupportRolesWithEchelon.length; index++) {
      if (role.equals(SupportRolesWithEchelon[index][0])) {
        return (String) SupportRolesWithEchelon[index][1];
      }
    }

    return MilitaryEchelon.UNDEFINED;
  }

}










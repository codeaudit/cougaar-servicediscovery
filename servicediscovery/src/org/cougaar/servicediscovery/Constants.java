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

import org.cougaar.planning.ldm.plan.RelationshipType;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.Verb;
import org.cougaar.servicediscovery.description.LineageScheduleElement;
import org.cougaar.util.Configuration;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

import java.net.URL;

public class Constants {
  private static Logger logger = Logging.getLogger(Constants.class);

  private Constants() {}

  public interface SDScheduleElementType {

    Class LINEAGE = LineageScheduleElement.class;
  }

  public interface Verbs {
    public static final String REPORTFORDUTY = "ReportForDuty";
    public static final String REPORTFORSERVICE = "ReportForService";
    public static final String FINDPROVIDERS = "FindProviders";
    public static final String REGISTERSERVICES = "RegisterServices";

    public static final Verb ReportForDuty = Verb.get(REPORTFORDUTY);
    public static final Verb ReportForService = Verb.get(REPORTFORSERVICE);
    public static final Verb FindProviders = Verb.get(FINDPROVIDERS);
    public static final Verb RegisterServices = Verb.get(REGISTERSERVICES);
  }

  public static interface Prepositions {
    public static final String FOR_OPLAN_STAGES = "ForOplanStages";
  }

  public static class Roles {
    /**
     * Insure that Role constants are initialized. Actually does
     * nothing, but the classloader insures that all static
     * initializers have been run before executing any code in this
     * class.
     **/
    public static void init() {
    }

    static {
      Role.create("Self", "Self");
      Role.create("", RelationshipTypes.SUPERIOR);
      Role.create("Administrative", RelationshipTypes.SUPERIOR);
      Role.create("Operational", RelationshipTypes.SUPERIOR);
      Role.create("Support", RelationshipTypes.SUPERIOR);

      Role.create("AircraftMaintenance", RelationshipTypes.PROVIDER);
      Role.create("FixedWingAircraftMaintenance", RelationshipTypes.PROVIDER);
      Role.create("RotaryWingAircraftMaintenance", RelationshipTypes.PROVIDER);
      Role.create("GroundVehicleMaintenance", RelationshipTypes.PROVIDER);
      Role.create("WheeledVehicleMaintenance", RelationshipTypes.PROVIDER);
      Role.create("TrackedVehicleMaintenance", RelationshipTypes.PROVIDER);
      Role.create("DryCargoTransport", RelationshipTypes.PROVIDER);
      Role.create("Part", RelationshipTypes.PROVIDER);
    }


    public static final Role SUPERIOR = Role.getRole(RelationshipTypes.SUPERIOR_SUFFIX);
    public static final Role SUBORDINATE = Role.getRole(RelationshipTypes.SUBORDINATE_SUFFIX);
    public static final Role ADMINISTRATIVESUPERIOR = Role.getRole("Administrative" + RelationshipTypes.SUPERIOR_SUFFIX);
    public static final Role ADMINISTRATIVESUBORDINATE = Role.getRole("Administrative" + RelationshipTypes.SUBORDINATE_SUFFIX);
    public static final Role OPERATIONALSUPERIOR = Role.getRole("Operational" + RelationshipTypes.SUPERIOR_SUFFIX);
    public static final Role OPERATIONALSUBORDINATE = Role.getRole("Operational" + RelationshipTypes.SUBORDINATE_SUFFIX);
    public static final Role SUPPORTSUPERIOR = Role.getRole("Support" + RelationshipTypes.SUPERIOR_SUFFIX);
    public static final Role SUPPORTSUBORDINATE = Role.getRole("Support" + RelationshipTypes.SUBORDINATE_SUFFIX);

  }

  public interface RelationshipTypes {
    String SUPERIOR_SUFFIX = "Superior";
    String SUBORDINATE_SUFFIX = "Subordinate";
    RelationshipType SUPERIOR = RelationshipType.create(SUPERIOR_SUFFIX, SUBORDINATE_SUFFIX);

    String PROVIDER_SUFFIX = "Provider";
    String CUSTOMER_SUFFIX = "Customer";
    RelationshipType PROVIDER = RelationshipType.create(PROVIDER_SUFFIX, CUSTOMER_SUFFIX);
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
      return new URL(Configuration.getInstallURL(), "servicediscovery/data/serviceprofiles/");
    } catch (java.net.MalformedURLException mue) {
      logger.error("Exception constructing service profile URL: " , mue);
      return null;
    }
  }

}










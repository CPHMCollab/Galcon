package ais.jason;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import ais.PlayerUtils;
import ais.PlayerUtils.Location;
import ais.PlayerUtils.PlanetOwner;
import galaxy.Fleet;
import galaxy.Planet;
import galaxy.Player;

public class StrategicLocationAI extends Player {
   private static final double HAVE_ADVANTAGE_DEFENSE = 10;
   private static final double HAVE_ADVANTAGE_OFFENSE = 20;
   private static final double ARE_TIED_OFFENSE = 15;
   private static final double ARE_TIED_DEFENSE = 15;
   private static final double AT_DISADVANTAGE_DEFENSE = 20;
   private static final double AT_DISADVANTAGE_OFFENSE = 10;
   
   private static final double UNIT_ADVANTAGE_RATIO = 1.2;
   private static final double UNIT_DISADVANTAGE_RATIO = 0.8;
   private static final double PRODUCTION_ADVANTAGE_RATIO = 1.05;
   private static final double PRODUCTION_DISADVANTAGE_RATIO = 0.95;
   
   private static final double BASE_DISTANCE_FACTOR = 20;
   private static final double DISTANCE_WEIGHTING = 0.2;
   private static final double AGGRESSION = 0.7;
   
   private ArrayList<PlanetUtility> allPlanetInfo;
   private double averageStrategicValue;
   
   /** PlayMode details
    ***************************************************************
    ********************************** PRODUCTION *****************
    ******************  MORE        *  SAME        *  LESS        *
    ***************************************************************
    ********-> MORE  *  CONFIDENT   *  CONFIDENT   *  AGGRESSIVE  *
    * UNITS -> SAME  *  NORMAL      *  NORMAL      *  DESPARATE   *
    ********-> LESS  *  DEFENSIVE   *  DESPARATE   *  DESPARATE   *
    ***************************************************************
    */
   
   private enum PlayMode {
      CONFIDENT,
      AGGRESSIVE,
      NORMAL,
      DEFENSIVE,
      DESPARATE
   }

   private class PlanetUtility {
      Planet planet;
      PlanetOwner eventualOwner;
      int units;
      int extraUnits;
      int minBaseConquerCost;
      double baseStrategicValue;
      double offensiveValue;
      double defensiveValue;
   }
   
   public StrategicLocationAI() {
      super(new Color(0,100,100), "Strategic Location AI");
   }

   public StrategicLocationAI(Color c) {
      super(c, "Strategic Location AI");
   }

   public double getValue(PlanetUtility p, Location averageLocation, double variance) {
      PlanetOwner owner = PlanetOwner.getOwner(p.planet, this);
      
      double distanceFactor = (variance + BASE_DISTANCE_FACTOR) / (averageLocation.distance(p.planet) + BASE_DISTANCE_FACTOR);
      return (owner == PlanetOwner.NOBODY ? 1.0 : AGGRESSION) * Math.pow(distanceFactor, DISTANCE_WEIGHTING) / p.planet.PRODUCTION_TIME / (10 + p.units);
   }
   
   public double getDefensiveValue(Planet p, double baseValue) {
      Planet nearestOwn = PlayerUtils.getNearestOwnedPlanet(planets, p, this);
      double distToAlly = nearestOwn != null ? p.distanceTo(nearestOwn) : Integer.MAX_VALUE;

      return baseValue / distToAlly; //the nearer our own planets are the better they can be defended
   }

   public double getOffensiveValue(Planet p, double baseValue) {
      Planet nearestEnemy = PlayerUtils.getNearestEnemyPlanet(planets, p, this);
      double distToEnemy = nearestEnemy != null ? p.distanceTo(nearestEnemy) : Integer.MAX_VALUE;

      return baseValue / distToEnemy; //the nearer our enemy's planets are the faster we can strike them
   }
   
   public ArrayList<PlanetUtility> sortByBaseValue(List<PlanetUtility> planets, Location averageLocation, double variance) {
      ArrayList<PlanetUtility> rtn = new ArrayList<PlanetUtility>(planets);
      Collections.sort(rtn, (a, b) -> {
         return Double.compare(getValue(b, averageLocation, variance), getValue(a, averageLocation, variance));
      });
      return rtn;
   }

   @Override
   protected void turn() {
      List<Planet> myPlanets = PlayerUtils.getPlanetsOwnedByPlayer(planets, this);
      List<Planet> notMyPlanets = PlayerUtils.getPlanetsNotOwnedByPlayer(planets, this);
      List<Planet> enemyPlanets = PlayerUtils.getOpponentsPlanets(planets, this);
      List<Planet> unownedPlanets = PlayerUtils.getUnoccupiedPlanets(planets);
      
      int myTotalUnits = PlayerUtils.getMyUnitCount(fleets, planets, this);
      int enemyTotalUnits = PlayerUtils.getOpponentUnitCount(fleets, planets, this);
      int mySpareUnits = 0;
      double myProduction = PlayerUtils.getMyTotalProductionFrequency(planets, this);
      double enemyProduction = PlayerUtils.getMyTotalProductionFrequency(planets, this);
      double currentUnitRatio = myTotalUnits / enemyTotalUnits;
      double currentProductionRatio = myProduction / enemyProduction;
      
      Location averageLocation = Location.center(myPlanets);
      double variance = Location.variance(myPlanets);
      
      PlayMode mode;
      if(currentUnitRatio > UNIT_ADVANTAGE_RATIO) { //cover more units
         if(currentProductionRatio > PRODUCTION_DISADVANTAGE_RATIO) { //cover more/same production
            mode = PlayMode.CONFIDENT;
         }
         else { //cover less production
            mode = PlayMode.AGGRESSIVE;
         }
      } else if(currentUnitRatio < UNIT_DISADVANTAGE_RATIO) { //cover less units
         if(currentProductionRatio > PRODUCTION_ADVANTAGE_RATIO) { //cover more production
            mode = PlayMode.DEFENSIVE;
         }
         else { //cover less/equal production
            mode = PlayMode.DESPARATE;
         }
      } else { //cover same units
         if(currentProductionRatio > PRODUCTION_DISADVANTAGE_RATIO) { //cover more/same production
            mode = PlayMode.NORMAL;
         }
         else { //cover less production
            mode = PlayMode.DESPARATE;
         }
      }
      mode = PlayMode.NORMAL;
      //aggregate information about planets
      for(int i = 0; i < allPlanetInfo.size(); i++) {
         PlanetUtility temp = allPlanetInfo.get(i);
         temp.units = planets[i].getNumUnits();
         temp.eventualOwner = PlayerUtils.getCurrentEventualOwner(planets[i], fleets, this);
         //roughly estimate how many units it will take to shift the eventualOwner of a planet to us
         int incomingAlly = PlayerUtils.getPlayersIncomingFleetCount(planets[i], fleets, this);
         int incomingEnemy = PlayerUtils.getOpponentsIncomingFleetCount(planets[i], fleets, this);
         temp.minBaseConquerCost = (temp.planet.getOwner() == this ? -temp.units : temp.units) - incomingAlly + incomingEnemy + 1;
         temp.extraUnits = temp.units - PlayerUtils.getOpponentsIncomingFleetCount(temp.planet, fleets, this);
         if(temp.eventualOwner != PlanetOwner.PLAYER) {
            //go under the assumption that whoever owns it now gets the full benefit of unit production
            if(!temp.planet.isNeutral()) {
               if(temp.planet.getOwner() == this) { //minus production from cost if ours
                  temp.minBaseConquerCost -= ((planets[i].getProductionFrequency() / Fleet.SPEED) *
                        PlayerUtils.getNearestOwnedPlanet(planets, planets[i], this).distanceTo(planets[i]));
               } else { //add production to cost if enemy's
                  temp.minBaseConquerCost += (planets[i].getProductionFrequency() / Fleet.SPEED) *
                        PlayerUtils.getNearestOwnedPlanet(planets, planets[i], this).distanceTo(planets[i]) + 10;
               }
            }
         }
         temp.defensiveValue = getDefensiveValue(temp.planet, temp.baseStrategicValue);
         temp.offensiveValue = getOffensiveValue(temp.planet, temp.baseStrategicValue);
      }
      
      //defense loop
      for(PlanetUtility p: allPlanetInfo) {
         if(p.planet.ownedBy(this) && p.eventualOwner != PlanetOwner.PLAYER) {
            for(PlanetUtility other: allPlanetInfo) {
               if(other.planet.ownedBy(this) && other != p && other.minBaseConquerCost < 0) { //planet is under no threat itself and not the same one
                  //send as many units as required or as many as can be spared if not enough
                  int willSend = other.extraUnits > p.minBaseConquerCost ? p.minBaseConquerCost : other.extraUnits;
                  other.units -= willSend;
                  other.extraUnits -= willSend;
                  p.minBaseConquerCost -= willSend;
                  addAction(other.planet, p.planet, willSend);
               }
            }
         }
      }
      
      for(PlanetUtility p: allPlanetInfo) {
         if(p.minBaseConquerCost < 0 && p.units > 0) {
            mySpareUnits += p.units - PlayerUtils.getOpponentsIncomingFleetCount(p.planet, fleets, this);
         }
      }
      
      switch(mode) {
      case CONFIDENT:
         break;
      case AGGRESSIVE:
         break;
      case DEFENSIVE:
         break;
      case DESPARATE:
         break;
      case NORMAL:
         ArrayList<PlanetUtility> targets = sortByBaseValue(allPlanetInfo, averageLocation, variance);
         for(int i = 0; i < allPlanetInfo.size() && targets.size() > 0; i++) {
            PlanetUtility current = allPlanetInfo.get(i);
            if(current.planet.ownedBy(this)) {
               PlanetUtility temp  = targets.get(0);
               if(mySpareUnits > temp.minBaseConquerCost) {
                  if(current.minBaseConquerCost < 0 && current.units > 0) {
                     int willSend = current.extraUnits > temp.minBaseConquerCost ? temp.minBaseConquerCost : current.extraUnits;
                     current.units -= willSend;
                     current.extraUnits -= willSend;
                     temp.minBaseConquerCost -= willSend;
                     if(willSend == temp.minBaseConquerCost) {
                        targets.remove(0);
                     }
                     addAction(current.planet, temp.planet, willSend);
                  }
               } else {
                  targets.remove(0);
                  i--;
               }
            }
         }
         break;
      default:
         break;
      }
      
   }

   @Override
   protected void newGame() {
      allPlanetInfo = new ArrayList<PlanetUtility>();
      
      for(Planet p: planets) {
         PlanetUtility newPU = new PlanetUtility();
         newPU.planet = p;
         
         //Add distance to each planet (if this one will just add 0)
         for(Planet other: planets) {
            newPU.baseStrategicValue += p.distanceTo(other);
         }
         
         //Set base strategic values to the inverse of the total distance
         newPU.baseStrategicValue = 1000 / newPU.baseStrategicValue; 
         allPlanetInfo.add(newPU);
         averageStrategicValue += newPU.baseStrategicValue;
      }
      
      averageStrategicValue /= allPlanetInfo.size();
   }

   @Override
   protected String storeSelf() {
      return null;
   }
}

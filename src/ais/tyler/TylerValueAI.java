package ais.tyler;

import java.awt.Color;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ais.PlayerUtils;
import galaxy.Fleet;
import galaxy.Planet;
import galaxy.Player;

public class TylerValueAI extends Player {
   
   // CONSTANTS (only calculated once)
   private boolean firstTurn = true;
   private double farthestPlanetDistance = 0;
   
   // VARIABLES (update every turn)
   private int oppUnitCount = 0;
   private int myUnitCount = 0;
   private boolean winning = false;
   
   private boolean opponentMadeMove = false;
   private int turnCount = 0;

   public TylerValueAI() {
      super(new Color(50,100,0), "Tyler Value AI");
   }
   
   public TylerValueAI(Color c) {
      super(c, "Tyler Value AI");
   }
   
   @Override
   protected void turn() {
      if (firstTurn) {
         calculateConstants();
         firstTurn = false;
      }
      
      updateVariables();
      
      if (!opponentMadeMove && PlayerUtils.getOpponentsFleets(fleets, this).size() > 0) {
         opponentMadeMove = true;
      }
      
      if (opponentMadeMove || turnCount > 200) {
         valueAI();
      }
      
      turnCount++;
   }
   
   private void calculateConstants() {
      calculateFarthestPlanetDistance();
   }
   
   private void calculateFarthestPlanetDistance() {
      for (int i = 0; i < planets.length; i++) {
         Planet a = planets[i];
         for (int j = i + 1; j < planets.length; j++) {
            Planet b = planets[j];
            double dist = a.distanceTo(b);
            if (dist > farthestPlanetDistance) {
               farthestPlanetDistance = dist;
            }
         }
      }
   }
   
   private void updateVariables() {
      myUnitCount = PlayerUtils.getMyUnitCount(fleets, planets, this);
      oppUnitCount = PlayerUtils.getOpponentUnitCount(fleets, planets, this);
      
      winning = myUnitCount > oppUnitCount;
   }
   
   ///////////////////////
   //      SORTING      //
   ///////////////////////
   
   private void greedySort(List<Planet> planets, PlayerUtils.Location center) {
      Collections.sort(planets, new Comparator<Planet>() {
         @Override
         public int compare(Planet p1, Planet p2) {
            return oppPlanetValue(p2, center) - oppPlanetValue(p1, center);
         }
      });
   }
   
   private void distSort(List<Planet> planets, Planet p) {
      Collections.sort(planets, new Comparator<Planet>() {
         @Override
         public int compare(Planet p1, Planet p2) {
            return (int) (p1.distanceTo(p) - p2.distanceTo(p));
         }
      });
   }
   
   // The higher the value, the better the planet is (used for planets not owned by this player).
   private int oppPlanetValue(Planet p, PlayerUtils.Location center) {
      int value = 0;
      
      // scale the production value to 0-1 (1 being least production time).
      double productionValue = -(p.PRODUCTION_TIME - 100) / 66.0;
      
      // scale the unit value to 0-1 (1 being least # of units).
      // opponent planets can go negative on this value if they have > 50 units.
      double unitValue = -(p.getNumUnits() - 50) / 50.0;
      
      // scale the distance value to 0-1 (1 being least distance).
      double distValue = -(center.distance(p) - farthestPlanetDistance) / farthestPlanetDistance;
      
      if (winning) {
         // prefer larger planets and not care as much about unit count or range (opponent planets slightly preferred).
         value += productionValue * 5;
         value += p.isNeutral() ? 0 : 0.5;
         value += unitValue;
         value += distValue;
      } else {
         // prefer planets with small unit count and closer range (neutral planets slightly preferred).
         value += productionValue * 0.5;
         value += p.isNeutral() ? 0.5 : 0;
         value += unitValue * 5;
         value += distValue;
      }
      
      return value;
   }

   ///////////////////////
   //        AIs        //
   ///////////////////////
   
   private void valueAI() {
      List<Planet> myPlanets = PlayerUtils.getPlanetsOwnedByPlayer(planets, this);
      List<Planet> unownedPlanets = PlayerUtils.getUnoccupiedPlanets(planets);
      List<Planet> oppPlanets = PlayerUtils.getOpponentsPlanets(planets, this);
      List<Planet> otherPlanets = PlayerUtils.getPlanetsNotOwnedByPlayer(planets, this);
      
      if (myPlanets.size() == 0) {
         return;
      }
      
      /*
      Sort all planets based upon size, units on the planet, and distance from center of my planets.
      
      size 50 units 0
      size 50 units 25
      size 25 units 10
      
      
      
      
      
      
      
       */
      
      PlayerUtils.Location center = PlayerUtils.Location.center(myPlanets);
      greedySort(otherPlanets, center);
      
      int expendableUnits = totalExpendableUnits(myPlanets);
      
      for (Planet p : otherPlanets) {
         distSort(myPlanets, p);
         if (PlayerUtils.getCurrentEventualOwner(p, fleets, this) != PlayerUtils.PlanetOwner.PLAYER) {
            int myUnitsEnRoute = PlayerUtils.getPlayersIncomingFleetCount(p, fleets, this);
            int unitsNeededToCapturePlanet = unitsNeededToCapturePlanet(p);
            if (expendableUnits > unitsNeededToCapturePlanet - myUnitsEnRoute) {
               for (Planet myP : myPlanets) {
                  if (myUnitsEnRoute < unitsNeededToCapturePlanet) {
                     int unitsToSend = Math.min(unitsNeededToCapturePlanet, expendableUnits(myP));
                     addAction(myP, p, unitsToSend);
                     expendableUnits -= unitsToSend;
                     myUnitsEnRoute += unitsToSend;
                  }
               }
            }
         }
      }
      
      
      
      
      
      
      
      
      
      
      
      
      
      
//      
//      greedySort(oppPlanets);
//      greedySort(otherPlanets);
//      
//      int expendableUnits = totalExpendableUnits(myPlanets);
//      
//      for (Planet p : oppPlanets) {
//         distSort(myPlanets, p);
//         if (PlayerUtils.getCurrentEventualOwner(p, fleets, this) != PlayerUtils.PlanetOwner.PLAYER) {
//            int myUnitsEnRoute = PlayerUtils.getPlayersIncomingFleetCount(p, fleets, this);
//            int unitsNeededToCapturePlanet = unitsNeededToCapturePlanet(p);
//            if (expendableUnits > unitsNeededToCapturePlanet - myUnitsEnRoute) {
//               for (Planet myP : myPlanets) {
//                  if (myUnitsEnRoute < unitsNeededToCapturePlanet) {
//                     int unitsToSend = Math.min(unitsNeededToCapturePlanet, expendableUnits(myP));
//                     addAction(myP, p, unitsToSend);
//                     expendableUnits -= unitsToSend;
//                     myUnitsEnRoute += unitsToSend;
//                  }
//               }
//            }
//         }
//      }
//      
//      // If we still have some extra units that haven't been sent to the opponent, 
//      // try to capture neutral planets (only ones with small unit count).
//      if (expendableUnits > 0) {
//         for (Planet p : otherPlanets) {
//            if (p.getNumUnits() < expendableUnits / 4) {
//               int myUnitsEnRoute = PlayerUtils.getPlayersIncomingFleetCount(p, fleets, this);
//               int unitsNeededToCapturePlanet = unitsNeededToCapturePlanet(p);
//               if (myUnitsEnRoute < unitsNeededToCapturePlanet) {
//                  for (Planet myP : myPlanets) {
//                     if (myUnitsEnRoute < unitsNeededToCapturePlanet) {
//                        int unitsToSend = Math.min(unitsNeededToCapturePlanet, expendableUnits(myP));
//                        addAction(myP, p, unitsToSend);
//                        expendableUnits -= unitsToSend;
//                        myUnitsEnRoute += unitsToSend;
//                     }
//                  }
//               }
//            }
//         }
//      }
   }

   ///////////////////////
   //     UTILITES      //
   ///////////////////////
   
   private int totalExpendableUnits(List<Planet> myPlanets) {
      int sum = 0;
      for (Planet p : myPlanets) {
         sum += expendableUnits(p);
      }
      return sum;
   }
   
   private int expendableUnits(Planet p) {
      return Math.max(0, p.getNumUnits() - p.RADIUS / 5);
   }
   
   // TODO: optimize this method
   private int unitsNeededToCapturePlanet(Planet p) {
      int myUnits = PlayerUtils.getPlayersIncomingFleetCount(p, fleets, this);
      int oppUnits = PlayerUtils.getOpponentsIncomingFleetCount(p, fleets, this);
      
      int unitsGeneratedByPlanet = (int)(distOfFarthestFleet(PlayerUtils.getMyFleets(fleets, this), p)) % p.PRODUCTION_TIME + 2;
      
      if (p.isNeutral()) {
         return (oppUnits + p.getNumUnits()) - myUnits + 15;
      } else {
         return (oppUnits + p.getNumUnits() + unitsGeneratedByPlanet) - myUnits + 15;
      }
   }
   
   private double distOfFarthestFleet(List<Fleet> fleets, Planet p) {
      double maxDist = 0;
      for (Fleet f : fleets) {
         if (f.getDestination().equals(p)) {
            double dist = f.distanceLeft();
            if (dist > maxDist) {
               maxDist = dist;
            }
         }
      }
      return maxDist;
   }
   
   @Override
   protected void newGame() {}
   
   @Override
   protected String storeSelf() { return null; }
}

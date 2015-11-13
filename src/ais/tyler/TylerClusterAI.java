package ais.tyler;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import ais.PlayerUtils;
import galaxy.Fleet;
import galaxy.Planet;
import galaxy.Player;

public class TylerClusterAI extends Player {
   
   private static final int NEAR_PLANET_COUNT = 3;
   
   // CONSTANTS (only calculated once)
   private boolean firstTurn = true;
   private double farthestPlanetDistance = 0;
   
   // VARIABLES (update every turn)
   private int oppUnitCount = 0;
   private int myUnitCount = 0;
   private boolean winning = false;
   
   private boolean opponentMadeMove = false;
   private int turnCount = 0;

   public TylerClusterAI() {
      super(new Color(50,100,0), "Cluster AI");
   }
   
   public TylerClusterAI(Color c) {
      super(c, "Cluster AI");
   }
   
   @Override
   protected void turn() {
      if (firstTurn) {
         calculateConstants();
         firstTurn = false;
      }
      
      updateVariables();
      
      clusterAI();
      
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
      myUnitCount = Player.numUnitsOwnedBy(this);
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
   
   private void sizeSort(List<Planet> planets) {
      Collections.sort(planets, new Comparator<Planet>() {
         @Override
         public int compare(Planet p1, Planet p2) {
            return (int) (p1.PRODUCTION_TIME - p2.PRODUCTION_TIME);
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
   
   private void clusterAI() {
      List<Planet> allPlanets = Arrays.asList(planets);
      List<Planet> myPlanets = PlayerUtils.getPlanetsOwnedByPlayer(planets, this);
      List<Planet> unownedPlanets = PlayerUtils.getUnoccupiedPlanets(planets);
      List<Planet> oppPlanets = PlayerUtils.getOpponentsPlanets(planets, this);
      List<Planet> otherPlanets = PlayerUtils.getPlanetsNotOwnedByPlayer(planets, this);
      
      if (myPlanets.size() == 0) {
         return;
      }
      
      /*
      

      for each of my planets,
         get the nearest 5 planets
         for each of the 5 planets
            get the number of units I already sent there.
            get the number of units the enemy sent there.
            if I have enough units to capture it from my current planet, send them.
               send units











      
      
      
      
      
      
      
       */
      HashMap<Planet, Integer> myUnitsSent = new HashMap<>();
      for (Planet p : planets) {
         myUnitsSent.put(p, 0);
      }
      
      boolean firstPlanet = true;
      for (Planet myPlanet : myPlanets) {
         distSort(allPlanets, myPlanet);
         int expendableUnits = getExpendableUnits(myPlanet);
         
         List<Planet> nearPlanets;
         if (firstPlanet) {
            firstPlanet = false;
            nearPlanets = allPlanets.subList(1, allPlanets.size());
         } else {
            // Get the planets closest this planet.
            nearPlanets = allPlanets.subList(1, Math.min(NEAR_PLANET_COUNT + 1, allPlanets.size()));
            int myPlanetCount = 0;
            for (int i = 0; i < nearPlanets.size(); i++) {
               if (allPlanets.get(i).ownedBy(this)) {
                  myPlanetCount++;
               }
            }
            
            if (myPlanetCount == NEAR_PLANET_COUNT) {
               if (myPlanet.getNumUnits() > 10) {
                  addAction(myPlanet, nearPlanets.get((int)(Math.random() * (nearPlanets.size() - 1))), 1);
               }
            }
         }
         
         for (int i = 0; i < nearPlanets.size(); i++) {
            Planet p = allPlanets.get(i);
            if (!p.ownedBy(this)) {
            int enemyIncomingUnits = PlayerUtils.getOpponentsIncomingFleetCount(p, fleets, this);
            int myIncomingUnits = PlayerUtils.getOpponentsIncomingFleetCount(p, fleets, this);
            int myUnitsAlreadySent = myUnitsSent.get(p);
            int unitsAtPlanet = (int)(p.isNeutral() ? p.getNumUnits() : p.getNumUnits() + p.PRODUCTION_TIME / myPlanet.distanceTo(p) + 1);
            if (p.isNeutral()) {
               int unitsNeededToCapturePlanet = unitsAtPlanet - (myIncomingUnits + myUnitsAlreadySent) + 10;
               unitsNeededToCapturePlanet += Math.max(0, enemyIncomingUnits - (myIncomingUnits + myUnitsAlreadySent));
               if (unitsNeededToCapturePlanet > 0 && expendableUnits > unitsNeededToCapturePlanet) {
                  if (unitsNeededToCapturePlanet < myUnitCount / 3) {
                  addAction(myPlanet, p, unitsNeededToCapturePlanet);
                  expendableUnits -= unitsNeededToCapturePlanet;
                  myUnitsSent.put(p, unitsNeededToCapturePlanet + myUnitsAlreadySent);
                  }
               }
            } else {
               int unitsNeededToCapturePlanet = (unitsAtPlanet + enemyIncomingUnits) - (myIncomingUnits + myUnitsAlreadySent) + 10;
               if (unitsNeededToCapturePlanet > 0 && expendableUnits > unitsNeededToCapturePlanet) {
                  if (unitsNeededToCapturePlanet < myUnitCount / 3) {
                  addAction(myPlanet, p, unitsNeededToCapturePlanet);
                  expendableUnits -= unitsNeededToCapturePlanet;
                  myUnitsSent.put(p, unitsNeededToCapturePlanet + myUnitsAlreadySent);
                  }
               }
            }
            }
         }
      }
      
      
      
      
      
      
      
      
      
      
      
      
      
      
      
      
      
      
//      PlayerUtils.Location center = PlayerUtils.Location.center(myPlanets);
//      greedySort(otherPlanets, center);
//      
//      int expendableUnits = totalExpendableUnits(myPlanets);

      
      
      
      
      
      
      
      
      
      
      
      
      
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
         sum += getExpendableUnits(p);
      }
      return sum;
   }
   
   private int getExpendableUnits(Planet p) {
      return Math.min(Math.max(0, p.getNumUnits() - p.RADIUS / 2), myUnitCount);
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

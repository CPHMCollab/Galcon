package ais.tyler;

import java.awt.Color;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ais.PlayerUtils;
import galaxy.Fleet;
import galaxy.Planet;
import galaxy.Player;

public class TylerAI extends Player {
   
   private static final int MAX_UNIT_SPREAD_TO_DETECT_STALEMATE = 10;
   
   private boolean opponentMadeMove = false;
   private boolean stalemate = false;
   private int turnCount = 0;
   private int maxUnits = 0;
   private int minUnits = 1000;
   private int stalemateTimer = 0;

   public TylerAI() {
      super(new Color(50,100,0), "Tyler AI");
   }
   
   public TylerAI(Color c) {
      super(c, "Tyler AI");
   }
   
   @Override
   protected void turn() {
      if (PlayerUtils.getOpponentsFleets(fleets, this).size() > 0) {
         opponentMadeMove = true;
      }
      
      int myUnits = PlayerUtils.getMyUnitCount(fleets, planets, this);
      if (myUnits > maxUnits) {
         maxUnits = myUnits;
         if (maxUnits - minUnits > MAX_UNIT_SPREAD_TO_DETECT_STALEMATE) {
            minUnits = maxUnits - MAX_UNIT_SPREAD_TO_DETECT_STALEMATE;
            stalemateTimer = 0;
            stalemate = false;
         }
      }
      if (myUnits < minUnits) {
         minUnits = myUnits;
         if (maxUnits - minUnits > MAX_UNIT_SPREAD_TO_DETECT_STALEMATE) {
            maxUnits = minUnits + MAX_UNIT_SPREAD_TO_DETECT_STALEMATE;
            stalemateTimer = 0;
            stalemate = false;
         }
      }
      
      if (stalemateTimer > 500) {
         stalemate = false;
         stalemateTimer = 0;
      } else if (stalemateTimer > 200) {
         stalemate = true;
      }
      
      if ((opponentMadeMove || turnCount > 200) && !stalemate) {
         firstAI();
      }
      
      turnCount++;
      stalemateTimer++;
   }
   
   ///////////////////////
   //      SORTING      //
   ///////////////////////
   
   private void greedySort(List<Planet> planets) {
      Collections.sort(planets, new Comparator<Planet>() {
         @Override
         public int compare(Planet p1, Planet p2) {
            return planetValue(p2) - planetValue(p1);
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
   
   // The higher the value, the better the planet is
   private int planetValue(Planet p) {
      //return -p.getNumUnits();
      
      int value1 = (int) (p.PRODUCTION_TIME + p.getNumUnits() * 5);// + p.distanceTo(p1) / 5);
      value1 += p.getColor().equals(Color.GRAY) ? 0 : 2;
      return -value1;
   }

   ///////////////////////
   //        AIs        //
   ///////////////////////
   
   private void firstAI() {
      List<Planet> myPlanets = PlayerUtils.getPlanetsOwnedByPlayer(planets, this);
      List<Planet> otherPlanets = PlayerUtils.getUnoccupiedPlanets(planets);
      List<Planet> oppPlanets = PlayerUtils.getOpponentsPlanets(planets, this);
      
      greedySort(oppPlanets);
      greedySort(otherPlanets);
      
      int expendableUnits = totalExpendableUnits(myPlanets);
      
      for (Planet p : oppPlanets) {
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
      
      // If we still have some extra units that haven't been sent to the opponent, 
      // try to capture neutral planets (only ones with small unit count).
      if (expendableUnits > 0) {
         for (Planet p : otherPlanets) {
            if (p.getNumUnits() < expendableUnits / 4) {
               int myUnitsEnRoute = PlayerUtils.getPlayersIncomingFleetCount(p, fleets, this);
               int unitsNeededToCapturePlanet = unitsNeededToCapturePlanet(p);
               if (myUnitsEnRoute < unitsNeededToCapturePlanet) {
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
      }
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
         return (oppUnits + p.getNumUnits()) - myUnits + 1;
      } else {
         return (oppUnits + p.getNumUnits() + unitsGeneratedByPlanet) - myUnits + 1;
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

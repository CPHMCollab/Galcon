package ais.jono;

import galaxy.Fleet;
import galaxy.Planet;
import galaxy.Player;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import ais.PlayerUtils;
import ais.PlayerUtils.Location;
import ais.PlayerUtils.PlanetOwner;

public class ContestInfluenceAI extends Player {
   private static final boolean USE_MOVE_FORWARDS = false;
   private static final int MIN_AGGRESSIVE_DEFENSE = 10;
   private static final int MIN_DEFENSIVE_DEFENSE = 1;
   private static final double AGGRESSION = 2.0;
   private static final double BASE_DISTANCE_FACTOR = 50;
   private static final double DISTANCE_WEIGHTING = 0.2;
   private static final double UNIT_COUNT_POSITION_WEIGHT = 0.8;
   private static final double UNIT_GEN_POSITION_WEIGHT = 0.2;
   private static final double CAPTURE_SAFTEY_MARGIN = 1.02;
   
   Planet take;
   List<Planet> retake;
   private boolean contest;
   private Set<Planet> mine;
   
   public ContestInfluenceAI() {
      this(Color.ORANGE);
   }
   
   public ContestInfluenceAI(Color c) {
      super(c, "Contest Influence AI");
   }
   
   @Override
   protected void turn() {
      List<Planet> myPlanets = PlayerUtils.getPlanetsOwnedByPlayer(planets, this);
      for (Planet p : myPlanets) {
         if (PlayerUtils.getCurrentEventualOwner(p, fleets, this) == PlayerUtils.PlanetOwner.PLAYER) {
            mine.add(p);
         }
      }
      if (myPlanets.size() == 0) {
         return;
      }
      
      boolean defending = false;
      Planet target = null;
      int needed = 0;
      for (Planet p : mine) {
         if (PlayerUtils.getCurrentEventualOwner(p, fleets, this) != PlayerUtils.PlanetOwner.PLAYER) {
            needed =
                  PlayerUtils.getOpponentsIncomingFleetCount(p, fleets, this) -
                  p.getNumUnits() -
                  PlayerUtils.getPlayersIncomingFleetCount(p, fleets, this) +
                  MIN_DEFENSIVE_DEFENSE;
            needed = Math.max(needed, 4);
            target = p;
            defending = true;
            break;
         }
      }
      
      if (defending) {
         int available = 0;
         if (target != null) {
            final Planet finalTarget = target;
            for (Planet p : myPlanets.stream().sorted((Planet a, Planet b) -> Double.compare(new Location(a).distance(finalTarget), new Location(b).distance(finalTarget))).collect(Collectors.toList())) {
               if (p != target) {
                  int contribution = p.getNumUnits() - PlayerUtils.getIncomingFleetCount(p, fleets) - (defending ? MIN_DEFENSIVE_DEFENSE : MIN_AGGRESSIVE_DEFENSE);
                  
                  if (available + contribution > needed) {
                     addAction(p, target, needed - available);
                     available += contribution;
                     break;
                  }
                  available += contribution;
                  addAction(p, target, contribution);
               }
            }
         }
      } else {
         if (contest) {
            contest();
         } else {
            evaluatePosition();
            if (take == null) {
               if (USE_MOVE_FORWARDS) {
                  moveFleetsForwards();
               }
            } else {
               mine.add(take);
            }
         }
      }
   }
   
   public void moveFleetsForwards() {
      List<Planet> theirPlanets = PlayerUtils.getOpponentsPlanets(planets, this);
      if (theirPlanets.size() > 0) {
         Location theirUnitArea = Location.getUnitCountWeightedCenter(theirPlanets);
         Location theirProductionArea = Location.getProductionWeightedCenter(theirPlanets);
         theirUnitArea = theirUnitArea.multiply(UNIT_COUNT_POSITION_WEIGHT);
         theirProductionArea = theirProductionArea.multiply(UNIT_GEN_POSITION_WEIGHT);
         Location theirLocation = theirUnitArea.sum(theirProductionArea);
         Planet target = mine.stream().min((a, b) -> Double.compare(theirLocation.distance(a), theirLocation.distance(b))).get();
         for (Planet p : PlayerUtils.getPlanetsOwnedByPlayer(planets, this)) {
            int toSend = p.getNumUnits() - MIN_AGGRESSIVE_DEFENSE;
            if (toSend > 0) {
               addAction(p, target, toSend);
            }
         }
      }
   }
   
   public double getValue(Planet p, Location averageLocation, double variance) {
      double distanceFactor = (variance + BASE_DISTANCE_FACTOR) / (averageLocation.distance(p) + BASE_DISTANCE_FACTOR);
      return (p.getColor().equals(Color.GRAY) ? 1.0 : AGGRESSION) * Math.pow(distanceFactor, DISTANCE_WEIGHTING) / p.PRODUCTION_TIME / (10 + p.getNumUnits());
   }
   
   private void contest() {
      List<Planet> myPlanets = PlayerUtils.getPlanetsOwnedByPlayer(planets, this);
      List<Planet> theirPlanets = PlayerUtils.getOpponentsPlanets(planets, this);
      
      if (myPlanets.size() > 1 || theirPlanets.size() > 1) {
         contest = false;
         return;
      }
      
      if (take != null) {
         int toSendToTake = 0;
         while (!isEventualOwner(take, (int) Math.ceil(myPlanets.get(0).distanceTo(take) / Fleet.SPEED), toSendToTake)) {
            toSendToTake++;
         }
         if (toSendToTake > 0) {
            addAction(myPlanets.get(0), take, toSendToTake);
         }
      }
      
      for (Fleet fleet : PlayerUtils.getOpponentsFleets(fleets, this)) {
         if (retake.contains(fleet.getDestination())) {
            int distance = (int) Math.ceil(myPlanets.get(0).distanceTo(fleet.getDestination()) / Fleet.SPEED);
            int fleetDistance = (int) Math.ceil(fleet.distanceLeft()/Fleet.SPEED);
            if (distance > fleetDistance) {
               int toSend = 0;
               while (!isEventualOwner(fleet.getDestination(), distance, toSend)) toSend++;
               if (toSend > 0) {
                  addAction(myPlanets.get(0), fleet.getDestination(), toSend);
               }
               retake.remove(fleet.getDestination());
               mine.add(fleet.getDestination());
            }
         }
      }
   }

   private static class PlanetAction {
      public int time;
      public int amount;
      public PlanetOwner owner;
   }
   
   private boolean isEventualOwner(Planet p, int time, int amount) {
      PlanetOwner current;
      if (p.ownedBy(this)) {
         current = PlanetOwner.PLAYER;
      } else if (p.ownedByOpponentOf(this)) {
         current = PlanetOwner.OPPONENT;
      } else {
         current = PlanetOwner.NOBODY;
      }
      int updateCount = p.getUpdateCount() % p.PRODUCTION_TIME;
      int previousUnits = 0;
      int unitCount = p.getNumUnits();
      int currentTime = 0;
      List<PlanetAction> actions = new ArrayList<>();
      for (Fleet f : Arrays.asList(fleets).stream()
            .filter((fleet) -> fleet.getDestination() == p)
            .collect(Collectors.toList())) {
               PlanetAction action = new PlanetAction();
               action.time = (int) Math.ceil(f.distanceLeft()/Fleet.SPEED);
               action.amount = f.getNumUnits();
               if (f.ownedBy(this)) {
                  action.owner = PlanetOwner.PLAYER;
               } else {
                  action.owner = PlanetOwner.OPPONENT;
               }
               actions.add(action);
            }
      PlanetAction player = new PlanetAction();
      player.amount = amount;
      player.time = time;
      player.owner = PlanetOwner.PLAYER;
      actions.add(player);
      actions.sort((a, b) -> Integer.compare(a.time, b.time));
      for (PlanetAction pa : actions) {
         int passingTime = pa.time - currentTime;
         if (current != PlanetOwner.NOBODY) {
            updateCount += passingTime;
            int unitsToAdd = (updateCount + p.PRODUCTION_TIME - 1) / p.PRODUCTION_TIME - previousUnits;
            previousUnits += unitsToAdd;
            unitCount += unitsToAdd;
         }
         if (pa.owner == current) {
            unitCount += pa.amount;
         } else {
            unitCount -= pa.amount;
            if (unitCount == 0) {
               current = PlanetOwner.NOBODY;
            }
            if (unitCount < 0) {
               unitCount = -unitCount;
               current = pa.owner;
            }
         }
         currentTime += passingTime;
      }
      return current == PlanetOwner.PLAYER;
   }
   
   @Override
   protected void newGame() {
      mine = new HashSet<>();
      contest = true;
      
      List<Planet> myPlanets = PlayerUtils.getPlanetsOwnedByPlayer(planets, this);
      List<Planet> theirPlanets = PlayerUtils.getOpponentsPlanets(planets, this);
      List<Planet> unownedPlanets = PlayerUtils.getUnoccupiedPlanets(planets);
      
      if (myPlanets.size() != 1 || theirPlanets.size() != 1) {
         throw new RuntimeException("Unexpected starting situation MyPlanets: " + myPlanets.size() + " TheirPlanets: " + theirPlanets.size());
      }
      
      Planet me = myPlanets.get(0);
      Planet them = theirPlanets.get(0);
      
      int distance = (int) Math.ceil(me.distanceTo(them) / Fleet.SPEED);
      int distanceProduction = distance / me.PRODUCTION_TIME;
      
      Planet best = null;
      double bestValue = Double.MIN_VALUE;
      
      for (Planet p : unownedPlanets) {
         int toMe = (int) Math.ceil(p.distanceTo(me) / Fleet.SPEED);
         int toThem = (int) Math.ceil(p.distanceTo(them) / Fleet.SPEED);
         if (toMe <= toThem) {
            int takenContribution = 0;
            if (distance - toMe * 2 > 0) {
               takenContribution = (int) Math.floor((distance - toMe * 2) / p.PRODUCTION_TIME);
            }
            if (p.getNumUnits() + 1 - takenContribution < distanceProduction) {
               double value = 1.0 / p.PRODUCTION_TIME / (100 + p.getNumUnits());
               if (value > bestValue) {
                  bestValue = value;
                  best = p;
               }
            }
         }
      }
      take = best;
      
      retake = new ArrayList<>(unownedPlanets);
      
      for (Planet p : unownedPlanets) {
         int toMe = (int) Math.ceil(p.distanceTo(me) / Fleet.SPEED);
         int toThem = (int) Math.ceil(p.distanceTo(them) / Fleet.SPEED);
         if (toMe >= toThem) {
            int takenContribution = 0;
            if (distance - toThem * 2 > 0) {
               takenContribution = (int) Math.floor((distance - toThem * 2) / p.PRODUCTION_TIME);
            }
            if (p.getNumUnits() * CAPTURE_SAFTEY_MARGIN + 1 - takenContribution < distanceProduction) {
               retake.remove(p);
            }
         }
      }
   }
   
   private void evaluatePosition() {
      List<Planet> myPlanets = PlayerUtils.getPlanetsOwnedByPlayer(planets, this);
      List<Planet> theirPlanets = PlayerUtils.getOpponentsPlanets(planets, this);
      //List<Planet> unownedPlanets = PlayerUtils.getUnoccupiedPlanets(planets);
      List<Fleet> myFleets = PlayerUtils.getMyFleets(fleets, this);
      List<Fleet> theirFleets = PlayerUtils.getOpponentsFleets(fleets, this);
      
      /* if both are true may turn on aggro mode
      int myUnits = PlayerUtils.getMyUnitCount(fleets, planets, this);
      int theirUnits = PlayerUtils.getOpponentUnitCount(fleets, planets, this);
      boolean unitAdvantage = myUnits > theirUnits * ADVANTAGE_THRESHOLD;
      
      double myProduction = myPlanets.stream().collect(Collectors.summingDouble(p -> p.getProductionFrequency()));
      double theirProduction = theirPlanets.stream().collect(Collectors.summingDouble(p -> p.getProductionFrequency()));
      boolean productionAdvantage = myProduction > theirProduction * ADVANTAGE_THRESHOLD;
      */
      
      Location myUnitArea = Location.getUnitCountWeightedCenter(myPlanets);
      Location myProductionArea = Location.getProductionWeightedCenter(myPlanets);
      myUnitArea = myUnitArea.multiply(UNIT_COUNT_POSITION_WEIGHT);
      myProductionArea = myProductionArea.multiply(UNIT_GEN_POSITION_WEIGHT);
      Location myLocation = myUnitArea.sum(myProductionArea);
      double mySpread = Location.variance(myPlanets);
      
      Location theirLocation;
      if (theirPlanets.size() > 0) {
         Location theirUnitArea = Location.getUnitCountWeightedCenter(theirPlanets);
         Location theirProductionArea = Location.getProductionWeightedCenter(theirPlanets);
         theirUnitArea = theirUnitArea.multiply(UNIT_COUNT_POSITION_WEIGHT);
         theirProductionArea = theirProductionArea.multiply(UNIT_GEN_POSITION_WEIGHT);
         theirLocation = theirUnitArea.sum(theirProductionArea);
      } else {
         theirLocation = myLocation;
      }
      
      //double theirSpread = Location.variance(theirPlanets);
      
      Map<Planet, Double> myInfluence = new HashMap<>();
      for (Planet influencing : myPlanets) {
         for (Planet p : planets) {
            if (influencing == p) {
               addMap(myInfluence, p,  Double.valueOf(p.getNumUnits()));
            } else {
               double influence = influencing.getNumUnits() * 0.5 * (new Location(influencing).distance(p) + BASE_DISTANCE_FACTOR) / (theirLocation.distance(p) + BASE_DISTANCE_FACTOR);
               addMap(myInfluence, p, influence);
            }
         }
      }
      
      for (Fleet f : myFleets) {
         addMap(myInfluence, f.getDestination(), f.getNumUnits());
      }
      
      Map<Planet, Double> theirInfluence = new HashMap<>();
      for (Planet influencing : theirPlanets) {
         for (Planet p : planets) {
            if (influencing == p) {
               addMap(theirInfluence, p, Double.valueOf(p.getNumUnits()));
            } else {
               double influence = influencing.getNumUnits() * Math.pow(0.5 * (new Location(influencing).distance(p) + BASE_DISTANCE_FACTOR) / (myLocation.distance(p) + BASE_DISTANCE_FACTOR), DISTANCE_WEIGHTING);
               addMap(theirInfluence, p, influence);
            }
         }
      }
      
      for (Fleet f : theirFleets) {
         addMap(theirInfluence, f.getDestination(), f.getNumUnits());
      }
      
      List<Planet> potentialTargets = new ArrayList<>();
      for (Planet p : planets) {
         if (myInfluence.get(p) - (p.isNeutral() ? p.getNumUnits() : 0) > CAPTURE_SAFTEY_MARGIN * (theirInfluence.containsKey(p) ? theirInfluence.get(p) : 0)) {
            potentialTargets.add(p);
         }
      }
      
      take = null;
      double bestValue = 0;
      for (Planet p : potentialTargets) {
         if (!mine.contains(p)) {
            double value = getValue(p, myLocation, mySpread);
            if (value > bestValue) {
               bestValue = value;
               take = p;
            }
         }
      }
   }
   
   private static void addMap(Map<Planet, Double> map, Planet p, double val) {
      map.put(p, val + (map.containsKey(p) ?  map.get(p) : 0));
   }

   @Override
   protected String storeSelf() {
      return null;
   }
}

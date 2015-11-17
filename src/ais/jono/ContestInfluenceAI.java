package ais.jono;

import galaxy.Fleet;
import galaxy.Planet;
import galaxy.Player;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import ais.PlayerUtils;
import ais.PlayerUtils.Location;
import ais.PlayerUtils.PlanetOwner;

public class ContestInfluenceAI extends Player {
   
   private List<CIConstants> currentGeneration = new ArrayList<>();
   
   private static final int POOL_SIZE = 6;
   private static final int GAMES_PER_SAMPLE = 40;
   private int gameCount = 0;
   private int position = 0;
   
   public static class CIConstants {
      private final boolean USE_MOVE_FORWARDS = false;
      private final int MIN_AGGRESSIVE_DEFENSE; //this does nothing
      private final int MIN_DEFENSIVE_DEFENSE;
      private final double AGGRESSION;
      private final double BASE_DISTANCE_FACTOR;
      private final double DISTANCE_WEIGHTING;
      private final double UNIT_COUNT_POSITION_WEIGHT;
      private final double UNIT_GEN_POSITION_WEIGHT;
      private final double CAPTURE_SAFTEY_MARGIN;
      private int winCount = 0;
      
      public CIConstants() {
         /*
         MIN_AGGRESSIVE_DEFENSE = 10;
         MIN_DEFENSIVE_DEFENSE = 1;
         AGGRESSION = 2.0;
         BASE_DISTANCE_FACTOR = 50;
         DISTANCE_WEIGHTING = 0.2;
         UNIT_COUNT_POSITION_WEIGHT = 0.8;
         UNIT_GEN_POSITION_WEIGHT = 0.2;
         CAPTURE_SAFTEY_MARGIN = 1.02;
         */
         
         //for distance value defender
         MIN_AGGRESSIVE_DEFENSE = 69;
         MIN_DEFENSIVE_DEFENSE = 1;
         AGGRESSION = 2.3146709908825276;
         BASE_DISTANCE_FACTOR = 406.3556802091704;
         DISTANCE_WEIGHTING = 0.10419349293965774;
         UNIT_COUNT_POSITION_WEIGHT = 0.8323919872011701;
         UNIT_GEN_POSITION_WEIGHT = 0.9137373218479841;
         CAPTURE_SAFTEY_MARGIN = 1.3037189962512432;
      }
      
      public void printSettings() {
         System.out.println("MIN_AGGRESSIVE_DEFENSE = " + MIN_AGGRESSIVE_DEFENSE + ";");
         System.out.println("MIN_DEFENSIVE_DEFENSE = " + MIN_DEFENSIVE_DEFENSE + ";");
         System.out.println("AGGRESSION = " + AGGRESSION + ";");
         System.out.println("BASE_DISTANCE_FACTOR = " + BASE_DISTANCE_FACTOR + ";");
         System.out.println("DISTANCE_WEIGHTING = " + DISTANCE_WEIGHTING + ";");
         System.out.println("UNIT_COUNT_POSITION_WEIGHT = " + UNIT_COUNT_POSITION_WEIGHT + ";");
         System.out.println("UNIT_GEN_POSITION_WEIGHT = " + UNIT_GEN_POSITION_WEIGHT + ";");
         System.out.println("CAPTURE_SAFTEY_MARGIN = " + CAPTURE_SAFTEY_MARGIN + ";");
      }

      public CIConstants(int mad, int mdd, double ag, double bdf, double dw, double ucpr, double ugpr, double csm) {
         MIN_AGGRESSIVE_DEFENSE = mad;
         MIN_DEFENSIVE_DEFENSE = mdd;
         AGGRESSION = ag;
         BASE_DISTANCE_FACTOR = bdf;
         DISTANCE_WEIGHTING = dw;
         UNIT_COUNT_POSITION_WEIGHT = ucpr;
         UNIT_GEN_POSITION_WEIGHT = ugpr;
         CAPTURE_SAFTEY_MARGIN = csm;
      }
      
      public CIConstants(CIConstants a, CIConstants b) {
         Random rnd = new Random();
         MIN_AGGRESSIVE_DEFENSE     = getCombination(rnd.nextInt(3), a.MIN_AGGRESSIVE_DEFENSE, b.MIN_AGGRESSIVE_DEFENSE);
         MIN_DEFENSIVE_DEFENSE      = getCombination(rnd.nextInt(3), a.MIN_DEFENSIVE_DEFENSE, b.MIN_DEFENSIVE_DEFENSE);
         AGGRESSION                 = getCombination(rnd.nextInt(3), a.AGGRESSION, b.AGGRESSION);
         BASE_DISTANCE_FACTOR       = getCombination(rnd.nextInt(3), a.BASE_DISTANCE_FACTOR, b.BASE_DISTANCE_FACTOR);
         DISTANCE_WEIGHTING         = getCombination(rnd.nextInt(3), a.DISTANCE_WEIGHTING, b.DISTANCE_WEIGHTING);
         UNIT_COUNT_POSITION_WEIGHT = getCombination(rnd.nextInt(3), a.UNIT_COUNT_POSITION_WEIGHT, b.UNIT_COUNT_POSITION_WEIGHT);
         UNIT_GEN_POSITION_WEIGHT   = getCombination(rnd.nextInt(3), a.UNIT_GEN_POSITION_WEIGHT, b.UNIT_GEN_POSITION_WEIGHT);
         CAPTURE_SAFTEY_MARGIN      = getCombination(rnd.nextInt(3), a.CAPTURE_SAFTEY_MARGIN, b.CAPTURE_SAFTEY_MARGIN);
      }
      
      private static double getCombination(int mode, double a, double b) {
         switch (mode) {
         case 0:
            return a;
         case 1:
            return b;
         case 2:
            return (a + b) / 2;
         default:
            throw new RuntimeException("Unexpected mode");
         }
      }
      
      private static int getCombination(int mode, int a, int b) {
         switch (mode) {
         case 0:
            return a;
         case 1:
            return b;
         case 2:
            return (a + b) / 2;
         default:
            throw new RuntimeException("Unexpected mode");
         }
      }
      
      public static CIConstants generateRandomConstants() {
         Random rnd = new Random();
         return new CIConstants(rnd.nextInt(100), rnd.nextInt(100), rnd.nextDouble() * 3, 
               rnd.nextDouble() * 1000, rnd.nextDouble() * 2, rnd.nextDouble(), 
               rnd.nextDouble(), rnd.nextDouble() + 0.5);
      }
   }
   
   private int turn = 0;
   
   Planet take;
   List<Planet> retake;
   private boolean contest;
   private Set<Planet> mine;
   private CIConstants constants = new CIConstants();
   private final boolean LEARNING;
   
   public ContestInfluenceAI(boolean learning) {
      this(Color.ORANGE, learning);
   }
   
   public ContestInfluenceAI(Color c, boolean learning) {
      super(c, "Contest Influence AI");
      LEARNING = learning;
      if (learning) {
         for (int i = 0; i < POOL_SIZE; i++) {
            currentGeneration.add(CIConstants.generateRandomConstants());
            constants = currentGeneration.get(0);
         }
      }
   }
   
   @Override
   protected void turn() {
      turn++;
      if (turn > 10000) {
         return;
      }
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
                  constants.MIN_DEFENSIVE_DEFENSE;
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
                  int contribution = p.getNumUnits() - PlayerUtils.getIncomingFleetCount(p, fleets) - (defending ? constants.MIN_DEFENSIVE_DEFENSE : constants.MIN_AGGRESSIVE_DEFENSE);
                  
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
               if (constants.USE_MOVE_FORWARDS) {
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
         theirUnitArea = theirUnitArea.multiply(constants.UNIT_COUNT_POSITION_WEIGHT / (constants.UNIT_COUNT_POSITION_WEIGHT + constants.UNIT_GEN_POSITION_WEIGHT));
         theirProductionArea = theirProductionArea.multiply(constants.UNIT_GEN_POSITION_WEIGHT / (constants.UNIT_COUNT_POSITION_WEIGHT + constants.UNIT_GEN_POSITION_WEIGHT));
         Location theirLocation = theirUnitArea.sum(theirProductionArea);
         Planet target = mine.stream().min((a, b) -> Double.compare(theirLocation.distance(a), theirLocation.distance(b))).get();
         for (Planet p : PlayerUtils.getPlanetsOwnedByPlayer(planets, this)) {
            int toSend = p.getNumUnits() - constants.MIN_AGGRESSIVE_DEFENSE;
            if (toSend > 0) {
               addAction(p, target, toSend);
            }
         }
      }
   }
   
   public double getValue(Planet p, Location averageLocation, double variance) {
      double distanceFactor = (variance + constants.BASE_DISTANCE_FACTOR) / (averageLocation.distance(p) + constants.BASE_DISTANCE_FACTOR);
      return (p.getColor().equals(Color.GRAY) ? 1.0 : constants.AGGRESSION) * Math.pow(distanceFactor, constants.DISTANCE_WEIGHTING) / p.PRODUCTION_TIME / (10 + p.getNumUnits());
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
      turn = 0;
      
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
            if (p.getNumUnits() * constants.CAPTURE_SAFTEY_MARGIN + 1 - takenContribution < distanceProduction) {
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
      myUnitArea = myUnitArea.multiply(constants.UNIT_COUNT_POSITION_WEIGHT);
      myProductionArea = myProductionArea.multiply(constants.UNIT_GEN_POSITION_WEIGHT);
      Location myLocation = myUnitArea.sum(myProductionArea);
      double mySpread = Location.variance(myPlanets);
      
      Location theirLocation;
      if (theirPlanets.size() > 0) {
         Location theirUnitArea = Location.getUnitCountWeightedCenter(theirPlanets);
         Location theirProductionArea = Location.getProductionWeightedCenter(theirPlanets);
         theirUnitArea = theirUnitArea.multiply(constants.UNIT_COUNT_POSITION_WEIGHT);
         theirProductionArea = theirProductionArea.multiply(constants.UNIT_GEN_POSITION_WEIGHT);
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
               double influence = influencing.getNumUnits() * 0.5 * (new Location(influencing).distance(p) + constants.BASE_DISTANCE_FACTOR) / (theirLocation.distance(p) + constants.BASE_DISTANCE_FACTOR);
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
               double influence = influencing.getNumUnits() * Math.pow(0.5 * (new Location(influencing).distance(p) + constants.BASE_DISTANCE_FACTOR) / (myLocation.distance(p) + constants.BASE_DISTANCE_FACTOR), constants.DISTANCE_WEIGHTING);
               addMap(theirInfluence, p, influence);
            }
         }
      }
      
      for (Fleet f : theirFleets) {
         addMap(theirInfluence, f.getDestination(), f.getNumUnits());
      }
      
      List<Planet> potentialTargets = new ArrayList<>();
      for (Planet p : planets) {
         if (myInfluence.get(p) - (p.isNeutral() ? p.getNumUnits() : 0) > constants.CAPTURE_SAFTEY_MARGIN * (theirInfluence.containsKey(p) ? theirInfluence.get(p) : 0)) {
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
   
   @Override
   protected void endGame(boolean victorious) {
      if (LEARNING) {
         if (victorious) {
            constants.winCount++;
            System.out.println("Game " + position + "," + gameCount + " won");
         } else {
            System.out.println("Game " + position + "," + gameCount + " lost");
         }
         gameCount++;
         if (gameCount == GAMES_PER_SAMPLE) {
            gameCount = 0;
            position++;
            if (position >= currentGeneration.size()) {
               List<CIConstants> nextGeneration = new ArrayList<>();
               Collections.shuffle(currentGeneration);
               Collections.sort(currentGeneration, (a,b) -> -Integer.compare(a.winCount, b.winCount));
               System.out.println("Best settings:");
               currentGeneration.get(0).printSettings();
               System.out.print("Generation complete, performance:");
               for (CIConstants c : currentGeneration) {
                  System.out.print(" " + c.winCount);
                  c.winCount = 0;
               }
               System.out.println();
               for (int i = 0; i < POOL_SIZE / 2; i++) {
                  nextGeneration.add(currentGeneration.get(i));
                  nextGeneration.add(new CIConstants(currentGeneration.get(i), currentGeneration.get(i + 1)));
                  nextGeneration.add(new CIConstants(currentGeneration.get(i), CIConstants.generateRandomConstants()));
               }
               currentGeneration = nextGeneration;
               position = 0;
            }
            constants = currentGeneration.get(position);
         }
      }
   }
}

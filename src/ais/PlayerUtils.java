package ais;

import galaxy.DimensionMismatchException;
import galaxy.Fleet;
import galaxy.Planet;
import galaxy.Player;
import galaxy.Unit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PlayerUtils {

   public static class Location {
      private double[] coords;

      public Location(Planet p) {
         this(p.getCoords());
      }

      public Location(Location other) {
         this(other.coords);
      }

      public Location(double[] coords) {
         this.coords = new double[coords.length];
         for (int i = 0; i < coords.length; i++) {
            this.coords[i] = coords[i];
         }
      }

      public Location(int dimension) {
         coords = new double[dimension];
         for (int i = 0; i < coords.length; i++) {
            coords[i] = 0;
         }
      }

      public double distance(Planet other) {
         return distance(new Location(other));
      }

      public double distance(Location other) {
         verifyMatchingDimensions(other);
         double sum = 0;
         for (int i = 0; i < this.coords.length; i++) {
            sum += (this.coords[i] - other.coords[i]) * (this.coords[i] - other.coords[i]);
         }
         return Math.sqrt(sum);
      }

      public Location sum(Planet other) {
         return sum(new Location(other));
      }

      public Location sum(Location other) {
         verifyMatchingDimensions(other);
         Location rtn = new Location(this.coords.length);
         for (int i = 0; i < this.coords.length; i++) {
            rtn.coords[i] = this.coords[i] + other.coords[i];
         }
         return rtn;
      }

      public Location difference(Planet other) {
         return difference(new Location(other));
      }

      public Location difference(Location other) {
         verifyMatchingDimensions(other);
         Location rtn = new Location(this.coords.length);
         for (int i = 0; i < this.coords.length; i++) {
            rtn.coords[i] = this.coords[i] - other.coords[i];
         }
         return rtn;
      }

      public String toString() {
         StringBuilder rtn = new StringBuilder();
         rtn.append("<Location: ");
         for (int i = 0; i < coords.length; i++) {
            rtn.append(coords[i] + ", ");
         }
         return rtn.substring(0, rtn.length() - 3) + ">";
      }

      public static Location center(@SuppressWarnings("rawtypes") List list) {
         Location[] locations = new Location[list.size()];
         for (int i = 0; i < list.size(); i++) {
            if (list.get(i) instanceof Location) {
               locations[i] = (Location) list.get(i);
            } else if (list.get(i) instanceof Planet) {
               locations[i] = new Location((Planet) list.get(i));
            } else {
               throw new RuntimeException("I dont take those");
            }
         }
         return center(locations);
      }

      public static Location center(Location ... locations) {
         if (locations.length == 0) return null; //whoever would do this, screw you
         Location rtn = new Location(locations[0].coords.length);
         for (int i = 0; i < locations.length; i++) {
            rtn.verifyMatchingDimensions(locations[i]);
            for (int j = 0; j < rtn.coords.length; j++) {
               rtn.coords[j] += locations[i].coords[j];
            }
         }
         for (int j = 0; j < rtn.coords.length; j++) {
            rtn.coords[j] = rtn.coords[j] / locations.length;
         }
         return rtn;
      }
      
      public static Location getProductionWeightedCenter(List<Planet> list) {
         if (list.size() == 0) return null; //whoever would do this, screw you
         Location rtn = new Location(list.get(0).getCoords().length);
         double weights = 0;
         for (int i = 0; i < list.size(); i++) {
            for (int j = 0; j < rtn.coords.length; j++) {
               rtn.coords[j] += list.get(i).getCoords()[j] * list.get(i).getProductionFrequency();
               weights += list.get(i).getProductionFrequency();
            }
         }
         if (weights == 0) {
            weights = 1;
         }
         for (int j = 0; j < rtn.coords.length; j++) {
            rtn.coords[j] = rtn.coords[j] / list.size() / weights;
         }
         return rtn;
      }

      public static Location getUnitCountWeightedCenter(List<Planet> list) {
         if (list.size() == 0) return null; //whoever would do this, screw you
         Location rtn = new Location(list.get(0).getCoords().length);
         double weights = 0;
         for (int i = 0; i < list.size(); i++) {
            for (int j = 0; j < rtn.coords.length; j++) {
               rtn.coords[j] += list.get(i).getCoords()[j] * list.get(i).getNumUnits();
               weights += list.get(i).getNumUnits();
            }
         }
         if (weights == 0) {
            weights = 1;
         }
         for (int j = 0; j < rtn.coords.length; j++) {
            rtn.coords[j] = rtn.coords[j] / list.size() / weights;
         }
         return rtn;
      }

      public static double variance(@SuppressWarnings("rawtypes") List list) {
         Location[] locations = new Location[list.size()];
         for (int i = 0; i < list.size(); i++) {
            if (list.get(i) instanceof Location) {
               locations[i] = (Location) list.get(i);
            } else if (list.get(i) instanceof Planet) {
               locations[i] = new Location((Planet) list.get(i));
            } else {
               throw new RuntimeException("I dont take those");
            }
         }
         return variance(locations);
      }

      public static double variance(Location ... locations) {
         Location average = center(locations);
         if (average == null) return 0;
         double[] values = new double[average.coords.length];
         for (int i = 0; i < values.length; i++) {
            values[i] = 0;
         }
         for (int i = 0; i < locations.length; i++) {
            for (int j = 0; j < values.length; j++) {
               values[j] = values[j] + (locations[i].coords[j] - average.coords[j]) * (locations[i].coords[j] - average.coords[j]);
            }
         }
         double rtn = 0.0;
         if (locations.length >= 2) {
            for (int i = 0; i < values.length; i++) {
               rtn += values[i] / (locations.length - 1);
            }
         }
         return Math.sqrt(rtn);
      }

      public Location multiply(double value) {
         Location rtn = new Location(this);
         for (int i = 0; i < rtn.coords.length; i++) {
            rtn.coords[i] *= value;
         }
         return rtn;
      }

      private void verifyMatchingDimensions(Location other) {
         if (this.coords.length != other.coords.length) {
            throw new DimensionMismatchException();
         }
      }
   }

   public static List<Planet> getPlanetsOwnedByPlayer(Planet[] planets, Player player) {
      ArrayList<Planet> rtn = new ArrayList<>();
      for (Planet p : planets) {
         if (p.ownedBy(player)) {
            rtn.add(p);
         }
      }
      return rtn;
   }

   public static List<Planet> getPlanetsNotOwnedByPlayer(Planet[] planets, Player player) {
      ArrayList<Planet> rtn = new ArrayList<>();
      for (Planet p : planets) {
         if (!p.ownedBy(player)) {
            rtn.add(p);
         }
      }
      return rtn;
   }

   public static List<Planet> getUnoccupiedPlanets(Planet[] planets) {
      ArrayList<Planet> rtn = new ArrayList<>();
      for (Planet p : planets) {
         if (p.isNeutral()) {
            rtn.add(p);
         }
      }
      return rtn;
   }

   public static List<Planet> getOwnedPlanets(Planet[] planets) {
      ArrayList<Planet> rtn = new ArrayList<>();
      for (Planet p : planets) {
         if (!p.isNeutral()) {
            rtn.add(p);
         }
      }
      return rtn;
   }

   public static List<Planet> getOpponentsPlanets(Planet[] planets, Player player) {
      return Arrays.asList(planets).stream().filter((p) -> p.ownedByOpponentOf(player)).collect(Collectors.toList());
   }

   public static Planet getNearestPlanet(Planet[] planets, Planet planet) {
      double bestDistance = Double.MAX_VALUE;
      Planet rtn = null;
      for (Planet p : planets) {
         double thisDistance = planet.distanceTo(p);
         if (thisDistance < bestDistance) {
            bestDistance = thisDistance;
            rtn = p;
         }
      }
      return rtn;
   }

   public static Planet getNearestNotOwnedPlanet(Planet[] planets, Planet planet, Player player) {
      double bestDistance = Double.MAX_VALUE;
      Planet rtn = null;
      for (Planet p : planets) {
         if (!p.ownedBy(player)) {
            double thisDistance = planet.distanceTo(p);
            if (thisDistance < bestDistance) {
               bestDistance = thisDistance;
               rtn = p;
            }
         }
      }
      return rtn;
   }

   public static Planet getNearestEnemyPlanet(Planet[] planets, Planet planet, Player player) {
      double bestDistance = Double.MAX_VALUE;
      Planet rtn = null;
      for (Planet p : getOpponentsPlanets(planets, player)) {
         double thisDistance = p.distanceTo(planet);
         if (thisDistance < bestDistance) {
            bestDistance = thisDistance;
            rtn = p;
         }
      }
      return rtn;
   }

   public static int getIncomingFleetCount(Planet p, Fleet[] fleets) {
      int rtn = 0;
      for (Fleet f : fleets) {
         if (f.getDestination().equals(p)) {
            rtn += f.getNumUnits();
         }
      }
      return rtn;
   }

   public static int getPlayersIncomingFleetCount(Planet planet, Fleet[] fleets, Player player) {
      int rtn = 0;
      for (Fleet f : fleets) {
         if (f.getDestination().equals(planet) && f.ownedBy(player)) {
            rtn += f.getNumUnits();
         }
      }
      return rtn;
   }

   public static int getOpponentsIncomingFleetCount(Planet planet, Fleet[] fleets, Player player) {
      int rtn = 0;
      for (Fleet f : fleets) {
         if (f.getDestination().equals(planet) && !f.ownedBy(player)) {
            rtn += f.getNumUnits();
         }
      }
      return rtn;
   }

   public static List<Planet> sortByDistance(List<Planet> planets, Planet planet) {
      List<Planet> rtn = new ArrayList<Planet>(planets);
      Collections.sort(rtn, (a, b) -> {
         return Double.compare(a.distanceTo(planet), b.distanceTo(planet));
      });
      return rtn;
   }

   public static List<Fleet> getMyFleets(Fleet[] fleets, Player player) {
      return Arrays.asList(fleets).stream().filter((fleet) -> fleet.ownedBy(player)).collect(Collectors.toList());
   }

   public static List<Fleet> getOpponentsFleets(Fleet[] fleets, Player player) {
      return Arrays.asList(fleets).stream().filter((fleet) -> !fleet.ownedBy(player)).collect(Collectors.toList());
   }

   public static int getMyUnitCount(Fleet[] fleets, Planet[] planets, Player player) {
      return Arrays.asList(fleets).stream().filter((fleet) -> fleet.ownedBy(player)).collect(Collectors.summingInt((fleet) -> fleet.getNumUnits())) +
            Arrays.asList(planets).stream().filter((planet) -> planet.ownedBy(player)).collect(Collectors.summingInt((planet) -> planet.getNumUnits()));
   }

   public static int getOpponentUnitCount(Fleet[] fleets, Planet[] planets, Player player) {
      return Arrays.asList(fleets).stream().filter((fleet) -> fleet.ownedByOpponentOf(player)).collect(Collectors.summingInt((fleet) -> fleet.getNumUnits())) +
            Arrays.asList(planets).stream().filter((planet) -> planet.ownedByOpponentOf(player)).collect(Collectors.summingInt((planet) -> planet.getNumUnits()));
   }

   public static enum PlanetOwner {
      NOBODY,
      PLAYER,
      OPPONENT;

      public static PlanetOwner getOwner(Unit u, Player p) {
         if (u.ownedBy(p)) {
            return PLAYER;
         } else if (u.ownedByOpponentOf(p)) {
            return OPPONENT;
         } else {
            return NOBODY;
         }
      }
   }

   public static PlanetOwner getCurrentEventualOwner(Planet p, Fleet[] fleets, Player player) {
      PlanetOwner current;
      if (p.ownedBy(player)) {
         current = PlanetOwner.PLAYER;
      } else if (p.ownedByOpponentOf(player)) {
         current = PlanetOwner.OPPONENT;
      } else {
         current = PlanetOwner.NOBODY;
      }
      int updateCount = p.getUpdateCount() % p.PRODUCTION_TIME;
      int previousUnits = 0;
      int unitCount = p.getNumUnits();
      int currentTime = 0;
      for (Fleet f : Arrays.asList(fleets).stream()
            .filter((fleet) -> fleet.getDestination() == p)
            .sorted((a, b) -> Double.compare(a.distanceLeft(), b.distanceLeft()))
            .collect(Collectors.toList())) {
         int passingTime = (int) Math.ceil(f.distanceLeft()/Fleet.SPEED) - currentTime;
         if (current != PlanetOwner.NOBODY) {
            updateCount += passingTime;
            int unitsToAdd = (updateCount + p.PRODUCTION_TIME - 1) / p.PRODUCTION_TIME - previousUnits;
            previousUnits += unitsToAdd;
            unitCount += unitsToAdd;
         }
         if ((f.ownedBy(player) && current == PlanetOwner.PLAYER) || (f.ownedByOpponentOf(player) && current == PlanetOwner.OPPONENT)) {
            unitCount += f.getNumUnits();
         } else {
            unitCount -= f.getNumUnits();
            if (unitCount == 0) {
               current = PlanetOwner.NOBODY;
            }
            if (unitCount < 0) {
               unitCount = -unitCount;
               if (f.ownedBy(player)) {
                  current = PlanetOwner.PLAYER;
               } else {
                  current = PlanetOwner.OPPONENT;
               }
            }
         }
         currentTime += passingTime;
      }
      return current;
   }

   public static Planet getNearestOwnedPlanet(Planet[] planets, Planet planet, Player player) {
      double bestDistance = Double.MAX_VALUE;
      Planet rtn = null;
      for (Planet p : planets) {
         //Get the nearest owned planet that is not the planet being checked
         if (p.ownedBy(player) && !p.equals(planet)) {
            double thisDistance = planet.distanceTo(p);
            if (thisDistance < bestDistance) {
               bestDistance = thisDistance;
               rtn = p;
            }
         }
      }
      return rtn;
   }

   public static int getEnemyUnitsOnPlanets(Planet[] planets, Player player) {
      int unitCount = 0;
      for(Planet p: planets) {
         if(!p.ownedBy(player) && !p.isNeutral()) {
            unitCount += p.getNumUnits();
         }
      }
      return unitCount;
   }

   public static int getMyUnitsOnPlanets(Planet[] planets, Player player) {
      int unitCount = 0;
      for(Planet p: planets) {
         if(p.ownedBy(player)) {
            unitCount += p.getNumUnits();
         }
      }
      return unitCount;
   }

   public static int getEnemyUnitsInFleets(Fleet[] fleets, Player player) {
      int unitCount = 0;
      for(Fleet f: fleets) {
         if(!f.ownedBy(player) && !f.isNeutral()) {
            unitCount += f.getNumUnits();
         }
      }
      return unitCount;
   }

   public static int getMyUnitsInFleets(Fleet[] fleets, Player player) {
      int unitCount = 0;
      for(Fleet f: fleets) {
         if(f.ownedBy(player)) {
            unitCount += f.getNumUnits();
         }
      }
      return unitCount;
   }
   
   public static double getMyTotalProductionFrequency(Planet[] planets, Player player) {
      double productionFrequency = 0;
      for(Planet p: planets) {
         if(p.ownedBy(player)) {
            productionFrequency += p.getProductionFrequency();
         }
      }
      return productionFrequency;
   }
   
   public static double getEnemyTotalProductionFrequency(Planet[] planets, Player player) {
      double productionFrequency = 0;
      for(Planet p: planets) {
         if(!p.ownedBy(player) && !p.isNeutral()) {
            productionFrequency += p.getProductionFrequency();
         }
      }
      return productionFrequency;
   }
}

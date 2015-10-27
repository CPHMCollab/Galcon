package ais;

import galaxy.Fleet;
import galaxy.Planet;
import galaxy.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PlayerUtils {
   
   public static class Location {
      private double[] coords;
      public static class DimensionMismatchException extends RuntimeException {}
      
      public Location(Planet p) {
         this(p.getCoords());
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
         if (locations.length > 2) {
            for (int i = 0; i < values.length; i++) {
               rtn += values[i] / (locations.length - 1);
            }
         }
         return Math.sqrt(rtn);
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
      List<Planet> rtn = getOwnedPlanets(planets);
      rtn.removeAll(getPlanetsOwnedByPlayer(planets,player));
      return rtn;
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
}

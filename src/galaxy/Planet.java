package galaxy;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Scanner;

public final class Planet extends Unit {
   public static final int MAX_RADIUS = 50;
   public static final int MIN_RADIUS = 12;
   public static final int MAX_NEUTRAL_UNITS = 50;
   public static final int MIN_PRODUCE_TIME = 34;
   public static final int MAX_PRODUCE_TIME = 100;

   static int id = 0;
   final int ID = id++;


   public final int RADIUS, PRODUCTION_TIME;
   private int updateCnt = 0;
   private boolean recentlyConquered = false;

   private static LinkedList<Planet> planets = new LinkedList<Planet>();

   private Planet(Player owner, int numUnits, int radius, int prodTime, double ... coords) {
      super(owner, numUnits, coords);
      RADIUS = radius;
      PRODUCTION_TIME = prodTime;

      planets.add(this);
   }

   static Planet generatePlanetFromString(String str) {
      Scanner s = new Scanner(str);

      double[] coords = new double[Main.DIMENSIONS.length];
      for (int i = 0; i < Main.DIMENSIONS.length; i++) {
         coords[i] = s.nextDouble();
      }

      Planet p = new Planet(Main.getPlayer(s.nextInt()), 
            s.nextInt(), s.nextInt(), s.nextInt(), coords);
      s.close();
      return p;
   }
   
   static Planet generateCenterPlanet() {
	   double[] coords = new double[Main.DIMENSIONS.length];
	   for (int i = 0; i < Main.DIMENSIONS.length; i++) {
		   coords[i] = Main.DIMENSIONS[i] / 2;
	   }
	   return new Planet(null, MAX_NEUTRAL_UNITS, MAX_RADIUS, MIN_PRODUCE_TIME, coords);
   }

   static Planet generateStartingPlanet(Player owner) {
      return new Planet(owner, 100, MAX_RADIUS, MIN_PRODUCE_TIME, getLocation(MAX_RADIUS));
   }
   
   static void generateSymmetricStartingPlanets() {
      double[][] locations = getRadialLocations(MAX_RADIUS);
      for (int i = 0; i < locations.length; i++) {
         new Planet(Main.players[i], 100, MAX_RADIUS, MIN_PRODUCE_TIME, locations[i]);
      }
   }

   static Planet generateRandomPlanet() {
      int numUnits = (int) (Math.random() * MAX_NEUTRAL_UNITS);
      int radius = (int) (Math.random() * (MAX_RADIUS - MIN_RADIUS) + MIN_RADIUS);
      int prodTime = (int) ((1 - ((double) radius - MIN_RADIUS) / (MAX_RADIUS - MIN_RADIUS)) * 
            (MAX_PRODUCE_TIME - MIN_PRODUCE_TIME) + MIN_PRODUCE_TIME);
      double[] coords = getLocation(radius);
      return new Planet(null, numUnits, radius, prodTime, coords);
   }
   
   static void generateSymmetricPlanets() {
      int numUnits = (int) (Math.random() * MAX_NEUTRAL_UNITS);
      int radius = (int) (Math.random() * (MAX_RADIUS - MIN_RADIUS) + MIN_RADIUS);
      int prodTime = (int) ((1 - ((double) radius - MIN_RADIUS) / (MAX_RADIUS - MIN_RADIUS)) * 
            (MAX_PRODUCE_TIME - MIN_PRODUCE_TIME) + MIN_PRODUCE_TIME);

      double[][] locations = getRadialLocations(radius);
      for (int i = 0; i < locations.length; i++) {
         new Planet(null, numUnits, radius, prodTime, locations[i]);
      }
   }
   
   private static double[][] getRadialLocations(int planetRadius) {
      if (Main.DIMENSIONS.length == 1) {
         return get1DLocations(planetRadius);
      } else if (Main.DIMENSIONS.length == 2) {
         return get2DRadialLocations(planetRadius);
      } else if (Main.DIMENSIONS.length >= 3) {
         return getNDRadialLocations(planetRadius);
      } else {
         //throw new Exception("There are no dimensions in this game. A planet cannot be created in 0-dimensional space.");
         System.out.println("There are no dimensions in this game. A planet cannot be created in 0-dimensional space.");
         return new double[0][0];
      }
   }
   
   private static double[][] get1DLocations(int planetRadius) {
      if (Main.players.length != 2) {
    	 //throw new Exception("Wrong number of players. There can only be two players in 1-dimensional space.");
    	 System.out.println("Wrong number of players. There can only be two players in 1-dimensional space.");
    	 return new double[0][0];
      }
      double[] coords1 = new double[1];
      double[] coords2 = new double[1];

      do {
         coords1[0] = Math.random() * (Main.DIMENSIONS[0] / 2 - planetRadius * 2) + planetRadius;
      } while (checkOverlappingOtherPlanets(planetRadius, coords1));
      coords2[0] = Main.DIMENSIONS[0] - coords1[0];
      
      double[][] locs = { coords1, coords2 };
      return locs;
   }
   
   private static double[][] get2DRadialLocations(int planetRadius) {
      double minDimension = Math.min(Main.DIMENSIONS[0] / 2, Main.DIMENSIONS[1] / 2) - planetRadius * 2;
      double maxAngle = Math.toRadians(360 / Main.players.length);

      double randomRadius;
      double randomAngle;
      double[] coords = new double[2];
      do {
         randomRadius = Math.random() * minDimension + planetRadius;
         randomAngle = Math.random() * maxAngle;
         coords[0] = randomRadius * Math.cos(randomAngle) + Main.DIMENSIONS[0] / 2;
         coords[1] = randomRadius * Math.sin(randomAngle) + Main.DIMENSIONS[1] / 2;
      } while (checkOverlappingOtherPlanets(planetRadius, coords));
      // TODO: there might be an edge case where there are a lot of players and a planet is
      // generated very close to the center. It may not overlap with existing planets, but it
      // might overlap with the other planets that will be generated symmetrically to it for
      // the other players.
      //    Best way to fix would be to add a method in the while check that checks symmetric
      //    locations to make sure they won't overlap.
      double[][] locations = new double[Main.players.length][];
      locations[0] = coords;
      for (int i = 1; i < Main.players.length; i++) {
         double[] newCoords = new double[2];
         randomAngle += maxAngle;
         newCoords[0] = randomRadius * Math.cos(randomAngle) + Main.DIMENSIONS[0] / 2;
         newCoords[1] = randomRadius * Math.sin(randomAngle) + Main.DIMENSIONS[1] / 2;
         locations[i] = newCoords;
      }

      return locations;
   }
   
   // https://en.wikipedia.org/wiki/N-sphere#Spherical_coordinates
   private static double[][] getNDRadialLocations(int planetRadius) {
      // Get the smallest dimension and make that the
      // max radius of the spherical coordinates
      double minDimension = Main.DIMENSIONS[0];
      for (int i = 1; i < Main.DIMENSIONS.length; i++) {
         if (Main.DIMENSIONS[i] < minDimension) {
            minDimension = Main.DIMENSIONS[i];
         }
      }
      // almost every angle varies from 0-180 degrees
      double maxAngle = Math.toRadians(360 / Main.players.length);

      double[] coords = new double[Main.DIMENSIONS.length];
      double[] randomAngles = new double[Main.DIMENSIONS.length - 2];
      double randomRadius;
      double lastAngle;
      do {
         randomRadius = Math.random() * minDimension;
         double calculatedSin = 1;
         for (int i = 0; i < Main.DIMENSIONS.length - 2; i++) {
            randomAngles[i] = Math.random() * maxAngle;
            coords[i] = randomRadius * calculatedSin * Math.cos(randomAngles[i]) + Main.DIMENSIONS[i] / 2;
            calculatedSin *= Math.sin(randomAngles[i]);
         }
         // The last two coords use a diff angle (0-360 degrees).
         lastAngle = Math.random() * maxAngle * 2;
         coords[Main.DIMENSIONS.length - 2] = randomRadius * calculatedSin * Math.cos(lastAngle) + Main.DIMENSIONS[Main.DIMENSIONS.length - 2] / 2;
         coords[Main.DIMENSIONS.length - 1] = randomRadius * calculatedSin * Math.sin(lastAngle) + Main.DIMENSIONS[Main.DIMENSIONS.length - 1] / 2;
      } while (checkOverlappingOtherPlanets(planetRadius, coords));
      // TODO: there might be an edge case where there are a lot of players and a planet is
      // generated very close to the center. It may not overlap with existing planets, but it
      // might overlap with the other planets that will be generated symmetrically to it for
      // the other players.
      //    Best way to fix would be to add a method in the while check that checks symmetric
      //    locations to make sure they won't overlap.

      // get the locations where each of the other player's planets would be
      double[][] locations = new double[Main.players.length][];
      locations[0] = coords;
      for (int p = 1; p < Main.players.length; p++) {
         double[] newCoords = new double[Main.DIMENSIONS.length];
         
         // update the angles for the next player
         for (int i = 0; i < randomAngles.length; i++) {
          randomAngles[i] += maxAngle;
         }
         lastAngle += maxAngle * 2;
         
         // calculate the new coords
         double calculatedSin = 1;
         for (int i = 0; i < Main.DIMENSIONS.length - 2; i++) {
            newCoords[i] = randomRadius * calculatedSin * Math.cos(randomAngles[i]) + Main.DIMENSIONS[i] / 2;
            calculatedSin *= Math.sin(randomAngles[i]);
         }
         // The last two coords use a diff angle (0-360 degrees).
         newCoords[Main.DIMENSIONS.length - 2] = randomRadius * calculatedSin * Math.cos(lastAngle) + Main.DIMENSIONS[Main.DIMENSIONS.length - 2] / 2;
         newCoords[Main.DIMENSIONS.length - 1] = randomRadius * calculatedSin * Math.sin(lastAngle) + Main.DIMENSIONS[Main.DIMENSIONS.length - 1] / 2;
         locations[p] = newCoords;
      }
      return locations;
   }

   private static double[] getLocation(int radius) {
      double[] coords = new double[Main.DIMENSIONS.length];
      do {
         for (int i = 0; i < Main.DIMENSIONS.length; i++) {
            coords[i] = Math.random() * (Main.DIMENSIONS[i] - radius * 2) + radius;
         }
      } while (checkOverlappingOtherPlanets(radius, coords));
      return coords;
   }

   private static boolean checkOverlappingOtherPlanets(int radius, double ... coords) {
      for(Planet p : planets) {
         if(p.distanceTo(coords) < radius + p.RADIUS + 10) {
            return true;
         }
      }
      return false;
   }

   //********** END PLANET GENERATION CODE **********//


   static void updateAll() {
      for(Planet p : planets) {
         p.update();
      }
   }

   private void update() {
      if(!ownedBy(null) && updateCnt++ % PRODUCTION_TIME == 0) {
         numUnits++;
      }
   }

   static Planet[] getAllPlanets() {
      Planet[] solarSystem = new Planet[planets.size()];
      int i = 0;
      for (Planet p : planets) {
         solarSystem[i++] = p;
      }
      return solarSystem;
   }

   static Player isGameOver() {
      Iterator<Planet> iter = planets.iterator();
      Player winner = null;

      while (iter.hasNext() && (winner = iter.next().owner) == null);

      if (winner == null) {
         
      }

      while (iter.hasNext()) {
         if (iter.next().ownedByOpponentOf(winner)) {
            return null;
         }
      }
      
      

      return Fleet.checkWinner(winner) ? winner : null; 
   }

   void hitBy(Fleet f) {
      if(f.ownedBy(owner)) {
         numUnits += f.getNumUnits();
      } else {
         numUnits -= f.getNumUnits();
         if(numUnits < 0) {
            recentlyConquered = true; // Variable for visualizer, visualizer resets. 
            owner = f.getOwner();
            numUnits *= -1;
         } else if (numUnits == 0) {
            owner = null;
         }
      }
   }
   
   public double getProductionFrequency() {
	  return 1. / (double)this.PRODUCTION_TIME;
   }

   boolean checkRecentlyConquered() {
      boolean recent = recentlyConquered;
      recentlyConquered = false;
      return recent;
   }

   Fleet sendFleet(Planet target, int numSent) {
      numSent = Math.min(numSent, numUnits);
      if (numSent > 0) {
         numUnits -= numSent;
         return new Fleet(numSent, owner, target, getCoords());
      } else {
         return null;
      }
   }

   static void clear() {
      planets.clear();
   }

   static int getNumUnitsInPlanets(Player p)  {
      int count = 0;
      for(Planet f : planets) {
         if(f.ownedBy(p)) {
            count += f.numUnits;
         }
      }
      return count;
   }
   
   public int getUpdateCount() {
      return updateCnt;
   }
}

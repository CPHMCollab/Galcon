package galaxy;

import human.MeatSackAI;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import stats.DefaultStats;
import visualizers.DefaultVisualizer;
import visualizersthreedee.Display;
import ais.*;

class GameSettings {
   static boolean debugMode = false, logGame = false;

   static BufferedWriter gameLog = logGame ? makeLogFile("galconset-" + formatDate(new Date())) : null;

   static Player [] players = {new DistanceValueDefenderAI(Color.cyan), new DistanceValueDefenderAI(Color.magenta)};

   /* 2D game settings
   static final int[] DIMENSIONS = {1280, 800};
   static final int PLANET_DENSITY = 64000; // Planets per volume units
   static Visualizer visualizer = new DefaultVisualizer(DIMENSIONS);
   //*/
   
   //* 3D game settings
   static final int[] DIMENSIONS = {1000, 1000, 1000};
   static final int PLANET_DENSITY = 15000000; // Planets per volume units
   static Visualizer visualizer = new Display(DIMENSIONS);
   //*/
   
   static final int NUM_PLANETS = worldSize() / PLANET_DENSITY;//16;
   static final int FLEET_SPEED = 2;

   static final int PLAYERS_PER_GAME = 2;
   static final int NUM_ROUNDS = 5000;

   static final int FRAME_TIME = 10;
   
   Director director = new Director();

   static Stats createStats(Player p) {
      return new DefaultStats(p);
   }



   void debug(String str) {
      if (debugMode) {
         System.out.println(str);
      }
   }

   static BufferedWriter makeLogFile(String filename) {
      try {
         return new BufferedWriter(new FileWriter(new File(filename)));
      } catch (IOException e) {
         System.err.println("Couldn't make log file.");
         System.exit(0);
         return null;
      }
   }

   static String formatDate(Date date) {
      String str = "";
      str += date.getTime();
      return str;
   }

   static int worldSize() {
      int prod = 1;
      for (int i : DIMENSIONS) {
         prod *= i;
      }
      return prod;
   }
}

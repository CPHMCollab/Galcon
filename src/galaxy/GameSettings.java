package galaxy;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import stats.DefaultStats;
import visualizers.twodimensiondefault.DefaultVisualizer;
import ais.jono.ContestInfluenceAI;
import ais.jono.ValueDefenderAI;

class GameSettings {
   static final boolean debugMode = false, logGame = false;

   static BufferedWriter gameLog = logGame ? makeLogFile("galconset-" + formatDate(new Date())) : null;
   static Player p1 = new ValueDefenderAI();
   static Player p2 = new ContestInfluenceAI(false);
   static Player [] players = {p1, p2};
   
//   static final int[] DIMENSIONS = {1000, 1000, 1000};
//   static final int NUM_PLANETS = 16;
//   static Visualizer visualizer = new Display(DIMENSIONS);

   static final int[] DIMENSIONS = {1600, 900};
   static final int NUM_PLANETS = 16;
   static Visualizer visualizer = new DefaultVisualizer(DIMENSIONS);

   static final int FLEET_SPEED = 3;
   public static final boolean SYMMETRIC = false;
   static final int PLAYERS_PER_GAME = 2;
   static final int NUM_ROUNDS = 1000000;

   static final int FRAME_TIME = 10;
   static final Director director = new Director();

   

   static final Stats createStats(Player p) {
      return new DefaultStats(p);
   }



   static final void debug(String str) {
      if (debugMode) {
         System.out.println(str);
      }
   }

   static final BufferedWriter makeLogFile(String filename) {
      try {
         return new BufferedWriter(new FileWriter(new File(filename)));
      } catch (IOException e) {
         System.err.println("Couldn't make log file.");
         System.exit(0);
         return null;
      }
   }

   static final String formatDate(Date date) {
      String str = "";
      str += date.getTime();
      return str;
   }

   static final int worldSize() {
      int prod = 1;
      for (int i : DIMENSIONS) {
         prod *= i;
      }
      return prod;
   }
}

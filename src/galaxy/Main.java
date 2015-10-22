package galaxy;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

class Main extends GameSettings {
   public static void main(String[] args) {
      new Main();
   }

   static boolean pause = false;
   static boolean skipGame = false;

   private Main() {
      debug("Starting creation " + NUM_PLANETS);

      if (visualizer != null) {
         Timer timer = new Timer();
         timer.schedule(new TimerTask() {
            @Override
            public void run() {
               if (!director.done()) {
                  next();
                  visualizer.update();
               }
            }
         }, 0, FRAME_TIME);
      } else {
         while (!director.done()) {
            next();
         }
      }

   }

   /**
    * The next game tic
    */
   void next() {
      if (skipGame) {
         skipGame = false;
         director.skipGame();
      }

      if (!pause && !director.done()) {
         director.next();
      }
   }

   /**
    * For make map from text
    * @param ID
    * @return
    */
   static Player getPlayer(int ID) {
      return null;
   }

   /**
    * For visualizer to be able to pause game
    */
   static void togglePause() {
      pause = !pause;
   }

   /**
    * For visualizer to be able to skip games
    */
   static void skipGame() {
      skipGame = true;
   }

   static void writeToLog(String str) {
      try {
         gameLog.write(str);
      } catch (IOException e) {
         System.err.println("Couldn't write to log file.");
      }
   }

   static void resetVisualizer() {
      if (visualizer != null) {
         visualizer.nextGame();
      }
   }
}
package galaxy;

import java.awt.Color;
import java.util.LinkedList;

import ais.PlayerUtils;

public abstract class Player {
   public final Color COLOR;
   public final String NAME;
   private static int currentId = 0;
   public final int ID = currentId++;

   protected Planet[] planets = Planet.getAllPlanets();
   protected Fleet[] fleets;
   private LinkedList<Action> actions = new LinkedList<Action>();


   /*
    *  Arbitrary creation of new players does not matter since ID is forced unique and only main-created
    *  players can actually get asked for turns. 
    */
   protected Player(Color c, String name) {
      COLOR = c;
      NAME = name;
   }

   protected final void debug(String str) {
      if (Main.debugMode) {
         System.out.println(str);
      }
   }

   final void nextGame() {
      planets = Planet.getAllPlanets();
      newGame();
   }

   protected abstract void newGame();

   final LinkedList<Action> getActions() {
      return actions;
   }

   final void doTurn() {
      actions = new LinkedList<Action>();
      fleets = Fleet.getAllFleets();
      turn();
   }

   protected final Action addAction(Planet start, Planet target, int numUnits) {
      Action a = new Action(start, target, numUnits, this);
      actions.add(a);
      return a;
   }
   
   protected final void clearActions() {
      actions.clear();
   }

   protected abstract void turn();

   protected abstract String storeSelf();

   /**
    * Checks if two players are equal. Works with null. 
    * @param p1
    * @param p2
    * @return Whether two players are equal. Both null returns true, only one null returns false. 
    */
   protected static final boolean areEqual(Player p1, Player p2) {
      return (p1 == null || p2 == null) ? p1 == p2 : p1.ID == p2.ID;
   }

   protected static final int numUnitsOwnedBy(Player p) {
      return Galaxy.numUnitsOwnedBy(p);
   }

   protected static final int numUnitsInPlanets(Player p) {
      return Planet.getNumUnitsInPlanets(p);
   }

   protected static final int numUnitsInFleets(Player p) {
      return Fleet.getNumUnitsInFleets(p);
   }
   
}












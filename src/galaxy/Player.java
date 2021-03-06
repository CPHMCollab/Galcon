package galaxy;

import java.awt.Color;
import java.util.LinkedList;

public abstract class Player extends GameSettings {
   public final Color COLOR;
   public final String NAME;
   private static int currentId = 0;
   public final int ID = currentId++;

   protected Planet[] planets;
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

   final void nextGame(Planet[] newMap) {
      planets = newMap;
      newGame();
   }

   /*
    * New game for an AI. 
    * Non-final to allow it to be optional. 
    */
   protected void newGame() {}

   protected final boolean ownedByMe(Unit u) {
      return u != null && u.ownedBy(this);
   }

   final LinkedList<Action> getActions() {
      return actions;
   }

   final void doTurn(Fleet[] currentFleets) {
      actions = new LinkedList<Action>();
      fleets = currentFleets;
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

   /*
    * Used to allow an AI to write its state to a file.
    * AI is expected to be able to recreate itself from this string. 
    * Note that order of operations is new AI -> loadFromStore and the 
    * save string should be written as such. 
    * Non-final to allow it to be optional. 
    * '#' character is disallowed and will be cleaned if used.
    */
   protected String storeSelf() {
      return null;
   }
   
   /*
    * Used to allow an AI to restore its state from a file. 
    * See above method. 
    * Non-final to allow it to be optional. 
    */
   protected void loadFromStore(String oldself) {}
   
   //notify player about end of game
   protected void endGame(boolean vicorious) {}

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
      return Director.numUnitsOwnedBy(p);
   }

   protected static final int numUnitsInPlanets(Player p) {
      return Director.numUnitsInPlanets(p);
   }

   protected static final int numUnitsInFleets(Player p) {
      return Director.numUnitsInFleets(p);
   }
}












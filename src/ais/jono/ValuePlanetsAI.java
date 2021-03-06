package ais.jono;

import galaxy.Planet;
import galaxy.Player;

import java.awt.Color;
import java.util.List;

import ais.PlayerUtils;

public class ValuePlanetsAI extends Player {
   private static int MIN_DEFENSE = 15;
   
   public ValuePlanetsAI() {
      super(Color.RED, "Value Planets AI");
   }

   public double getValue(Planet p) {
      return (p.ownedByOpponentOf(this) ? 1400.0 : 1000.0) / p.PRODUCTION_TIME / (100 + p.getNumUnits());
   }
   
   @Override
   protected void turn() {
      List<Planet> myPlanets = PlayerUtils.getPlanetsOwnedByPlayer(planets, this);
      List<Planet> otherPlanets = PlayerUtils.getPlanetsNotOwnedByPlayer(planets, this);
      
      Planet target = null;
      double best = Double.MIN_VALUE;
      for (Planet p : otherPlanets) {
         double value = getValue(p);
         if (value > best) {
            if (PlayerUtils.getPlayersIncomingFleetCount(p, fleets, this) == 0) {
               target = p;
               best = value;
            }
         }
      }
      
      int needed = target.getNumUnits() + 20;
      int available = 0;
      for (Planet p : myPlanets) {
         int contribution = p.getNumUnits() - PlayerUtils.getIncomingFleetCount(p, fleets) - MIN_DEFENSE;
         
         if (available + contribution > needed) {
            addAction(p, target, needed - available);
            available += contribution;
            break;
         }
         available += contribution;
         addAction(p, target, contribution);
      }
      
      if (available < needed) {
         clearActions();
      }
   }

   @Override
   protected void newGame() {
      
   }

   @Override
   protected String storeSelf() {
      return null;
   }
}

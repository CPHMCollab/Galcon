package ais;

import galaxy.Action;
import galaxy.Planet;
import galaxy.Player;

import java.awt.Color;
import java.util.List;

public class ValueDefenderAI extends Player {
   private static int MIN_DEFENSE = 5;
   
   public ValueDefenderAI() {
      super(new Color(40,0,0), "Value Defender AI");
   }

   public double getValue(Planet p) {
      return (p.getColor().equals(Color.GRAY) ? 1000.0 : 2000.0) / p.PRODUCTION_TIME / (100 + p.getNumUnits());
   }
   
   @Override
   protected void turn() {
      List<Planet> myPlanets = PlanetUtils.getPlanetsOwnedByPlayer(planets, this);
      List<Planet> otherPlanets = PlanetUtils.getPlanetsNotOwnedByPlayer(planets, this);
      
      boolean defending = false;
      
      Planet target = null;
      int needed = 0;
      for (Planet p : myPlanets) {
         needed = 
               PlanetUtils.getOpponentsIncomingFleetCount(p, fleets, this) -
               p.getNumUnits() -
               PlanetUtils.getPlayersIncomingFleetCount(p, fleets, this) +
               MIN_DEFENSE;
         if (needed > 0) {
            defending = true;
            target = p;
            break;
         }
      }
      
      if (target == null) {
         double best = Double.MIN_VALUE;
         for (Planet p : otherPlanets) {
            double value = getValue(p);
            if (value > best) {
               if (PlanetUtils.getPlayersIncomingFleetCount(p, fleets, this) == 0) {
                  target = p;
                  best = value;
               }
            }
         }
         needed = target.getNumUnits() + 20;
      }
      
      int available = 0;
      for (Planet p : myPlanets) {
         if (p != target) {
            int contribution = p.getNumUnits() - PlanetUtils.getIncomingFleetCount(p, fleets) - MIN_DEFENSE;
            
            if (available + contribution > needed) {
               actions.add(new Action(p, target, needed - available));
               available += contribution;
               break;
            }
            available += contribution;
            actions.add(new Action(p, target, contribution));
         }
      }
      
      if (available < needed) {
         actions.clear();
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
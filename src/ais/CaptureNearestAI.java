package ais;

import galaxy.Action;
import galaxy.Planet;
import galaxy.Player;

import java.awt.Color;
import java.util.List;

public class CaptureNearestAI extends Player {

   public CaptureNearestAI() {
      super(new Color(10,10,10), "Capture Nearest AI");
   }

   @Override
   protected void newGame() {
      
   }

   @Override
   protected void turn() {
      List<Planet> myPlanets = PlayerUtils.getPlanetsOwnedByPlayer(planets, this);
      List<Planet> otherPlanets = PlayerUtils.getPlanetsNotOwnedByPlayer(planets, this);
      
      for (Planet p : myPlanets) {
         for (Planet pOther : PlayerUtils.sortByDistance(otherPlanets, p)) {
            if (p.getNumUnits() - PlayerUtils.getIncomingFleetCount(p, fleets) > 3 * pOther.getNumUnits()) {
               int currentCount = PlayerUtils.getPlayersIncomingFleetCount(pOther, fleets, this);
               if (currentCount == 0) {
                  addAction(p, pOther, 2 * pOther.getNumUnits());
                  break;
               }
            }
         }
      }
   }

   @Override
   protected String storeSelf() {
      return null;
   }
   
}

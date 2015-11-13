package ais.tyler;

import java.awt.Color;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ais.PlayerUtils;
import galaxy.Planet;
import galaxy.Player;

public class TylerRandomAI extends Player {
   
   public TylerRandomAI() {
      super(new Color(50,100,0), "Pseudo Random AI");
   }
   
   public TylerRandomAI(Color c) {
      super(c, "Pseudo Random AI");
   }
   
   @Override
   protected void turn() {
      pseudoRandomAI();
   }
   
   ///////////////////////
   //      SORTING      //
   ///////////////////////
   
   private void greedySort(List<Planet> planets) {
      Collections.sort(planets, new Comparator<Planet>() {
         @Override
         public int compare(Planet p1, Planet p2) {
            return planetValue(p2) - planetValue(p1);
         }
      });
   }
   
   // The higher the value, the better the planet is
   private int planetValue(Planet p) {
      return -p.getNumUnits();
   }

   ///////////////////////
   //        AIs        //
   ///////////////////////
   
   private void pseudoRandomAI() {
      List<Planet> myPlanets = PlayerUtils.getPlanetsOwnedByPlayer(planets, this);
      List<Planet> otherPlanets = PlayerUtils.getPlanetsNotOwnedByPlayer(planets, this);
      
      greedySort(otherPlanets);
      
      for (Planet p : myPlanets) {
         if (p.getNumUnits() > 10) {
            int rand = (int)(Math.random() * 2);
            int index = 0;
            while (rand % 2 != 0) {
               index++;
               rand = (int)(Math.random() * 2);
            }
            if (otherPlanets.size() > 0) {
               addAction(p, otherPlanets.get(index % otherPlanets.size()), 1);
            }
         }
      }
   }
   
//   private void randomAI() {
//      List<Planet> myPlanets = PlayerUtils.getPlanetsOwnedByPlayer(planets, this);
//      
//      for (Planet p : myPlanets) {
//         if (p.getNumUnits() > 10) {
//            addAction(p, planets[(int)(Math.random() * planets.length)], 1);
//         }
//      }
//   }
   
   @Override
   protected void newGame() {}
   
   @Override
   protected String storeSelf() { return null; }
}

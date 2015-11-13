package ais.cody;

import galaxy.Fleet;
import galaxy.Planet;
import galaxy.Player;

import java.awt.Color;
import java.util.ArrayList;

import ais.PlayerUtils;


public class ValueCapture extends Player {
	private static final double FUTURE_COEFFICIENT = 2;
	private static final double DISTANCE_COEFFICIENT = .3;
	private double health;
	private Vector heart; 
	private int turn;

   public ValueCapture() {
      super(new Color(255,255,255), "ValueCapture");
      turn = 0;
   }
   
   public ValueCapture(Color c) {
      super(c, "ValueCapture");
   }
   
   private void calculateHealth() {
	   double[] planetPosition;
	   Vector planetVector;
	   
	   health = 0;
	   heart = new Vector(0.,0.,0.);
	   
	   for (Planet p : PlayerUtils.getPlanetsOwnedByPlayer(planets, this)) {
		   this.health += p.getNumUnits();
		   
		   planetPosition = p.getCoords();
		   try {
			   planetVector = new Vector(planetPosition[0], planetPosition[1], planetPosition[2]);
		   }
		   catch (Exception ex) {
			   planetVector = new Vector(planetPosition[0], planetPosition[1], 0);
		   }
		   planetVector = Vector.scale(planetVector, p.getNumUnits());
		   heart = Vector.add(heart, planetVector);
	   }
	   
	   heart = Vector.scale(heart, 1. / health);
   }
   
   @Override
   protected void turn() {
	  if (turn++ < 10)
		  return;
	  
      ArrayList<Planet> myPlanets = new ArrayList<Planet>(PlayerUtils.getPlanetsOwnedByPlayer(planets, this));
      ArrayList<Planet> opponentsPlanets = new ArrayList<Planet>(PlayerUtils.getOpponentsPlanets(planets, this));
      ArrayList<Planet> neutralPlanets = new ArrayList<Planet>(PlayerUtils.getPlanetsNotOwnedByPlayer(planets, this));
      Planet target = null;
      ArrayList<Planet> attackers = new ArrayList<Planet>();
      int[] unitsToSend;
      int i;
      double distance;
      double least;
      double cost;
      double weightedCost;
      double targetCost = 0;
      int strength;
      int totalToSend = 0;
      boolean canAttack;
      
      neutralPlanets.removeAll(opponentsPlanets);
      
      calculateHealth();
      
      // find the best planet to send units to
      least = Double.MAX_VALUE;
      for (Planet to : planets) {
    	  cost = costOfPlanet(to);
    	  weightedCost = cost;
    	  distance = to.distanceTo(Vector.getCoords(heart));
    	  weightedCost += DISTANCE_COEFFICIENT * distance;
    	  weightedCost -= (FUTURE_COEFFICIENT * to.getProductionFrequency());
    	  if (cost >= 0 && weightedCost < least) {
    		  target = to;
    		  least = weightedCost;
    	  }
      }
      
      
      // determine who should send units
      if (target != null && least != Double.MAX_VALUE) {
          targetCost = costOfPlanet(target);
    	  for (Planet from : myPlanets) {
    		  if (planetStrength(from) < -1)
    			  attackers.add(from);
    	  }
    	  
    	  if (!attackers.isEmpty()) {
    		  unitsToSend = new int[attackers.size()];
	    	  canAttack = true;
    		  while (totalToSend <= targetCost && canAttack) {
	    		  canAttack = false;
	    		  i = 0;
		    	  for (Planet from : attackers) {
		    		  strength = planetStrength(from);
		    		  if (-strength - unitsToSend[i] > 2) {
		    			  unitsToSend[i]++;
		    			  totalToSend++;
		    			  canAttack = true;
		    		  }
		    		  i++;
		    	  }
	    	  }
    		  
    		  if (totalToSend > targetCost) {
	    		  i = 0;
	    		  for (Planet from : attackers)
	    			  addAction(from, target, unitsToSend[i++]);
    		  }
		  }
      }
      
   }
   
   private double costOfPlanet(Planet target) {
	   double cost = Double.MAX_VALUE;
	   double timeCost;
	   
	   timeCost = (target.getProductionFrequency() * (target.distanceTo(Vector.getCoords(heart)) / Fleet.SPEED)) + 1;
	   
	   if (target.ownedBy(this)) {
			   cost = ((double)planetStrength(target));
	   }
	   else if (target.ownedBy(null)) {
		   cost = ((double)planetStrength(target) + 1) ;
	   }
	   else {
		   cost = ((double)planetStrength(target) + 1) + timeCost;
	   }
	   
	   
	   return cost;
   }

   private int planetStrength(Planet planet) {
	   int myStrength;
	   int enemyStrength;
	   Fleet[] allFleets = Fleet.getAllFleets();
	   
	   if (planet.ownedBy(this)) {
		   myStrength = planet.getNumUnits();
		   enemyStrength = 0;
	   }
	   else {
		   myStrength = 0;
		   enemyStrength = planet.getNumUnits();
	   }
	   
	   for (Fleet fleet : allFleets) 
		   if (fleet.getDestination().equals(planet)) {
			   if (fleet.ownedBy(this)) {
				   myStrength += fleet.getNumUnits();
			   }
			   else
				   enemyStrength += fleet.getNumUnits();
		   }
	   
	   return enemyStrength - myStrength;
   }
   
   @Override
   protected void newGame() {
      
   }

   @Override
   protected String storeSelf() {
      return null;
   }
}

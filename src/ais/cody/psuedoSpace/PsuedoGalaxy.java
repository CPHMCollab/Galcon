package ais.cody.psuedoSpace;

import java.util.ArrayList;

import ais.cody.Vector;
import galaxy.Fleet;
import galaxy.Planet;
import galaxy.Player;

public class PsuedoGalaxy {
	public ArrayList<PsuedoPlanet> psuedoPlanets;
	public ArrayList<PsuedoFleet> psuedoFleets;
	public Vector heart; 
	public Vector enemyHeart; 
	
	public PsuedoGalaxy(Planet[] planets, Fleet[] fleets, Player me) {
		psuedoPlanets = new ArrayList<PsuedoPlanet>();
		psuedoFleets = new ArrayList<PsuedoFleet>();
		
		for (Planet planet : planets)
			psuedoPlanets.add(new PsuedoPlanet(planet, me));
		
		for (Fleet fleet : fleets)
			psuedoFleets.add(new PsuedoFleet(fleet, me, psuedoPlanets));
		findCenters();
	}
	
	public PsuedoGalaxy(PsuedoGalaxy psuedoGalaxy) {
		psuedoPlanets = new ArrayList<PsuedoPlanet>();
		psuedoFleets = new ArrayList<PsuedoFleet>();
		
		for (PsuedoPlanet psuedoPlanet : psuedoGalaxy.psuedoPlanets)
			psuedoPlanets.add(new PsuedoPlanet(psuedoPlanet));
		
		for (PsuedoFleet psuedoFleet : psuedoGalaxy.psuedoFleets)
			psuedoFleets.add(new PsuedoFleet(psuedoFleet));
		findCenters();
	}
	
	public void advance(int time, ArrayList<PsuedoAction> psuedoActions) {
		ArrayList<PsuedoFleet> updatedFleets = new ArrayList<PsuedoFleet>();

		for (PsuedoPlanet psuedoPlanet : psuedoPlanets)
			psuedoPlanet.advance(time);
		
		for (PsuedoAction psuedoAction : psuedoActions) 
			psuedoFleets.add(psuedoAction.act());
		
		for (PsuedoFleet psuedoFleet : psuedoFleets)
			psuedoFleet.advance(time);
		
		for (PsuedoFleet psuedoFleet : psuedoFleets)
			if (psuedoFleet.turnsLeft > 0)
				updatedFleets.add(psuedoFleet);
		
		psuedoFleets = updatedFleets;
		
		findCenters();
	}
	
	public ArrayList<PsuedoPlanet> myPlanets() {
		ArrayList<PsuedoPlanet> myPlanets = new ArrayList<PsuedoPlanet>();
		for (PsuedoPlanet psuedoPlanet : psuedoPlanets)
			if (psuedoPlanet.mine()) 
				myPlanets.add(psuedoPlanet);
		
		return myPlanets;
	}
	
	public ArrayList<PsuedoPlanet> enemyPlanets() {
		ArrayList<PsuedoPlanet> enemyPlanets = new ArrayList<PsuedoPlanet>();
		for (PsuedoPlanet psuedoPlanet : psuedoPlanets)
			if (psuedoPlanet.enemy()) 
				enemyPlanets.add(psuedoPlanet);
		
		return enemyPlanets;
	}
	
	public ArrayList<PsuedoPlanet> neutralPlanets() {
		ArrayList<PsuedoPlanet> neutralPlanets = new ArrayList<PsuedoPlanet>();
		for (PsuedoPlanet psuedoPlanet : psuedoPlanets)
			if (psuedoPlanet.neutral == true) 
				neutralPlanets.add(psuedoPlanet);
		
		return neutralPlanets;
	}
	
	public ArrayList<PsuedoFleet> myFleets() {
		ArrayList<PsuedoFleet> myFleets = new ArrayList<PsuedoFleet>();
		for (PsuedoFleet psuedoFleet : psuedoFleets)
			if (psuedoFleet.mine()) 
				myFleets.add(psuedoFleet);
		
		return myFleets;
	}
	
	public ArrayList<PsuedoFleet> enemyFleets() {
		ArrayList<PsuedoFleet> enemyFleets = new ArrayList<PsuedoFleet>();
		for (PsuedoFleet psuedoFleet : psuedoFleets)
			if (psuedoFleet.enemy()) 
				enemyFleets.add(psuedoFleet);
		
		return enemyFleets;
	}
	
	public int psuedoPlanetStrength(PsuedoPlanet psuedoPlanet) {
	   int planetStrength = psuedoPlanet.strength;
	   
	   for (PsuedoFleet psuedoFleet : psuedoFleets) 
		   if (psuedoFleet.target.equals(psuedoPlanet))
			   planetStrength += psuedoFleet.strength;
		   
	   
	   return planetStrength;
		
	}
	
   private void findCenters() {
	   double health;
	   double enemyHealth;
	   Vector planetVector;
	   
	   health = 0;	   
	   enemyHealth = 0;
	   heart = new Vector(0.,0.,0.);
	   
	   for (PsuedoPlanet p : myPlanets()) {
		   health -= p.strength;
		   
		   planetVector = p.position;
		   planetVector = Vector.scale(planetVector, -p.strength);
		   heart = Vector.add(heart, planetVector);
	   }
	   
	   heart = Vector.scale(heart, 1. / health);
	   
	   enemyHeart = new Vector(0.,0.,0.);
	   
	   for (PsuedoPlanet p : enemyPlanets()) {
		   enemyHealth += p.strength;
		   
		   planetVector = p.position;
		   planetVector = Vector.scale(planetVector, p.strength);
		   enemyHeart = Vector.add(enemyHeart, planetVector);
	   }
	   
	   enemyHeart = Vector.scale(enemyHeart, 1. / enemyHealth);
   }
   
   private double spread(Vector center, ArrayList<PsuedoPlanet> planets) {
	   double sum = 0;
	   double difference;
	   
	   for (PsuedoPlanet planet : planets) {
		   difference = Vector.abs(Vector.sub(center, planet.position));
		   sum += difference * difference;
	   }
	   
	   sum /= planets.size();
	   
	   return sum;
   }
	
	public double health(double productionValue, double spreadValue) {
		int health = 0;
		for (PsuedoPlanet psuedoPlanet : psuedoPlanets)
			if (psuedoPlanet.neutral == false) {
				health += psuedoPlanetStrength(psuedoPlanet);
				if (psuedoPlanet.mine()) 
					health -= psuedoPlanet.productionFrequency * productionValue;
				else
					health += psuedoPlanet.productionFrequency * productionValue;
			}
		
		for (PsuedoFleet psuedoFleet : psuedoFleets)
			health += psuedoFleet.strength;
		
		health -= spreadValue * spread(heart, myPlanets());
		health += spreadValue * spread(enemyHeart, enemyPlanets());
		
		return health;
	}
	
	public String toString() {
		String str = "PsuedoGalaxy: \n";
		str += "   Neutral Planets: \n";
		for (PsuedoPlanet psuedoPlanet : neutralPlanets())
			str += "      " + psuedoPlanet.toString() + "\n";
		str += "   My Planets: \n";
		for (PsuedoPlanet psuedoPlanet : myPlanets())
			str += "      " + psuedoPlanet.toString() + "\n";
		str += "   Enemy Planets: \n";
		for (PsuedoPlanet psuedoPlanet : enemyPlanets())
			str += "      " + psuedoPlanet.toString() + "\n";
		str += "   My Fleets: \n";
		for (PsuedoFleet psuedoFleet : myFleets())
			str += "      " + psuedoFleet.toString() + "\n";
		str += "   Enemy Fleets: \n";
		for (PsuedoFleet psuedoFleet : enemyFleets())
			str += "      " + psuedoFleet.toString() + "\n";
		return str;
	}
	
}

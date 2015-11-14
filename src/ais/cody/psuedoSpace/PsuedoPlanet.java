package ais.cody.psuedoSpace;

import ais.cody.Vector;
import galaxy.Planet;
import galaxy.Player;

public class PsuedoPlanet {
	   public Vector position;
	   public int strength;
	   public double productionFrequency;
	   public Planet realPlanet;
	   double partialProduction;
	   public boolean neutral;
	   
	   public PsuedoPlanet(Planet planet, Player me) {
		   position = new Vector(planet.getCoords());
		   
		   strength = planet.ownedBy(me) ? -planet.getNumUnits() : planet.getNumUnits();   
		   productionFrequency = planet.getProductionFrequency();
		   realPlanet = planet;
		   partialProduction = 0;
		   neutral = planet.ownedBy(null) ? true : false;
	   }
	   
	   public PsuedoPlanet(PsuedoPlanet psuedoPlanet) {
		   position = psuedoPlanet.position;
		   strength = psuedoPlanet.strength;
		   productionFrequency = psuedoPlanet.productionFrequency;
		   realPlanet = psuedoPlanet.realPlanet;
		   partialProduction = 0;
		   neutral = psuedoPlanet.neutral;
	   }
	   
	   public void fleetArrives(int units) {
		   int currentStrength = strength;
		   
		   if (neutral) {
			   if (units > 0) {
				   if (strength >= units)
					   strength -= units;
				   else {
					   strength = -(strength - units);
					   neutral = false;
				   }
			   }
			   else {
				   strength += units;
				   if (strength < -units)
					   neutral = false;
			   }
		   }
		   else {
			   if (strength + units == 0) {
				   strength = 0;
				   neutral = true;
			   }
			   else {
				   strength += units;
			   }
			   if (currentStrength > 0 && strength <= 0 || currentStrength <= 0 && strength > 0)
				   partialProduction = 0;
			   
		   }
	   }
	   
	   public void fleetDeparts(int units) {
		   strength -= units;
	   }
	   
	   public void advance(int time) {
		   double unitsProduced;
		   if (neutral == false) {
			   unitsProduced = (double)time * productionFrequency;
			   unitsProduced += partialProduction;
			   partialProduction = unitsProduced - (int)unitsProduced;
			   
			   if (mine())
				   strength -= (int)unitsProduced;
			   else
				   strength += (int)unitsProduced;
		   }
	   }
	   
	   public final double distanceTo(PsuedoPlanet planet) {
	      Vector difference = Vector.sub(position, planet.position);
	      return Vector.abs(difference);
	   }
	   
	   public final double distanceTo(double[] coords) {
	      Vector difference = Vector.sub(position, new Vector(coords));
	      return Vector.abs(difference);
	   }
	   
	   public boolean mine() {
		   return strength < 0;
	   }
	   
	   public boolean enemy() {
		   return (strength > 0 && neutral == false);
	   }
	   
	   public String toString() {
		   return "Planet: location: " + position + " strength " + strength;
	   }
	   
	   public boolean equals(PsuedoPlanet other) {
		   return (position.equals(other.position) && strength == other.strength);
	   }
}
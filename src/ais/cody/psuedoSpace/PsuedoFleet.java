package ais.cody.psuedoSpace;

import java.util.ArrayList;

import ais.cody.Vector;
import galaxy.Fleet;
import galaxy.Player;

public class PsuedoFleet {
	public PsuedoPlanet target;
	public int strength;
	public int turnsLeft;
	
	public PsuedoFleet(PsuedoAction psuedoAction) {
		target = psuedoAction.to;
		strength = psuedoAction.numUnits;
		turnsLeft = (int)(target.distanceTo(Vector.getCoords(psuedoAction.from.position)) / Fleet.SPEED);
	}
	
	public PsuedoFleet(Fleet fleet, Player me, ArrayList<PsuedoPlanet> psuedoPlanets) {		
		for (PsuedoPlanet psuedoPlanet : psuedoPlanets) 
			if (psuedoPlanet.realPlanet.equals(fleet.getDestination()))
				target = psuedoPlanet;
		
		strength = fleet.ownedBy(me) ? -fleet.getNumUnits() : fleet.getNumUnits();
		turnsLeft = (int)(fleet.distanceLeft() / Fleet.SPEED);
	}
	
	public PsuedoFleet(PsuedoFleet psuedoFleet) {
		target = psuedoFleet.target;
		strength = psuedoFleet.strength;
		turnsLeft = psuedoFleet.turnsLeft;
	}
	
	public boolean mine() {
		return strength < 0;
	}
	
	public boolean enemy() {
		return strength > 0;
	}
	
	public void advance(int time) {
		turnsLeft -= time;
		if (turnsLeft <= 0) 
			target.fleetArrives(strength);
	}
	
	public String toString() {
		return "Fleet: Sending " + strength + " units to planet " + target.strength + ". " + turnsLeft + " turns left.";
	}
}

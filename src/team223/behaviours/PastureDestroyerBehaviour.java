package team223.behaviours;

import team223.AStar;
import team223.FastRandom;
import team223.MyConstants;
import team223.RobotBehaviour;
import team223.states.AttackEnemiesInSight;
import team223.states.Fleeing;
import team223.states.InterruptibleGotoLocation;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.MovementType;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public final class PastureDestroyerBehaviour extends RobotBehaviour {

	private static final boolean VERBOSE = false;

	private final FastRandom rnd;

	public PastureDestroyerBehaviour(final RobotController rc, FastRandom rnd , MapLocation enemyHQLocation) 
	{
		super(rc,enemyHQLocation);
		this.rnd=rnd;
	}

	@Override
	public void perform() throws GameActionException 
	{
		if ( ! rc.isActive() ) {
			return;
		}

		if ( state instanceof Fleeing ) 
		{
			state = state.perform();
			return;
		} 

		if ( rc.getHealth() < MyConstants.FLEE_HEALTH ) {
			state = new Fleeing( rc , rnd , enemyHQLocation );
			if ( MyConstants.DEBUG_MODE ) { behaviourStateChanged(); }			
			state = state.perform();
			return;
		}
		
		if ( state instanceof InterruptibleGotoLocation ) 
		{
			state = state.perform();
			if ( state == null ) 
			{
				// destination reached
				if ( VERBOSE ) System.out.println("Pasture destroyer is at "+rc.getLocation() );
				state = new AttackEnemiesInSight(rc);
				if ( MyConstants.DEBUG_MODE ) { behaviourStateChanged(); }
			}
			return;			
		}		

		if ( state instanceof AttackEnemiesInSight ) {
			state = state.perform();
			return;
		}

		if ( MyConstants.DEBUG_MODE ) System.out.println("Looking for pastures...");	

		final MapLocation[] pastrLocations = rc.sensePastrLocations( rc.getTeam().opponent() );
		if (pastrLocations.length == 0 ) {
			if ( MyConstants.DEBUG_MODE ) {
				System.out.println("No pastures found to destroy");
				rc.setIndicatorString( 1 , "No pastures to destroy" );
			}
			return;
		}

		if ( MyConstants.DEBUG_MODE ) System.out.println("Pastures found: "+pastrLocations.length);
		if ( pastrLocations.length == 0 ) {
			return;
		}

		final MapLocation myLocation = rc.getLocation();
		final MapLocation target = pickClosestTarget(myLocation,pastrLocations);
		if ( target == null ) {
			if ( MyConstants.DEBUG_MODE) System.out.println("No (more) pastures");
			return;
		}

		if ( rc.canAttackSquare( target ) ) {
			if ( MyConstants.DEBUG_MODE) System.out.println("Attacking pastures (enemies) in sight , available target: "+target);					
			state = new AttackEnemiesInSight(rc);
			if ( MyConstants.DEBUG_MODE ) { behaviourStateChanged(); }
			return;
		} 
		
		Direction d = myLocation.directionTo( target );
		if ( rc.canMove( d ) ) {
			int distanceSquared = myLocation.add( d ).distanceSquaredTo( target );
			if ( distanceSquared <= RobotType.SOLDIER.attackRadiusMaxSquared ) {
				if ( VERBOSE ) System.out.println("Sneaking to "+myLocation.add( d )+" of target "+target+" brings me to distance "+distanceSquared);							
				rc.sneak( d );
				state = new AttackEnemiesInSight(rc);
				if ( MyConstants.DEBUG_MODE ) { behaviourStateChanged(); }
				return;
			}
		}
		if ( VERBOSE ) System.out.println("Cannot attack "+target+" from "+myLocation+" , distance: "+myLocation.distanceSquaredTo( target )+" (max: "+RobotType.SOLDIER.attackRadiusMaxSquared+")");

		if ( MyConstants.DEBUG_MODE) {
			System.out.println("Calculating path to closest pasture "+target);
		}

		state = new InterruptibleGotoLocation( rc , MovementType.RUN , rnd , enemyHQLocation ) {

			@Override
			protected boolean hasArrivedAtDestination(MapLocation current, MapLocation dstLoc) {
				return current.distanceSquaredTo( dstLoc ) <= RobotType.SOLDIER.attackRadiusMaxSquared*0.5f;							
			}

			@Override
			public boolean setStartAndDestination(AStar finder) {
				finder.setRoute( rc.getLocation() , target );
				return true;
			}
		};
		if ( MyConstants.DEBUG_MODE ) behaviourStateChanged(); 
	}

	private MapLocation pickClosestTarget(MapLocation location,MapLocation[] pastrLocations) 
	{
		MapLocation closest = null;		
		int index = -1;
		int closestDistanceSqrt=-1;
		for (int i = 0; i < pastrLocations.length; i++) {
			MapLocation loc = pastrLocations[i];
			if ( loc != null) {
				int distSqrt = loc.distanceSquaredTo( location );
				if ( closest == null || distSqrt < closestDistanceSqrt ) {
					closestDistanceSqrt = distSqrt;
					closest = loc;
					index = i;
				}
			}
		}
		if ( closest != null ) {
			pastrLocations[index]=null;
			return closest;
		}			
		return null;
	}	
}
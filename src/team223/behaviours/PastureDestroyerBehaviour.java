package team223.behaviours;

import team223.AStar;
import team223.MyConstants;
import team223.RobotBehaviour;
import team223.Utils;
import team223.states.AttackEnemiesInSight;
import team223.states.InterruptibleGotoLocation;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.MovementType;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public final class PastureDestroyerBehaviour extends RobotBehaviour {

	public PastureDestroyerBehaviour(final RobotController rc) 
	{
		super(rc);
	}

	@Override
	public void perform() throws GameActionException 
	{
		if ( state instanceof InterruptibleGotoLocation ) 
		{
			state = state.perform();
			if ( state == null ) 
			{
				// destination reached
				if ( MyConstants.PASTR_DESTROYER_VERBOSE ) System.out.println("Pasture destroyer is at "+rc.getLocation() );
				state = new AttackEnemiesInSight(rc).perform();
				if ( MyConstants.DEBUG_MODE ) { behaviourStateChanged(); }
			}
			return;			
		}		

		if ( state instanceof AttackEnemiesInSight ) {
			state = state.perform();
			return;
		}

		final MapLocation[] pastrLocations = rc.sensePastrLocations( rc.getTeam().opponent() );

		if (pastrLocations.length == 0 ) {
			if ( MyConstants.DEBUG_MODE ) {
				rc.setIndicatorString( 1 , "No pastures to destroy" );
			}
			return;
		}

		final MapLocation myLocation = rc.getLocation();
		final MapLocation target = pickClosestTarget(rc,myLocation,pastrLocations);
		if ( target == null ) {
			if ( MyConstants.DEBUG_MODE) System.out.println("No (more) pastures");
			return;
		}

		if ( rc.canAttackSquare( target ) ) {
			if ( MyConstants.DEBUG_MODE) System.out.println("Attacking pastures (enemies) in sight , available target: "+target);					
			state = new AttackEnemiesInSight(rc).perform();
			if ( MyConstants.DEBUG_MODE ) { behaviourStateChanged(); }
			return;
		} 
		
		Direction d = myLocation.directionTo( target );
		if ( rc.canMove( d ) ) {
			int distanceSquared = myLocation.add( d ).distanceSquaredTo( target );
			if ( distanceSquared <= RobotType.SOLDIER.attackRadiusMaxSquared ) {
				if ( MyConstants.PASTR_DESTROYER_VERBOSE ) System.out.println("Sneaking to "+myLocation.add( d )+" of target "+target+" brings me to distance "+distanceSquared);
				while ( ! rc.isActive() ) {
					rc.yield();
				}
				rc.sneak( d );
				state = new AttackEnemiesInSight(rc).perform();
				if ( MyConstants.DEBUG_MODE ) { behaviourStateChanged(); }
				return;
			}
		}
		if ( MyConstants.PASTR_DESTROYER_VERBOSE ) System.out.println("Cannot attack "+target+" from "+myLocation+" , distance: "+myLocation.distanceSquaredTo( target )+" (max: "+RobotType.SOLDIER.attackRadiusMaxSquared+")");

		if ( MyConstants.DEBUG_MODE) {
			System.out.println("Calculating path to closest pasture "+target);
		}

		state = new InterruptibleGotoLocation( rc , MovementType.RUN , false ) {

			@Override
			protected boolean hasArrivedAtDestination(MapLocation current, MapLocation dstLoc) {
				return current.distanceSquaredTo( dstLoc ) <= RobotType.SOLDIER.attackRadiusMaxSquared*0.5f;							
			}

			@Override
			public boolean setStartAndDestination(boolean retry) 
			{
				if ( retry ) 
				{
					MapLocation[] pastrLocations = rc.sensePastrLocations( rc.getTeam().opponent() );
					if ( pastrLocations.length != 0 ) {
						MapLocation newTarget = pickClosestTarget(rc,myLocation,pastrLocations);
						if ( newTarget != null ) {
							AStar.setRoute( rc.getLocation() , newTarget , MyConstants.PASTURE_DESTROYER_PATH_FINDING_TIMEOUT_ROUNDS );
							return true;
						}
					}
					return false;
				} else {
					AStar.setRoute( rc.getLocation() , target , MyConstants.PASTURE_DESTROYER_PATH_FINDING_TIMEOUT_ROUNDS );
				} 
				return true;
			}

			@Override
			public boolean abortOnTimeout() {
				return true;
			}
		}.perform();
		if ( MyConstants.DEBUG_MODE ) behaviourStateChanged(); 
	}

	private MapLocation pickClosestTarget(RobotController rc,MapLocation location,MapLocation[] pastrLocations) 
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
			MapLocation result = Utils.findRandomLocationNear( rc , closest, 1 , MyConstants.SOLDIER_ATTACK_RANGE );
			if ( result != null ) {
				return result;
			}
			return closest;
		}			
		return null;
	}	
}
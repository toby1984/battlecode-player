package team223.behaviours;

import team223.*;
import team223.states.Attacking;
import team223.states.Fleeing;
import battlecode.common.*;

public class CowboyBehaviour extends RobotBehaviour {

	private final FastRandom rnd;
	
	private int movementFailures=0;
	private Direction generalDirection;
	
	public CowboyBehaviour(RobotController rc,FastRandom rnd) {
		this.rnd=rnd;
		final Direction[] candidates = Utils.getMovementCandidateDirections( rc );
		generalDirection = candidates[ rnd.nextInt( candidates.length ) ];
	}

	@Override
	public void perform(RobotController rc) throws GameActionException 
	{
		if ( ! rc.isActive() || rc.getType() == RobotType.PASTR ) {
			return;
		}
		
		if ( state instanceof Fleeing ) {
			state = state.perform(rc);
			return;
		}
		
		if ( rc.getHealth() < 50 ) {
			state = new Fleeing(rnd);
			if ( MyConstants.DEBUG_MODE ) { changedBehaviour(rc); }
			state = state.perform( rc );
			return;
		}

		if ( state instanceof Attacking ) 
		{
			state = state.perform( rc );
			return;
		}

		Robot closestEnemy = Utils.findClosestEnemy( rc , Utils.findEnemies( rc , 5 ) );
		if ( closestEnemy != null ) 
		{
			if ( MyConstants.DEBUG_MODE) System.out.println("Cowbow is attacking "+closestEnemy.getID());
			state = new Attacking(closestEnemy);
			if ( MyConstants.DEBUG_MODE ) { changedBehaviour(rc); }
			state = state.perform( rc );
			return;
		}		
		
		// count sheeps,eh,cows
		double cowCount = 0;
		MapLocation myLoc = rc.getLocation();
		for ( int i = 0 ; i < Utils.DIRECTIONS.length ; i++ ) {
			cowCount += rc.senseCowsAtLocation( myLoc.add( Utils.DIRECTIONS[i] ) );
		}
		cowCount += rc.senseCowsAtLocation( myLoc );
		
		if ( cowCount > MyConstants.MIN_COWS_FOR_PASTURE ) {
			
			Robot[] friendlies = rc.senseNearbyGameObjects(Robot.class , GameConstants.PASTR_RANGE*GameConstants.PASTR_RANGE , rc.getTeam() );
			boolean construct = true;
			for (int i = 0; i < friendlies.length; i++) {
				Robot r = friendlies[i];
				RobotInfo ri = rc.senseRobotInfo( r );
				if ( ri.type == RobotType.PASTR || ri.isConstructing ) {
					construct = false;
					break;
				}
			}
			
			if ( construct ) {
				if ( MyConstants.DEBUG_MODE) System.out.println("Starting PASTR construction");
				rc.construct( RobotType.PASTR );
				return;
			}
		}
		
		// wander in general direction
		Direction d = generalDirection;
		if ( ! rc.canMove( generalDirection ) ) {
			d = Utils.randomMovementDirection(rnd,rc);
		} else {
			movementFailures++;
		}
		
		if ( d != Direction.NONE ) {
			if ( movementFailures > 9 ) {
				generalDirection=d;
			}
			rc.sneak(d);
			movementFailures=0;
		} else {
			movementFailures++;
		}
	}

	@Override
	public String toString() {
		return "Cowboy";
	}
}
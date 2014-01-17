package team223.behaviours;

import team223.AStar;
import team223.FastRandom;
import team223.MyConstants;
import team223.RobotBehaviour;
import team223.Utils;
import team223.states.Attacking;
import team223.states.Fleeing;
import team223.states.InterruptibleGotoLocation;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.MovementType;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public final class DestroyerBehaviour extends RobotBehaviour {

	private final FastRandom rnd;
	
	protected static final int UNKNOWN_HEALTH= -99999;
	
	public DestroyerBehaviour(final RobotController rc,FastRandom random,MapLocation enemyHQLocation) {
		super(rc,enemyHQLocation);
		this.rnd = random;
	}
	
	@Override
	public void perform() throws GameActionException {

		if ( ! rc.isActive() ) {
			return;
		}
		
		if ( state instanceof Fleeing ) {
			state = state.perform();
			return;
		} 
		
		Robot[] enemies=null;
		if ( rc.getHealth() < MyConstants.FLEE_HEALTH ) 
		{
			enemies = Utils.findEnemies( rc , RobotType.SOLDIER.attackRadiusMaxSquared );
			if ( Utils.getEstimatedHealOfThreats( rc , enemies ) >= rc.getHealth() ) 
			{ 
				state = new Fleeing( rc , rnd );
				if ( MyConstants.DEBUG_MODE ) { behaviourStateChanged(); }
				state = state.perform();
				return;
			}
		}
		
		if ( state instanceof Attacking ) {
			state = state.perform();
			return;
		}
		
		if ( enemies == null ) {
			enemies = Utils.findEnemies(rc , MyConstants.SOLDIER_SEEK_ENEMY_RANGE_SQUARED);
		}
		
		Robot closestEnemy = Utils.findClosestEnemy( rc , enemies);
		if ( closestEnemy != null ) 
		{
			state = new Attacking(rc,closestEnemy , enemyHQLocation );
			if ( MyConstants.DEBUG_MODE ) { behaviourStateChanged(); }
			state = state.perform();
			return;
		}
		
		if ( state instanceof InterruptibleGotoLocation ) {
			state = state.perform();
			return;
		}		
		
		// home-in on enemy HQ
		if ( enemyHQLocation.distanceSquaredTo( rc.getLocation() ) > MyConstants.SOLIDER_HOMEIN_ON_HQ_DISTANCE_SQUARED ) 
		{
			state = new InterruptibleGotoLocation(rc , MovementType.RUN,rnd) {

				@Override
				public boolean isOccupied(MapLocation loc) throws GameActionException {
					return rc.canSenseSquare( loc ) ? rc.senseObjectAtLocation( loc ) != null : false;							
				}

				@Override
				protected boolean hasArrivedAtDestination(MapLocation current, MapLocation dstLoc) {
					final int dx = Math.abs( dstLoc.x - current.x );
					final int dy = Math.abs( dstLoc.y - current.y );
					return dx <= 1 && dy <= 1;					
				}

				@Override
				public boolean setStartAndDestination(AStar finder) 
				{
					MapLocation dst = Utils.findRandomLocationNear( rc , enemyHQLocation , 
							MyConstants.ENEMY_HQ_SAFE_DISTANCE ,  
							MyConstants.ENEMY_HQ_SAFE_DISTANCE*2, rnd );
					if ( dst != null ) {
						finder.setRoute( rc.getLocation() , dst );
						return true;
					}
					return false;
				}
			};
			
			if ( MyConstants.DEBUG_MODE ) { behaviourStateChanged(); }
			state = state.perform();
		} 
		else 
		{
			Direction d = Utils.randomMovementDirection(rnd,rc);
			if ( d != Direction.NONE ) {
				if ( rc.getLocation().add( d ).distanceSquaredTo( enemyHQLocation ) > RobotType.HQ.attackRadiusMaxSquared ) {
					rc.move(d);
				}
			}
		}
	}
	
	@Override
	public String toString() {
		return "Destroyer";
	}
}
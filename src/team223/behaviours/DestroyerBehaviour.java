package team223.behaviours;

import java.util.List;

import team223.*;
import team223.states.Attacking;
import team223.states.Fleeing;
import team223.states.GotoLocation;
import battlecode.common.*;

public class DestroyerBehaviour extends RobotBehaviour {

	private final FastRandom rnd;
	
	private final AStar finder;
	
	public DestroyerBehaviour(final RobotController rc,FastRandom random,MapLocation enemyHQLocation) {
		super(rc,enemyHQLocation);
		this.rnd = random;
    	this.finder = new AStar(rc) 
    	{
			@Override
			protected boolean isCloseEnoughToTarget(team223.AStar.PathNode<MapLocation> node) 
			{
				return hasArrivedAtDestination( node.value , destination );
			}

			@Override
			public boolean isOccupied(MapLocation loc) throws GameActionException {
				return false;
			}
		};		
	}
	
	@Override
	public void perform() throws GameActionException {

		if ( ! rc.isActive() ) {
			return;
		}
		
		if ( state instanceof Fleeing ) {
			state = state.perform( rc );
			return;
		} 
		
		Robot[] enemies=null;
		if ( rc.getHealth() < MyConstants.FLEE_HEALTH ) 
		{
			enemies = Utils.findEnemies( rc , RobotType.SOLDIER.attackRadiusMaxSquared );
			if ( Utils.getEstimatedHealOfThreats( rc , enemies ) >= rc.getHealth() ) 
			{ 
				state = new Fleeing( rnd );
				if ( MyConstants.DEBUG_MODE ) { changedBehaviour(rc); }
				state = state.perform( rc );
				return;
			}
		}
		
		if ( state instanceof Attacking ) {
			state = state.perform( rc );
			return;
		}
		
		if ( enemies == null ) {
			enemies = Utils.findEnemies(rc , MyConstants.SOLDIER_SEEK_ENEMY_RANGE_SQUARED);
		}
		
		Robot closestEnemy = Utils.findClosestEnemy( rc , enemies);
		if ( closestEnemy != null ) 
		{
			state = new Attacking(closestEnemy , enemyHQLocation );
			if ( MyConstants.DEBUG_MODE ) { changedBehaviour(rc); }
			state = state.perform( rc );
			return;
		}
		
		if ( state instanceof GotoLocation ) {
			state = state.perform( rc );
			return;
		}		
		
		// home-in on enemy HQ
		if ( enemyHQLocation.distanceSquaredTo( rc.getLocation() ) > MyConstants.SOLIDER_HOMEIN_ON_HQ_DISTANCE_SQUARED ) 
		{
			List<MapLocation> path = findPathToHQ( rc );
			if ( path != null ) {
				state = new GotoLocation( path , MovementType.RUN) {

					@Override
					protected List<MapLocation> recalculatePath(RobotController rc) throws GameActionException {
						
						return findPathToHQ(rc);
					}

					@Override
					protected boolean hasArrivedAtDestination(MapLocation current, MapLocation dstLoc) 
					{
						return DestroyerBehaviour.this.hasArrivedAtDestination( current,dstLoc );
					}
				};
				if ( MyConstants.DEBUG_MODE ) { changedBehaviour(rc); }
				state = state.perform(rc);
				return;
			} 
			if ( MyConstants.DEBUG_MODE ) System.out.println("ERROR: No path to HQ ?");
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
	
	protected boolean hasArrivedAtDestination(MapLocation current,MapLocation dstLoc) 
	{
		final int dx = Math.abs( dstLoc.x - current.x );
		final int dy = Math.abs( dstLoc.y - current.y );
		return dx <= 1 && dy <= 1;
	}
	
	protected List<MapLocation> findPathToHQ(RobotController rc) throws GameActionException {
		MapLocation dst = Utils.findRandomLocationNear( rc , enemyHQLocation , 
				MyConstants.ENEMY_HQ_SAFE_DISTANCE ,  
				MyConstants.ENEMY_HQ_SAFE_DISTANCE*2, rnd );
		if ( dst != null ) {
			return finder.findPath( rc.getLocation() , dst );
		}
		return null;
	}

	@Override
	public String toString() {
		return "Destroyer";
	}
}
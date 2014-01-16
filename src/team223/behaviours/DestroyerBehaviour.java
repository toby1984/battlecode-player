package team223.behaviours;

import java.util.List;

import team223.*;
import team223.states.Attacking;
import team223.states.Fleeing;
import team223.states.GotoLocation;
import battlecode.common.*;

public class DestroyerBehaviour extends RobotBehaviour {

	private final FastRandom random;
	
	public DestroyerBehaviour(FastRandom random) {
		this.random = random;
	}
	
	@Override
	public void perform(RobotController rc) throws GameActionException {

		if ( ! rc.isActive() ) {
			return;
		}
		
		if ( state instanceof Fleeing ) {
			state = state.perform( rc );
			return;
		} 
		
		Robot[] enemies=null;
		if ( rc.getHealth() < 50 ) 
		{
			state = new Fleeing( random );
			if ( MyConstants.DEBUG_MODE ) { changedBehaviour(rc); }
			state = state.perform( rc );
			return;
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
			state = new Attacking(closestEnemy);
			if ( MyConstants.DEBUG_MODE ) { changedBehaviour(rc); }
			state = state.perform( rc );
			return;
		}
		
		if ( state instanceof GotoLocation ) {
			state = state.perform( rc );
			return;
		}		
		
		// home-in on enemy HQ
		final MapLocation enemyHQLocation = rc.senseEnemyHQLocation();
		if ( enemyHQLocation.distanceSquaredTo( rc.getLocation() ) > MyConstants.SOLIDER_HOMEIN_ON_HQ_DISTANCE_SQUARED ) 
		{
			PathInfo info = new PathInfo( findPathToHQ( rc ) );
			if ( info.path != null ) {
				state = new GotoLocation(info,MovementType.RUN) {

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
		} else {
			wander(rc);
		}
	}
	
	private void wander(RobotController rc) throws GameActionException {
		Direction d = Utils.randomMovementDirection(random,rc);
		if ( d != Direction.NONE ) {
			rc.move(d);
		}
	}
	
	protected boolean hasArrivedAtDestination(MapLocation current,MapLocation dstLoc) 
	{
		final int dx = Math.abs( dstLoc.x - current.x );
		final int dy = Math.abs( dstLoc.y - current.y );
		return dx <= 1 && dy <= 1;
	}
	
	protected List<MapLocation> findPathToHQ(RobotController rc) throws GameActionException {
		final MapLocation enemyHQLocation = rc.senseEnemyHQLocation();			
		MapLocation dst = Utils.findRandomLocationNear( rc , enemyHQLocation , 
				MyConstants.ENEMY_HQ_SAFE_DISTANCE ,  
				MyConstants.ENEMY_HQ_SAFE_DISTANCE*2, random );
		if ( dst != null ) {
			return findPath( rc , rc.getLocation() , dst );
		}
		return null;
	}

    protected List<MapLocation> findPath(final RobotController rc,final MapLocation startLoc,final MapLocation dstLoc) throws GameActionException {
    	
    	final MapLocationAStar pathFinder = new MapLocationAStar(startLoc,dstLoc) 
    	{
			@Override
			protected boolean isCloseEnoughToTarget(team223.AStar.PathNode<MapLocation> node) 
			{
				return hasArrivedAtDestination( node.value , destination );
			}

			@Override
			public TerrainTile senseTerrainTile(MapLocation loc) {
				return rc.senseTerrainTile( loc );
			}

			@Override
			public boolean isOccupied(MapLocation loc) throws GameActionException {
				return false;
			}
		};
		return Utils.findPath( pathFinder );
    }		
	@Override
	public String toString() {
		return "Destroyer";
	}
}
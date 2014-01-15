package team223.states;

import java.util.List;

import team223.FastRandom;
import team223.MapLocationAStar;
import team223.MyConstants;
import team223.PathInfo;
import team223.State;
import team223.Utils;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public class ApproachEnemyHQ extends State {

	private PathInfo pathInfo = null;
	
	private final FastRandom rnd;
	
	public ApproachEnemyHQ(FastRandom rnd) {
		this.rnd = rnd;
	}
	
	@Override
	public State perform(RobotController rc) throws GameActionException 
	{
		MapLocation next = pathInfo != null ? pathInfo.getStepAfter( rc.getLocation() ) : null;
		if ( next == null ) 
		{
			final MapLocation enemyHQLocation = rc.senseEnemyHQLocation();			
			MapLocation dst = Utils.findRandomLocationNear( rc , enemyHQLocation , MyConstants.ENEMY_HQ_SAFE_DISTANCE ,  MyConstants.ENEMY_HQ_SAFE_DISTANCE*2, rnd );
			if ( dst != null ) {
				List<MapLocation> path = findPath( rc , rc.getLocation() , dst );
				if ( path != null && path.size() >= 2 ) 
				{
					pathInfo = new PathInfo( path );
					next = pathInfo.path.get(1);
				} else {
					if ( MyConstants.DEBUG_MODE ) if ( MyConstants.DEBUG_MODE) System.out.println("ERROR: Failed to find location near enemy HQ ?");					
				}
			} else {
				if ( MyConstants.DEBUG_MODE ) if ( MyConstants.DEBUG_MODE) System.out.println("Failed to find location near enemy HQ ?");
			}
		}
		if ( next != null )
		{
			if ( hasArrivedAtDestination( rc.getLocation() , pathInfo.end() ) ) {
				return null;
			}
			
			Direction direction = rc.getLocation().directionTo( next );
			if ( direction == Direction.OMNI ) {
				return null;
			}
			if ( rc.canMove( direction ) ) 
			{
				rc.move( direction );
			} 
		}	
		return this;
	}
	
	protected static boolean hasArrivedAtDestination(MapLocation current,MapLocation dstLoc) 
	{
		final int dx = Math.abs( dstLoc.x - current.x );
		final int dy = Math.abs( dstLoc.y - current.y );
		return dx <= 1 && dy <= 1;
	}

    protected static List<MapLocation> findPath(final RobotController rc,final MapLocation startLoc,final MapLocation dstLoc) throws GameActionException {
    	
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
}
package myplayer.states;

import java.util.List;

import myplayer.MapLocationAStar;
import myplayer.PathInfo;
import myplayer.State;
import myplayer.Utils;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public class MoveToEnemyHQ extends State {

	private PathInfo pathInfo = null;
	
	@Override
	public State perform(RobotController rc) throws GameActionException 
	{
		MapLocation next = pathInfo != null ? pathInfo.getStepAfter( rc.getLocation() ) : null;
		if ( next == null ) 
		{
			final MapLocation enemyHQLocation = rc.senseEnemyHQLocation();			
			System.out.println("Looking for path from "+rc.getLocation()+" to "+enemyHQLocation);		
			
			MapLocation dst = Utils.findRandomLocationNear( rc , enemyHQLocation , 5 );
			if ( dst != null ) {
				List<MapLocation> path = findPath( rc , rc.getLocation() , dst );
				if ( path != null && path.size() >= 2 ) 
				{
					pathInfo = new PathInfo( path );
					System.out.println("Got path "+pathInfo.path+" from "+rc.getLocation()+" to "+enemyHQLocation);				
					next = pathInfo.path.get(1);
				}
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

    protected static List<MapLocation> findPath(final RobotController rc,final MapLocation startLoc,final MapLocation dstLoc) {
    	
    	final MapLocationAStar pathFinder = new MapLocationAStar() 
    	{
			@Override
			protected boolean isCloseEnoughToTarget(myplayer.AStar.PathNode<MapLocation> node) 
			{
				return hasArrivedAtDestination( node.value , dstLoc );
			}

			@Override
			public TerrainTile senseTerrainTile(MapLocation loc) {
				return rc.senseTerrainTile( loc );
			}
		};
		pathFinder.setStart( startLoc );
		pathFinder.setDestination( dstLoc );
		return Utils.findPath( pathFinder );
    }	
}
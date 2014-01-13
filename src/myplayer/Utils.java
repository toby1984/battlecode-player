package myplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import myplayer.AStar.PathNode;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.TerrainTile;

public class Utils {

	public static final FastRandom rnd = new FastRandom(0xdeadbeef);
	
	public static final Direction[] DIRECTIONS = {Direction.NORTH, 
		Direction.NORTH_EAST, 
		Direction.EAST, 
		Direction.SOUTH_EAST, 
		Direction.SOUTH, 
		Direction.SOUTH_WEST, 
		Direction.WEST, 
		Direction.NORTH_WEST};
	
	public static MapLocation findRandomLocationNear(RobotController rc,MapLocation loc,int radius) {
		
		final int minX = Math.max(0, loc.x - radius );
		final int minY = Math.max(0, loc.y - radius );
		
		final int maxX = Math.min( rc.getMapWidth()  , loc.x + radius );
		final int maxY = Math.min( rc.getMapHeight() , loc.y + radius );

		final int dx = maxX - minX;
		final int dy = maxY - minY;
		
		for ( int retries = 10 ; retries > 0 ; retries-- ) 
		{
			int x = minX+(int) (dx*rnd.nextFloat());
			int y = minY+(int) (dy*rnd.nextFloat());
			final MapLocation l = new MapLocation(x,y);
			TerrainTile tileType = rc.senseTerrainTile( l );
			
			if (  tileType == TerrainTile.NORMAL || tileType == TerrainTile.ROAD ) {
				return l;
			}
		}
		return null;
	}
	
	public static Direction getDirection(int dx,int dy) 
	{
		for ( Direction d : Direction.values() ) 
		{
			if ( d.dx == dx && d.dy == dy ) {
				return d;
			}
		}		
		return Direction.NONE;
	}
	
	public static Direction randomDirection() {
		return DIRECTIONS[ rnd.nextInt( DIRECTIONS.length ) ];
	}	
	
	public static List<MapLocation> findPath(MapLocationAStar pathFinder) 
	{
		PathNode<MapLocation> node = pathFinder.findPath();
		if ( node == null ) {
			return null;
		}
		
		List<MapLocation> path = new ArrayList<MapLocation>();
		do {
			path.add( node.value );
			node = node.parent;
		} while ( node != null );
		Collections.reverse( path );
		return path;		
	}
	
	public static final Robot findClosestEnemy(RobotController rc,int distanceSquared) throws GameActionException 
	{
		final Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class,distanceSquared,rc.getTeam().opponent());		
		
		Robot closestRobot=null;
		int closestDistanceSquared=0;

		MapLocation myLocation = rc.getLocation();
		for ( Robot enemy : nearbyEnemies ) 
		{
			RobotInfo robotInfo = rc.senseRobotInfo( enemy );
			if (isEnemyTarget( robotInfo ) ) 
			{
				int distance = robotInfo.location.distanceSquaredTo( myLocation );
				if ( closestRobot == null || distance < closestDistanceSquared ) {
					closestRobot = enemy;
					closestDistanceSquared = distance;
				}
			}
		}
		return closestRobot;
	}

	public static boolean isEnemyTarget(RobotInfo ri) 
	{
		switch(ri.type) {
			case PASTR:
			case SOLDIER:
				return true;
			default:
				return false;
		}
	}	
}

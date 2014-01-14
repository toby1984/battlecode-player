package lowflyingcows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lowflyingcows.AStar.PathNode;
import battlecode.common.*;

public class Utils {

	public static final Direction[] DIRECTIONS = {Direction.NORTH, 
		Direction.NORTH_EAST, 
		Direction.EAST, 
		Direction.SOUTH_EAST, 
		Direction.SOUTH, 
		Direction.SOUTH_WEST, 
		Direction.WEST, 
		Direction.NORTH_WEST};
	
	public static MapLocation findRandomLocationNear(RobotController rc,MapLocation loc,int minRadius,int maxRadius,FastRandom rnd) {
		
		final int minSquared = minRadius*minRadius;
		final int maxSquared = maxRadius*maxRadius;
		
		final int minX = Math.max(0, loc.x - maxRadius );
		final int minY = Math.max(0, loc.y - maxRadius );
		
		final int maxX = Math.min( rc.getMapWidth()  , loc.x + maxRadius );
		final int maxY = Math.min( rc.getMapHeight() , loc.y + maxRadius );

		final int dx = maxX - minX;
		final int dy = maxY - minY;
		
		for ( int retries = 10 ; retries > 0 ; retries-- ) 
		{
			int x = minX+(int) (dx*rnd.nextFloat());
			int y = minY+(int) (dy*rnd.nextFloat());
			float distX = x-loc.x;
			float distY = y-loc.y;
			float distSquared = distX*distX+distY*distY;
			if ( distSquared >= minSquared && distSquared <= maxSquared) 
			{
				final MapLocation l = new MapLocation(x,y);
				TerrainTile tileType = rc.senseTerrainTile( l );
				
				if (  tileType == TerrainTile.NORMAL || tileType == TerrainTile.ROAD ) {
					return l;
				}
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
	
	public static Direction randomDirection(FastRandom rnd) {
		return DIRECTIONS[ rnd.nextInt( DIRECTIONS.length ) ];
	}
	
	public static Direction randomMovementDirection(FastRandom rnd,RobotController rc) 
	{
		Direction d = DIRECTIONS[ rnd.nextInt( DIRECTIONS.length ) ];		
		for ( int retries = 7 ; retries > 0 ; retries-- ) {
			if ( rc.canMove( d ) ) {
				return d;
			}
			d = d.rotateLeft();
		}
		return Direction.NONE;
	}		
	
	public static Direction[] getMovementCandidateDirections(RobotController rc) 
	{
		MapLocation location = rc.getLocation();
		
		int wThird = rc.getMapWidth() / 3;
		int hThird = rc.getMapHeight() / 3;
		
		int locX = location.x / wThird;
		int locY = location.y / hThird;
	
		/*
		 * +--------------+--------------+------------+
		 * |E,SE,S        |E,SE,S,SW,W   |S,SW,W      |
		 * +--------------+--------------+------------+
		 * |N,NE,E,SE,S   | DIRECTIONS   |N,S,SW,W,NW |
		 * +--------------+--------------+------------+
		 * |N,NE,E        | N,NE,E,W,NW  |N,W,NW      | 
		 * +--------------+--------------+------------+ 
		 * 
		 */
		switch(locY) {
			case 0:
				switch(locX) {
					case 0:
						return new Direction[] { Direction.EAST , Direction.SOUTH_EAST , Direction.SOUTH };
					case 1:
						return new Direction[] { Direction.EAST , Direction.SOUTH_EAST , Direction.SOUTH , Direction.SOUTH_WEST , Direction.WEST };
					case 2:
						return new Direction[] { Direction.SOUTH , Direction.SOUTH_WEST , Direction.WEST };
				}
				break;
			case 1:
				switch(locX) {
					case 0:
						return new Direction[] { Direction.NORTH , Direction.NORTH_EAST , Direction.EAST , Direction.SOUTH_EAST , Direction.SOUTH };
					case 1:
						return DIRECTIONS;
					case 2:
						return new Direction[] { Direction.NORTH , Direction.SOUTH , Direction.SOUTH_WEST , Direction.WEST , Direction.NORTH_WEST };
				}
				break;
			case 2:
				switch(locX) {
					case 0:
						return new Direction[] { Direction.NORTH , Direction.NORTH_EAST , Direction.EAST };
					case 1:
						return new Direction[] { Direction.NORTH , Direction.NORTH_EAST , Direction.EAST , Direction.WEST , Direction.NORTH_WEST };
					case 2:
						return new Direction[] { Direction.NORTH , Direction.WEST , Direction.NORTH_WEST };
				}				
				break;
		}
		throw new RuntimeException("Unreachable code reached");
	}
	
	public static List<MapLocation> findPath(MapLocationAStar pathFinder) throws GameActionException 
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
	
	public static final Robot[] findEnemies(RobotController rc,int distanceSquared) {
		return rc.senseNearbyGameObjects(Robot.class,distanceSquared,rc.getTeam().opponent());			
	}
	
	public static final Robot findClosestEnemy(RobotController rc,Robot[] nearbyEnemies) throws GameActionException 
	{
		Robot closestRobot=null;
		int closestDistanceSquared=0;

		MapLocation myLocation = rc.getLocation();
		for ( Robot enemy : nearbyEnemies ) 
		{
			RobotInfo robotInfo = rc.senseRobotInfo( enemy );
			if (isAttackTarget( robotInfo ) ) 
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
	
	public static MapLocation getCenterOfMass(RobotController rc,Robot[] enemies) throws GameActionException 
	{
		float mx = 0;
		float my = 0;
		int count =0;
		for ( int i = 0 ; i < enemies.length ; i++ ) 
		{
			RobotInfo info = rc.senseRobotInfo( enemies[i] );
			if ( info.type == RobotType.SOLDIER || info.type == RobotType.HQ ) {
				mx += info.location.x;
				my += info.location.y;
				count++;
			}
		}
		if ( count == 0 ) {
			return null;
		}
		return new MapLocation( (int) Math.round( mx/count) , (int) Math.round( my / count) );		
	}
	
	public static List<Robot> getThreats(RobotController rc,Robot[] enemies) throws GameActionException {
		
		List<Robot> result = new ArrayList<Robot>();
		for ( int i = 0 ; i < enemies.length ; i++ ) 
		{
			Robot r = enemies[i];
			RobotInfo info = rc.senseRobotInfo( r );
			if ( info.type == RobotType.SOLDIER ) {
				result.add( r );
			}
		}
		return result;
	}

	public static boolean isAttackTarget(RobotInfo ri) 
	{
		switch(ri.type) 
		{
			case PASTR:
			case SOLDIER:
				return true;
			default:
				return false;
		}
	}	
}
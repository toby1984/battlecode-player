package team223;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class Utils {

	public static final Direction[] DIRECTIONS = {
		Direction.SOUTH, 		
		Direction.NORTH, 
		Direction.SOUTH_EAST, 
		Direction.WEST,
		Direction.NORTH_EAST,
		Direction.EAST, 			
		Direction.NORTH_WEST,
		Direction.SOUTH_WEST, 		
		};

	public static MapLocation findRandomLocationNear(RobotController rc,MapLocation center,int minRadius,int maxRadius) {

		final int delta = (int) Math.sqrt(maxRadius-minRadius);
		int retries = 10;
		do
		{
			int x = minRadius+RobotPlayer.rnd.nextInt(delta);
			int y = minRadius+RobotPlayer.rnd.nextInt(delta);
			
			/*
			 *  0 | 1
			 * ---+---
			 *  3 | 2
			 * 
			 */
			final MapLocation l;
			switch( RobotPlayer.rnd.nextInt(4) ) 
			{
				case 0: // Quadrant 0
					l = new MapLocation( center.x - x,center.y - y);
					break;
				case 1: // Quadrant 1
					l = new MapLocation(center.x + x,center.y - y);
					break;
				case 2: // Quadrant 2
					l = new MapLocation(center.x + x, center.y + y);
					break;
				case 3: // Quadrant 3
					l = new MapLocation(center.x - x,-center.y + y);
					break;
				default:
					throw new RuntimeException("Unreachable code reached");
			}

			switch( rc.senseTerrainTile( l ) ) 
			{
				case NORMAL:
				case ROAD:
					int dist = (int) Math.sqrt( l.distanceSquaredTo( center ) );
					if ( dist < minRadius || dist > maxRadius ) {
						throw new RuntimeException("Result out of range ( "+minRadius+" , "+maxRadius+"): center="+center+",result="+l+",distance="+dist);
					}
					return l;
				default:
			}
		} while( retries-- > 0 );
		if ( MyConstants.DEBUG_MODE ) System.out.println(">>>>>>>>>>>>>>>> Failed to find random location around ("+center+") , minRadius: "+minRadius+",maxRadius: "+maxRadius+" after 10 retries");
		return null;
	}

	public static Direction randomDirection() {
		return DIRECTIONS[ RobotPlayer.rnd.nextInt( DIRECTIONS.length ) ];
	}

	public static Direction randomMovementDirection(RobotController rc) 
	{
		Direction d = DIRECTIONS[ RobotPlayer.rnd.nextInt( DIRECTIONS.length ) ];
		MapLocation myLocation = rc.getLocation();
		for ( int retries = 7 ; retries > 0 ; retries-- ) {
			if ( rc.canMove( d ) && myLocation.add( d ).distanceSquaredTo( RobotPlayer.enemyHQ ) > RobotType.HQ.attackRadiusMaxSquared  ) {
				return d;
			}
			d = d.rotateLeft();
		}
		return Direction.NONE;
	}	

	public static <T> void shuffle(T[] arr) 
	{
		for (int i=arr.length; i>1; i--) {
			int i1 = i-1;
			int j = RobotPlayer.rnd.nextInt(i);
			T tmp = arr[i1];
			arr[i1] = arr[j];
			arr[j] = tmp;
		}
	}
	
	public static Direction[] getMovementCandidateDirections(RobotController rc) 
	{
		MapLocation location = rc.getLocation();

		int wThird = (int) Math.ceil( rc.getMapWidth() / 3.0f );
		int hThird = (int) Math.ceil( rc.getMapHeight() / 3.0f );

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
		throw new RuntimeException("Unreachable code reached, map size: "+rc.getMapWidth()+"x"+rc.getMapHeight()+" , location: "+location+" , x: "+locX+" , y: "+locY);
	}

	public static final class RobotAndInfo 
	{
		public final Robot robot;
		public final RobotInfo info;
		
		public final int robotId;
		
		public RobotAndInfo(Robot robot, RobotInfo info) {
			this.robot = robot;
			this.info = info;
			this.robotId = robot.getID();
		}

		public boolean isWithinAttackRange(MapLocation myLocation,RobotType myType) {
			return info.location.distanceSquaredTo( myLocation ) <= myType.attackRadiusMaxSquared;
		}
		
		@Override
		public String toString() 
		{
			return "Robot #"+robot.getID()+" ("+info.type+" , health "+info.health+" )";
		}

		@Override
		public int hashCode() 
		{
			return robotId;
		}

		@Override
		public boolean equals(Object obj) {
			if ( obj instanceof RobotAndInfo) {
				return ((RobotAndInfo) obj).robotId == this.robotId;
			}
			return false;
		}
	}
	
	public static final RobotAndInfo pickEnemyToAttack(RobotController rc,Robot[] nearbyEnemies,EnemyBlacklist enemyBlacklist) throws GameActionException 
	{
		RobotAndInfo primaryRobot=null;
		int primaryDistanceSquared=0;
		
		RobotAndInfo secondaryRobot=null;
		int secondaryDistanceSquared=0;		
		
		RobotAndInfo ternaryRobot=null;
		int ternaryDistanceSquared=0;			

		final MapLocation myLocation = rc.getLocation();
		final int myAttackRange = rc.getType().attackRadiusMaxSquared;

		for ( int i = 0 ; i < nearbyEnemies.length ; i++)
		{
			Robot enemy = nearbyEnemies[i];
			if ( enemyBlacklist != null && enemyBlacklist.containsKey( enemy.getID() ) ) {
				if ( MyConstants.DEBUG_MODE ) { System.out.println("Ignoring blacklisted enemy "+enemyBlacklist.get( enemy.getID() ) ); }
				continue;
			}
			RobotInfo robotInfo = rc.senseRobotInfo( enemy );
			switch( robotInfo.type ) 
			{
				case SOLDIER:
					int distance = robotInfo.location.distanceSquaredTo( myLocation );
					if ( primaryRobot == null ) 
					{
						primaryRobot = new RobotAndInfo(enemy,robotInfo);
						primaryDistanceSquared = distance;					
					} 
					else if ( distance < primaryDistanceSquared ) 
					{
						boolean inRange1 = ( primaryDistanceSquared <= myAttackRange );
						boolean inRange2 = ( distance <= myAttackRange );
						
						if ( inRange1 && inRange2 ) 
						{
							if ( robotInfo.health <= primaryRobot.info.health ) {
								primaryRobot = new RobotAndInfo(enemy,robotInfo);
								primaryDistanceSquared = distance;								
							}
						} else {
							primaryRobot = new RobotAndInfo(enemy,robotInfo);
							primaryDistanceSquared = distance;
						}						
					}
					break;
				case PASTR:
					distance = robotInfo.location.distanceSquaredTo( myLocation );
					if ( secondaryRobot == null ) 
					{
						secondaryRobot = new RobotAndInfo(enemy,robotInfo);
						secondaryDistanceSquared = distance;
					} 
					else if ( distance < secondaryDistanceSquared ) 
					{
						boolean inRange1 = ( secondaryDistanceSquared <= myAttackRange );
						boolean inRange2 = ( distance <= myAttackRange );
						
						if ( inRange1 && inRange2 ) 
						{
							if ( robotInfo.health <= secondaryRobot.info.health ) {
								secondaryRobot = new RobotAndInfo(enemy,robotInfo);
								secondaryDistanceSquared = distance;								
							}
						} else {
							secondaryRobot = new RobotAndInfo(enemy,robotInfo);
							secondaryDistanceSquared = distance;
						}
					}
					break;
				case NOISETOWER:
					distance = robotInfo.location.distanceSquaredTo( myLocation );
					if ( ternaryRobot == null ) 
					{
						ternaryRobot = new RobotAndInfo(enemy,robotInfo);
						ternaryDistanceSquared = distance;
					} 
					else if ( distance < ternaryDistanceSquared ) 
					{
						boolean inRange1 = ( ternaryDistanceSquared <= myAttackRange );
						boolean inRange2 = ( distance <= myAttackRange );
						
						if ( inRange1 && inRange2 ) 
						{
							if ( robotInfo.health <= ternaryRobot.info.health ) {
								ternaryRobot = new RobotAndInfo(enemy,robotInfo);
								ternaryDistanceSquared = distance;								
							}
						} else {
							ternaryRobot = new RobotAndInfo(enemy,robotInfo);
							ternaryDistanceSquared = distance;
						}
					}					
				default:
			}
		}
		
		if ( primaryRobot != null ) {
			return primaryRobot;
		}
		if ( secondaryRobot != null ) {
			return secondaryRobot;
		}
		return ternaryRobot;
	}

	public static MapLocation getMassCenterOfThreats(RobotController rc,Robot[] enemies) throws GameActionException 
	{
		float mx = 0;
		float my = 0;
		int count =0;
		for ( int i = 0 ; i < enemies.length ; i++ ) 
		{
			RobotInfo info = rc.senseRobotInfo( enemies[i] );
			if ( info.type == RobotType.SOLDIER || info.type == RobotType.HQ ) { // only soldiers and HQ can harm us
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

	public static int getEstimatedHealthOfThreats(RobotController rc,Robot[] enemies) throws GameActionException 
	{
		int result =0;
		int opponentCount=0;
		for ( int i = 0 ; i < enemies.length ; i++ ) 
		{
			RobotInfo info = rc.senseRobotInfo( enemies[i] );
			if ( info.type == RobotType.SOLDIER || info.type == RobotType.HQ ) { // only soldiers and HQ can harm us
				result += info.health;
				opponentCount++;
			}
		}
		return result*opponentCount;
	}	
}
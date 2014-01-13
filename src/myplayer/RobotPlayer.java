package myplayer;

import java.util.*;

import myplayer.AStar.PathNode;
import battlecode.common.*;
import battlecode.engine.instrumenter.lang.ConcurrentHashMap;

public class RobotPlayer 
{
	private static final Random rnd = new Random();

	private static final Direction[] DIRECTIONS = {Direction.NORTH, 
		Direction.NORTH_EAST, 
		Direction.EAST, 
		Direction.SOUTH_EAST, 
		Direction.SOUTH, 
		Direction.SOUTH_WEST, 
		Direction.WEST, 
		Direction.NORTH_WEST};
	
	public static final int BORDER_LEFT = 1;
	public static final int BORDER_RIGHT = 2;
	public static final int BORDER_TOP = 4;
	public static final int BORDER_BOTTOM = 8;
	
	protected static final class MovementResult {
		
		public int borders=0;
		public GameObject obstacle;
	}

	private static final MovementResult movementResult = new MovementResult();
	
	private static final int pastrRangeSquared = GameConstants.PASTR_RANGE * GameConstants.PASTR_RANGE;
	
	private static final Map<Integer,MyRobotData> ROBOT_DATA = new Hashtable<Integer, RobotPlayer.MyRobotData>();
	
	protected static final class PathInfo 
	{
		public final MapLocation[] path;
		
		public PathInfo(MapLocation[] path) {
			this.path = path;
		}
		
		public MapLocation getStepAfter(MapLocation current) 
		{
			for ( int i = 0 ; i < path.length ; i++ ) {
				if ( path[i].equals( current ) ) {
					if ( (i+1) < path.length ) {
						return path[i+1];
					}
					return null;
				}
			}
			return null;
		}
		
		public boolean hasPathFor(MapLocation start,MapLocation end) {
			return start.equals( start() ) && end.equals( end() );
		}
		
		public MapLocation start() {
			return path != null ? path[0] : null;
		}
		
		public MapLocation end() {
			return path != null ? path[ path.length-1 ] : null;
		}		
	}
	
	protected static final class MyRobotData 
	{
		public final boolean isDestroyer;
		public Direction heading;
		public PathInfo pathInfo;
		
		public MyRobotData(Direction heading) 
		{
			this.heading = heading;
			this.isDestroyer = rnd.nextFloat() <= 0.3f;
		}
		
		public boolean isDestroyer() { return isDestroyer; }
		
		@Override
		public String toString() {
			return "Destroyer="+isDestroyer();
		}
	}

	private static final RobotInfo findClosestRobot(RobotController rc,Robot[] nearbyEnemies) throws GameActionException 
	{
		RobotInfo closestRobot=null;
		int closestDistanceSquared=0;

		MapLocation myLocation = rc.getLocation();
		for ( Robot enemy : nearbyEnemies ) 
		{
			RobotInfo robotInfo = rc.senseRobotInfo( enemy );
			if (isSuitableTarget( robotInfo ) ) 
			{
				int distance = robotInfo.location.distanceSquaredTo( myLocation );
				if ( closestRobot == null || distance < closestDistanceSquared ) {
					closestRobot = robotInfo;
					closestDistanceSquared = distance;
				}
			}
		}
		return closestRobot;
	}

	private static boolean isSuitableTarget(RobotInfo ri) {
		switch(ri.type) {
		case PASTR:
		case SOLDIER:
			return true;
		default:
			return false;
		}
	}
	private static Direction randomDirection() {
		return DIRECTIONS[ rnd.nextInt( DIRECTIONS.length ) ];
	}

	public static void run(RobotController rc) 
	{
		while(true) 
		{
			try 
			{
				if (rc.getType() == RobotType.HQ) 
				{
					// spawn robot if possible
					handleHeadQuarter(rc);
				} 
				else if (rc.getType() == RobotType.SOLDIER && rc.isActive() ) 
				{
					handleSoldier(rc);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			rc.yield();
		} 
	}

	private static void handleSoldier(RobotController rc) throws GameActionException 
	{
		final Integer id = rc.getRobot().getID();
		MyRobotData robotData = ROBOT_DATA.get(id);
		if ( robotData == null ) 
		{
			Direction heading=null;
			for ( int retry = 8 ; retry > 0 ; retry-- ) {
				heading = randomDirection();
				if ( rc.canMove( heading ) ) {
					break;
				}
			}
			robotData = new MyRobotData( heading );		
			System.out.println("Created "+robotData);
			ROBOT_DATA.put( id , robotData);
		}
		
		final Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class,pastrRangeSquared,rc.getTeam().opponent());					
		if ( nearbyEnemies.length > 0 ) // always attack closest enemy robots in sight
		{
			RobotInfo closestRobot = findClosestRobot( rc , nearbyEnemies );
			if ( closestRobot != null ) 
			{
				System.out.println("Attacking "+closestRobot.robot.getID() );
				
				if ( rc.canAttackSquare( closestRobot.location ) ) {
					rc.attackSquare( closestRobot.location );
					return;				
				} 
	
				// at least try to move towards enemy robot
				Direction direction = rc.getLocation().directionTo( closestRobot.location );
				if ( rc.canMove( direction ) ) {
					rc.move( direction );
					return;
				} 
			}
		} 
		
		if ( robotData.isDestroyer() ) // move towards the enemy HQ and attack any enemy robot in its vicinity 
		{ 
			System.err.println("Handling destroyer #"+robotData);
			final MapLocation enemyHQLocation = rc.senseEnemyHQLocation();
			final int distToHQSquared = rc.getLocation().distanceSquaredTo( enemyHQLocation );
			
			if ( distToHQSquared > 5 ) 
			{
				MapLocation next = robotData.pathInfo != null ? robotData.pathInfo.getStepAfter( rc.getLocation() ) : null;				
				if ( next == null ) {
					System.out.println("Looking for path from "+rc.getLocation()+" to "+enemyHQLocation);					
					final MapLocation[] path = findPath( rc , rc.getLocation() , enemyHQLocation );
					if ( path != null ) {
						robotData.pathInfo = new PathInfo( path );
						System.out.println("Got path "+path+" from "+rc.getLocation()+" to "+enemyHQLocation);
						next = path[1];
					}
				}
				
				if ( next != null )
				{
					int dx = next.x - rc.getLocation().x;
					int dy = next.y - rc.getLocation().y;
					Direction direction = getDirection( dx,dy );
					if ( direction != Direction.NONE  && rc.canMove( direction ) ) 
					{
						rc.move( direction );
					} 
				}
			}
		} 
		else 
		{
			// all other robots will just move in a random direction, attacking any enemy robot
			// they come across and convert into a PASTR as soon as neither a HQ nor any other robot are 
			// within GameConstants.PASTR_RANGE

			// no enemies in sight, check for friendlies
			Robot[] nearbyFriends = rc.senseNearbyGameObjects(Robot.class,pastrRangeSquared,rc.getTeam());
			if ( nearbyFriends.length > 0 || rc.senseHQLocation().distanceSquaredTo( rc.getLocation() ) <= pastrRangeSquared ) 
			{
				if ( ! tryGoto( rc , robotData.heading , true ) ) 
				{
					int dx = robotData.heading.dx;
					int dy = robotData.heading.dy;
					
					if ( movementResult.obstacle != null && rc.getRobot().getID() < movementResult.obstacle.getID() ) 
					{
						// blocked by one of our robots , robot with lower ID will always yield the way
						return;
					}
					
					if ( (movementResult.borders|BORDER_TOP)!= 0 || (movementResult.borders|BORDER_BOTTOM)!= 0 ) 
					{
						dy = -dy;
					}
					
					if ( (movementResult.borders|BORDER_LEFT)!= 0 || (movementResult.borders|BORDER_RIGHT)!= 0 ) 
					{
						dx = -dx;
					}
					
					if ( dx != robotData.heading.dx || dy != robotData.heading.dy ) 
					{
						for ( Direction d : Direction.values() ) 
						{
							if ( d.dx == dx && d.dy == dy ) {
								if ( rc.canMove( d ) ) {
									rc.sneak( d );
								} 
								robotData.heading = d;
								break;
							}
						}
					} else {
						Direction heading = randomDirection();
						if ( rc.canMove( heading ) ) {
							rc.sneak( heading );
							robotData.heading = heading;							
						} 
					}			
				}
			} 
			else if ( ! rc.isConstructing() ) 
			{
				rc.construct( RobotType.PASTR );
			}
		}
	}
	
	private static Direction getDirection(int dx,int dy) 
	{
		for ( Direction d : Direction.values() ) 
		{
			if ( d.dx == dx && d.dy == dy ) {
				return d;
			}
		}		
		return Direction.NONE;
	}
	
	private static boolean tryGoto(RobotController rc,Direction d,boolean sneak) throws GameActionException 
	{
		movementResult.obstacle=null;
		movementResult.borders=0;
		
		if ( rc.canMove( d ) ) 
		{
			if ( sneak ) {
				rc.sneak( d );
			} else {
				rc.move( d );
			}
			return true;
		}
		
		final MapLocation newLocation = rc.getLocation().add( d );
		if ( isOffMap( newLocation ) ) 
		{
			if ( newLocation.x < 0 ) {
				movementResult.borders|=BORDER_LEFT;
			} else if ( newLocation.x > GameConstants.MAP_MAX_WIDTH ) {
				movementResult.borders|=BORDER_RIGHT;
			}
			
			if ( newLocation.y < 0 ) {
				movementResult.borders|=BORDER_TOP;
			} else if ( newLocation.y > GameConstants.MAP_MAX_HEIGHT ) {
				movementResult.borders|=BORDER_BOTTOM;
			}			
		} else {
			movementResult.obstacle = rc.senseObjectAtLocation( newLocation );
		}
		return false;
	}
	
	private static boolean isOffMap(MapLocation l) {
		return l.y < 0 || l.x < 0 || l.x > GameConstants.MAP_MAX_WIDTH || l.y > GameConstants.MAP_MAX_HEIGHT; 
	}
	
	private static void handleHeadQuarter(RobotController rc) throws GameActionException 
	{
		// check if a robot is spawnable and spawn one if it is
		if ( rc.isActive() && rc.senseRobotCount() < GameConstants.MAX_ROBOTS ) 
		{
			// try to spawn robot at random location
			for ( int retry = 4 ; retry > 0 ; retry-- ) 
			{
				Direction direction = randomDirection();
				if ( rc.senseObjectAtLocation( rc.getLocation().add( direction ) ) == null) 
				{
					rc.spawn(direction);
					break;
				}
			}
		}
	}
    
    protected static MapLocation[] findPath(final RobotController rc,final MapLocation startLoc,final MapLocation dstLoc) {
    	
    	final AStar<MapLocation> pathFinder = new AStar<MapLocation>() {

    		@Override
    		protected MapLocation getStart() {
    			return startLoc;
    		}
    		
    		@Override
    		protected MapLocation getDestination() {
    			return dstLoc;
    		}
    		
			@Override
			protected boolean preconditionsValid() {
				return true;
			}

			@Override
			protected void beforeSearchStart() { }

			@Override
			protected boolean isCloseEnoughToTarget(myplayer.AStar.PathNode<MapLocation> node) 
			{
				final int dx = Math.abs( dstLoc.x - node.value.x );
				final int dy = Math.abs( dstLoc.y - node.value.y );
				return dx <= 1 && dy <= 1;
			}

			@Override
			protected float calcMovementCost(myplayer.AStar.PathNode<MapLocation> current) 
			{
		        float cost=0;
		        if( current.parent != null ) 
		        {
		        	final float dist = (float) Math.sqrt( current.value.distanceSquaredTo( current.parent.value ) );
		        	cost = current.parent.g() + dist;
		        }
		        return cost;
			}
			
			@Override
			protected float calcEstimatedCost( myplayer.AStar.PathNode<MapLocation> node) 
			{
		    	// WEIGHTED A-STAR !!!
		    	float dist = (float) Math.sqrt( getDestination().distanceSquaredTo(  node.value ) );
				return 4 * dist;
			}

			@Override
			protected void scheduleNeighbors(myplayer.AStar.PathNode<MapLocation> parent) 
			{
				int x = parent.value.x;
				int y = parent.value.y;
				for ( int dx = -1 ; dx <= 1 ; dx++ ) 
				{
					for ( int dy = -1 ; dy <= 1 ; dy++ ) 
					{
						if ( dx != 0 || dy != 0 ) {
							final int newX = x+dx;
							final int newY = y+dy;
							
							final MapLocation newLocation = new MapLocation(newX,newY);
							final TerrainTile tile = rc.senseTerrainTile( newLocation );
							switch(tile) {
								case NORMAL:
								case ROAD:
									maybeAddNeighbor( parent , newLocation );									
									break;
								default:
							}
						}
					}
				}
			}
		};
		
		pathFinder.setStart( startLoc );
		pathFinder.setDestination( dstLoc );
		
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
		return path.toArray( new MapLocation[ path.size() ] );
    }	
}

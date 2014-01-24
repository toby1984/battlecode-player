package test;

import java.util.List;

import team223.State;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class BugGotoLocation extends State {

	private static final boolean VERBOSE = false;
	
	private static final Direction[] DIRS = { Direction.NORTH , Direction.NORTH_EAST,Direction.EAST,Direction.SOUTH_EAST,Direction.SOUTH,
		Direction.SOUTH_WEST,Direction.WEST,Direction.NORTH_WEST};
	
	private final MapLocation destination;
	
	private List<MapLocation> mLine;
	
	private Direction initialHeading;
	private Direction currentHeading;
	
	private boolean avoidingObstacle;
	
	private int minDistance;	
	private MapLocation minHit;
	
	public BugGotoLocation(RobotController rc,MapLocation destination) {
		super(rc);
		this.destination = destination;
	}
	
	private boolean isOnLine(MapLocation l) {
		return mLine.contains( l );
	}	
	
	private MapLocation nextStepNoRemove(MapLocation current) 
	{
		for ( int i = 0 ; i < mLine.size() ; i++ ) 
		{
			if ( mLine.get(i).equals( current ) ) {
				if ( (i+1) < mLine.size() ) 
				{
					return mLine.get(i+1);
				}
				return null;
			}
		}
		return null;
	}	
	
	private MapLocation nextStep(MapLocation current) 
	{
		for ( int i = 0 ; i < mLine.size() ; i++ ) 
		{
			if ( mLine.get(i).equals( current ) ) 
			{
				if ( (i+1) < mLine.size() ) 
				{
					if ( (i-1) >= 0 ) {
						MapLocation removed = mLine.remove(i-1);
						if ( VERBOSE) System.out.println("Dequeued "+removed+" , remaining: "+mLine);
						return mLine.get(i);
					}
					return mLine.get(i+1);
				}
				return null;
			}
		}
		return null;
	}
	
	private void updateMinHit(MapLocation l) 
	{
		int distToDestination = l.distanceSquaredTo( destination ); 
		if ( minHit == null || distToDestination < minDistance ) {
			minHit =l; 
			minDistance=distToDestination;
		}
	}

	@Override
	public State perform() throws GameActionException 
	{
		if ( rc.getLocation().equals( destination ) ) {
			if ( VERBOSE) System.out.println("Destination reached");
			return null;
		}
		
		if ( mLine == null ) {
			mLine = Bresenham.line(rc.getLocation() , destination );
			initialHeading= rc.getLocation().directionTo( mLine.get(1) );
			currentHeading=initialHeading;
		}
		
		if ( ! rc.isActive() ) {
			return this;
		}
		
		if ( avoidingObstacle ) 
		{
			if ( isOnLine( rc.getLocation() ) ) 
			{
				Direction d = rc.getLocation().directionTo( nextStepNoRemove( rc.getLocation() ) );
				if ( isWalkable( rc.getLocation().add( d ) ) ) {
					if ( VERBOSE) System.out.println("Back on track at "+rc.getLocation()+", remaining path: "+mLine); 
					rc.move( d ); 
					currentHeading=initialHeading;
					avoidingObstacle=false;
					minHit=null;
					return this;
				}
				
				if ( rc.getLocation().equals( minHit ) ) {
					if ( VERBOSE) System.out.println("Walking in circles, no path.");
					return null;					
				}
				
				updateMinHit(rc.getLocation());
				
				if ( VERBOSE) System.out.println("Back on track but not walkable");
				d = initialHeading;
				for ( int i=7; i > 0 ; i-- ) 
				{
					d = d.rotateLeft();
					MapLocation newLoc = rc.getLocation().add( d );
					if ( isWalkable( newLoc ) ) 
					{
						currentHeading = d;
						rc.move( d );
						return this;
					}
				}				
				if ( VERBOSE) System.out.println("Surrounded ??!");
				return null;
			}
			
			Direction newDir = rotateRight90(currentHeading); // rotate( currentHeading, 90 );
			if ( isWalkable( rc.getLocation().add( newDir ) ) ) 
			{
				currentHeading = newDir;				
				rc.move( newDir );
				return this;
			}
			newDir = rotateRight45(currentHeading); // rotate( currentHeading , 45 );
			if ( isWalkable( rc.getLocation().add( newDir ) ) ) 
			{
				currentHeading=newDir;
				rc.move( newDir );
				return this;
			}			
			
			newDir = currentHeading;
			if ( isWalkable( rc.getLocation().add( newDir ) ) ) 
			{
				currentHeading=newDir;
				rc.move( newDir );
				return this;
			}
			
			for ( int i=5; i > 0 ; i-- ) {
				newDir  = newDir.rotateLeft();
				MapLocation newLoc = rc.getLocation().add( newDir );
				if ( isWalkable( newLoc ) ) 
				{
					currentHeading = newDir;
					rc.move( newDir );
					return this;
				}
			}			
			return null;
		}
		
		MapLocation next=nextStep( rc.getLocation() );
		if ( next != null ) 
		{
			Direction d = rc.getLocation().directionTo( next );
			MapLocation newLoc = rc.getLocation().add( d );
			if ( isWalkable( newLoc ) ) {
				currentHeading=d;
				rc.move( d );
				return this;
			}
			
			// obstacle !!
			updateMinHit(rc.getLocation());
			
			for ( int i=7; i > 0 ; i-- ) {
				d = d.rotateLeft();
				newLoc = rc.getLocation().add( d );
				if ( isWalkable( newLoc ) ) 
				{
					avoidingObstacle=true;	
					currentHeading = d;
					rc.move( d );
					return this;
				}
			}
			if ( VERBOSE) System.out.println("Surrounded ??!");
			return null;
		}
		if ( VERBOSE) System.out.println("End of path reached?");
		return null;
	}
	
	private static Direction rotateRight90(Direction d) 
	{
		switch( d ) {
			case NORTH:
				return Direction.EAST;
			case NORTH_EAST:
				return Direction.SOUTH_EAST;
			case EAST:
				return Direction.SOUTH;
			case SOUTH_EAST:
				return Direction.SOUTH_WEST;
			case SOUTH:
				return Direction.WEST;
			case SOUTH_WEST:
				return Direction.NORTH_WEST;
			case WEST:
				return Direction.NORTH;
			case NORTH_WEST:
				return Direction.NORTH_EAST;
			default:
				throw new RuntimeException("Unreachable code reached");
		}
	}	
	
	private static Direction rotateRight45(Direction d) 
	{
		switch( d ) {
			case NORTH:
				return Direction.NORTH_EAST;
			case NORTH_EAST:
				return Direction.EAST;
			case EAST:
				return Direction.SOUTH_EAST;
			case SOUTH_EAST:
				return Direction.SOUTH;
			case SOUTH:
				return Direction.SOUTH_WEST;
			case SOUTH_WEST:
				return Direction.WEST;
			case WEST:
				return Direction.NORTH_WEST;
			case NORTH_WEST:
				return Direction.NORTH;
			default:
				throw new RuntimeException("Unreachable code reached");
		}
	}	
	
	private boolean isWalkable(MapLocation l)
	{
		switch( rc.senseTerrainTile( l ) ) {
			case ROAD:
			case NORMAL:
				return true;
			default:
		}
		return false;
	}
}
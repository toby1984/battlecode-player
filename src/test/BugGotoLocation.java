package test;

import java.util.List;

import team223.State;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class BugGotoLocation extends State {

	private final MapLocation destination;
	
	private Bresenham bresenham;
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
	
	private MapLocation nextStepNoRemove(MapLocation current) 
	{
		for ( int i = 0 ; i < mLine.size() ; i++ ) {
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
		for ( int i = 0 ; i < mLine.size() ; i++ ) {
			if ( mLine.get(i).equals( current ) ) {
				if ( (i+1) < mLine.size() ) 
				{
					if ( (i-1) >= 0 ) {
						MapLocation removed = mLine.remove(i-1);
						System.out.println("Dequeued "+removed+" , remaining: "+mLine);
						return mLine.get(i);
					}
					return mLine.get(i+1);
				}
				return null;
			}
		}
		return null;
	}
	
	private boolean isOnLine(MapLocation l) {
		return mLine.contains( l );
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
			System.out.println("Destination reached");
			return null;
		}
		
		if ( bresenham == null ) {
			bresenham= new Bresenham();
			bresenham.setRoute( rc.getLocation() , destination );
			mLine = bresenham.line();
			initialHeading= rc.getLocation().directionTo( mLine.get(1) );
			currentHeading=initialHeading;
		}
		
		if ( avoidingObstacle ) 
		{
			if ( isOnLine( rc.getLocation() ) ) 
			{
				// we're back on track again
				Direction d = rc.getLocation().directionTo( nextStepNoRemove( rc.getLocation() ) );
				if ( isWalkable( rc.getLocation().add( d ) ) ) {
					System.out.println("Back on track at "+rc.getLocation()+", remaining path: "+mLine); 
					rc.move( d ); 
					currentHeading=initialHeading;
					avoidingObstacle=false;
					minHit=null;
					return this;
				}
				
				if ( rc.getLocation().equals( minHit ) ) {
					System.out.println("Walking in circles, no path.");
					return null;					
				}
				
				updateMinHit(rc.getLocation());
				
				System.out.println("Back on track but not walkable");
				d = initialHeading;
				for ( int i=7; i > 0 ; i-- ) {
					d = d.rotateLeft();
					MapLocation newLoc = rc.getLocation().add( d );
					if ( isWalkable( newLoc ) ) 
					{
						currentHeading = d;
						rc.move( d );
						return this;
					}
				}				
				System.out.println("Surrounded ??!");
				return null;
			}
			
			Direction newDir = rotate( currentHeading, 90 );
			if ( isWalkable( rc.getLocation().add( newDir ) ) ) 
			{
				currentHeading = newDir;				
				rc.move( newDir );
				return this;
			}
			newDir = rotate( currentHeading , 45 );
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
			System.out.println("Surrounded ??!");
			return null;
		}
		System.out.println("End of path reached");
		return null;
	}
	
	private static final Direction[] DIRS = { Direction.NORTH , Direction.NORTH_EAST,Direction.EAST,Direction.SOUTH_EAST,Direction.SOUTH,
		Direction.SOUTH_WEST,Direction.WEST,Direction.NORTH_WEST};
	
	private static Direction rotate(Direction d, int degrees) {
		switch( degrees ) 
		{
			case -90:
				return getDirection( getDirIndex( d ) - 1 );
			case -45:
				return getDirection( getDirIndex( d )- 1 );
			case 0:
				return d;
			case 45:
				return getDirection( getDirIndex( d )+1 );
			case 90:
				return getDirection( getDirIndex( d )+2 );	
			default:
				throw new IllegalArgumentException("Invalid angle: "+degrees);
		}
	}
	
	private static int getDirIndex(Direction d) 
	{
		switch( d ) {
			case NORTH:
				return 0;
			case NORTH_EAST:
				return 1;
			case EAST:
				return 2;
			case SOUTH_EAST:
				return 3;
			case SOUTH:
				return 4;
			case SOUTH_WEST:
				return 5;
			case WEST:
				return 6;
			case NORTH_WEST:
				return 7;
		}
		throw new RuntimeException("Unreachable code reached");
	}
	
	private static Direction getDirection(int index) {
		if ( index < 0 ) {
			index = DIRS.length+index;
		} else if ( index >= DIRS.length ) {
			index -= DIRS.length;
		}
		return DIRS[index];
	}
	
	private Direction getDirectionTowardsInitialDir() 
	{
		int i1 = getDirIndex(initialHeading);
		int i2 = getDirIndex(currentHeading);
		if ( i1==i2) {
			return initialHeading;
		} 
		if ( i2 < i1 ) {
			return getDirection(i2+1);
		}
		return getDirection(i2-1);
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

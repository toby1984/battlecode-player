package team223.states;

import java.util.List;

import team223.MyConstants;
import team223.State;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.MovementType;
import battlecode.common.RobotController;

public abstract class GotoLocation extends State {

	private final MovementType movementType;
	
	private int movementFailureCount = 0;
	
	private List<MapLocation> currentPath;
	private int currentPathSize=0;
	private MapLocation destination;
	
	public GotoLocation(List<MapLocation> path,MovementType movementType) {
		this.movementType = movementType;
		setNewPath( path );
	}
	
	public final MapLocation getDestination() {
		return destination;
	}
	
	private void setNewPath(List<MapLocation> path) {
		currentPath = path;
		currentPathSize = path.size();
		destination = path.get( currentPathSize-1 );
	}
	
	private MapLocation nextStep(MapLocation current) 
	{
		for ( int i = 0 ; i < currentPathSize ; i++ ) 
		{
			MapLocation loc = currentPath.get(i);
			if ( loc.equals( current ) ) {
				if ( (i+1) < currentPathSize ) {
					return currentPath.get(i+1);
				}
				break;
			} 
		}
		return null;
	}	
	
	protected abstract List<MapLocation> recalculatePath(RobotController rc) throws GameActionException;
	
	@Override
	public final State perform(RobotController rc) throws GameActionException 
	{
		final MapLocation myLocation = rc.getLocation();
		
		if ( hasArrivedAtDestination( myLocation , destination ) ) {
			return null;
		} 
		
		MapLocation next = nextStep( myLocation );
		if ( next != null ) 
		{
			Direction direction = myLocation.directionTo( next );
			if ( direction == Direction.OMNI ) {
				return null;
			}
			if ( rc.canMove( direction ) ) 
			{
				movementFailureCount=0;				
				if ( movementType == MovementType.RUN ) {
					rc.move( direction );
				} else {
					rc.sneak( direction );
				}
			} 
			else 
			{
				movementFailureCount++;
				if ( MyConstants.DEBUG_MODE) System.out.println("Failed to move "+myLocation+" -> "+next+" (count: "+movementFailureCount+")");
				
				if ( movementFailureCount > MyConstants.MAX_PATH_MOVEMENT_FAILURES ) 
				{
					// 	movement failed too many times, some new obstacle is blocking us...recalculate path					
					if ( MyConstants.DEBUG_MODE) System.out.println("Re-calculating path "+myLocation+" -> "+next);						
					movementFailureCount = 0;
					List<MapLocation> newPath = recalculatePath(rc);
					if ( newPath == null ) {
						if ( MyConstants.DEBUG_MODE) System.out.println("ERROR: Failed to recalculate path" );						
						return null;
					}
					setNewPath( newPath );
				}
			}
			return this;
		} 
		else {
			if ( MyConstants.DEBUG_MODE) System.out.println("ERROR: At "+rc.getLocation()+" , no next step on path "+currentPath );
			
			List<MapLocation> newPath  = recalculatePath(rc);
			if ( newPath != null ) {
				setNewPath( newPath );
				return this;
			}
		}
		return null;
	}
	
	protected abstract boolean hasArrivedAtDestination(MapLocation current,MapLocation dstLoc);
	
    @Override
    public String toString() {
    	return getClass().getName();
    }	
}
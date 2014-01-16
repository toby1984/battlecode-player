package team223.states;

import java.util.List;

import team223.MyConstants;
import team223.PathInfo;
import team223.State;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.MovementType;
import battlecode.common.RobotController;

public abstract class GotoLocation extends State {

	private PathInfo pathInfo = null;
	private final MovementType movementType;
	
	private int movementFailureCount = 0;
	
	public GotoLocation(PathInfo info,MovementType movementType) {
		this.pathInfo = info;
		this.movementType = movementType;
	}
	
	public MapLocation getDestination() {
		return pathInfo.end();
	}
	
	protected abstract List<MapLocation> recalculatePath(RobotController rc) throws GameActionException;
	
	@Override
	public State perform(RobotController rc) throws GameActionException 
	{
		final MapLocation myLocation = rc.getLocation();
		
		if ( hasArrivedAtDestination( myLocation , pathInfo.end() ) ) {
			return null;
		} 
		
		MapLocation next = pathInfo.getStepAfter( myLocation );
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
					pathInfo = new PathInfo( recalculatePath(rc) );
					if ( pathInfo.path == null ) {
						if ( MyConstants.DEBUG_MODE) System.out.println("ERROR: Failed to recalculate path" );						
						return null;
					}
				}
			}
			return this;
		} else {
			pathInfo = new PathInfo( recalculatePath(rc) );
			if ( pathInfo.path != null ) {
				return this;
			}
			if ( MyConstants.DEBUG_MODE) System.out.println("ERROR: At "+rc.getLocation()+" , no next step on path "+pathInfo.path );
		}
		return null;
	}
	
	protected abstract boolean hasArrivedAtDestination(MapLocation current,MapLocation dstLoc);
	
    @Override
    public String toString() {
    	return getClass().getName();
    }	
}

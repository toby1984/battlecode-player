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
//		else {
//			int dst = myLocation.distanceSquaredTo( pathInfo.end() );
//			if ( MyConstants.DEBUG_MODE) System.out.println("distanceSqrt( "+myLocation+" -> "+pathInfo.end()+"): "+dst+" but needed <= "+RobotType.SOLDIER.attackRadiusMaxSquared );
//		}
		
		MapLocation next = pathInfo.getStepAfter( myLocation );
		if ( next != null ) 
		{
			Direction direction = myLocation.directionTo( next );
			if ( direction == Direction.OMNI ) {
				return null;
			}
			if ( rc.canMove( direction ) ) 
			{
				if ( movementType == MovementType.RUN ) {
					rc.move( direction );
				} else {
					rc.sneak( direction );
				}
				movementFailureCount=0;
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
						if ( MyConstants.DEBUG_MODE) System.out.println("Failed to recalculating path" );						
						return null;
					}
				}
			}
			return this;
		}	
		return null;
	}
	
	protected abstract boolean hasArrivedAtDestination(MapLocation current,MapLocation dstLoc); 
}

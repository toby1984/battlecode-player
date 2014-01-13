package myplayer.states;

import myplayer.PathInfo;
import myplayer.State;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.MovementType;
import battlecode.common.RobotController;

public class GotoLocation extends State {

	private PathInfo pathInfo = null;
	private final MovementType movementType;
	
	public GotoLocation(PathInfo info,MovementType movementType) {
		this.pathInfo = info;
		this.movementType = movementType;
	}
	
	@Override
	public State perform(RobotController rc) throws GameActionException 
	{
		if ( hasArrivedAtDestination( rc.getLocation() , pathInfo.end() ) ) {
			return null;
		}
		
		MapLocation next = pathInfo.getStepAfter( rc.getLocation() );
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
				if ( movementType == MovementType.RUN ) {
					rc.move( direction );
				} else {
					rc.sneak( direction );
				}
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
}

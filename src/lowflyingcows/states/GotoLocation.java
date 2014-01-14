package lowflyingcows.states;

import java.util.List;

import lowflyingcows.PathInfo;
import lowflyingcows.State;
import battlecode.common.*;

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
//			System.out.println("distanceSqrt( "+myLocation+" -> "+pathInfo.end()+"): "+dst+" but needed <= "+RobotType.SOLDIER.attackRadiusMaxSquared );
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
			} 
			else 
			{
				movementFailureCount++;
				System.out.println("Failed to move "+myLocation+" -> "+next+" (count: "+movementFailureCount+")");
				if ( movementFailureCount > 3 ) 
				{
					System.out.println("Re-calculating path "+myLocation+" -> "+next);						
					movementFailureCount = 0;
					// 	recalculate path
					pathInfo = new PathInfo( recalculatePath(rc) );
					if ( pathInfo.path == null ) {
						System.out.println("Failed to recalculating path" );						
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

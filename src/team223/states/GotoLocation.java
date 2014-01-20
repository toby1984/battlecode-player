package team223.states;

import java.util.List;

import team223.AStar.PathFindingResultCallback;
import team223.AStar.TimeoutResult;
import team223.MyConstants;
import team223.State;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.MovementType;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public abstract class GotoLocation extends State {

	private final MovementType movementType;

	private int movementFailureCount = 0;

	private List<MapLocation> currentPath;
	private int currentPathSize=0;
	private MapLocation destination;
	
	private final boolean invokeBeforeMove;
	
	private int previousStep = Integer.MAX_VALUE;
	
	private State tempState;

	private final PathFindingResultCallback recalculatePathCallback = new PathFindingResultCallback() {

		@Override
		public void foundPath(List<MapLocation> path) {
			if ( MyConstants.GOTO_LOCATION_VERBOSE ) System.out.println("Recalculating path succeeded.");				
			setNewPath( path );							
		}

		@Override
		public void foundNoPath() {
			if ( MyConstants.GOTO_LOCATION_VERBOSE) System.out.println("ERROR: Failed to recalculate path" );									
		}

		@Override
		public TimeoutResult onTimeout() {
			return TimeoutResult.CONTINUE;
		}
	};

	public GotoLocation(RobotController rc,List<MapLocation> path,MovementType movementType,boolean invokeBeforeMove) 
	{
		super(rc);
		this.invokeBeforeMove = invokeBeforeMove;
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
		if ( MyConstants.GOTO_LOCATION_VERBOSE ) System.out.println("setNewPath(): "+path);
	}

	private MapLocation nextStep(MapLocation current) 
	{
		for ( int i = 0 ; i < currentPathSize ; i++ ) 
		{
			if ( currentPath.get(i).equals( current ) ) {
				previousStep=i+1;
				if ( previousStep < currentPathSize ) {
					return currentPath.get(previousStep);
				}
				break;
			} 
		}
		return null;
	}	
	
	protected State beforeMove() {
		return null;
	}

	@Override
	public final State perform() throws GameActionException 
	{
		if ( tempState != null ) {
			tempState = tempState.perform();
			return this;
		}
		
		final MapLocation myLocation = rc.getLocation();
		if ( hasArrivedAtDestination( myLocation , destination ) ) {
			if ( MyConstants.GOTO_LOCATION_VERBOSE) System.out.println("Arrived at destination "+destination);
			return null;
		} 

		if ( ! rc.isActive() ) {
			rc.yield();
		}
		
		MapLocation next = nextStep( myLocation );
		if ( next != null ) 
		{
			Direction direction = myLocation.directionTo( next );
			if ( direction == Direction.OMNI ) {
				return null;
			}
			
			if ( invokeBeforeMove ) 
			{
				tempState = beforeMove();
				if ( tempState != null ) {
					if ( MyConstants.GOTO_LOCATION_VERBOSE) System.out.println("beforeMove() returned "+tempState);
					tempState = tempState.perform();
					return this;
				}
				if ( ! rc.isActive() ) {
					return this;
				}
			}
			
			if ( rc.canMove( direction ) )
			{
				movementFailureCount=0;		
				if ( rc.isActive() ) {
					if ( movementType == MovementType.RUN ) {
						rc.move( direction );
					} else {
						rc.sneak( direction );
					}
				}
			} 
			else 
			{
				movementFailureCount++;

				if ( MyConstants.GOTO_LOCATION_VERBOSE) System.out.println("Failed to move "+rc.getLocation()+" -> "+next+" (count: "+movementFailureCount+")");
				
				// check what's blocking our way
				Robot object = (Robot) rc.senseObjectAtLocation( myLocation.add( direction ) );
				if ( object != null ) 
				{
					RobotInfo ri = rc.senseRobotInfo( object);
					if ( MyConstants.GOTO_LOCATION_VERBOSE) System.out.println("Something is in the way: "+ri.type+" , Team "+ri.team);
					
					if ( ri.team != rc.getTeam() ) 
					{
						switch( ri.type ) {
							case SOLDIER:
							case PASTR:
							case NOISETOWER:
								if ( MyConstants.GOTO_LOCATION_VERBOSE) System.out.println("Attacking enemy robot "+ri);
								tempState = new Attacking( rc , (Robot) object );
								break;
						}
					}
				}

				if ( movementFailureCount > MyConstants.MAX_PATH_MOVEMENT_FAILURES ) 
				{
					// 	movement failed too many times, some new obstacle is blocking us...recalculate path					
					if ( MyConstants.GOTO_LOCATION_VERBOSE ) System.out.println("Re-calculating path "+rc.getLocation()+" -> "+next);						
					movementFailureCount = 0;
					return recalculatePath( recalculatePathCallback );
				}
			}
			return this;
		} 

		if ( MyConstants.GOTO_LOCATION_VERBOSE) System.out.println("ERROR: At "+myLocation+" , no next step on path "+currentPath+" , recalculating path" );
		movementFailureCount = 0;
		return recalculatePath( recalculatePathCallback );
	}

	protected abstract State recalculatePath(PathFindingResultCallback callback) throws GameActionException;

	protected abstract boolean hasArrivedAtDestination(MapLocation current,MapLocation dstLoc);

	@Override
	public String toString() {
		return getClass().getName();
	}	
}
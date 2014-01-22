package team223.states;

import java.util.List;

import team223.AStar.Callback;
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
	
	private int previousStep = Integer.MAX_VALUE;
	
	// private State tempState;

	private final Callback recalculatePathCallback = new Callback() {

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
		public boolean abortOnTimeout() {
			return false;
		}
	};

	public GotoLocation(RobotController rc,List<MapLocation> path,MovementType movementType) 
	{
		super(rc);
		this.movementType = movementType;
		setNewPath( path );
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
	
	@Override
	public final State perform() throws GameActionException 
	{
		if ( hasArrivedAtDestination( rc.getLocation() , destination ) ) 
		{
			if ( MyConstants.GOTO_LOCATION_VERBOSE) System.out.println("Arrived at destination "+destination);
			return null;
		} 
		
		if ( ! rc.isActive() ) {
			rc.yield();
		}

		MapLocation next = nextStep( rc.getLocation() );
		if ( next != null ) 
		{
			Direction direction = rc.getLocation().directionTo( next );
			if ( direction == Direction.OMNI ) {
				return null;
			}
			
			if ( rc.canMove( direction ) )
			{
				movementFailureCount=0;					
				if ( movementType == MovementType.RUN ) {
					if ( MyConstants.GOTO_LOCATION_VERBOSE) System.out.println("Running "+rc.getLocation()+" -> "+next);						
					rc.move( direction );
				} else {
					if ( MyConstants.GOTO_LOCATION_VERBOSE) System.out.println("Sneaking "+rc.getLocation()+" -> "+next);
					rc.sneak( direction );
				}
			} 
			else 
			{
				movementFailureCount++;

				if ( MyConstants.GOTO_LOCATION_VERBOSE) System.out.println("Failed to move "+rc.getLocation()+" -> "+next+" (count: "+movementFailureCount+")");
				
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

		if ( MyConstants.GOTO_LOCATION_VERBOSE) System.out.println("ERROR: At "+rc.getLocation()+" , no next step on path "+currentPath+" , recalculating path" );
		movementFailureCount = 0;
		return recalculatePath( recalculatePathCallback );
	}

	protected abstract State recalculatePath(Callback callback) throws GameActionException;

	protected abstract boolean hasArrivedAtDestination(MapLocation current,MapLocation dstLoc);

	@Override
	public String toString() {
		return getClass().getName();
	}	
}
package team223.states;

import java.util.List;

import team223.AStar;
import team223.AStar.PathFindingResultCallback;
import team223.MyConstants;
import team223.State;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.MovementType;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public abstract class InterruptibleGotoLocation extends State implements AStar.Callback {

	protected static final int UNKNOWN_HEALTH= -99999;
	
	public static boolean INTR_GOTO_LOCATION_VERBOSE = false;

	private final AStar finder;
	private final MovementType movementType;
	protected final RobotController rc;

	private State activeState;

	private double currentHealth = UNKNOWN_HEALTH;
	
	private PathFindingResultCallback callback;
	
	public InterruptibleGotoLocation(final RobotController rc,MovementType movementType)
	{
		super(rc);
		
		this.rc = rc;
		this.movementType = movementType;

		finder = new AStar(rc) {

			@Override
			public boolean isWalkable(MapLocation loc) throws GameActionException 
			{
				if ( rc.canSenseSquare(loc) ) 
				{
					Robot object = (Robot) rc.senseObjectAtLocation( loc );
					if ( object != null ) 
					{
						RobotInfo robot = rc.senseRobotInfo( object );
						switch( robot.type) {
							case HQ:
								return false;
							case NOISETOWER:
								return robot.team != rc.getTeam();
							case SOLDIER:
								// enemies will be killed, friendlies will hopefully go out of the way...
								return robot.team != rc.getTeam();
							case PASTR:
								return robot.team != rc.getTeam();
							default:
						}
					}
				}
				return true;				
			}

			@Override
			protected boolean isCloseEnoughToTarget(PathNode  node) {
				return InterruptibleGotoLocation.this.hasArrivedAtDestination( node.value  , destination );
			}
		};
	}	

	@Override
	public final void foundPath(List<MapLocation> path) 
	{
		if ( INTR_GOTO_LOCATION_VERBOSE ) System.out.println("Found path , switching to GotoLocation");
		
		activeState = new GotoLocation( rc , path , movementType ) {

			@Override
			protected boolean hasArrivedAtDestination(MapLocation current, MapLocation dstLoc) {
				return InterruptibleGotoLocation.this.hasArrivedAtDestination(current, dstLoc);
			}
			
			@Override
			protected State recalculatePath(PathFindingResultCallback callback) throws GameActionException
			{
				if ( MyConstants.DEBUG_MODE ) System.out.println("Recalculating path ( InterruptibleGotoLocation#foundPath() )");
				
				activeState = null;
				InterruptibleGotoLocation.this.callback = callback;
				finder.reset();
				if ( ! setStartAndDestination( finder , true ) ) {
					if ( MyConstants.DEBUG_MODE ) System.out.println("setStartAndDestination() in recalculatePath() failed");
					finder.abort();
				}
				return null; // MUST RETURN NULL since GotoLocation#perform() and NOT the outer "this" InterruptibleGotoLocation instance.
				// This method was invoked by "activeState = activeState.perform() in InterruptibleGotoLocation#perform() ,
				// returning the outer instance would lead to a StackOverflow / infinite recursion
			}
		};		
		
		try {
			if ( callback != null ) {
				callback.foundPath( path );
			}
		} finally {
			callback = null;
			foundPathHook( path );
		}
	}
	
	@Override
	public final void foundNoPath() {
		activeState = null;
		try {
			if ( callback != null ) {
				callback.foundNoPath();
			}
		} finally {
			callback = null;
			try {
				foundNoPathHook();
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public final State perform() throws GameActionException 
	{
		if ( activeState != null ) 
		{
			activeState  = activeState.perform();
			if ( activeState != null ) {
				return this;
			}
		}
		
		if ( finder.isStarted() ) 
		{
			if ( finder.isFinished() ) {
				if ( INTR_GOTO_LOCATION_VERBOSE ) System.out.println("perform() returns, search finished.");
				return null;
			}
			if ( finder.isAborted() ) 
			{
				if ( INTR_GOTO_LOCATION_VERBOSE ) System.out.println("Path finding aborted.");
				return null; // 
			}
			finder.continueFindPath();
			return this;
		}
		
		if ( INTR_GOTO_LOCATION_VERBOSE ) System.out.println("perform() starts path finding");			
		
		finder.reset();
		
		if ( setStartAndDestination( finder , false ) ) {
			finder.findPath( this );
			return this;
		}
		return null;
	}

	public final MapLocation getDestination() {
		return finder.getDestination();
	}
	
	// Subclassing hooks
	protected void foundPathHook(List<MapLocation> path) {
	}
	
	protected void foundNoPathHook() throws GameActionException {
	}	
	
	public State onLowRobotHealth(double currentRobotHealth) 
	{
		return null;
	}

	public State onAttack(double currentRobotHealth) 
	{
		return new AttackEnemiesInSight(rc);				
	}		

	protected abstract boolean hasArrivedAtDestination(MapLocation current, MapLocation dstLoc);	

	public abstract boolean setStartAndDestination(AStar finder,boolean retry) throws GameActionException;
}
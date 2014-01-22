package team223.states;

import java.util.List;

import team223.AStar;
import team223.AStar.Callback;
import team223.MyConstants;
import team223.State;
import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.MovementType;
import battlecode.common.RobotController;

public abstract class InterruptibleGotoLocation extends State implements AStar.Callback {

	private final MovementType movementType;
	protected final RobotController rc;

	protected State activeState;

	private Callback callback;
	
	private final boolean maxSpeed;
	private boolean finished;
	
	public InterruptibleGotoLocation(final RobotController rc,MovementType movementType,boolean maxSpeed)
	{
		super(rc);
		
		this.rc = rc;
		this.movementType = movementType;
		this.maxSpeed = maxSpeed;
		AStar.reset();
	}	

	@Override
	public final void foundPath(List<MapLocation> path) 
	{
		if ( MyConstants.INTR_GOTO_LOCATION_VERBOSE ) System.out.println("Found path , switching to GotoLocation");
		
		activeState = new GotoLocation( rc , path , movementType ) {

			@Override
			protected boolean hasArrivedAtDestination(MapLocation current, MapLocation dstLoc) {
				return InterruptibleGotoLocation.this.hasArrivedAtDestination(current, dstLoc);
			}
			
			@Override
			protected State recalculatePath(Callback callback) throws GameActionException
			{
				if ( MyConstants.DEBUG_MODE ) System.out.println("Recalculating path ( InterruptibleGotoLocation#foundPath() )");
				
				activeState = null;
				InterruptibleGotoLocation.this.callback = callback;
				AStar.reset();
				
				if ( ! setStartAndDestination( true ) ) {
					if ( MyConstants.DEBUG_MODE ) System.out.println("setStartAndDestination() in recalculatePath() failed");
					AStar.abort();
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
	public final void foundNoPath() 
	{
		if ( MyConstants.INTR_GOTO_LOCATION_VERBOSE ) {
			System.out.println("foundNoPath()");
		}
		
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
		if ( MyConstants.INTR_GOTO_LOCATION_VERBOSE ) System.out.println("Path finder state: "+AStar.getState() );
		
		if ( activeState != null ) 
		{
			activeState  = activeState.perform();
			if ( activeState != null ) {
				return this;
			}
			finished = true;
		}
		
		if ( AStar.isInterrupted() ) {
			if ( MyConstants.INTR_GOTO_LOCATION_VERBOSE )  System.out.println("Continueing path finding , round: "+Clock.getRoundNum());				
			AStar.continueFindPath();
			return this;
		}
		
		if ( AStar.started ) 
		{
			if ( AStar.finished ) {
				if ( MyConstants.INTR_GOTO_LOCATION_VERBOSE ) System.out.println("perform() returned, search finished.");
				return null;
			}
			if ( AStar.aborted ) 
			{
				if ( MyConstants.INTR_GOTO_LOCATION_VERBOSE ) System.out.println("Path finding aborted.");
				return null; // 
			}

			if ( MyConstants.INTR_GOTO_LOCATION_VERBOSE ) System.out.println("Resetting path finding.");
			AStar.reset();
		}
		
		if ( ! finished && setStartAndDestination( false ) ) 
		{
			if ( maxSpeed ) 
			{
				if ( MyConstants.INTR_GOTO_LOCATION_VERBOSE ) System.out.println("perform() starts path finding (non-interruptible)");	
				AStar.findPathNonInterruptible( this );
				return null;
			} 
			if ( MyConstants.INTR_GOTO_LOCATION_VERBOSE ) System.out.println("perform() starts path finding (interruptible)");					
			AStar.findPath( this );
			return this;
		} else if ( MyConstants.INTR_GOTO_LOCATION_VERBOSE ) System.out.println("setStartAndDestination() returned false");
		return null;
	}

	public final MapLocation getDestination() {
		return AStar.getDestination();
	}
	
	// Subclassing hooks
	protected void foundPathHook(List<MapLocation> path) {
	}
	
	protected void foundNoPathHook() throws GameActionException {
	}	
	
	protected abstract boolean hasArrivedAtDestination(MapLocation current, MapLocation dstLoc);	

	public abstract boolean setStartAndDestination(boolean retry) throws GameActionException;
}
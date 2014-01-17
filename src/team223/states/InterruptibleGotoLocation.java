package team223.states;

import java.util.List;

import team223.AStar;
import team223.AStar.PathFindingResultCallback;
import team223.AStar.Result;
import team223.FastRandom;
import team223.MyConstants;
import team223.State;
import battlecode.common.GameActionException;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.MovementType;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public abstract class InterruptibleGotoLocation extends State implements AStar.Callback {

	protected static final int UNKNOWN_HEALTH= -99999;
	
	private static boolean VERBOSE = false;

	private final AStar finder;
	private final MovementType movementType;
	protected final RobotController rc;
	protected final FastRandom rnd;

	private State activeState;

	private double currentHealth = UNKNOWN_HEALTH;
	
	private PathFindingResultCallback callback;
	
	private final MapLocation enemyHQ;
	
	public InterruptibleGotoLocation(final RobotController rc,MovementType movementType,FastRandom rnd,MapLocation enemyHQ)
	{
		super(rc);
		
		this.enemyHQ = enemyHQ;
		this.rnd = rnd;
		this.rc = rc;
		this.movementType = movementType;

		finder = new AStar(rc) {

			@Override
			public boolean isOccupied(MapLocation loc) throws GameActionException {
				return InterruptibleGotoLocation.this.isOccupied( loc );
			}

			@Override
			protected boolean isCloseEnoughToTarget(PathNode<MapLocation> node) {
				return InterruptibleGotoLocation.this.hasArrivedAtDestination( node.value  , destination );
			}
		};
	}	

	@Override
	public Result checkInterrupt() 
	{
		State newState = null;
		if ( currentHealth == UNKNOWN_HEALTH ) 
		{
			currentHealth = rc.getHealth();
			if ( VERBOSE ) System.out.println("Initialized health to "+currentHealth);
			if ( currentHealth < MyConstants.FLEE_HEALTH ) { 
				newState = onLowRobotHealth( currentHealth );
				if ( VERBOSE ) System.out.println("Robot is low on health ("+currentHealth+") , switched to state: "+newState);				
			}
		} 
		else 
		{
			double newHealth = rc.getHealth();
			if ( VERBOSE ) System.out.println("Robot health is now "+newHealth);
			if ( newHealth < currentHealth ) 
			{
				// we're under attack 
				currentHealth = UNKNOWN_HEALTH; // invalidate health, we don't know when we'll be called again (if ever)						
				newState = onAttack( newHealth );
				if ( VERBOSE ) System.out.println("Robot health diminished ("+currentHealth+") , switched to state "+newState);				
			} else {
				currentHealth = newHealth;
				return Result.CONTINUE;
			}
		}
		
		if ( newState != null ) {
			activeState = newState;
			return Result.INTERRUPT;
		} 
		return Result.CONTINUE;
	}

	@Override
	public final void foundPath(List<MapLocation> path) 
	{
		if ( VERBOSE ) System.out.println("Found path , switching to GotoLocation");
		
		activeState = new GotoLocation( rc , path , movementType , isInvokeBeforeMove() ) {

			@Override
			protected boolean hasArrivedAtDestination(MapLocation current, MapLocation dstLoc) {
				return InterruptibleGotoLocation.this.hasArrivedAtDestination(current, dstLoc);
			}
			
			@Override
			protected State beforeMove() {
				return InterruptibleGotoLocation.this.beforeEachMove();
			}

			@Override
			protected State recalculatePath(PathFindingResultCallback callback)
			{
				if ( MyConstants.DEBUG_MODE ) System.out.println("Recalculating path");
				reset();
				InterruptibleGotoLocation.this.callback = callback;
				return null; // MUST RETURN NULL since GotoLocation#perform() and NOT the outer "this" InterruptibleGotoLocation instance.
				// This method was was invoked by "activeState = activeState.perform() in InterruptibleGotoLocation#perform() ,
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
	
	protected State beforeEachMove() {
		return null;
	}
	
	public boolean isInvokeBeforeMove() {
		return false;
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
			foundNoPathHook();
		}
	}
	
	protected void reset() {
		activeState = null;
		finder.reset();
		setStartAndDestination( finder );
	}

	@Override
	public final State perform() throws GameActionException 
	{
		if ( activeState != null ) 
		{
			if ( VERBOSE ) System.out.println("perform( "+activeState+" )");
			
			activeState  = activeState.perform();
			
			if ( activeState != null ) {
				return this;
			}
		}
		
		if ( finder.isStarted() ) 
		{
			if ( finder.isFinished() ) {
				if ( VERBOSE ) System.out.println("perform() returns, search finished.");
				return null;
			}
			if ( VERBOSE ) System.out.println("perform() continues path finding");			
			finder.continueFindPath();
			return this;
		}
		
		if ( VERBOSE ) System.out.println("perform() starts path finding");			
		if ( setStartAndDestination( finder ) ) {
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
	
	protected void foundNoPathHook() {
	}	
	
	public State onLowRobotHealth(double currentRobotHealth) 
	{
		return new Fleeing(rc,rnd , enemyHQ );
	}

	public State onAttack(double currentRobotHealth) 
	{
		if ( currentRobotHealth < MyConstants.FLEE_HEALTH ) {
			return new Fleeing(rc,rnd,enemyHQ);
		}
		return new AttackEnemiesInSight(rc);				
	}		

	public boolean isOccupied(MapLocation loc) throws GameActionException 
	{
		if ( rc.canSenseSquare(loc) ) 
		{
			GameObject object = rc.senseObjectAtLocation( loc );
			if ( object instanceof Robot) 
			{
				RobotInfo robot = rc.senseRobotInfo( (Robot) object );
				switch( robot.type) {
					case HQ:
						return true;
					case NOISETOWER:
					case SOLDIER:
					case PASTR:
						return robot.team == rc.getTeam();
					default:
						return false;
				}
			}
		}
		return false;
	}

	protected abstract boolean hasArrivedAtDestination(MapLocation current, MapLocation dstLoc);	

	public abstract boolean setStartAndDestination(AStar finder);
}
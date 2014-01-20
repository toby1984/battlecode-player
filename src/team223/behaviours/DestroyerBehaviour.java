package team223.behaviours;

import team223.AStar;
import team223.AStar.TimeoutResult;
import team223.EnemyBlacklist;
import team223.MyConstants;
import team223.RobotBehaviour;
import team223.RobotPlayer;
import team223.State;
import team223.Utils;
import team223.Utils.RobotAndInfo;
import team223.states.AttackEnemiesInSight;
import team223.states.Attacking;
import team223.states.InterruptibleGotoLocation;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.MovementType;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public final class DestroyerBehaviour extends RobotBehaviour {

	private static final int DEFAULT_HOMEIN_TIMEOUT = 1000;
	
	private static final int MAX_HOMEIN_TIMEOUT =1000;	

	protected static final int UNKNOWN_HEALTH= -99999;
	
	private final EnemyBlacklist enemyBlacklist = new EnemyBlacklist(110);
	
	private RobotAndInfo currentTarget;
	
	private int homeInTimeout = DEFAULT_HOMEIN_TIMEOUT;
	
	public DestroyerBehaviour(final RobotController rc) {
		super(rc);
	}
	
	@Override
	public void perform() throws GameActionException {

		if ( state != null ) {
			state = state.perform();
			return;
		}		
		
		if ( MyConstants.DEBUG_MODE ) { System.out.println("Sensing enemies, current state: "+state); }
		
		enemyBlacklist.removeStaleEntries();
		
		final Robot[] enemies = rc.senseNearbyGameObjects( Robot.class , MyConstants.SOLDIER_SEEK_ENEMY_RANGE_SQUARED , RobotPlayer.enemyTeam );
		currentTarget = enemies.length > 0 ? Utils.pickEnemyToAttack( rc , enemies, enemyBlacklist ) :null;
		
		if ( currentTarget != null ) 
		{
			if ( MyConstants.DEBUG_MODE ) { System.out.println("Picked enemy "+currentTarget.robot ); }	
			
			if ( rc.canAttackSquare( currentTarget.info.location ) ) 
			{
				if ( rc.isActive() ) {
					rc.attackSquare( currentTarget.info.location );
				}
				state = new Attacking(rc,currentTarget.robot );
				if ( MyConstants.DEBUG_MODE ) { behaviourStateChanged(); }					
				return;
			} 
			
			if ( MyConstants.DEBUG_MODE ) { System.out.println("Enemy too far away, calculating path to enemy "+currentTarget.robot+" at "+currentTarget.info.location ); }
			
			state = new InterruptibleGotoLocation(rc , MovementType.RUN) {

				@Override
				protected boolean hasArrivedAtDestination(MapLocation current, MapLocation dstLoc) {
					return current.equals(dstLoc);
				}
				
				@Override
				public State onLowRobotHealth(double currentRobotHealth) {
					// fight till we drop
					return null;
				}
				
				@Override
				public State onAttack(double currentRobotHealth) {
					// fight till we drop
					return new AttackEnemiesInSight(rc);
				}
				
				@Override
				public boolean setStartAndDestination(AStar finder,boolean retry) throws GameActionException 
				{
					if ( retry ) 
					{
						if ( MyConstants.DEBUG_MODE ) System.out.println("Path finding failure, blacklisting target "+currentTarget);
						enemyBlacklist.add( currentTarget.robot );
						enemyBlacklist.removeStaleEntries();
						
						final Robot[] enemies = rc.senseNearbyGameObjects( Robot.class , MyConstants.SOLDIER_SEEK_ENEMY_RANGE_SQUARED , RobotPlayer.enemyTeam );
						currentTarget = Utils.pickEnemyToAttack( rc , enemies , enemyBlacklist );
						if ( currentTarget == null ) {
							return false;
						}
						if ( rc.canAttackSquare( currentTarget.info.location ) ) { // enemy is in range
							if ( MyConstants.DEBUG_MODE ) System.out.println("Enemy in attack range, not using path finding.");
							return false;
						}
						finder.setRoute( rc.getLocation() , currentTarget.info.location , 10 );
						return true;
					} 
					else if ( currentTarget != null && rc.canSenseObject( currentTarget.robot ) ) 
					{
						RobotInfo ri = rc.senseRobotInfo( currentTarget.robot );
						if ( rc.canAttackSquare( ri.location ) ) { // enemy is in range
							if ( MyConstants.DEBUG_MODE ) System.out.println("Enemy in attack range, not using path finding.");
							return false;
						}						
						finder.setRoute( rc.getLocation() , ri.location , 10 );
						return true;							
					}
					return false;
				}

				@Override
				public TimeoutResult onTimeout() 
				{
					if ( MyConstants.DEBUG_MODE ) System.out.println("Path finding timeout, blacklisting target "+currentTarget);
					enemyBlacklist.add( currentTarget.robot );
					enemyBlacklist.removeStaleEntries();			
					return TimeoutResult.ABORT;
				}
			}.perform();
			if ( MyConstants.DEBUG_MODE ) { behaviourStateChanged(); }
			return;
		}
		
		// home-in on enemy HQ
		if ( RobotPlayer.enemyHQ.distanceSquaredTo( rc.getLocation() ) > MyConstants.SOLIDER_HOMEIN_ON_HQ_DISTANCE_SQUARED ) 
		{
			state = new InterruptibleGotoLocation(rc , MovementType.RUN ) {

				@Override
				protected boolean hasArrivedAtDestination(MapLocation current, MapLocation dstLoc) {
					return current.equals(dstLoc);
				}
				
				@Override
				public State onLowRobotHealth(double currentRobotHealth) {
					return null;
				}
				
				@Override
				public State onAttack(double currentRobotHealth) {
					return new AttackEnemiesInSight(rc);
				}
				
				protected void foundPathHook(java.util.List<MapLocation> path) {
					homeInTimeout = DEFAULT_HOMEIN_TIMEOUT;
				}
				
				protected void foundNoPathHook() throws GameActionException 
				{
					Direction d = Utils.randomMovementDirection(rc);
					if ( d != Direction.NONE ) 
					{
						while ( !rc.isActive() ) {
							rc.yield();
						}
						rc.move(d);
					}
				}

				@Override
				public boolean setStartAndDestination(AStar finder, boolean retry) throws GameActionException 
				{
					if ( retry ) {
						Direction d = Utils.randomMovementDirection(rc);
						if ( d != Direction.NONE ) 
						{
							while ( !rc.isActive() ) {
								rc.yield();
							}
							rc.move(d);
						}
					}
					
					MapLocation dst = Utils.findRandomLocationNear( rc , RobotPlayer.enemyHQ ,  MyConstants.ENEMY_HQ_SAFE_DISTANCE_MIN , MyConstants.ENEMY_HQ_SAFE_DISTANCE_MAX );
					if ( MyConstants.DEBUG_MODE ) System.out.println("Homing in on enemy HQ at "+RobotPlayer.enemyHQ+" , picked destination: "+dst);						
					if ( dst != null ) {
						finder.setRoute( rc.getLocation() , dst , homeInTimeout );
						return true;
					}
					return false;
				}

				@Override
				public TimeoutResult onTimeout() throws GameActionException 
				{
					homeInTimeout=Math.min(MAX_HOMEIN_TIMEOUT , (int) (homeInTimeout*3f ) );	
					if ( MyConstants.DEBUG_MODE ) System.out.println("Home-in timeout is now "+homeInTimeout);
					
					Direction d = Utils.randomMovementDirection(rc);
					if ( d != Direction.NONE ) 
					{
						while ( !rc.isActive() ) {
							rc.yield();
						}
						rc.move(d);
					}					
					return TimeoutResult.ABORT;
				}
			}.perform();
			
			if ( MyConstants.DEBUG_MODE ) { behaviourStateChanged(); }
		} 
		else 
		{
			if ( rc.getActionDelay() < 1 ) 
			{
				wander();
			}
		}
	}
	
	private void wander() throws GameActionException 
	{
		if ( MyConstants.DEBUG_MODE ) System.out.println("Wandering");
		
		if ( rc.isActive() ) 
		{
			Direction d = Utils.randomMovementDirection(rc);
			if ( d != Direction.NONE ) 
			{
					rc.move(d);
			}		
		}
	}
	
	@Override
	public String toString() {
		return "Destroyer";
	}
}
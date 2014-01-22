package team223.behaviours;

import team223.AStar;
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
	
	private static final int MAX_HOMEIN_TIMEOUT = 1000;	

	protected static final int UNKNOWN_HEALTH= -99999;

	private static boolean fullSpeedPathFinding = false;
	
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
		if ( MyConstants.DESTROYER_VERBOSE ) { System.out.println("Sensing enemies, current state: "+state); }
		
		enemyBlacklist.removeStaleEntries();
		
		final Robot[] enemies = rc.senseNearbyGameObjects( Robot.class , MyConstants.SOLDIER_SEEK_ENEMY_RANGE_SQUARED , RobotPlayer.enemyTeam );
		currentTarget = enemies.length > 0 ? Utils.pickEnemyToAttack( rc , enemies, enemyBlacklist ) :null;
		
		if ( currentTarget != null ) 
		{
			if ( MyConstants.DESTROYER_VERBOSE ) { System.out.println("Picked enemy "+currentTarget.robot ); }	
			
			if ( rc.canAttackSquare( currentTarget.info.location ) ) 
			{
				if ( rc.isActive() ) {
					rc.attackSquare( currentTarget.info.location );
				}
				state = new Attacking(rc,currentTarget.robot );
				if ( MyConstants.DESTROYER_VERBOSE ) { behaviourStateChanged(); }					
				return;
			} 
			
			if ( MyConstants.DESTROYER_VERBOSE ) { System.out.println("Enemy too far away, calculating path to enemy "+currentTarget.robot+" at "+currentTarget.info.location ); }
			
			state = new InterruptibleGotoLocation(rc , MovementType.RUN , false ) {

				@Override
				protected boolean hasArrivedAtDestination(MapLocation current, MapLocation dstLoc) {
					return current.equals(dstLoc);
				}
				
				protected void foundNoPathHook() throws GameActionException {
					if ( MyConstants.DESTROYER_VERBOSE ) System.out.println("foundNoPathHook(): Path finding failure, blacklisting target "+currentTarget);
					enemyBlacklist.add( currentTarget.robot );
					enemyBlacklist.removeStaleEntries();					
				}
				
				@Override
				public boolean setStartAndDestination(boolean retry) throws GameActionException 
				{
					if ( retry ) 
					{
						if ( MyConstants.DESTROYER_VERBOSE ) System.out.println("Path finding failure, blacklisting target "+currentTarget);
						enemyBlacklist.add( currentTarget.robot );
						enemyBlacklist.removeStaleEntries();
						
						final Robot[] enemies = rc.senseNearbyGameObjects( Robot.class , MyConstants.SOLDIER_SEEK_ENEMY_RANGE_SQUARED , RobotPlayer.enemyTeam );
						currentTarget = Utils.pickEnemyToAttack( rc , enemies , enemyBlacklist );
						if ( currentTarget == null ) {
							return false;
						}
						if ( rc.canAttackSquare( currentTarget.info.location ) ) { // enemy is in range
							if ( MyConstants.DESTROYER_VERBOSE ) System.out.println("Enemy in attack range, not using path finding.");
							return false;
						}
						AStar.setRoute( rc.getLocation() , currentTarget.info.location , 10 );
						return true;
					} 
					else if ( currentTarget != null && rc.canSenseObject( currentTarget.robot ) ) 
					{
						RobotInfo ri = rc.senseRobotInfo( currentTarget.robot );
						if ( rc.canAttackSquare( ri.location ) ) { // enemy is in range
							if ( MyConstants.DESTROYER_VERBOSE ) System.out.println("Enemy in attack range, not using path finding.");
							return false;
						}						
						AStar.setRoute( rc.getLocation() , ri.location , 10 );
						return true;							
					}
					return false;
				}

				@Override
				public boolean abortOnTimeout() 
				{
					if ( MyConstants.DESTROYER_VERBOSE ) System.out.println("Path finding timeout, blacklisting target "+currentTarget);
					enemyBlacklist.add( currentTarget.robot );
					enemyBlacklist.removeStaleEntries();			
					return true;
				}
			}.perform();
			
			if ( MyConstants.DESTROYER_VERBOSE ) { behaviourStateChanged(); }
			return;
		}
		
		// home-in on enemy HQ
		if ( RobotPlayer.enemyHQ.distanceSquaredTo( rc.getLocation() ) > MyConstants.SOLIDER_HOMEIN_ON_HQ_DISTANCE_SQUARED ) 
		{
			if ( MyConstants.DESTROYER_VERBOSE ) {
				System.out.println("Too far from enemy HQ, homing in");
			}	

			AStar.reset();
			
			state = new InterruptibleGotoLocation(rc , MovementType.RUN , fullSpeedPathFinding ) {

				@Override
				protected boolean hasArrivedAtDestination(MapLocation current, MapLocation dstLoc) {
					return current.equals(dstLoc);
				}
				
				protected void foundPathHook(java.util.List<MapLocation> path) {
					homeInTimeout = DEFAULT_HOMEIN_TIMEOUT;
				}
				
				protected void foundNoPathHook() throws GameActionException 
				{
					Direction d = Utils.randomMovementDirection(rc);
					if ( d != Direction.NONE && rc.isActive() ) 
					{
						rc.move(d);
					}
				}

				@Override
				public boolean setStartAndDestination(boolean retry) throws GameActionException 
				{
					if ( MyConstants.DESTROYER_VERBOSE ) {
						System.out.println("Homing in on enemy HQ,currently at "+rc.getLocation()+"( retry: "+retry+")");
					}
					
					MapLocation dst = Utils.findRandomLocationNear( rc , RobotPlayer.enemyHQ ,  MyConstants.ENEMY_HQ_SAFE_DISTANCE_MIN , MyConstants.ENEMY_HQ_SAFE_DISTANCE_MAX );
					if ( MyConstants.DESTROYER_VERBOSE ) System.out.println("Homing in on enemy HQ at "+RobotPlayer.enemyHQ+" , picked destination: "+dst);						
					if ( dst != null ) 
					{
						if ( retry ) 
						{
							Direction d = Utils.randomMovementDirection(rc);
							if ( d != Direction.NONE && rc.isActive() ) 
							{
								rc.move(d);
								rc.yield();
							}
						}						
						AStar.setRoute( rc.getLocation() , dst , homeInTimeout );
						return true;
					}
					if ( MyConstants.DESTROYER_VERBOSE ) {
						System.out.println("Failed to find random location near enemy HQ");
					}					
					return false;
				}

				@Override
				public boolean abortOnTimeout() throws GameActionException 
				{
					homeInTimeout=Math.min(MAX_HOMEIN_TIMEOUT , (int) (homeInTimeout*3f ) );	
					if ( MyConstants.DESTROYER_VERBOSE ) System.out.println("Home-in timeout is now "+homeInTimeout);
					
					Direction d = Utils.randomMovementDirection(rc);
					if ( d != Direction.NONE ) 
					{
						while ( !rc.isActive() ) {
							rc.yield();
						}
						rc.move(d);
					}					
					return true;
				}
			}.perform();
			
			if ( MyConstants.DESTROYER_VERBOSE ) {
				System.out.println("Path finding returned");
			}
			
			fullSpeedPathFinding = false;
			
			if ( MyConstants.DESTROYER_VERBOSE ) { behaviourStateChanged(); }
		} 
		else 
		{
			if ( rc.getActionDelay() < 1 ) 
			{
				wander();
			} else {
				if ( MyConstants.DESTROYER_VERBOSE ) {
					System.out.println("Not wandering, action delay: "+rc.getActionDelay());
				}					
			}
		}
	}
	
	private void wander() throws GameActionException 
	{
		if ( MyConstants.DESTROYER_VERBOSE ) System.out.println("Wandering");
		
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
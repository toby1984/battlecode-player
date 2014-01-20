package team223.behaviours;

import team223.*;
import team223.AStar.TimeoutResult;
import team223.Utils.RobotAndInfo;
import team223.states.AttackEnemiesInSight;
import team223.states.Attacking;
import team223.states.InterruptibleGotoLocation;
import battlecode.common.*;

public final class DestroyerBehaviour extends RobotBehaviour {

	protected static final int UNKNOWN_HEALTH= -99999;
	
	private boolean checkForEnemiesAtEachStep = true;
	
	private final EnemyBlacklist enemyBlacklist = new EnemyBlacklist(110);
	
	private RobotAndInfo currentTarget;
	
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
		
		final Robot[] enemies = rc.senseNearbyGameObjects( Robot.class , MyConstants.SOLDIER_SEEK_ENEMY_RANGE_SQUARED , RobotPlayer.enemyTeam );
		
		enemyBlacklist.removeStaleEntries();
		
		currentTarget = Utils.pickEnemyToAttack( rc , enemies, enemyBlacklist );
		if ( currentTarget != null ) 
		{
			checkForEnemiesAtEachStep = false;
			
			if ( MyConstants.DEBUG_MODE ) { System.out.println("Picked enemy "+currentTarget.robot ); }	
			
			if ( rc.canAttackSquare( currentTarget.info.location ) ) {
				state = new Attacking(rc,currentTarget.robot ).perform();
				if ( MyConstants.DEBUG_MODE ) { behaviourStateChanged(); }
				return;
			} 
			
			if ( MyConstants.DEBUG_MODE ) { System.out.println("Enemy too far away, calculating path to enemy "+currentTarget.robot+" at "+currentTarget.info.location ); }
			
			state = new InterruptibleGotoLocation(rc , MovementType.RUN, 10 ) {

				@Override
				protected boolean hasArrivedAtDestination(MapLocation current, MapLocation dstLoc) {
					return current.equals(dstLoc);
				}
				
				@Override
				public State onLowRobotHealth(double currentRobotHealth) {
					// we fight till be drop
					return null;
				}
				
				@Override
				public State onAttack(double currentRobotHealth) {
					// we fight till be drop
					return new AttackEnemiesInSight(rc);
				}
				
				@Override
				public boolean isInvokeBeforeMove() {
					return checkForEnemiesAtEachStep;
				}				
				
				@Override
				protected State beforeEachMove() 
				{
					Robot[] enemies = rc.senseNearbyGameObjects( Robot.class , RobotType.SOLDIER.attackRadiusMaxSquared , RobotPlayer.enemyTeam );
					if ( enemies.length > 0 ) {
						if ( MyConstants.DEBUG_MODE ) { System.out.println("Enemies in attack range , interrupting path finding to enemy"); }
						return new AttackEnemiesInSight(rc);
					}
					return null;
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
						finder.setRoute( rc.getLocation() , currentTarget.info.location);
						return true;
					} 
					else if ( currentTarget != null && rc.canSenseObject( currentTarget.robot ) ) 
					{
						RobotInfo ri = rc.senseRobotInfo( currentTarget.robot );
						finder.setRoute( rc.getLocation() , ri.location );
						return true;							
					}
					return false;
				}

				@Override
				public TimeoutResult onTimeout() {
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
			state = new InterruptibleGotoLocation(rc , MovementType.RUN, 30 ) {

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
				
				@Override
				public boolean isInvokeBeforeMove() {
					return checkForEnemiesAtEachStep;
				}
				
				@Override
				protected State beforeEachMove() 
				{
					Robot[] enemies = rc.senseNearbyGameObjects( Robot.class , RobotType.SOLDIER.attackRadiusMaxSquared , RobotPlayer.enemyTeam );
					if ( enemies.length > 0 ) {
						if ( MyConstants.DEBUG_MODE ) System.out.println("Interrupting path finding to enemy HQ, enemies in range.");
						return new AttackEnemiesInSight(rc);
					}
					return null;
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
						finder.setRoute( rc.getLocation() , dst );
						return true;
					}
					return false;
				}

				@Override
				public TimeoutResult onTimeout() throws GameActionException {
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
			if ( MyConstants.DEBUG_MODE ) System.out.println("Wandering");					
			Direction d = Utils.randomMovementDirection(rc);
			if ( d != Direction.NONE ) 
			{
				while ( !rc.isActive() ) {
					rc.yield();
				}
				rc.move(d);
			}
		}
	}
	
	@Override
	public String toString() {
		return "Destroyer";
	}
}
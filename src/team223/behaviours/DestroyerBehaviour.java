package team223.behaviours;

import team223.AStar;
import team223.FastRandom;
import team223.MyConstants;
import team223.RobotBehaviour;
import team223.RobotPlayer;
import team223.State;
import team223.Utils;
import team223.Utils.RobotAndInfo;
import team223.states.AttackEnemiesInSight;
import team223.states.Attacking;
import team223.states.Fleeing;
import team223.states.InterruptibleGotoLocation;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.MovementType;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public final class DestroyerBehaviour extends RobotBehaviour {

	protected static final int UNKNOWN_HEALTH= -99999;
	
	private boolean checkForEnemiesAtEachStep = true;
	
	public DestroyerBehaviour(final RobotController rc) {
		super(rc);
	}
	
	@Override
	public void perform() throws GameActionException {

		if ( ! rc.isActive() ) {
			return;
		}
		
		if ( state instanceof Fleeing ) {
			state = state.perform();
			return;
		} 
		
		Robot[] enemies=null;
		if ( rc.getHealth() < MyConstants.FLEE_HEALTH ) 
		{
			enemies = Utils.findEnemies( rc , RobotType.SOLDIER.attackRadiusMaxSquared );
			if ( Utils.getEstimatedHealOfThreats( rc , enemies ) >= rc.getHealth() ) 
			{ 
				state = new Fleeing( rc );
				if ( MyConstants.DEBUG_MODE ) { behaviourStateChanged(); }
				state = state.perform();
				return;
			}
		}
		
		if ( state instanceof Attacking ) {
			state = state.perform();
			return;
		}		
		
		if ( state instanceof InterruptibleGotoLocation) {
			state = state.perform();
			return;
		}
		
		if ( enemies == null ) {
			enemies = Utils.findEnemies(rc , MyConstants.SOLDIER_SEEK_ENEMY_RANGE_SQUARED);
		}
		
		final RobotAndInfo closestEnemy = Utils.pickEnemyToAttack( rc , enemies);
		if ( closestEnemy != null ) 
		{
			checkForEnemiesAtEachStep = false;
			
			if ( MyConstants.DEBUG_MODE ) { System.out.println("Picked enemy "+closestEnemy.robot ); }	
			
			if ( rc.canAttackSquare( closestEnemy.info.location ) ) {
				state = new Attacking(rc,closestEnemy.robot );
				if ( MyConstants.DEBUG_MODE ) { behaviourStateChanged(); }
				state = state.perform();
				return;
			} 
			
			if ( MyConstants.DEBUG_MODE ) { System.out.println("Enemy too far away, calculating path to enemy "+closestEnemy.robot ); }
			
			state = new InterruptibleGotoLocation(rc , MovementType.RUN) {

				@Override
				protected boolean hasArrivedAtDestination(MapLocation current, MapLocation dstLoc) {
					final int dx = Math.abs( dstLoc.x - current.x );
					final int dy = Math.abs( dstLoc.y - current.y );
					return dx <= 1 && dy <= 1;					
				}
				
				@Override
				public boolean isInvokeBeforeMove() {
					return checkForEnemiesAtEachStep;
				}				
				
				@Override
				protected State beforeEachMove() 
				{
					Robot[] enemies = Utils.findEnemies(rc , RobotType.SOLDIER.attackRadiusMaxSquared );
					if ( enemies.length > 0 ) {
						return new AttackEnemiesInSight(rc);
					}
					return null;
				}

				@Override
				public boolean setStartAndDestination(AStar finder) 
				{
					finder.setRoute( rc.getLocation() , closestEnemy.info.location );
					return true;
				}
			};
			if ( MyConstants.DEBUG_MODE ) { behaviourStateChanged(); }			
			state = state.perform();
			return;
		}
		
		// home-in on enemy HQ
		if ( RobotPlayer.enemyHQ.distanceSquaredTo( rc.getLocation() ) > MyConstants.SOLIDER_HOMEIN_ON_HQ_DISTANCE_SQUARED ) 
		{
			state = new InterruptibleGotoLocation(rc , MovementType.RUN) {

				@Override
				protected boolean hasArrivedAtDestination(MapLocation current, MapLocation dstLoc) {
					final int dx = Math.abs( dstLoc.x - current.x );
					final int dy = Math.abs( dstLoc.y - current.y );
					return dx <= 1 && dy <= 1;					
				}
				
				@Override
				public boolean isInvokeBeforeMove() {
					return checkForEnemiesAtEachStep;
				}
				
				@Override
				protected State beforeEachMove() 
				{
					Robot[] enemies = Utils.findEnemies(rc , RobotType.SOLDIER.attackRadiusMaxSquared );
					if ( enemies.length > 0 ) {
						return new AttackEnemiesInSight(rc);
					}
					return null;
				}				

				@Override
				public boolean setStartAndDestination(AStar finder) 
				{
					MapLocation dst = Utils.findRandomLocationNear( rc , RobotPlayer.enemyHQ , 
							MyConstants.ENEMY_HQ_SAFE_DISTANCE ,  
							MyConstants.ENEMY_HQ_SAFE_DISTANCE*2);
					if ( dst != null ) {
						finder.setRoute( rc.getLocation() , dst );
						return true;
					}
					return false;
				}
			};
			
			if ( MyConstants.DEBUG_MODE ) { behaviourStateChanged(); }
			state = state.perform();
		} 
		else 
		{
			Direction d = Utils.randomMovementDirection(rc);
			if ( d != Direction.NONE ) {
				if ( rc.getLocation().add( d ).distanceSquaredTo( RobotPlayer.enemyHQ ) > RobotType.HQ.attackRadiusMaxSquared ) {
					rc.move(d);
				}
			}
		}
	}
	
	@Override
	public String toString() {
		return "Destroyer";
	}
}
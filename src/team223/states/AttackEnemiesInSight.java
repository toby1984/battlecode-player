package team223.states;

import team223.MyConstants;
import team223.State;
import team223.Utils;
import team223.Utils.RobotAndInfo;
import battlecode.common.GameActionException;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public final class AttackEnemiesInSight extends State {

	private State activeState;

	public AttackEnemiesInSight(RobotController rc) {
		super(rc);
	}
	
	@Override
	public State perform() throws GameActionException 
	{
		if ( activeState != null ) {
			activeState = activeState.perform();
			return this;
		}
		
		Robot[] enemies = rc.senseNearbyGameObjects(Robot.class , rc.getLocation() , RobotType.SOLDIER.attackRadiusMaxSquared , rc.getTeam().opponent() );
		
		if ( MyConstants.ATTACK_IN_SIGHT_VERBOSE ) System.out.println("Sensed "+enemies.length+" enemies in attack range");			
		if ( MyConstants.DEBUG_MODE) System.out.println("AttackEnemiesInSight - sensed "+enemies.length+" enemies around "+rc.getLocation());
		
		if ( enemies.length > 0 ) 
		{
			RobotAndInfo currentEnemy = Utils.pickEnemyToAttack( rc , enemies , null );		
			if ( currentEnemy != null ) 
			{
				if ( MyConstants.DEBUG_MODE) System.out.println("Attacking "+currentEnemy);
				activeState = new Attacking( rc , currentEnemy.robot , false );				
				if ( rc.isActive() ) {
					rc.attackSquare( currentEnemy.info.location );
					rc.yield();
				}
				return this;
			}
		}
		if ( MyConstants.DEBUG_MODE) System.out.println("AttackEnemiesInSight - no more enemies to attack, returning");
		return null;
	}
	
    @Override
    public String toString() {
    	return getClass().getSimpleName();
    }	
}
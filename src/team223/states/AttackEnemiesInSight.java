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

	private static final boolean VERBOSE = false;
	
	private Robot[] enemies;

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
		
		enemies = rc.senseNearbyGameObjects(Robot.class , rc.getLocation() , RobotType.SOLDIER.attackRadiusMaxSquared , rc.getTeam().opponent() );
		if ( VERBOSE ) System.out.println("Sensed "+enemies.length+" enemies in attack range");			
		if ( MyConstants.DEBUG_MODE) System.out.println("AttackEnemiesInSight - sensed "+enemies.length+" enemies around "+rc.getLocation());
		
		RobotAndInfo currentEnemy = Utils.pickEnemyToAttack( rc , enemies );		
		if ( currentEnemy != null ) {
			activeState = new Attacking( rc , currentEnemy.robot , rc.senseEnemyHQLocation() , false );
			activeState = activeState.perform();
			return this;
		}
		return null;
	}

    @Override
    public String toString() {
    	return getClass().getSimpleName();
    }	
}
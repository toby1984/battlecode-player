package myplayer.behaviours;

import myplayer.IRobotBehaviour;
import myplayer.State;
import myplayer.Utils;
import myplayer.states.AttackEnemy;
import myplayer.states.MoveToEnemyHQ;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class DestroyerBehaviour implements IRobotBehaviour {

	private static final int DST_TO_HQ_SQUARED = 10;
	
	private static final int ATTACK_ENEMY_DST_SQUARED = 12;
	
	private State state;
	
	public DestroyerBehaviour() {
	}
	
	@Override
	public void perform(RobotController rc) throws GameActionException {

		if ( state instanceof AttackEnemy ) {
			state = state.perform( rc );
			return;
		}
		
		Robot closestEnemy = Utils.findClosestEnemy( rc , ATTACK_ENEMY_DST_SQUARED );
		if ( closestEnemy != null ) 
		{
			state = new AttackEnemy(closestEnemy);
			state = state.perform( rc );
			return;
		}
		
		final MapLocation enemyHQLocation = rc.senseEnemyHQLocation();
		if ( enemyHQLocation.distanceSquaredTo( rc.getLocation() ) > DST_TO_HQ_SQUARED ) 
		{
			if ( state == null || ! (state instanceof MoveToEnemyHQ ) ) {
				state = new MoveToEnemyHQ();
			}
		} 
		else 
		{
			// wander
			// System.out.println("Wandering");
			for ( int retry = 8 ; retry > 0 ; retry-- ) {
				Direction d = Utils.randomDirection();
				if ( rc.canMove( d ) ) {
					rc.move(d);
					break;
				}
			}
			return;
		}
		
		if ( state != null ) {
			state = state.perform( rc );
		}
	}
	
	@Override
	public String toString() {
		return "Destroyer";
	}
}

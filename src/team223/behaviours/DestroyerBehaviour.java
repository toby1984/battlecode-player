package team223.behaviours;

import team223.FastRandom;
import team223.IRobotBehaviour;
import team223.MyConstants;
import team223.State;
import team223.Utils;
import team223.states.ApproachEnemyHQ;
import team223.states.Attacking;
import team223.states.Fleeing;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;

public class DestroyerBehaviour implements IRobotBehaviour {

	private State state;
	
	private final FastRandom random;
	
	public DestroyerBehaviour(FastRandom random) {
		this.random = random;
	}
	
	@Override
	public void perform(RobotController rc) throws GameActionException {

		if ( ! rc.isActive() ) {
			return;
		}
		
		if ( state instanceof Fleeing ) {
			state = state.perform( rc );
			return;
		} 
		
		Robot[] enemies=null;
		if ( rc.getHealth() < 50 ) 
		{
//			enemies = Utils.findEnemies(rc , MyConstants.SOLDIER_SEEK_ENEMY_RANGE_SQUARED);
//			
//			// gauge threats
//			double enemyHealth = 0;
//			for ( Robot r : enemies ) 
//			{
//				RobotInfo ri = rc.senseRobotInfo( r ); 
//				if ( Utils.isThreat( ri ) ) {
//					enemyHealth += ri.health;
//				}
//			}
//			
//			if ( enemyHealth > rc.getHealth() ) {
				state = new Fleeing( random );
				state = state.perform( rc );
				return;
//			}
		}
		
		if ( state instanceof Attacking ) {
			state = state.perform( rc );
			return;
		}
		
		if ( enemies == null ) {
			enemies = Utils.findEnemies(rc , MyConstants.SOLDIER_SEEK_ENEMY_RANGE_SQUARED);
		}
		
		Robot closestEnemy = Utils.findClosestEnemy( rc , enemies);
		if ( closestEnemy != null ) 
		{
			state = new Attacking(closestEnemy);
			state = state.perform( rc );
			return;
		}
		
		// home-in on enemy HQ
		final MapLocation enemyHQLocation = rc.senseEnemyHQLocation();
		if ( enemyHQLocation.distanceSquaredTo( rc.getLocation() ) > MyConstants.SOLIDER_HOMEIN_ON_HQ_DISTANCE_SQUARED ) 
		{
			if ( state == null || ! (state instanceof ApproachEnemyHQ ) ) {
				state = new ApproachEnemyHQ(random);
			}
		} 
		else 
		{
			// wander around the HQ
			Direction d = Utils.randomMovementDirection(random,rc);
			if ( d != Direction.NONE ) {
				rc.move(d);
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
package lowflyingcows.behaviours;

import lowflyingcows.*;
import lowflyingcows.states.ApproachEnemyHQ;
import lowflyingcows.states.Attacking;
import lowflyingcows.states.Fleeing;
import battlecode.common.*;

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
		else if ( rc.getHealth() < 50 ) 
		{
			state = new Fleeing( random );
			state = state.perform( rc );
			return;
		}
		
		if ( state instanceof Attacking ) {
			state = state.perform( rc );
			return;
		}
		
		Robot[] enemies = Utils.findEnemies(rc , MyConstants.SOLDIER_SEEK_ENEMY_RANGE_SQUARED);
		
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

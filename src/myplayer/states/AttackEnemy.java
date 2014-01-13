package myplayer.states;

import myplayer.State;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class AttackEnemy extends State {

	private final Robot enemy;
	
	public AttackEnemy(Robot enemy) {
		this.enemy=enemy;
	}
	
	@Override
	public State perform(RobotController rc) throws GameActionException 
	{
		if ( ! rc.canSenseObject( enemy ) || enemyIsDead(rc) ) {
			return null; // either dead or out of range
		}
		
		MapLocation enemyLocation = rc.senseLocationOf( enemy );
		
		if ( rc.canAttackSquare( enemyLocation ) ) 
		{
			rc.attackSquare( enemyLocation );
		} else {
			// move towards enemy
			Direction direction = rc.getLocation().directionTo( enemyLocation );
			if ( rc.canMove( direction ) ) {
				rc.move( direction );
			}
		}
		return this;
	}

	private boolean enemyIsDead(RobotController rc) throws GameActionException 
	{
		final RobotInfo info = rc.senseRobotInfo( (Robot) enemy );
		return info.health <= 0;
	}
}

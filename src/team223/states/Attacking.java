package team223.states;

import team223.State;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class Attacking extends State {

	private final Robot enemy;
	
	public Attacking(Robot enemy) {
		this.enemy=enemy;
	}
	
	@Override
	public State perform(RobotController rc) throws GameActionException 
	{
		if ( ! rc.canSenseObject( enemy ) || enemyIsDead(rc) ) {
			return null; // either dead or out of sensor range
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

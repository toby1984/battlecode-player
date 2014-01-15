package team223.states;

import team223.FastRandom;
import team223.MyConstants;
import team223.State;
import team223.Utils;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;

public class Fleeing extends State {

	private static final int ATTACK_ENEMY_DST_SQUARED = 12;
	
	private FastRandom rnd;
	
	public Fleeing(FastRandom rnd) {
		this.rnd=rnd;
	}
	
	@Override
	public State perform(RobotController rc) throws GameActionException 
	{
		if ( rc.getHealth() < 50 ) 
		{
			Robot[] enemies = Utils.findEnemies(rc , ATTACK_ENEMY_DST_SQUARED);
			MapLocation centerOfMass = Utils.getCenterOfMass( rc , enemies );
			if ( centerOfMass != null ) 
			{
				Direction d = rc.getLocation().directionTo( centerOfMass ).opposite();
				if ( d == Direction.OMNI ) {
					d = Utils.randomMovementDirection( rnd , rc );
					if ( d != Direction.NONE ) 
					{
						rc.move(d);
					} else {
						if ( MyConstants.DEBUG_MODE) System.out.println("Nowhere to escape?");
					} 
					return this;
				}
				for ( int retries = 7 ; retries > 0 ; retries--) {
					if ( rc.canMove( d ) ) {
						rc.move( d );
						return this;
					}
					d = d.rotateLeft();
				}
			}
			return this;			
		}
		return null;
	}

}

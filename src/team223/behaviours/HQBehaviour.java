package team223.behaviours;

import team223.*;
import team223.states.Attacking;
import battlecode.common.*;

public class HQBehaviour extends RobotBehaviour{

	private final FastRandom rnd;
	
	public HQBehaviour(FastRandom rnd) {
		this.rnd=rnd;
	}
	
	@Override
	public void perform(RobotController rc) throws GameActionException 
	{
		// check if a robot is spawnable and spawn one if it is
		if ( ! rc.isActive() ) {
			return;
		}
		
		if ( state instanceof Attacking) {
			state = state.perform( rc );
			return;
		}
		
		final Robot[] enemies = Utils.findEnemies( rc , RobotType.HQ.attackRadiusMaxSquared );
		Robot enemy = Utils.findClosestEnemy( rc , enemies );
		if ( enemy != null ) 
		{
			state = new Attacking( enemy );
			if ( MyConstants.DEBUG_MODE ) { changedBehaviour(rc); }
			if ( MyConstants.DEBUG_MODE) System.out.println("HQ is attacking #"+enemy.getID());
			state.perform( rc );
			return;
		}
		
		if ( rc.senseRobotCount() < GameConstants.MAX_ROBOTS ) 
		{
			// spawn robot at random location
			for ( int retry = 8 ; retry > 0 ; retry-- ) 
			{
				Direction direction = Utils.randomDirection(rnd);
				if ( rc.senseObjectAtLocation( rc.getLocation().add( direction ) ) == null) 
				{
					rc.spawn(direction);
					return;
				}
			}
		}		
	}

}

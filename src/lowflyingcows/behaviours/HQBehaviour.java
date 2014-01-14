package lowflyingcows.behaviours;

import lowflyingcows.*;
import lowflyingcows.states.Attacking;
import battlecode.common.*;

public class HQBehaviour implements IRobotBehaviour{

	private final FastRandom rnd;
	
	private State state;
	
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
			state.perform( rc );
			return;
		}
		
		if ( rc.senseRobotCount() < GameConstants.MAX_ROBOTS ) 
		{
			// spawn robot at random location
			for ( int retry = 4 ; retry > 0 ; retry-- ) 
			{
				Direction direction = Utils.randomDirection(rnd);
				if ( rc.senseObjectAtLocation( rc.getLocation().add( direction ) ) == null) 
				{
					rc.spawn(direction);
					break;
				}
			}
		}		
	}

}

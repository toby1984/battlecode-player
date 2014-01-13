package myplayer.behaviours;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;
import myplayer.IRobotBehaviour;
import myplayer.Utils;

public class HQBehaviour implements IRobotBehaviour {

	
	@Override
	public void perform(RobotController rc) throws GameActionException {

		// check if a robot is spawnable and spawn one if it is
		if ( rc.isActive() && rc.senseRobotCount() < GameConstants.MAX_ROBOTS ) 
		{
			// try to spawn robot at random location
			for ( int retry = 4 ; retry > 0 ; retry-- ) 
			{
				Direction direction = Utils.randomDirection();
				if ( rc.senseObjectAtLocation( rc.getLocation().add( direction ) ) == null) 
				{
					rc.spawn(direction);
					break;
				}
			}
		}		
	}
}

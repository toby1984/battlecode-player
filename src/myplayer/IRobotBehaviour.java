package myplayer;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public interface IRobotBehaviour {

	public void perform(RobotController rc) throws GameActionException ;
}

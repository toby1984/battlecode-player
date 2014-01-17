package team223;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public abstract class State {

	protected final RobotController rc;
	
	public State(RobotController rc) {
		this.rc = rc;
	}
	
	public abstract State perform() throws GameActionException ;
}

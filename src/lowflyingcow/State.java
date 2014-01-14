package lowflyingcow;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public abstract class State {

	public abstract State perform(RobotController rc) throws GameActionException ;
}

package team223;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public interface IRobotBehaviour {

	public static final IRobotBehaviour NOP_BEHAVIOUR = new IRobotBehaviour() {
		
		@Override
		public void perform(RobotController rc) throws GameActionException {
		}
	};
	
	public void perform(RobotController rc) throws GameActionException ;
}

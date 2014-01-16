package team223;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public abstract class RobotBehaviour {

	protected volatile State state;
	
	public static final RobotBehaviour NOP_BEHAVIOUR = new RobotBehaviour() {
		
		@Override
		public void perform(RobotController rc) throws GameActionException {
		}
	};
	
	public abstract void perform(RobotController rc) throws GameActionException;
	
	public final void changedBehaviour(RobotController rc) {
		if ( state == null ) {
			System.out.println("(*) State change: <NONE>");
		} else {
			System.out.println("(*) State change: "+state);
		}
		rc.setIndicatorString(1 , state.toString() );
	}
}

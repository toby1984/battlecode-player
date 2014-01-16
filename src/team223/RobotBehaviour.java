package team223;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public abstract class RobotBehaviour {

	protected volatile State state;
	
	protected final MapLocation enemyHQLocation;
	
	public static final RobotBehaviour NOP_BEHAVIOUR = new RobotBehaviour(null) {
		
		@Override
		public void perform(RobotController rc) throws GameActionException {
		}
	};
	
	public RobotBehaviour(MapLocation enemyHQLocation) {
		this.enemyHQLocation = enemyHQLocation;
	}
	
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

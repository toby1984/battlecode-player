package team223;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public abstract class RobotBehaviour {

	protected static State state;
	
	protected static RobotController rc;
	
	public static final RobotBehaviour NOP_BEHAVIOUR = new RobotBehaviour(null) {
		
		@Override
		public void perform() throws GameActionException {
		}
	};
	
	public RobotBehaviour(RobotController rc) {
		RobotBehaviour.rc = rc;
	}
	
	public abstract void perform() throws GameActionException;
	
	public final void changedBehaviour(RobotController rc,String message) 
	{
		String text;
		if ( state == null ) {
			text = "(*) State change: <NONE> ("+message+")";
		} else {
			text = "(*) State change: "+state+" ("+message+")";
		}
		rc.setIndicatorString(1 , text );		
	}
	
	public final void behaviourStateChanged() {
		String text;		
		if ( state == null ) {
			text = "(*) State change: <NONE>";
		} else {
			text = "(*) State change: "+state;
		}
		rc.setIndicatorString(1 , text );
	}
}

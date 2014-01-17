package team223;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public abstract class RobotBehaviour {

	protected volatile State state;
	
	protected final MapLocation enemyHQLocation;
	protected final RobotController rc;
	
	public static final RobotBehaviour NOP_BEHAVIOUR = new RobotBehaviour(null,null) {
		
		@Override
		public void perform() throws GameActionException {
		}
	};
	
	public RobotBehaviour(RobotController rc, MapLocation enemyHQLocation) {
		this.enemyHQLocation = enemyHQLocation;
		this.rc = rc;
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
	
	public final void changedBehaviour(RobotController rc) {
		String text;		
		if ( state == null ) {
			text = "(*) State change: <NONE>";
		} else {
			text = "(*) State change: "+state;
		}
		rc.setIndicatorString(1 , text );
	}
}

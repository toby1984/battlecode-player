package team223.states;

import team223.RobotPlayer;
import team223.State;
import battlecode.common.*;

public final class Attacking extends State {

	private final Robot enemy;
	
	private int movementFailures = 0;
	private final boolean moveable;
	
	public Attacking(RobotController rc,Robot enemy) {
		this(rc,enemy,true);
	}
	
	public Attacking(RobotController rc,Robot enemy,boolean moveable) {
		super(rc);
		this.enemy=enemy;
		this.moveable = moveable;
	}
	
	@Override
	public State perform() throws GameActionException 
	{
		if ( ! rc.canSenseObject( enemy ) || rc.senseRobotInfo( (Robot) enemy ).health <= 0 ) {
			return null; // either dead or out of sensor range
		}
		
		MapLocation enemyLocation = rc.senseLocationOf( enemy );
		if ( rc.canAttackSquare( enemyLocation ) ) 
		{
			rc.attackSquare( enemyLocation );
			return this;
		} 
		
		if ( moveable )
		{
			// try to move towards enemy
			final MapLocation myLocation = rc.getLocation();				
			Direction direction = myLocation.directionTo( enemyLocation );
			if ( rc.canMove( direction ) ) 
			{
				// make sure not to get in firing range of enemy HQ
				if ( myLocation.add( direction ).distanceSquaredTo( RobotPlayer.enemyHQ ) > RobotType.HQ.attackRadiusMaxSquared ) {
					rc.move( direction );
					return this;
				}
				return null;
			} 
			movementFailures++;
			if ( movementFailures > 3 ) {
				return null;
			}
			return this;
		}
		
		return null;
	}

    @Override
    public String toString() {
    	return getClass().getSimpleName()+" ("+enemy+")";
    }	
}
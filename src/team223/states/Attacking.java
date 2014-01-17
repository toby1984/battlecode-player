package team223.states;

import team223.State;
import battlecode.common.*;

public final class Attacking extends State {

	private final Robot enemy;
	private final MapLocation enemyHQLocation;
	
	private int movementFailures = 0;
	private final boolean moveable;
	
	public Attacking(RobotController rc,Robot enemy,MapLocation enemyHQLocation) {
		this(rc,enemy,enemyHQLocation,true);
	}
	
	public Attacking(RobotController rc,Robot enemy,MapLocation enemyHQLocation,boolean moveable) {
		super(rc);
		this.enemy=enemy;
		this.enemyHQLocation = enemyHQLocation;
		this.moveable = moveable;
	}
	
	@Override
	public State perform() throws GameActionException 
	{
		if ( ! rc.canSenseObject( enemy ) || enemyIsDead(rc) ) {
			return null; // either dead or out of sensor range
		}
		
		MapLocation enemyLocation = rc.senseLocationOf( enemy );
		if ( rc.canAttackSquare( enemyLocation ) ) 
		{
			rc.attackSquare( enemyLocation );
		} 
		else if ( moveable )
		{
			// move towards enemy
			final MapLocation myLocation = rc.getLocation();				
			Direction direction = myLocation.directionTo( enemyLocation );
			if ( rc.canMove( direction ) ) 
			{
				// make sure not to get in firing range of enemy HQ
				if ( myLocation.add( direction ).distanceSquaredTo( enemyHQLocation ) > RobotType.HQ.attackRadiusMaxSquared ) {
					rc.move( direction );
				} else {
					return null;
				}
			} else {
				movementFailures++;
				if ( movementFailures > 3 ) {
					return null;
				}
			}
		} else {
			return null;
		}
		return this;
	}

	private boolean enemyIsDead(RobotController rc) throws GameActionException 
	{
		final RobotInfo info = rc.senseRobotInfo( (Robot) enemy );
		return info.health <= 0;
	}
	
    @Override
    public String toString() {
    	return getClass().getSimpleName()+" ("+enemy+")";
    }	
}

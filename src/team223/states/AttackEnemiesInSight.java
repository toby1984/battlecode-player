package team223.states;

import team223.MyConstants;
import team223.State;
import battlecode.common.*;

public final class AttackEnemiesInSight extends State {

	private static final boolean VERBOSE = false;
	
	private Robot[] enemies;

	private int roundCount = 0;
	private Robot currentEnemy;

	public AttackEnemiesInSight(RobotController rc) {
		super(rc);
	}
	
	@Override
	public State perform() throws GameActionException 
	{
		roundCount++;
		if ( enemies == null || roundCount > 3 ) {
			roundCount = 0;
			enemies = rc.senseNearbyGameObjects(Robot.class , rc.getLocation() , RobotType.SOLDIER.attackRadiusMaxSquared , rc.getTeam().opponent() );
			if ( VERBOSE ) System.out.println("Sensed "+enemies.length+" enemies in attack range");			
			if ( MyConstants.DEBUG_MODE) System.out.println("AttackEnemiesInSight - sensed "+enemies.length+" enemies around "+rc.getLocation());
		}

		if ( currentEnemy == null || ! rc.canSenseObject( currentEnemy ) ) 
		{
			if ( VERBOSE ) {
				if ( currentEnemy == null ) {
					System.out.println("No current enemy,picking one");
				} else {
					System.out.println("Current enemy is no longer in sensor range");					
				}
			}
			currentEnemy = pickEnemy( rc );
			return currentEnemy != null ? this : null;
		} 

		RobotInfo ri = rc.senseRobotInfo( currentEnemy );
		if ( VERBOSE ) System.out.println("Current enemy: "+ri.robot.getID()+" , type: "+ri.type+", health:"+ri.health+", distance= "+rc.getLocation().distanceSquaredTo( ri.location ) );		
		if ( ri.health <= 0 || ! rc.canAttackSquare( ri.location ) ) {
			if ( VERBOSE ) {
				if ( ri.health <= 0  ) {
					System.out.println("Picking new target [Enemy "+ri.robot.getID()+" has died]");
				} else {
					System.out.println("Picking new target [Enemy "+ri.robot.getID()+" is not in attack range]");					
				}
			}
			currentEnemy = pickEnemy( rc );
			return currentEnemy != null ? this : null;
		}
		rc.attackSquare( ri.location );
		return this;
	}

	private Robot pickEnemy(RobotController rc) throws GameActionException 
	{
		if ( MyConstants.DEBUG_MODE) System.out.println("Picking enemy...");
		
		Robot candidate = null;
		int candidateIndex = -1;
		RobotType candidateType=null;
		
		for (int i = 0; i < enemies.length; i++) 
		{
			Robot r = enemies[i];
			if ( r != null ) 
			{
				if ( rc.canSenseObject( r ) ) 
				{
					RobotInfo robotInfo = rc.senseRobotInfo( r );
					if ( robotInfo.health > 0 && rc.canAttackSquare( robotInfo.location ) ) // TODO: Performance - maybe unnecessary ??
					{
						RobotType type = robotInfo.type;
						if ( type == RobotType.SOLDIER || type == RobotType.PASTR ) 
						{
							if ( candidate == null || ( candidateType ==  RobotType.PASTR && type == RobotType.SOLDIER ) ) { // kill SOLDIERs before trying to kill PASTRs
								candidate = r;
								candidateIndex = i;
								candidateType=type;
								if ( type == RobotType.SOLDIER ) {
									break;
								}
							} 					
						} else {
							if ( MyConstants.DEBUG_MODE) System.out.println("Discarding robot #"+r.getID()+" that is not of required type (actual: "+type+")");								
							enemies[i] = null; // discard , robot not of required type
						} 
					} else {
						if ( MyConstants.DEBUG_MODE) System.out.println("Discarding robot #"+r.getID()+" that is not within attack range");						
						enemies[i] = null; // discard , robot dead or not within attack range					
					}
				} else {
					if ( MyConstants.DEBUG_MODE) System.out.println("Discarding robot #"+r.getID()+" that cannot be sensed");
					enemies[i]=null; // discard, robot not in sensor range
				}
			}
		}
		if ( candidate != null ) {
			enemies[candidateIndex]=null;
		}
		if ( VERBOSE ) System.out.println("Next enemy to destroy: "+candidate);
		return candidate;
	}
	
    @Override
    public String toString() {
    	return getClass().getSimpleName();
    }	
}
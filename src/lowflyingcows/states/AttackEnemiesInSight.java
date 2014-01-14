package lowflyingcows.states;

import lowflyingcows.State;
import battlecode.common.*;

public class AttackEnemiesInSight extends State {

	private Robot[] enemies;

	private int roundCount = 0;
	private Robot currentEnemy;

	@Override
	public State perform(RobotController rc) throws GameActionException 
	{
		roundCount++;
		if ( enemies == null || roundCount > 0 ) {
			roundCount = 0;
			enemies = rc.senseNearbyGameObjects(Robot.class , rc.getLocation() , RobotType.SOLDIER.attackRadiusMaxSquared , rc.getTeam().opponent() );
			System.out.println("AttackEnemiesInSight - sensed "+enemies.length+" enemies around "+rc.getLocation());
		}

		if ( currentEnemy == null || ! rc.canSenseObject( currentEnemy ) ) 
		{
			currentEnemy = pickEnemy( rc );
			return currentEnemy != null ? this : null;
		} 

		RobotInfo ri = rc.senseRobotInfo( currentEnemy );
		if ( ri.health <= 0 || ! rc.canAttackSquare( ri.location ) ) {
			currentEnemy = pickEnemy( rc );
			return currentEnemy != null ? this : null;
		}
		rc.attackSquare( ri.location );
		return this;
	}

	private Robot pickEnemy(RobotController rc) throws GameActionException 
	{
		System.out.println("Picking enemy...");
		
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
							System.out.println("Discarding robot #"+r.getID()+" that is not of required type (actual: "+type+")");								
							enemies[i] = null; // discard , robot not of required type
						} 
					} else {
						System.out.println("Discarding robot #"+r.getID()+" that is not within attack range");						
						enemies[i] = null; // discard , robot dead or not within attack range					
					}
				} else {
					System.out.println("Discarding robot #"+r.getID()+" that cannot be sensed");
					enemies[i]=null; // discard, robot not in sensor range
				}
			}
		}
		if ( candidate != null ) {
			enemies[candidateIndex]=null;
		}
		System.out.println("Next enemy to destroy: "+candidate);
		return candidate;
	}
}
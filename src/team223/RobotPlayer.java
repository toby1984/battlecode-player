package team223;

import team223.behaviours.*;
import battlecode.common.*;

public class RobotPlayer 
{
	public static volatile FastRandom rnd;
	
	private static volatile RobotBehaviour behaviour;
	
	public static MapLocation myHQ;
	public static MapLocation enemyHQ;
	
	public static Team myTeam;
	public static Team enemyTeam;
	
	public static int id;
	
	public static RobotType myType;
	
	public static void run(RobotController rc) 
	{
		int invocationCount = 0;
		int stepsToBackOff=0;
		while(true) 
		{
			try 
			{
				if ( rnd == null ) 
				{
					final Integer id = rc.getRobot().getID(); 
					
					myTeam = rc.getTeam();
					enemyTeam = myTeam.opponent();
					
					RobotPlayer.id = id;
					
					myHQ = rc.senseHQLocation();
					enemyHQ = rc.senseEnemyHQLocation();
					
					myType = rc.getType();
					
					rnd = new FastRandom( (long) (31+31*id.intValue()) );
					
					stepsToBackOff = 5 + rnd.nextInt( 5 );
				}
				
				if ( behaviour == null ) 
				{
					if ( rc.getType() == RobotType.HQ ) {
						behaviour = new HQBehaviour( rc );
						if ( MyConstants.DEBUG_MODE ) {
							System.out.println("Robot is a HQ");
						}
						rc.setIndicatorString( 0 , "HQ");						
					} 
					else if ( rc.getType() != RobotType.PASTR ) 
					{
						int data = rc.readBroadcast( HQBehaviour.HQ_BROADCAST_CHANNEL );
						switch( data ) {
							case HQBehaviour.SPAWN_DESTROYER:
								behaviour = new DestroyerBehaviour( rc );		
								if ( MyConstants.DEBUG_MODE ) {
									System.out.println("Robot is a destroyer");
								}
								rc.setIndicatorString( 0 , "Destroyer");								
								break;
							case HQBehaviour.SPAWN_COWBOY:
								behaviour = new CowboyBehaviour( rc );
								if ( MyConstants.DEBUG_MODE ) {
									System.out.println("Robot is a cowboy");
								}
								rc.setIndicatorString( 0 , "Cowboy");									
								break;
							case HQBehaviour.SPAWN_PASTURE_DESTROYER:
								behaviour = new PastureDestroyerBehaviour( rc );
								if ( MyConstants.DEBUG_MODE ) {
									System.out.println("Robot is a Pasture destroyer");
								}
								rc.setIndicatorString( 0 , "Pasture destroyer");								
								break;
							default:
								throw new RuntimeException("Failed to read broadcast from HQ ?");
						}
					}
					rc.yield();
					continue;
				}
				
				if ( invocationCount > stepsToBackOff || rc.getType() != RobotType.SOLDIER ) 
				{
					behaviour.perform();
				} 
				else 
				{
					// clear spawn area
					invocationCount++;					
					MapLocation myLocation = rc.getLocation();
					Direction opposite = myLocation.directionTo( myHQ ).opposite();
					Direction m = null;
					if ( rc.canMove( opposite ) ) {
						 m = opposite;
					} else if ( rc.canMove( opposite.rotateLeft() ) ) {
						 m = opposite.rotateLeft();
					} else if ( rc.canMove( opposite.rotateRight() ) ) {
						m = opposite.rotateRight();
					}
					if ( m != null && rc.isActive() ) {
						rc.sneak(m);
					}
				}
			} 
			catch (Exception e) {
				e.printStackTrace();
			}
			rc.yield();
		} 
	}	
}
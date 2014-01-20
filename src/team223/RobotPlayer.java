package team223;

import team223.behaviours.*;
import battlecode.common.*;

public class RobotPlayer 
{
	public static FastRandom rnd;
	
	private static RobotBehaviour behaviour;
	
	public static MapLocation myHQ;
	public static MapLocation enemyHQ;
	
	public static Team myTeam;
	public static Team enemyTeam;
	
	public static boolean spawnAreaCleared;
	
	public static int pathFindingMaxTimeout;
	
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
					
					final int mapArea = rc.getMapWidth()*rc.getMapHeight();
					
					if ( mapArea >= (40*40 ) ) {
						pathFindingMaxTimeout = 200;
					} else {
						pathFindingMaxTimeout = 30;
					}
					System.out.println("Map size: "+rc.getMapWidth()+" x "+rc.getMapHeight());
					
					final Integer id = rc.getRobot().getID(); 
					
					myTeam = rc.getTeam();
					enemyTeam = myTeam.opponent();
					
					RobotPlayer.id = id;
					
					myHQ = rc.senseHQLocation();
					enemyHQ = rc.senseEnemyHQLocation();
					
					myType = rc.getType();
					
					rnd = new FastRandom( (long) (31+31*id.intValue()) );
					
					stepsToBackOff = 2 + rnd.nextInt( 2 );
					
					if ( rc.getType() == RobotType.HQ ) 
					{
						behaviour = new HQBehaviour( rc );
						if ( MyConstants.DEBUG_MODE ) {
							System.out.println("Robot is a HQ");
						}
						rc.setIndicatorString( 0 , "HQ");			
						spawnAreaCleared = true;
					} 
					else if ( rc.getType() == RobotType.PASTR ) 
					{
						behaviour = new PastrBehaviour(rc);
						spawnAreaCleared = true;
						rc.yield();
						continue;
					} 
					else 
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
				}
				
				if ( spawnAreaCleared ) 
				{
					behaviour.perform();
				} 
				else 
				{
					// clear spawn area
					spawnAreaCleared = invocationCount++ > stepsToBackOff;
					
					while ( ! rc.isActive() ) {
						rc.yield();
					}
					Direction opposite = rc.getLocation().directionTo( myHQ ).opposite();
					if ( rc.canMove( opposite ) ) 
					{
						rc.sneak( opposite );
					} 
					else if ( rc.canMove( opposite.rotateLeft() ) ) 
					{ 
						rc.sneak( opposite.rotateLeft() );
					} 
					else if ( rc.canMove( opposite.rotateRight() ) ) 
					{
						rc.sneak( opposite.rotateRight() );
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
package team223;

import team223.behaviours.CowboyBehaviour;
import team223.behaviours.DestroyerBehaviour;
import team223.behaviours.HQBehaviour;
import team223.behaviours.PastrBehaviour;
import team223.behaviours.PastureDestroyerBehaviour;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class RobotPlayer 
{
	public static FastRandom rnd;
	
	private static RobotBehaviour behaviour;
	
	public static MapLocation myHQ;
	public static MapLocation enemyHQ;
	
	public static Team myTeam;
	public static Team enemyTeam;
	
	public static boolean leftSpawnArea;
	
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
					final int id = rc.getRobot().getID(); 
					
					myTeam = rc.getTeam();
					enemyTeam = myTeam.opponent();
					
					RobotPlayer.id = id;
					
					myHQ = rc.senseHQLocation();
					enemyHQ = rc.senseEnemyHQLocation();
					
					myType = rc.getType();
					
					rnd = new FastRandom( 31+31*id );
					
					stepsToBackOff = 2 + rnd.nextInt( 2 );
					
					if ( rc.getType() == RobotType.HQ ) 
					{
						behaviour = new HQBehaviour( rc );
						if ( MyConstants.DEBUG_MODE ) {
							System.out.println("Robot is a HQ");
						}
						rc.setIndicatorString( 0 , "HQ");								
						leftSpawnArea = true;
					} 
					else if ( rc.getType() == RobotType.PASTR ) 
					{
						behaviour = new PastrBehaviour(rc);
						leftSpawnArea = true;
						rc.yield();
						continue;
					} 
					else 
					{
						int data = rc.readBroadcast( HQBehaviour.HQ_BROADCAST_CHANNEL );
						switch( data ) 
						{
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
				
				if ( leftSpawnArea ) 
				{
					behaviour.perform();
				} 
				else 
				{
					// clear spawn area
					leftSpawnArea = invocationCount++ > stepsToBackOff;
					
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
			
			if ( MyConstants.DEBUG_BYTECODE_LIMIT ) {
				if ( Clock.getBytecodeNum() > GameConstants.FREE_BYTECODES ) {
					System.out.println("bytecode limit exceeded: "+Clock.getBytecodeNum());
				}
			}
			rc.yield();
		} 
	}	
}
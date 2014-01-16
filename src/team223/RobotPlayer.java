package team223;

import team223.behaviours.*;
import battlecode.common.*;

public class RobotPlayer 
{
	private static volatile FastRandom random;
	private static volatile RobotBehaviour behaviour;
	
	public static void run(RobotController rc) 
	{
		int invocationCount = 0;
		MapLocation myHQ =null;
		
		int stepsToBackOff=0;
		while(true) 
		{
			try 
			{
				final Integer id = rc.getRobot().getID(); 
				if ( random == null ) 
				{
					random = new FastRandom( (long) (31+31*id.intValue()) );
					stepsToBackOff = 5 + random.nextInt( 5 );
					myHQ = rc.senseHQLocation();
				}
				
				if ( behaviour == null ) 
				{
					if ( rc.getType() == RobotType.HQ ) {
						behaviour = new HQBehaviour( random , rc.senseEnemyHQLocation() );
					} 
					else if ( rc.getType() != RobotType.PASTR ) 
					{
						int data = rc.readBroadcast( HQBehaviour.HQ_BROADCAST_CHANNEL );
						switch( data ) {
							case HQBehaviour.SPAWN_DESTROYER:
								behaviour = new DestroyerBehaviour( random , rc.senseEnemyHQLocation() );										
								break;
							case HQBehaviour.SPAWN_COWBOY:
								behaviour = new CowboyBehaviour( rc , random , rc.senseEnemyHQLocation() );								
								break;
							case HQBehaviour.SPAWN_PASTURE_DESTROYER:
								behaviour = new PastureDestroyerBehaviour( random , rc.senseEnemyHQLocation() );
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
					behaviour.perform( rc );
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
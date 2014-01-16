package team223;

import team223.behaviours.*;
import battlecode.common.*;

public class RobotPlayer 
{
	private static FastRandom random;
	private static RobotBehaviour behaviour;
	private static Direction awayFromHQ;	
	
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
					behaviour = chooseRobotBehaviour(rc,id);
					rc.setIndicatorString( 0 , "Type: "+behaviour.getClass().getSimpleName());
					stepsToBackOff = 3 + random.nextInt( 3 );
					myHQ = rc.senseHQLocation();
				}
				invocationCount++;
				if ( invocationCount > stepsToBackOff || rc.getType() != RobotType.SOLDIER ) {
					behaviour.perform( rc );
				} 
				else 
				{
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
					if ( m != null ) {
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
	
	private static RobotBehaviour chooseRobotBehaviour(RobotController rc,int robotID) 
	{
		MapLocation enemyHQLocation = rc.senseEnemyHQLocation();
		switch( rc.getType() ) 
		{
			case HQ:
				if ( MyConstants.DEBUG_MODE) System.out.println("SPAWNED: HQ");
				return new HQBehaviour( random , enemyHQLocation );
			case SOLDIER:
				int kind = robotID % 4;
				switch(kind) {
				case 0:
				case 1:
					if ( MyConstants.DEBUG_MODE) System.out.println("SPAWNED: Cowboy");
					return new CowboyBehaviour(rc,random, enemyHQLocation );	
				case 2:
					if ( MyConstants.DEBUG_MODE) System.out.println("SPAWNED: Ddestroyer");
					return new DestroyerBehaviour(random, enemyHQLocation );							
				case 3:
					if ( MyConstants.DEBUG_MODE) System.out.println("SPAWNED: pasture destroyer");					
					return new PastureDestroyerBehaviour( random , enemyHQLocation );	
				default:
					throw new RuntimeException("Unhandled kind: "+kind);
				}
//				float f = random.nextFloat();
//				if ( f <= 0.4 ) {
//					if ( MyConstants.DEBUG_MODE) System.out.println("SPAWNED: Cowboy");
//					return new CowboyBehaviour(rc,random);	
//				}
//				if ( f <= 0.6 ) {
//					if ( MyConstants.DEBUG_MODE) System.out.println("SPAWNED: pasture destroyer");					
//					return new PastureDestroyerBehaviour( random );					
//				}
//				if ( MyConstants.DEBUG_MODE) System.out.println("SPAWNED: Ddestroyer");
//				return new DestroyerBehaviour(random);				
			default:
				return RobotBehaviour.NOP_BEHAVIOUR;				
		}
	}
}
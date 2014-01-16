package team223;

import team223.behaviours.*;
import battlecode.common.RobotController;

public class RobotPlayer 
{
	private static FastRandom random;
	private static RobotBehaviour behaviour;
	
	public static void run(RobotController rc) 
	{
		while(true) 
		{
			try 
			{
				final Integer id = rc.getRobot().getID(); 
				if ( random == null ) 
				{
					random = new FastRandom( (long) (31+31*id.intValue()) );
				}
				if ( behaviour == null ) {
					behaviour = chooseRobotBehaviour(rc,id);
					rc.setIndicatorString( 0 , "Type: "+behaviour.getClass().getSimpleName());
				}
				behaviour.perform( rc );
			} 
			catch (Exception e) {
				e.printStackTrace();
			}
			rc.yield();
		} 
	}
	
	private static RobotBehaviour chooseRobotBehaviour(RobotController rc,int robotID) 
	{
		switch( rc.getType() ) 
		{
			case HQ:
				if ( MyConstants.DEBUG_MODE) System.out.println("SPAWNED: HQ");
				return new HQBehaviour( random );
			case SOLDIER:
				int kind = rc.getRobot().getID() % 4;
				switch(kind) {
				case 0:
				case 1:
					if ( MyConstants.DEBUG_MODE) System.out.println("SPAWNED: Cowboy");
					return new CowboyBehaviour(rc,random);	
				case 2:
					if ( MyConstants.DEBUG_MODE) System.out.println("SPAWNED: Ddestroyer");
					return new DestroyerBehaviour(random);							
				case 3:
					if ( MyConstants.DEBUG_MODE) System.out.println("SPAWNED: pasture destroyer");					
					return new PastureDestroyerBehaviour( random );	
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
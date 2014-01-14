package lowflyingcow;

import lowflyingcow.behaviours.*;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class RobotPlayer 
{
	private static FastRandom random;
	private static IRobotBehaviour behaviour;
	
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
					behaviour = chooseRobotBehaviour(rc);
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
	
	private static IRobotBehaviour chooseRobotBehaviour(RobotController rc) 
	{
		RobotType robotType = rc.getType();
		switch( robotType ) 
		{
			case HQ:
				System.out.println("Robot is a HQ");
				return new HQBehaviour( random );
			case SOLDIER:
				float f = random.nextFloat();
				if ( f <= 0.2 ) {
					System.out.println("Robot will be a pasture destroyer");					
					return new PastureDestroyerBehaviour( random );
				}
				
				if ( f <= 0.4 ) {
					System.out.println("Robot will be a cowboy");
					return new CowboyBehaviour(rc,random);					
				}
				System.out.println("Robot will be a destroyer");
				return new DestroyerBehaviour(random);				
			default:
				return IRobotBehaviour.NOP_BEHAVIOUR;				
		}
	}
}
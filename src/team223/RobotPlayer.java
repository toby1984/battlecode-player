package team223;

import team223.Utils.RobotAndInfo;
import team223.behaviours.CowboyBehaviour;
import team223.behaviours.DestroyerBehaviour;
import team223.behaviours.HQBehaviour;
import team223.behaviours.PastrBehaviour;
import team223.behaviours.PastureDestroyerBehaviour;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
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

	public static int id;

	public static RobotType myType;

	public static void run(RobotController rc) 
	{
		RobotPlayer.id = rc.getRobot().getID(); 

		AStar.rc = rc;
		AStar.mapWidth = rc.getMapWidth();
		AStar.mapHeight = rc.getMapHeight();

		myTeam = rc.getTeam();
		enemyTeam = myTeam.opponent();

		myHQ = rc.senseHQLocation();
		enemyHQ = rc.senseEnemyHQLocation();

		myType = rc.getType();

		rnd = new FastRandom( 31+31*RobotPlayer.id );

		final int stepsToBackOff;
		if ( rc.getType() == RobotType.HQ ) 
		{
			behaviour = new HQBehaviour( rc );
			if ( MyConstants.DEBUG_MODE ) {
				System.out.println("Robot is a HQ");
			}
			rc.setIndicatorString( 0 , "HQ");
			stepsToBackOff=0;
		} 
		else if ( rc.getType() == RobotType.PASTR ) 
		{
			behaviour = new PastrBehaviour(rc);
			stepsToBackOff=0;
		} 
		else 
		{
			stepsToBackOff = 2 + rnd.nextInt( 2 );
			
			int data;
			try 
			{
				data = rc.readBroadcast( HQBehaviour.HQ_BROADCAST_CHANNEL );
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
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		}
		
		// leave spawn area
		int invocationCount = 0;
		while ( invocationCount++ < stepsToBackOff ) 
		{
			// clear spawn area
			while ( ! rc.isActive() ) 
			{
				rc.yield();
			}			
			Direction opposite = rc.getLocation().directionTo( myHQ ).opposite();
			try 
			{
				if ( rc.canMove( opposite ) ) 
				{
					rc.sneak( opposite );
					rc.yield();					
				} 
				else if ( rc.canMove( opposite.rotateLeft() ) ) 
				{ 
					rc.sneak( opposite.rotateLeft() );
					rc.yield();
				} 
				else if ( rc.canMove( opposite.rotateRight() ) ) 
				{
					rc.sneak( opposite.rotateRight() );
					rc.yield();
				}
			} catch(GameActionException e) {
				e.printStackTrace();
			}
		}
		
		while(true) 
		{
			try 
			{
				final Robot[] enemies = rc.senseNearbyGameObjects( Robot.class , myType.attackRadiusMaxSquared , enemyTeam );
				if ( enemies.length > 0 ) 
				{
					RobotAndInfo target = Utils.pickEnemyToAttack( rc , enemies , null );
					if ( target != null && rc.isActive() ) 
					{
						rc.attackSquare( target.info.location );
					}
				}
				behaviour.perform();
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
package myplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Random;

import myplayer.behaviours.CowboyBehaviour;
import myplayer.behaviours.DestroyerBehaviour;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class RobotPlayer 
{
	private static final Random rnd = new Random();

	public static final int BORDER_LEFT = 1;
	public static final int BORDER_RIGHT = 2;
	public static final int BORDER_TOP = 4;
	public static final int BORDER_BOTTOM = 8;
	
	private static final Map<Integer,IRobotBehaviour> ROBOT_DATA = new Hashtable<Integer, IRobotBehaviour>();
	
	public static void run(RobotController rc) 
	{
		while(true) 
		{
			try 
			{
				if (rc.getType() == RobotType.HQ) 
				{
					// spawn robot if possible
					handleHeadQuarter(rc);
				} 
				else if (rc.getType() == RobotType.SOLDIER && rc.isActive() ) 
				{
					handleSoldier(rc);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			rc.yield();
		} 
	}
	
	private static void handleSoldier(RobotController rc) throws GameActionException 
	{
		final Integer id = rc.getRobot().getID();
		IRobotBehaviour behaviour = ROBOT_DATA.get(id);
		if ( behaviour == null ) 
		{
			float f = Utils.rnd.nextFloat();
			System.out.println("*** float = "+f);
			if ( f <= 0.6 ) {
				behaviour = new DestroyerBehaviour();
			}
			else { 
				behaviour = new CowboyBehaviour();
			}
			System.out.println("Created "+behaviour);
			ROBOT_DATA.put( id , behaviour);
		}
		behaviour.perform( rc );
	}
	
	private static void handleHeadQuarter(RobotController rc) throws GameActionException 
	{
		// check if a robot is spawnable and spawn one if it is
		if ( rc.isActive() && rc.senseRobotCount() < GameConstants.MAX_ROBOTS ) 
		{
			// try to spawn robot at random location
			for ( int retry = 4 ; retry > 0 ; retry-- ) 
			{
				Direction direction = Utils.randomDirection();
				if ( rc.senseObjectAtLocation( rc.getLocation().add( direction ) ) == null) 
				{
					rc.spawn(direction);
					break;
				}
			}
		}
	}
    
}
package team223.behaviours;

import team223.FastRandom;
import team223.MyConstants;
import team223.RobotBehaviour;
import team223.RobotPlayer;
import team223.Utils;
import team223.Utils.RobotAndInfo;
import team223.states.Attacking;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public final class HQBehaviour extends RobotBehaviour {

	private static final boolean VERBOSE = MyConstants.DEBUG_MODE;
	
	public static final int SPAWN_DESTROYER = 1;
	public static final int SPAWN_COWBOY = 2;
	public static final int SPAWN_PASTURE_DESTROYER  = 3;
	
	public static final int HQ_BROADCAST_CHANNEL = 42;
	
	private static int destroyerCount=0;
	private static int pastureDestroyerCount=0;
	private static int cowboyCount=0;
	
	public HQBehaviour(RobotController rc) {
		super(rc);
	}
	
	@Override
	public void perform() throws GameActionException 
	{
		// check if a robot is spawnable and spawn one if it is
		if ( ! rc.isActive() ) {
			return;
		}
		
		if ( state instanceof Attacking) {
			state = state.perform();
			return;
		}
		
		final Robot[] enemies = rc.senseNearbyGameObjects( Robot.class , RobotType.HQ.attackRadiusMaxSquared , RobotPlayer.enemyTeam );
		RobotAndInfo enemy = Utils.pickEnemyToAttack( rc , enemies );
		if ( enemy != null ) 
		{
			state = new Attacking( rc , enemy.robot, false );
			if ( MyConstants.DEBUG_MODE ) { behaviourStateChanged(); }
			if ( MyConstants.DEBUG_MODE) System.out.println("HQ is attacking #"+enemy.robot.getID());
			state.perform();
			return;
		}
		
		if ( rc.senseRobotCount() < GameConstants.MAX_ROBOTS ) 
		{
			// spawn robot at random location
			for ( int retry = 4 ; retry > 0 ; retry-- ) 
			{
				Direction direction = Utils.randomDirection();
				MapLocation loc = rc.getLocation().add( direction );
				TerrainTile tileType = rc.senseTerrainTile( loc );
				if ( ( tileType == TerrainTile.NORMAL || tileType == TerrainTile.ROAD)  && rc.senseObjectAtLocation( loc ) == null) 
				{
					int spawnType;
					if ( destroyerCount == 0 ) {
						spawnType = SPAWN_DESTROYER;	
						if ( VERBOSE ) System.out.println("Creating destroyer ( destroyers: "+destroyerCount+" / cowbows: "+cowboyCount+" / pastr destroyer: "+pastureDestroyerCount+")");
						destroyerCount++;
					} else {
						final int expectedCowboyCount = (int) Math.ceil( destroyerCount*0.8f );
						final int expectedPastureDestroyerCount = (int) Math.ceil( destroyerCount*0.4f );			
						
						if ( cowboyCount < expectedCowboyCount ) {
							spawnType = SPAWN_COWBOY;
							if ( VERBOSE ) System.out.println("Creating cowboy ( destroyers: "+destroyerCount+" / cowbows: "+cowboyCount+" / pastr destroyer: "+pastureDestroyerCount+")");							
							cowboyCount++;	
						} else if ( pastureDestroyerCount < expectedPastureDestroyerCount ) {
							spawnType = SPAWN_PASTURE_DESTROYER;
							if ( VERBOSE ) System.out.println("Creating pasture destroyer ( destroyers: "+destroyerCount+" / cowbows: "+cowboyCount+" / pastr destroyer: "+pastureDestroyerCount+")");
							pastureDestroyerCount++;
						} else {
							spawnType = SPAWN_DESTROYER;
							if ( VERBOSE ) System.out.println("Creating destroyer ( destroyers: "+destroyerCount+" / cowbows: "+cowboyCount+" / pastr destroyer: "+pastureDestroyerCount+")");
							destroyerCount++;
						}
					}
					rc.broadcast( HQ_BROADCAST_CHANNEL , spawnType );
					rc.spawn(direction);
					break;
				}
			}
		}		
	}
}
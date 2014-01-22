package team223.behaviours;

import team223.MyConstants;
import team223.RobotBehaviour;
import team223.RobotPlayer;
import team223.Utils;
import team223.Utils.RobotAndInfo;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public final class HQBehaviour extends RobotBehaviour {

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
		if ( ! rc.isActive() ) {
			return;
		}
		
		final Robot[] enemies = rc.senseNearbyGameObjects( Robot.class , RobotType.HQ.attackRadiusMaxSquared , RobotPlayer.enemyTeam );
		if ( enemies.length > 0 ) 
		{
			RobotAndInfo enemy = Utils.pickEnemyToAttack( rc , enemies , null );
			if ( enemy != null ) 
			{
				if ( rc.canAttackSquare( enemy.info.location )) 
				{
					if ( MyConstants.HQ_VERBOSE ) System.out.println("Attacking enemy "+enemy+" at "+enemy.info.location+" by shooting at "+enemy.info.location);
					rc.attackSquare( enemy.info.location);
					return;
				}
				if ( MyConstants.HQ_VERBOSE ) System.out.println("Not shooting at "+enemy+" to avoid friendly fire");
			}
		}
		
		if ( rc.senseRobotCount() < GameConstants.MAX_ROBOTS) 
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
						if ( MyConstants.HQ_VERBOSE ) System.out.println("Creating destroyer ( destroyers: "+destroyerCount+" / cowbows: "+cowboyCount+" / pastr destroyer: "+pastureDestroyerCount+")");
						destroyerCount++;
					} else {
						final int expectedCowboyCount = (int) Math.ceil( destroyerCount*0.8f );
						final int expectedPastureDestroyerCount = (int) Math.ceil( destroyerCount*0.6f );			
						
						if ( cowboyCount < expectedCowboyCount ) {
							spawnType = SPAWN_COWBOY;
							if ( MyConstants.HQ_VERBOSE ) System.out.println("Creating cowboy ( destroyers: "+destroyerCount+" / cowbows: "+cowboyCount+" / pastr destroyer: "+pastureDestroyerCount+")");							
							cowboyCount++;	
						} else if ( pastureDestroyerCount < expectedPastureDestroyerCount ) {
							spawnType = SPAWN_PASTURE_DESTROYER;
							if ( MyConstants.HQ_VERBOSE ) System.out.println("Creating pasture destroyer ( destroyers: "+destroyerCount+" / cowbows: "+cowboyCount+" / pastr destroyer: "+pastureDestroyerCount+")");
							pastureDestroyerCount++;
						} else {
							spawnType = SPAWN_DESTROYER;
							if ( MyConstants.HQ_VERBOSE ) System.out.println("Creating destroyer ( destroyers: "+destroyerCount+" / cowbows: "+cowboyCount+" / pastr destroyer: "+pastureDestroyerCount+")");
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
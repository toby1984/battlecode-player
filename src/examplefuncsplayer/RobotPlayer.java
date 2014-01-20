package examplefuncsplayer;

import battlecode.common.*;

import java.util.*;

import team223.Utils;

public class RobotPlayer {
	static Random rand;
	
	public static void run(RobotController rc) {
		rand = new Random();
		Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
		
		while(true) 
		{
			if (rc.getType() == RobotType.HQ) {
				try {					
					//Check if a robot is spawnable and spawn one if it is
					if ( rc.isActive() ) {
						
						Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class, RobotType.HQ.attackRadiusMaxSquared ,rc.getTeam().opponent());
						if (nearbyEnemies.length > 0 ) 
						{
							RobotInfo robotInfo = rc.senseRobotInfo(nearbyEnemies[0]);
							rc.attackSquare(robotInfo.location);
						} 
						else if ( rc.senseRobotCount() < 25) 
						{
							Direction d = rc.getLocation().directionTo( rc.senseEnemyHQLocation() );
outer:							
							for ( int retries = 7 ; retries > 0 ; retries-- ) 
							{
								MapLocation spawnLocation = rc.getLocation().add( d );
								switch ( rc.senseTerrainTile( spawnLocation ) ) {
									case NORMAL:
									case ROAD:
										if ( rc.senseObjectAtLocation( spawnLocation ) == null ) 
										{
											rc.spawn(d);
											break outer;
										}
										break;
									default:
								}
								d = d.rotateLeft();
							}
						}	
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			} 
			else if (rc.getType() == RobotType.SOLDIER) 
			{
				try {
					if (rc.isActive()) 
					{
						Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class, rc.getType().attackRadiusMaxSquared ,rc.getTeam().opponent());
						if (nearbyEnemies.length > 0 ) {
							RobotInfo robotInfo = rc.senseRobotInfo(nearbyEnemies[0]);
							rc.attackSquare(robotInfo.location);
							rc.yield();
							continue;
						}						
						
						int action = (rc.getRobot().getID()*rand.nextInt(101) + 50)%101;
						//Construct a PASTR
						if (action < 1 && rc.getLocation().distanceSquaredTo(rc.senseHQLocation()) > 2) {
							rc.construct(RobotType.PASTR);
						//Move in a random direction
						} else if (action < 80) {
							Direction moveDirection = directions[rand.nextInt(8)];
							if (rc.canMove(moveDirection)) {
								rc.move(moveDirection);
							}
						//Sneak towards the enemy
						} else {
							Direction toEnemy = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
							if (rc.canMove(toEnemy)) {
								rc.sneak(toEnemy);
							}
						}
					}
				} catch (Exception e) {
					System.out.println("Soldier Exception");
				}
			}
			
			rc.yield();
		}
	}
}

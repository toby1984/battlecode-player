package team223;

import battlecode.common.RobotType;

public interface MyConstants 
{
	public static final boolean DEBUG_MODE = false;
	
	public static final boolean DEBUG_BYTECODE_LIMIT = true;
	
	public static final boolean COWBOY_VERBOSE = false;

	public static final boolean HQ_VERBOSE = true; 

	public static final boolean PASTR_DESTROYER_VERBOSE = false;

	public static final boolean ASTAR_VERBOSE = false;

	public static final boolean ASTAR_DEBUG_RUNTIME = false;

	public static final boolean ATTACK_IN_SIGHT_VERBOSE = false;

	public static final boolean GOTO_LOCATION_VERBOSE = false;	
	
	// Ranges
	
	public static final int SOLDIER_ATTACK_RANGE = (int) Math.ceil( Math.sqrt( RobotType.SOLDIER.attackRadiusMaxSquared ) );
	
	public static final int SOLDIER_ATTACK_RANGE_SQUARED = RobotType.SOLDIER.attackRadiusMaxSquared;
	
	public static final int FLEE_HEALTH = 20;
	
	public static final int DESTROYER_PATH_FINDING_TIMEOUT_ROUNDS = 100;
	
	public static final int COWBOY_PATH_FINDING_TIMEOUT_ROUNDS = 200;
	
	public static final int PASTURE_DESTROYER_PATH_FINDING_TIMEOUT_ROUNDS = 200;
	
	/**
	 * Distance up to which destroyers will try to hunt-down enemy robots.
	 */
	public static final int SOLDIER_SEEK_ENEMY_RANGE_SQUARED = RobotType.SOLDIER.sensorRadiusSquared; 
	
	/**
	 * Closest distance to attack range of enemy HQ , used to avoid coming under direct fire from enemy HQ (note that HQs deal splash damage though, we're
	 * not going to completely avoid this area though).
	 */
	public static final int ENEMY_HQ_SAFE_DISTANCE = (int) Math.ceil( Math.sqrt( RobotType.HQ.attackRadiusMaxSquared ) );
	
	public static final int ENEMY_HQ_SAFE_DISTANCE_MIN = ENEMY_HQ_SAFE_DISTANCE;
	
	public static final int ENEMY_HQ_SAFE_DISTANCE_MAX = 7 + ENEMY_HQ_SAFE_DISTANCE;	
	
	public static final int ENEMY_HQ_SAFE_DISTANCE_SRT = ENEMY_HQ_SAFE_DISTANCE_MIN*ENEMY_HQ_SAFE_DISTANCE_MIN;
	
	/**
	 * If a destroyer is not currently attacking and farther away from the enemy HQ than this distance, it will 
	 * home-in on the enemy HQ.
	 */
	public static final int SOLIDER_HOMEIN_ON_HQ_DISTANCE_SQUARED = (int)  ( (ENEMY_HQ_SAFE_DISTANCE+1)*(ENEMY_HQ_SAFE_DISTANCE+1) );
	
	/**
	 * Minimum number of cows that need to be present for
	 * a {@link RobotType#SOLDIER} to become a {@link RobotType#PASTR}. 
	 */
	public static final int MIN_COWS_FOR_PASTURE = 600;

	/**
	 * Number of successive movement failures before re-calculating path
	 * to target. 
	 */
	public static final int MAX_PATH_MOVEMENT_FAILURES = 3;

	public static final int ENEMY_SAFE_DISTANCE = 1 + RobotType.HQ.attackRadiusMaxSquared; // HQ has longest firing range
}
package lowflyingcow;

import battlecode.common.RobotType;

public interface MyConstants 
{
	public static final int SOLDIER_ATTACK_RANGE = (int) Math.ceil( Math.sqrt( RobotType.SOLDIER.attackRadiusMaxSquared ) );
	
	public static final int SOLDIER_ATTACK_RANGE_SQUARED = SOLDIER_ATTACK_RANGE*SOLDIER_ATTACK_RANGE;
	
	/**
	 * Distance up to which destroyers will try to hunt-down enemy robots.
	 */
	public static final int SOLDIER_SEEK_ENEMY_RANGE_SQUARED = (SOLDIER_ATTACK_RANGE*2)*(SOLDIER_ATTACK_RANGE)*2;	
	
	/**
	 * Closest distance to attack range of enemy HQ , used to avoid coming under fire from enemy HQ.
	 */
	public static final int ENEMY_HQ_SAFE_DISTANCE = 15+1;
	
	public static final int ENEMY_HQ_SAFE_DISTANCE_SRT = ENEMY_HQ_SAFE_DISTANCE*ENEMY_HQ_SAFE_DISTANCE;
	
	/**
	 * If a destroyer is not currently attacking and farther away from the enemy HQ than this distance, it will 
	 * home-in on the enemy HQ.
	 */
	public static final int SOLIDER_HOMEIN_ON_HQ_DISTANCE_SQUARED = (int)  ( (ENEMY_HQ_SAFE_DISTANCE*1.5f)*(ENEMY_HQ_SAFE_DISTANCE*1.5f) );
	
	/**
	 * Minimum number of cows that need to be present for
	 * a {@link RobotType#SOLDIER} to become a {@link RobotType#PASTR}. 
	 */
	public static final int MIN_COWS_FOR_PASTURE = 1000;

	/**
	 * Number of successive movement failures before re-calculating path
	 * to target. 
	 */
	public static final int MAX_PATH_MOVEMENT_FAILURES = 4;
}
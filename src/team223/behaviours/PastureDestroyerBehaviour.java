package team223.behaviours;

import java.util.List;

import team223.*;
import team223.states.AttackEnemiesInSight;
import team223.states.Fleeing;
import team223.states.GotoLocation;
import battlecode.common.*;

public class PastureDestroyerBehaviour extends RobotBehaviour {

	private static final boolean VERBOSE = false;
	
	private MapLocation[] pastrLocations;
	private int roundCounter=0;

	private final FastRandom rnd;
	
	public PastureDestroyerBehaviour( FastRandom rnd) {
		this.rnd=rnd;
	}
	
	@Override
	public void perform(RobotController rc) throws GameActionException 
	{
		roundCounter++;
		if ( ! rc.isActive() ) {
			return;
		}
		
		if ( state instanceof Fleeing ) 
		{
			state = state.perform( rc );
			return;
		} 
		
		if ( rc.getHealth() < 50 ) {
			state = new Fleeing( rnd );
			if ( MyConstants.DEBUG_MODE ) { changedBehaviour(rc); }			
			state = state.perform( rc );
			return;
		}

		if ( state instanceof AttackEnemiesInSight ) {
			state = state.perform( rc );
			return;
		}
		
		if ( state instanceof GotoLocation ) 
		{
			GotoLocation oldState = (GotoLocation) state;
			if ( VERBOSE ) System.out.println("Moving towards "+oldState.getDestination() );
			state = state.perform(rc);
			if ( state == null ) 
			{
				// destination reached
				int dist = rc.getLocation().distanceSquaredTo( oldState.getDestination() );
				if ( VERBOSE ) System.out.println("Pasture destroyer is at "+rc.getLocation()+" and thus reached designated target location "+oldState.getDestination()+" (dist: "+dist+", required: "+
				RobotType.SOLDIER.attackRadiusMaxSquared);
				state = new AttackEnemiesInSight();
				if ( MyConstants.DEBUG_MODE ) { changedBehaviour(rc); }
			}
			return;			
		}
		
		if ( pastrLocations == null || pastrLocations.length == 0 || roundCounter > 10 ) 
		{
			roundCounter=0;
			pastrLocations = rc.sensePastrLocations( rc.getTeam().opponent() );
			if (pastrLocations.length == 0 ) {
				if ( MyConstants.DEBUG_MODE ) System.out.println("No pastures found");				
				return;
			}
			
			if ( MyConstants.DEBUG_MODE ) System.out.println("Pastures found: "+pastrLocations.length);
			MapLocation myLocation = rc.getLocation();
			
			while(true) 
			{
				final MapLocation target = pickClosestTarget(myLocation);
				if ( target == null ) {
					if ( MyConstants.DEBUG_MODE) System.out.println("No (more) pastures");
					return;
				}
				
				if ( rc.canAttackSquare( target ) ) {
					if ( MyConstants.DEBUG_MODE) System.out.println("Attacking pastures (enemies) in sight , available target: "+target);					
					state = new AttackEnemiesInSight();
					if ( MyConstants.DEBUG_MODE ) { changedBehaviour(rc); }
					return;
				} 
				else  
				{
					Direction d = myLocation.directionTo( target );
					if ( rc.canMove( d ) ) {
						int distanceSquared = myLocation.add( d ).distanceSquaredTo( target );
						if ( distanceSquared <= RobotType.SOLDIER.attackRadiusMaxSquared ) {
							if ( VERBOSE ) System.out.println("Sneaking to "+myLocation.add( d )+" of target "+target+" brings me to distance "+distanceSquared);							
							rc.sneak( d );
							state = new AttackEnemiesInSight();
							if ( MyConstants.DEBUG_MODE ) { changedBehaviour(rc); }
							return;
						}
					}
					if ( VERBOSE ) System.out.println("Cannot attack "+target+" from "+myLocation+" , distance: "+myLocation.distanceSquaredTo( target )+" (max: "+RobotType.SOLDIER.attackRadiusMaxSquared+")");
				}
				
				if ( MyConstants.DEBUG_MODE) {
					System.out.println("Calculating path to closest pasture "+target);
				}
				final List<MapLocation> path = findPath( rc , myLocation , target );
				if ( path != null ) 
				{
					if ( MyConstants.DEBUG_MODE) System.out.println("Pasture destroyer found path to "+target);							
					PathInfo pathInfo = new PathInfo(path); 
					state = new GotoLocation( pathInfo , MovementType.RUN ) {

						@Override
						protected List<MapLocation> recalculatePath(RobotController rc) throws GameActionException {
							return findPath( rc , rc.getLocation() , target );
						}
						
						@Override
						protected boolean hasArrivedAtDestination(MapLocation current, MapLocation dstLoc) {
							return PastureDestroyerBehaviour.this.hasArrivedAtDestination( current , dstLoc );
						}
					};
					if ( MyConstants.DEBUG_MODE ) {
						System.out.println("State is now: "+state.getClass().getName());
						changedBehaviour(rc); 
					}
					break;
				} else if ( MyConstants.DEBUG_MODE) {
					if ( MyConstants.DEBUG_MODE) System.out.println("Failed to find path to closest pasture");						
				}
			} 
		}
	}
	
	private MapLocation pickClosestTarget(MapLocation location) 
	{
		if ( pastrLocations.length > 0 ) 
		{
			MapLocation closest = null;		
			int index = -1;
			int closestDistanceSqrt=-1;
			for (int i = 0; i < pastrLocations.length; i++) {
				MapLocation loc = pastrLocations[i];
				if ( loc != null) {
					int distSqrt = loc.distanceSquaredTo( location );
					if ( closest == null || distSqrt < closestDistanceSqrt ) {
						closestDistanceSqrt = distSqrt;
						closest = loc;
						index = i;
					}
				}
			}
			if ( closest != null ) {
				pastrLocations[index]=null;
				return closest;
			}			
		}
		return null;
 	}
	
	private List<MapLocation> findPath(final RobotController rc,MapLocation start,final MapLocation end) throws GameActionException 
	{
		MapLocationAStar finder = new MapLocationAStar(start,end) {
			
			@Override
			protected boolean isCloseEnoughToTarget(PathNode<MapLocation> node) 
			{
				return hasArrivedAtDestination( node.value , destination );
			}
			
			@Override
			public TerrainTile senseTerrainTile(MapLocation loc) {
				return rc.senseTerrainTile( loc );
			}

			@Override
			public boolean isOccupied(MapLocation loc) throws GameActionException 
			{
				return rc.canSenseSquare( loc ) ? rc.senseObjectAtLocation( loc ) != null : false;
			}
		};
		return Utils.findPath(finder );
	}
	
	protected boolean hasArrivedAtDestination(MapLocation current, MapLocation dstLoc) {
		return current.distanceSquaredTo( dstLoc ) <= RobotType.SOLDIER.attackRadiusMaxSquared*0.5f;							
	}	
}
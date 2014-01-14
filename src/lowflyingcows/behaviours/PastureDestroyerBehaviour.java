package lowflyingcows.behaviours;

import java.util.List;

import lowflyingcows.*;
import lowflyingcows.states.AttackEnemiesInSight;
import lowflyingcows.states.Fleeing;
import lowflyingcows.states.GotoLocation;
import battlecode.common.*;

public class PastureDestroyerBehaviour implements IRobotBehaviour {

	private State state;
	
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
			System.out.println("Moving towards "+oldState.getDestination() );
			state = state.perform(rc);
			if ( state == null ) 
			{
				// destination reached
				int dist = rc.getLocation().distanceSquaredTo( oldState.getDestination() );
				System.out.println("Pasture destroyer is at "+rc.getLocation()+" and thus reached designated target location "+oldState.getDestination()+" (dist: "+dist+", required: "+
				RobotType.SOLDIER.attackRadiusMaxSquared);
				state = new AttackEnemiesInSight();
			}
			return;			
		}
		
		if ( pastrLocations == null || roundCounter > 10 ) 
		{
			roundCounter=0;
			pastrLocations = null;
			
			pastrLocations = rc.sensePastrLocations( rc.getTeam().opponent() );
			System.out.println("Pastures found: "+pastrLocations.length);
			MapLocation myLocation = rc.getLocation();
			
			while(true) 
			{
				final MapLocation target = pickTarget();
				if ( target == null ) {
					System.out.println("No pastures");
					return;
				}
				System.out.println("Pasture destroyer looking for path to "+target);
				final List<MapLocation> path = findPath( rc , myLocation , target );
				if ( path != null ) 
				{
					System.out.println("Pasture destroyer found path to "+target);							
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
					break;
				} 
			} 
		}
	}
	
	protected boolean hasArrivedAtDestination(MapLocation current, MapLocation dstLoc) {
		return current.distanceSquaredTo( dstLoc ) <= RobotType.SOLDIER.attackRadiusMaxSquared*0.5f;							
	}
	
	private MapLocation pickTarget() 
	{
		if ( pastrLocations.length > 0 ) 
		{
			for (int i = 0; i < pastrLocations.length; i++) {
				MapLocation loc = pastrLocations[i];
				if ( loc != null ) {
					pastrLocations[i] = null;
					return loc;
				}
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
}
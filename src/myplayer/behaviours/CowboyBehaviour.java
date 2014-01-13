package myplayer.behaviours;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import myplayer.IRobotBehaviour;
import myplayer.MapLocationAStar;
import myplayer.PathInfo;
import myplayer.State;
import myplayer.Utils;
import myplayer.states.AttackEnemy;
import myplayer.states.GotoLocation;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.MovementType;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public class CowboyBehaviour implements IRobotBehaviour {

	private LocationWithGrowthRate destination;
	private PathInfo pathInfo;

	private State state;

	public CowboyBehaviour() {
	}

	@Override
	public void perform(RobotController rc) throws GameActionException 
	{
		if ( ! rc.isActive() || rc.getType() == RobotType.PASTR ) {
			return;
		}

		if ( state instanceof AttackEnemy ) 
		{
			state = state.perform( rc );
			return;
		}

		Robot closestEnemy = Utils.findClosestEnemy( rc , 5 );
		if ( closestEnemy != null ) 
		{
			System.out.println("Cowbow is attacking "+closestEnemy.getID());
			state = new AttackEnemy(closestEnemy);
			state = state.perform( rc );
			pathInfo = null;
			return;
		}		

		boolean pickDestination = destination == null || ! isFree( destination , rc ) ;
		if ( pathInfo == null || pickDestination ) 
		{
			if ( pickDestination ) 
			{
				System.out.println("Picking destination...");
				destination = null;
				final List<LocationWithGrowthRate> locations = findLocations(rc);
				for ( int i = 0 ; i < locations.size(); i++ ) 
				{
					LocationWithGrowthRate loc = locations.get(i);
					if ( isFree( loc , rc ) ) 
					{
						System.out.println("FREE: "+loc);
						List<MapLocation> path = findPath( rc , rc.getLocation() , loc.location() );
						if ( path != null ) 
						{
							System.out.println("Picked destination: "+loc);
							pathInfo = new PathInfo(path);
							state = new GotoLocation( pathInfo , MovementType.SNEAK );
							destination = loc;
							break;
						} else {
							System.out.println("Found no path.");
						}
					} else {
						// System.out.println("Location "+loc+" is occupied");
					}
				}  
			} 
			else 
			{
				System.out.println("Recalculating path to "+destination);
				List<MapLocation> path = findPath( rc , rc.getLocation() , destination.location() );
				if ( path == null ) {
					System.out.println("Found no path.");					
					destination = null;
					return;
				}
				System.out.println("Moving after path recalculation");
				pathInfo = new PathInfo(path);		
				state = new GotoLocation( pathInfo , MovementType.SNEAK  );
			}
		}

		if ( state != null ) {
			State newState  = state.perform( rc );
			if ( newState == null ) 
			{
				// at destination
				if ( rc.isActive() && ! rc.isConstructing() ) 
				{
					rc.construct(RobotType.PASTR);
				}
			} 
		} 
	}

	protected boolean hasArrivedAtDestination(MapLocation current,MapLocation dstLoc) 
	{
		final int dx = Math.abs( dstLoc.x - current.x );
		final int dy = Math.abs( dstLoc.y - current.y );
		return dx <= 1 && dy <= 1;
	}

	protected List<MapLocation> findPath(final RobotController rc,final MapLocation startLoc,final MapLocation dstLoc) {

		final MapLocationAStar pathFinder = new MapLocationAStar() 
		{
			@Override
			protected boolean isCloseEnoughToTarget(myplayer.AStar.PathNode<MapLocation> node) 
			{
				return hasArrivedAtDestination( node.value , dstLoc );
			}

			@Override
			public TerrainTile senseTerrainTile(MapLocation loc) {
				return rc.senseTerrainTile( loc );
			}
		};
		pathFinder.setStart( startLoc );
		pathFinder.setDestination( dstLoc );		
		return Utils.findPath( pathFinder );
	}		

	protected LocationWithGrowthRate findPasture(RobotController rc) 
	{
		final List<LocationWithGrowthRate> locations = findLocations(rc);
		for ( int i = 0 ; i < locations.size(); i++ ) {
			if ( isFree( locations.get(i) , rc ) ) {
				return locations.get(i);
			}
		}    	
		return null;
	}

	private List<LocationWithGrowthRate> findLocations(RobotController rc) 
	{
		double[][] rate = rc.senseCowGrowth();
		
		final int maxX = rc.getMapWidth();
		final int maxY = rc.getMapHeight();

		final List<LocationWithGrowthRate> locations = new ArrayList<LocationWithGrowthRate>();
		for ( int x = 0 ; x < maxX ; x++ ) {
			for ( int y = 0 ; y < maxY ; y++ ) {
				double r = rate[x][y];
				if ( r > 0 ) {
					locations.add( new LocationWithGrowthRate( x , y , r ) );
				}
			}					
		}
		Collections.sort( locations );
		return locations;
	}

	private boolean isFree(LocationWithGrowthRate location,RobotController rc) {

		Robot[] robots = rc.senseNearbyGameObjects( Robot.class , location.location() , GameConstants.PASTR_RANGE*GameConstants.PASTR_RANGE , rc.getTeam() );
		for ( int i = 0 ; i < robots.length ; i++ ) 
		{
			if ( robots[i].getID() != rc.getRobot().getID() ) {
				return false;
			}
		}
		return true;
	}

	public static final class LocationWithGrowthRate implements Comparable<LocationWithGrowthRate> {

		public final int x;
		public final int y;
		public final double rate;
		private MapLocation loc;

		public LocationWithGrowthRate(int x,int y, double rate) {
			this.x = x;
			this.y = y;
			this.rate = rate;
		}

		public MapLocation location() {
			if ( loc == null ) {
				loc = new MapLocation(x,y);
			}
			return loc;
		}

		@Override
		public int compareTo(LocationWithGrowthRate o) 
		{
			if ( this.rate < o.rate ) {
				return -1;
			} 
			if ( this.rate > o.rate ) {
				return 1;
			}
			return 0;
		}
		
		@Override
		public String toString() {
			return "Location[x="+x+",y="+y+",rate="+rate+"]";
		}
	}    

	@Override
	public String toString() {
		return "Cowboy";
	}
}

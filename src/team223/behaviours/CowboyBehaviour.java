package team223.behaviours;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import team223.AStar;
import team223.FastRandom;
import team223.MyConstants;
import team223.RobotBehaviour;
import team223.Utils;
import team223.states.AttackEnemiesInSight;
import team223.states.Fleeing;
import team223.states.InterruptibleGotoLocation;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.MovementType;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public final class CowboyBehaviour extends RobotBehaviour {

	private final FastRandom rnd;

	private static final boolean VERBOSE = false;

	private int roundCount=0;

	private Direction generalDirection;

	private double[][] growth;

	private static final int PASTR_RANGE_SQUARED = GameConstants.PASTR_RANGE;

	private static final int MAX_CANDIDATE_LOCATIONS = 25;

	private static final int PASTR_RANGE  = (int) Math.floor( Math.sqrt( PASTR_RANGE_SQUARED ) );

	private static final int PASTR_RANGE_TWICE  = 2*PASTR_RANGE;

	// number of tiles within pastr range that need to have a cow growth rate > 0
	// in order for a MapLocation candidate to be considered a good pastr location

	// currently, ALL tiles need to have a non-zero growth rate  
	private static final int MIN_POPULATED_TILES_IN_PASTR_RANGE = PASTR_RANGE*PASTR_RANGE; 

	private MapLocation[] locations;

	private MapLocation currentDestination;

	private final Team myTeam;

	public CowboyBehaviour(final RobotController rc,Team myTeam,FastRandom rnd,MapLocation enemyHQLocation) {
		super(rc,enemyHQLocation);
		this.rnd=rnd;
		this.myTeam = myTeam;
		final Direction[] candidates = Utils.getMovementCandidateDirections( rc );
		generalDirection = candidates[ rnd.nextInt( candidates.length ) ];
	}

	@Override
	public void perform() throws GameActionException 
	{
		roundCount++;

		if ( locations == null ) 
		{
			growth = rc.senseCowGrowth();
			int w = rc.getMapWidth();
			int h = rc.getMapHeight();

			int xEnd = w - PASTR_RANGE_TWICE;
			int yEnd = h - PASTR_RANGE_TWICE;

			if ( VERBOSE ) System.out.println("PASTR_RANGE: "+PASTR_RANGE+" / minPop: "+MIN_POPULATED_TILES_IN_PASTR_RANGE);

			final List<MapLocation> candidates = new ArrayList<MapLocation>();
			int minDistanceToHQ = (int) Math.ceil( Math.sqrt( RobotType.HQ.attackRadiusMaxSquared ) + PASTR_RANGE );
			minDistanceToHQ *= minDistanceToHQ;
			
			for ( int x = PASTR_RANGE ; x < xEnd ; x+=PASTR_RANGE_TWICE ) 
			{
				for ( int y = PASTR_RANGE ; y < yEnd ; y += PASTR_RANGE_TWICE ) 
				{
					int sum = 0;
					for ( int x1 = x-PASTR_RANGE ; x1 < x+PASTR_RANGE ; x1++ ) 
					{
						for ( int y1 = y-PASTR_RANGE ; y1 < y+PASTR_RANGE ; y1++ ) 
						{
							if ( growth[x1][y1] > 0.3 ) {
								sum++;
							}								
						}
					}
					if ( sum >= MIN_POPULATED_TILES_IN_PASTR_RANGE ) 
					{
						MapLocation loc = new MapLocation(x,y);
						if ( loc.distanceSquaredTo( enemyHQLocation ) > minDistanceToHQ ) 
						{
							candidates.add( loc );
						}
					}
				}
			}

			final MapLocation myLocation=rc.getLocation();
			Collections.sort( candidates , new Comparator<MapLocation>() {

				@Override
				public int compare(MapLocation o1, MapLocation o2) 
				{
					int dist1 = myLocation.distanceSquaredTo( o1 );
					int dist2 = myLocation.distanceSquaredTo( o2 );
					if ( dist1 < dist2 ) {
						return -1;
					} 
					if ( dist1 > dist2 ) {
						return 1;
					}
					return 0;
				}
			} );

			if ( candidates.size() > MAX_CANDIDATE_LOCATIONS ) {
				this.locations = candidates.subList(0, MAX_CANDIDATE_LOCATIONS).toArray( new MapLocation[ MAX_CANDIDATE_LOCATIONS ] );					
			} else {
				this.locations = candidates.toArray( new MapLocation[candidates.size()] );
			}
			Utils.shuffle( this.locations , rnd );				
		}

		if ( ! rc.isActive() || rc.getType() == RobotType.PASTR ) {
			return;
		}

		if ( state instanceof Fleeing ) {
			state = state.perform();
			return;
		}

		if ( rc.getHealth() < MyConstants.FLEE_HEALTH ) {
			state = new Fleeing(rc,rnd);
			if ( MyConstants.DEBUG_MODE ) { behaviourStateChanged(); }
			state = state.perform(  );
			return;
		}

		if ( state instanceof AttackEnemiesInSight ) 
		{
			state = state.perform();
			return;
		}

		Robot[] enemies = Utils.findEnemies( rc , RobotType.SOLDIER.attackRadiusMaxSquared );
		if ( enemies.length > 0 ) 
		{
			for ( int i = 0 ; i < enemies.length ; i++ ) {
				RobotInfo ri = rc.senseRobotInfo( enemies[i] );
				if ( ri.type == RobotType.PASTR || ri.type == RobotType.SOLDIER ) {
					state = new AttackEnemiesInSight(rc);
					if ( MyConstants.DEBUG_MODE ) { behaviourStateChanged(); }					
					return;
				}
			}
		}

		if ( ! pathFindingMode(rc ) ) {
			wanderingMode(rc);
		} 
	}

	private boolean pathFindingMode(final RobotController rc) throws GameActionException 
	{
		if ( state instanceof InterruptibleGotoLocation) 
		{
			InterruptibleGotoLocation oldState = (InterruptibleGotoLocation) state;
			state = state.perform();
			if ( state == null ) { // arrived at destination ?
				MapLocation currentLoc = rc.getLocation();
				if ( hasArrivedAtDestination( currentLoc , currentDestination ) ) 
				{
					// at destination, construct pastr
					if ( VERBOSE ) System.out.println("Reached destination "+oldState.getDestination()+" , building PASTR");					
					rc.construct( RobotType.PASTR );
					return true;
				} 
				if ( VERBOSE ) System.out.println("Failed to reach destination "+oldState.getDestination()+" , choosing new");
				discardLocation( currentDestination );
			}
			return true;
		}

		if ( currentDestination != null ) 
		{
			if ( isTerminallyOccupied( currentDestination , rc ) ) {
				discardLocation( currentDestination );
				currentDestination = null;
			}
			// retry moving to this location
			gotoLocation( rc , currentDestination );
			return true;
		}

		currentDestination = null;

		for ( int i =0 ; i < locations.length ; i++ ) {
			final MapLocation loc = locations[i];
			if ( loc != null ) 
			{
				if ( ! isTerminallyOccupied( loc , rc ) ) {
					gotoLocation(rc,loc);
					return true;
				}
				locations[i]=null; // discard occupied locations
			}
		}
		if ( VERBOSE ) System.out.println("No (more) pasture candidates,giving up");
		return false;
	}

	private void gotoLocation(final RobotController rc,final MapLocation loc) throws GameActionException 
	{
		if ( VERBOSE ) System.out.println("Moving to pasture location "+loc);
		
		currentDestination = loc;
		
		state = new InterruptibleGotoLocation( rc , MovementType.SNEAK , rnd ) {

			@Override
			protected boolean hasArrivedAtDestination(MapLocation current,MapLocation dstLoc) {
				return current.equals( dstLoc );
			}

			@Override
			public boolean isOccupied(MapLocation loc) throws GameActionException {
				return rc.canSenseSquare( loc ) ? rc.senseObjectAtLocation( loc ) != null : false;					
			}

			@Override
			public boolean setStartAndDestination(AStar finder) {
				finder.setRoute( rc.getLocation() , loc );
				return true;
			}
		};
	}

	private boolean isTerminallyOccupied(MapLocation loc,RobotController rc) throws GameActionException 
	{
		for ( int x1 = loc.x-PASTR_RANGE ; x1 < loc.x+PASTR_RANGE ; x1++ ) 
		{
			for ( int y1 = loc.y-PASTR_RANGE ; y1 < loc.y+PASTR_RANGE ; y1++ ) 
			{
				switch( rc.senseTerrainTile( new MapLocation(x1,y1) ) ) {
					case VOID:
					case OFF_MAP:
						return true;
					default:
				}								
			}
		}					

		if ( rc.canSenseSquare( loc ) ) 
		{
			GameObject obj = rc.senseObjectAtLocation( loc );
			boolean isOccupied = obj != null;
			if ( obj instanceof Robot) {
				RobotInfo ri = rc.senseRobotInfo( (Robot) obj);
				if ( ri.team == myTeam) { 
					isOccupied = ri.type != RobotType.SOLDIER;
				} else {
					isOccupied = false; // treat as not occupied, we'll kill whatever stands in our way
				}
			}
			return isOccupied;
		}		
		return false;
	}

	private void discardLocation(MapLocation destination) {
		for ( int i =0 ; i < locations.length ; i++ ) 
		{
			if ( destination.equals( locations[i] ) ) {
				locations[i] = null;
				return;
			}
		}
	}

	protected boolean hasArrivedAtDestination(MapLocation current,MapLocation dstLoc) {
		return current.equals( dstLoc );
	}

	private void wanderingMode(RobotController rc) throws GameActionException {
		// count sheeps,eh,cows
		double cowCount = 0;
		MapLocation myLoc = rc.getLocation();
		for ( int i = 0 ; i < Utils.DIRECTIONS.length ; i++ ) {
			cowCount += rc.senseCowsAtLocation( myLoc.add( Utils.DIRECTIONS[i] ) );
		}
		cowCount += rc.senseCowsAtLocation( myLoc );

		if ( cowCount > MyConstants.MIN_COWS_FOR_PASTURE ) {

			Robot[] friendlies = rc.senseNearbyGameObjects(Robot.class , GameConstants.PASTR_RANGE*GameConstants.PASTR_RANGE , rc.getTeam() );
			boolean construct = true;
			for (int i = 0; i < friendlies.length; i++) {
				Robot r = friendlies[i];
				RobotInfo ri = rc.senseRobotInfo( r );
				if ( ri.type == RobotType.PASTR || ri.isConstructing ) {
					construct = false;
					break;
				}
			}

			if ( construct ) {
				if ( VERBOSE ) System.out.println("Starting PASTR construction");
				rc.construct( RobotType.PASTR );
				return;
			}
		}

		// wander in general direction
		if ( MyConstants.DEBUG_MODE ) {
			changedBehaviour( rc , "Wandering cowbows");
		}

		Direction d = generalDirection;
		Direction toMove = null;
		if ( ( roundCount % 10 ) == 0 ) {
			Direction newD = Utils.randomMovementDirection(rnd,rc);
			if ( newD != Direction.NONE ) {
				generalDirection = newD;
				d=newD;
			}
		}

		if ( ! rc.canMove( d ) ) 
		{
			d = generalDirection.rotateLeft();
			if ( ! rc.canMove( d ) ) {
				d = generalDirection.rotateRight();
				if ( ! rc.canMove( d ) ) {
					d = Utils.randomMovementDirection(rnd,rc);	
					if ( d != Direction.NONE ) {
						toMove=d;
						generalDirection=d;
					}
				} else {
					toMove = d;
				}
			} else {
				toMove = d;
			}
		} else {
			toMove = d;
		}

		if ( toMove != null ) 
		{
			rc.sneak(d);
		}		
	}

	@Override
	public String toString() {
		return "Cowboy";
	}
}
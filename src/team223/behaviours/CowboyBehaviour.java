package team223.behaviours;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import team223.AStar;
import team223.MyConstants;
import team223.RobotBehaviour;
import team223.RobotPlayer;
import team223.Utils;
import team223.Utils.RobotAndInfo;
import team223.states.AttackEnemiesInSight;
import team223.states.Attacking;
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

public final class CowboyBehaviour extends RobotBehaviour {

	private int roundCount=0;

	private Direction generalDirection;

	private double[][] growth;

	private static final int PASTR_RANGE_SQUARED = GameConstants.PASTR_RANGE;

	private static final int MAX_CANDIDATE_LOCATIONS = 25;

	private static final int PASTR_RANGE  = (int) Math.floor( Math.sqrt( PASTR_RANGE_SQUARED ) );

	private static final int PASTR_RANGE_TWICE  = 2*PASTR_RANGE;
	
	private static boolean fullSpeedPathFinding = true;	

	// number of tiles within pastr range that need to have a cow growth rate > 0
	// in order for a MapLocation candidate to be considered a good pastr location

	// currently, ALL tiles need to have a non-zero growth rate  
	private static final int MIN_POPULATED_TILES_IN_PASTR_RANGE = PASTR_RANGE*PASTR_RANGE; 

	private MapLocation[] locations;

	private MapLocation currentDestination;

	public CowboyBehaviour(final RobotController rc) {
		super(rc);
		final Direction[] candidates = Utils.getMovementCandidateDirections( rc );
		generalDirection = candidates[ RobotPlayer.rnd.nextInt( candidates.length ) ];
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

			if ( MyConstants.COWBOY_VERBOSE ) System.out.println("PASTR_RANGE: "+PASTR_RANGE+" / minPop: "+MIN_POPULATED_TILES_IN_PASTR_RANGE);

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
						if ( loc.distanceSquaredTo( RobotPlayer.enemyHQ ) > minDistanceToHQ ) 
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
			Utils.shuffle( this.locations );				
		}

		if ( state instanceof AttackEnemiesInSight ) 
		{
			state = state.perform();
			return;
		}

		Robot[] enemies = rc.senseNearbyGameObjects( Robot.class , RobotType.SOLDIER.attackRadiusMaxSquared , RobotPlayer.enemyTeam );
		if ( enemies.length > 0 ) 
		{
			RobotAndInfo enemyToAttack = Utils.pickEnemyToAttack( rc , enemies , null );
			if ( enemyToAttack != null ) 
			{
				state = new Attacking(rc , enemyToAttack.robot , true );				
				if ( rc.isActive() ) {
					rc.attackSquare( enemyToAttack.info.location );
					rc.yield();
				}
				return;
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
					if ( MyConstants.COWBOY_VERBOSE ) {
						System.out.println("Reached destination "+oldState.getDestination()+" , building PASTR");	
					}
					while ( ! rc.isActive() ) {
						rc.yield();
					}
					rc.construct( RobotType.PASTR );
					return true;
				} 
				if ( MyConstants.COWBOY_VERBOSE ) System.out.println("Failed to reach destination "+oldState.getDestination()+" , choosing new");
				discardLocation( currentDestination );
			}
			return true;
		}

		if ( currentDestination != null ) 
		{
			if ( isTerminallyOccupied( currentDestination , rc ) ) {
				discardLocation( currentDestination );
			} else {
				// retry moving to this location
				gotoLocation( rc , currentDestination );
				return true;
			}
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
		if ( MyConstants.COWBOY_VERBOSE ) System.out.println("No (more) pasture candidates,giving up");
		return false;
	}

	private void gotoLocation(final RobotController rc,final MapLocation loc) throws GameActionException 
	{
		if ( loc == null ) {
			new Exception("Location must not be null").printStackTrace();
			throw new IllegalArgumentException("Location must not be null");
		}
		
		if ( MyConstants.COWBOY_VERBOSE ) System.out.println("Moving to pasture location "+loc);
		
		currentDestination = loc;
		
		state = new InterruptibleGotoLocation( rc , MovementType.SNEAK , fullSpeedPathFinding ) {

			@Override
			protected boolean hasArrivedAtDestination(MapLocation current,MapLocation dstLoc) {
				return current.equals( dstLoc );
			}

			@Override
			public boolean setStartAndDestination(boolean retry) {
				AStar.setRoute( rc.getLocation() , loc , MyConstants.COWBOY_PATH_FINDING_TIMEOUT_ROUNDS );
				return true;
			}

			@Override
			public boolean abortOnTimeout() {
				return true;
			}
		}.perform();
		fullSpeedPathFinding = false;
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
				if ( ri.team == RobotPlayer.myTeam) { 
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
				if ( MyConstants.COWBOY_VERBOSE ) System.out.println("Starting PASTR construction");
				while ( ! rc.isActive() ) 
				{
					rc.yield();
				}				
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
			Direction newD = Utils.randomMovementDirection(rc);
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
					d = Utils.randomMovementDirection(rc);	
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

		if ( toMove != null && rc.isActive() ) 
		{
			rc.sneak(d);
		}		
	}

	@Override
	public String toString() {
		return "Cowboy";
	}
}
package test;

import team223.AStar;
import team223.AStar.Result;
import team223.FastRandom;
import team223.MyConstants;
import team223.State;
import team223.states.InterruptibleGotoLocation;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.MovementType;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.TerrainTile;

public class InterruptibleGotoLocationTest {

	public static void main(String[] args) throws GameActionException {

		final MapLocation from = new MapLocation(0,0);
		final MapLocation to = new MapLocation(10,10);
		
		final double[] health = {100};
		
		RobotController rc = new MockRobotController() {
			
			private int loopCounter;
			
			private MapLocation currentLocation = from;
			
			@Override
			public TerrainTile senseTerrainTile(MapLocation loc) { return TerrainTile.NORMAL; }
			
			@Override
			public double getHealth() { return health[0]; }
			
			public MapLocation getLocation() { return currentLocation; }
			
			@Override
			public boolean canMove(Direction dir) 
			{
				try {
					Thread.sleep( 250 );
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				loopCounter++;
				if ( loopCounter >= 3 && loopCounter < 4+MyConstants.MAX_PATH_MOVEMENT_FAILURES) {
					System.out.println("Blocking movements");
					return false;
				} 
				return true; 
			}
			
			@Override
			public void move(Direction dir) throws GameActionException {
				currentLocation = currentLocation.add(dir);
				System.out.println("TEST: Ran to "+currentLocation);
			}
			
			@Override
			public void sneak(Direction dir) throws GameActionException {
				currentLocation = currentLocation.add(dir);
				System.out.println("TEST: Sneaked to "+currentLocation);		
			}

			@Override
			public <T extends GameObject> T[] senseNearbyGameObjects(
					Class<T> type, int radiusSquared, Team team) {
				return (T[]) new Robot[0];
			}
			
			@Override
			public Team getTeam() { return Team.A; };

			@Override
			public boolean isActive() { return true; }
		};
		
		final FastRandom rnd = new FastRandom(0xdeadbeef);

		State currentState = new InterruptibleGotoLocation(rc,MovementType.RUN , rnd ) {

			@Override
			public boolean isOccupied(MapLocation loc) throws GameActionException 
			{
				return false;
			}

			@Override
			protected boolean hasArrivedAtDestination(MapLocation current, MapLocation dstLoc) {
				return current.equals( dstLoc );
			}

			@Override
			public boolean setStartAndDestination(AStar finder) {
				finder.setRoute( from , to );
				return true;
			}
		};
		
		do 
		{
			System.out.println("TEST: ==> state#perform()");
			currentState = currentState.perform();
			System.out.println("TEST: ==> state#perform() returned "+currentState);
		} while ( currentState != null );
		System.out.println("TEST: End.");
	}
	
	protected static abstract class MockRobotController implements RobotController {

		@Override
		public double getActionDelay() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int getMapWidth() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int getMapHeight() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Team getTeam() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Robot getRobot() {
			throw new UnsupportedOperationException();
		}

		@Override
		public RobotType getType() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isConstructing() {
			throw new UnsupportedOperationException();
		}

		@Override
		public RobotType getConstructingType() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int getConstructingRounds() {
			throw new UnsupportedOperationException();
		}

		@Override
		public double senseTeamMilkQuantity(Team t) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int senseRobotCount() {
			throw new UnsupportedOperationException();
		}

		@Override
		public GameObject senseObjectAtLocation(MapLocation loc) throws GameActionException {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T extends GameObject> T[] senseNearbyGameObjects(Class<T> type) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T extends GameObject> T[] senseNearbyGameObjects(Class<T> type,
				int radiusSquared) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T extends GameObject> T[] senseNearbyGameObjects(Class<T> type,
				int radiusSquared, Team team) {
			throw new UnsupportedOperationException();			
		}

		@Override
		public <T extends GameObject> T[] senseNearbyGameObjects(Class<T> type,
				MapLocation center, int radiusSquared, Team team) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Robot[] senseBroadcastingRobots() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Robot[] senseBroadcastingRobots(Team t) {
			throw new UnsupportedOperationException();
		}

		@Override
		public MapLocation[] senseBroadcastingRobotLocations() {
			throw new UnsupportedOperationException();
		}

		@Override
		public MapLocation[] senseBroadcastingRobotLocations(Team t) {
			throw new UnsupportedOperationException();
		}

		@Override
		public MapLocation senseLocationOf(GameObject o)
				throws GameActionException {
			throw new UnsupportedOperationException();
		}

		@Override
		public RobotInfo senseRobotInfo(Robot r) throws GameActionException {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean canSenseObject(GameObject o) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean canSenseSquare(MapLocation loc) {
			throw new UnsupportedOperationException();
		}

		@Override
		public MapLocation senseHQLocation() {
			throw new UnsupportedOperationException();
		}

		@Override
		public MapLocation senseEnemyHQLocation() {
			throw new UnsupportedOperationException();
		}

		@Override
		public MapLocation[] sensePastrLocations(Team t) {
			throw new UnsupportedOperationException();
		}

		@Override
		public double[][] senseCowGrowth() {
			throw new UnsupportedOperationException();
		}

		@Override
		public double senseCowsAtLocation(MapLocation loc)
				throws GameActionException {
			throw new UnsupportedOperationException();
		}

		@Override
		public int roundsUntilActive() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean canAttackSquare(MapLocation loc) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void attackSquare(MapLocation loc) throws GameActionException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void attackSquareLight(MapLocation loc)
				throws GameActionException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void broadcast(int channel, int data) throws GameActionException {
			throw new UnsupportedOperationException();
		}

		@Override
		public int readBroadcast(int channel) throws GameActionException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void spawn(Direction dir) throws GameActionException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void construct(RobotType type) throws GameActionException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void yield() {
		}

		@Override
		public void selfDestruct() throws GameActionException {
			throw new UnsupportedOperationException();			
		}

		@Override
		public void resign() {
			throw new UnsupportedOperationException();			
		}

		@Override
		public void wearHat() throws GameActionException {
			throw new UnsupportedOperationException();			
		}

		@Override
		public void setIndicatorString(int stringIndex, String newString) {
			System.out.println("IndicatorString[ "+stringIndex+"] = "+newString);
		}

		@Override
		public long getControlBits() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void addMatchObservation(String observation) {
			throw new UnsupportedOperationException();			
		}

		@Override
		public void setTeamMemory(int index, long value) {
			throw new UnsupportedOperationException();			
		}

		@Override
		public void setTeamMemory(int index, long value, long mask) {
			throw new UnsupportedOperationException();			
		}

		@Override
		public long[] getTeamMemory() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void breakpoint() {
			throw new UnsupportedOperationException();			
		}	
	}
}
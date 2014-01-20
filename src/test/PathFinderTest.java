package test;

import java.util.List;
import java.util.Random;

import team223.AStar;
import team223.AStar.Callback;
import team223.AStar.Result;
import team223.AStar.TimeoutResult;
import team223.RobotPlayer;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.TerrainTile;

public class PathFinderTest {

	public static void main(String[] args) throws GameActionException {

		final MapLocation from = new MapLocation(10,10);
		final MapLocation to = new MapLocation(10,11);

		RobotPlayer.enemyHQ = new MapLocation(100,100);

		RobotController rc = new MockRobotController() {
			@Override
			public TerrainTile senseTerrainTile(MapLocation loc) {
				return TerrainTile.NORMAL;
			}
		};
		
		AStar finder = new AStar(rc) {

			@Override
			protected boolean isCloseEnoughToTarget(PathNode  node) {
				return node.value.equals( to );
			}

			@Override
			public boolean isWalkable(MapLocation loc) throws GameActionException {
				return true;
			}
		};
		
		finder.setRoute( from , to , -1 );
		
		final Callback callback = new Callback() {
			
			@Override
			public void foundPath(List<MapLocation> path) {
				System.out.println("Found path"+path);
			}
			
			@Override
			public void foundNoPath() {
				System.out.println("Found no path");
			}
			
			@Override
			public Result checkInterrupt() {
				return Result.INTERRUPT;
			}

			@Override
			public TimeoutResult onTimeout() {
				System.out.println("Timeout");
				return TimeoutResult.CONTINUE;
			}
		};
		
		do {
			if ( ! finder.isStarted() ) {
				System.out.println("Starting search...");				
				finder.findPath( callback );
				System.out.println("Search returned");					
			} else {
				System.out.println("Continueing search...");					
				finder.continueFindPath();
				System.out.println("Search returned");				
			}
		} while ( ! finder.isFinished() );
		
	}
	
	protected static class MockRobotController implements RobotController {

		@Override
		public double getActionDelay() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public double getHealth() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public MapLocation getLocation() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int getMapWidth() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getMapHeight() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public Team getTeam() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Robot getRobot() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public RobotType getType() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean isConstructing() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public RobotType getConstructingType() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int getConstructingRounds() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public double senseTeamMilkQuantity(Team t) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int senseRobotCount() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public GameObject senseObjectAtLocation(MapLocation loc)
				throws GameActionException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <T extends GameObject> T[] senseNearbyGameObjects(Class<T> type) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <T extends GameObject> T[] senseNearbyGameObjects(Class<T> type,
				int radiusSquared) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <T extends GameObject> T[] senseNearbyGameObjects(Class<T> type,
				int radiusSquared, Team team) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <T extends GameObject> T[] senseNearbyGameObjects(Class<T> type,
				MapLocation center, int radiusSquared, Team team) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Robot[] senseBroadcastingRobots() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Robot[] senseBroadcastingRobots(Team t) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public MapLocation[] senseBroadcastingRobotLocations() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public MapLocation[] senseBroadcastingRobotLocations(Team t) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public MapLocation senseLocationOf(GameObject o)
				throws GameActionException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public RobotInfo senseRobotInfo(Robot r) throws GameActionException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean canSenseObject(GameObject o) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean canSenseSquare(MapLocation loc) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public MapLocation senseHQLocation() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public MapLocation senseEnemyHQLocation() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public MapLocation[] sensePastrLocations(Team t) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public double[][] senseCowGrowth() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public double senseCowsAtLocation(MapLocation loc)
				throws GameActionException {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int roundsUntilActive() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public boolean isActive() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void move(Direction dir) throws GameActionException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void sneak(Direction dir) throws GameActionException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean canMove(Direction dir) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean canAttackSquare(MapLocation loc) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void attackSquare(MapLocation loc) throws GameActionException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void attackSquareLight(MapLocation loc)
				throws GameActionException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void broadcast(int channel, int data) throws GameActionException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public int readBroadcast(int channel) throws GameActionException {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void spawn(Direction dir) throws GameActionException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void construct(RobotType type) throws GameActionException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void yield() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void selfDestruct() throws GameActionException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void resign() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void wearHat() throws GameActionException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setIndicatorString(int stringIndex, String newString) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public TerrainTile senseTerrainTile(MapLocation loc) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getControlBits() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void addMatchObservation(String observation) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setTeamMemory(int index, long value) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setTeamMemory(int index, long value, long mask) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public long[] getTeamMemory() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void breakpoint() {
			// TODO Auto-generated method stub
			
		}
		
	}
}

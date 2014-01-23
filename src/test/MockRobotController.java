package test;

import battlecode.common.*;

public class MockRobotController implements RobotController {

	public MapLocation myHQLocation=new MapLocation(5,5);
	public MapLocation enemyHQLocation=new MapLocation(20,20);
	public MapLocation robotLocation=new MapLocation(12,22);
	
	public double health;
	public int mapWidth=25;
	public int mapHeight=25;
	public Team team = Team.A;
	
	public int robotId= 42;
	public RobotType robotType=RobotType.SOLDIER;
	
	private int robotCount=1;	
	
	public Robot robot = new Robot() {

		@Override
		public int getID() {
			return robotId;
		}

		@Override
		public Team getTeam() {
			return team;
		}

		@Override
		public RobotLevel getRobotLevel() {
			return RobotLevel.ON_GROUND;
		}
	};
	
	@Override
	public double getActionDelay() {
		return 0;
	}

	@Override
	public double getHealth() {
		return health;
	}

	@Override
	public MapLocation getLocation() {
		return robotLocation;
	}

	@Override
	public int getMapWidth() {
		return mapWidth;
	}

	@Override
	public int getMapHeight() {
		return mapHeight;
	}

	@Override
	public Team getTeam() {
		return team;
	}

	@Override
	public Robot getRobot() {
		return robot;
	}

	@Override
	public RobotType getType() {
		return robotType;
	}

	@Override
	public boolean isConstructing() {
		return false;
	}

	@Override
	public RobotType getConstructingType() {
		return null;
	}

	@Override
	public int getConstructingRounds() {
		return 0;
	}

	@Override
	public double senseTeamMilkQuantity(Team t) {
		return 0;
	}

	@Override
	public int senseRobotCount() {
		return robotCount;
	}

	@Override
	public GameObject senseObjectAtLocation(MapLocation loc) throws GameActionException {
		return null;
	}

	@Override
	public <T extends GameObject> T[] senseNearbyGameObjects(Class<T> type) {
		return (T[]) new Robot[0];
	}

	@Override
	public <T extends GameObject> T[] senseNearbyGameObjects(Class<T> type, int radiusSquared) {
		return (T[]) new Robot[0];
	}

	@Override
	public <T extends GameObject> T[] senseNearbyGameObjects(Class<T> type,int radiusSquared, Team team) 
	{
		return (T[]) new Robot[0];
	}

	@Override
	public <T extends GameObject> T[] senseNearbyGameObjects(Class<T> type,MapLocation center, int radiusSquared, Team team) 
	{
		return (T[]) new Robot[0];
	}

	@Override
	public Robot[] senseBroadcastingRobots() {
		return new Robot[0];
	}

	@Override
	public Robot[] senseBroadcastingRobots(Team t) {
		return new Robot[0];
	}

	@Override
	public MapLocation[] senseBroadcastingRobotLocations() {
		return new MapLocation[0];
	}

	@Override
	public MapLocation[] senseBroadcastingRobotLocations(Team t) {
		return new MapLocation[0];
	}

	@Override
	public MapLocation senseLocationOf(GameObject o) throws GameActionException {
		return null;
	}

	@Override
	public RobotInfo senseRobotInfo(Robot r) throws GameActionException {
		return null;
	}

	@Override
	public boolean canSenseObject(GameObject o) {
		return false;
	}

	@Override
	public boolean canSenseSquare(MapLocation loc) {
		return loc.distanceSquaredTo( getLocation() ) <= getType().sensorRadiusSquared;
	}

	@Override
	public MapLocation senseHQLocation() {
		return myHQLocation;
	}

	@Override
	public MapLocation senseEnemyHQLocation() {
		return enemyHQLocation;
	}

	@Override
	public MapLocation[] sensePastrLocations(Team t) {
		return new MapLocation[0];
	}

	@Override
	public double[][] senseCowGrowth() {
		return new double[0][0];
	}

	@Override
	public double senseCowsAtLocation(MapLocation loc) throws GameActionException {
		return 0;
	}

	@Override
	public int roundsUntilActive() {
		return 0;
	}

	@Override
	public boolean isActive() {
		return true;
	}

	@Override
	public void move(Direction dir) throws GameActionException {
		if ( ! canMove(dir ) ) {
			throw new IllegalStateException("Cannot sneak to from "+getLocation()+" -> "+getLocation().add( dir ) );
		}
		robotLocation = getLocation().add( dir );		
	}

	@Override
	public void sneak(Direction dir) throws GameActionException 
	{
		if ( ! canMove(dir ) ) {
			throw new IllegalStateException("Cannot sneak to from "+getLocation()+" -> "+getLocation().add( dir ) );
		}
		robotLocation = getLocation().add( dir );
	}

	@Override
	public boolean canMove(Direction dir) 
	{
		switch( senseTerrainTile( getLocation().add( dir ) ) ) {
		case NORMAL:
		case ROAD:
			return getLocation().distanceSquaredTo( getLocation() ) <= 2; 
			default:
				return false;
		}
	}

	@Override
	public boolean canAttackSquare(MapLocation loc) {
		return loc.distanceSquaredTo( getLocation() ) <= getType().attackRadiusMaxSquared;
	}

	@Override
	public void attackSquare(MapLocation loc) throws GameActionException {
	}

	@Override
	public void attackSquareLight(MapLocation loc) throws GameActionException {
	}

	@Override
	public void broadcast(int channel, int data) throws GameActionException {
	}

	@Override
	public int readBroadcast(int channel) throws GameActionException {
		return 0;
	}

	@Override
	public void spawn(Direction dir) throws GameActionException {
	}

	@Override
	public void construct(RobotType type) throws GameActionException {
	}

	@Override
	public void yield() {
	}

	@Override
	public void selfDestruct() throws GameActionException {
	}

	@Override
	public void resign() {
	}

	@Override
	public void wearHat() throws GameActionException {
	}

	@Override
	public void setIndicatorString(int stringIndex, String newString) {
	}

	@Override
	public TerrainTile senseTerrainTile(MapLocation loc) {
		return TerrainTile.VOID;
	}

	@Override
	public long getControlBits() {
		return 0;
	}

	@Override
	public void addMatchObservation(String observation) {
	}

	@Override
	public void setTeamMemory(int index, long value) {
	}

	@Override
	public void setTeamMemory(int index, long value, long mask) {
	}

	@Override
	public long[] getTeamMemory() {
		return new long[0];
	}

	@Override
	public void breakpoint() {
	}
}
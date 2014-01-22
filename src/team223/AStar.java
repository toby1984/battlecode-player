package team223;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Stack;

import battlecode.common.Clock;
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

public final class AStar
{
	private static final int DEFAULT_INTERRUPT_CHECK_INTERVAL = 10;
	
	private static int INTERRUPT_CHECK_INTERVAL = DEFAULT_INTERRUPT_CHECK_INTERVAL;
    
    // nodes to check
    private static HashMap<PathNode,PathNode> openMap = new HashMap<PathNode, PathNode>(2000);
    private static PriorityQueue<PathNode> openList = new PriorityQueue<PathNode>(2000);

    // nodes ruled out
    private static HashSet<PathNode> closeList = new HashSet<PathNode>();
	
    protected static MapLocation start;
    protected static MapLocation destination;
    
    public static RobotController rc;
    
    public static int mapWidth;
    public static int mapHeight;
    
    private static int iterationCount;
    public static PathNode interruptedNode;
    
    public static boolean started = false;
    public static boolean finished = false;
    public static boolean aborted = false;
    
    private static int pathFindingTimeout;

    private static int totalElapsedRounds;
    private static int startedInRound;
    
    private static Callback callback;
    
    public static boolean[][] blockedTiles;
    
    public static String getState() {
    	return "started: "+started+" , finished: "+finished+", aborted: "+aborted+" , interrupted: "+isInterrupted();
    }
    
    public static enum Result {
    	INTERRUPT,ABORT,CONTINUE;
    }
    
    public interface Callback 
    {
    	public void foundPath(List<MapLocation> path);
    	
    	public boolean abortOnTimeout() throws GameActionException;
    	
    	public void foundNoPath();    	
    }
    
    public final static class PathNode implements Comparable<PathNode>
    {
        public PathNode parent;
        public final MapLocation value;
        
        public double f;
        public double g;
        
        private final int hashcode;
        
        public PathNode(MapLocation value) 
        {
            this.parent=null;
            this.value = value;
            this.hashcode = value.hashCode();
        }
        
        public PathNode(MapLocation value,PathNode parent) 
        {
            this.parent=parent;
            this.value = value;
            this.hashcode = value.hashCode();
        }        
        
        @Override
        public int compareTo(PathNode o) 
        {
        	return (int) Math.signum( this.f - o.f );
        }
        
        @Override
		public int hashCode() 
        {
        	return hashcode;
		}

		@Override
		public boolean equals(Object obj) 
		{
			return this.value.equals( ((PathNode) obj).value );
		}

		public final void f(double value) { this.f = value; }
        public final void g(double value) { this.g = value; }

        public final double f() { return f; }
        public final double g() { return g;}

        public final PathNode parent() { return parent; }

        public final int getNodeCount() {
            int result =1;
            PathNode current = this.parent;
            while( current != null ) {
                result++;
                current = current.parent;
            }
            return result;
        }
        
        /**
         * Returns a list of path nodes from start (first list element) to destination (last list element).
         * 
         * @return
         */
        public final List<PathNode> toList() 
        {
        	List<PathNode> result = new ArrayList<PathNode>();
        	PathNode current = this;
        	do 
        	{
        		result.add( current );
        		current = current.parent;
        	} while ( current != null );
        	Collections.reverse(result);
        	return result;
        }
        
        @Override
        public final String toString()
        {
            final Stack<PathNode> stack = new Stack<PathNode>();

            PathNode current = this;
            do {
                stack.push( current );
                current = current.parent;
            } while( current != null );

            final StringBuilder builder = new StringBuilder();
            while ( ! stack.isEmpty() ) {
                final PathNode pop = stack.pop();
                builder.append( "[ "+nodeToString( pop )+" ]");
                if ( ! stack.isEmpty() ) {
                    builder.append(" -> ");
                }
            }
            return builder.toString();            
        }
        
        private String nodeToString(PathNode n) {
        	return n.value == null ? "<NULL>" : n.value.toString();
        }
    }
    
	public static final boolean isInterrupted() {
		return started && interruptedNode != null;
	}
	
	public static final void continueFindPath() throws GameActionException 
	{
//		if ( interruptedNode == null || ! started || finished || aborted ) {
//			throw new IllegalStateException("Cannot continue (interrupted: "+isInterrupted()+" , started: "+started+" , finished: "+finished+" , aborted: "+aborted);
//		}
		
		if ( MyConstants.DEBUG_MODE) System.out.println("Continueing to find path "+start+" -> "+destination);
		mainLoop(interruptedNode);
	}
	
	public static void setRoute(MapLocation from,MapLocation to,int pathFindingTimeout) 
	{
		if ( from == null || to == null ) {
			throw new IllegalArgumentException("from/to must not be null (from: "+from+" / to: "+to+")");
		}
		if ( started ) {
			new Exception("Cannot change route on already started search").printStackTrace();
			throw new IllegalStateException("Cannot change route on already started search");
		}
		if ( MyConstants.DEBUG_MODE ) 
		{
			System.out.println("setRoute( timeout= "+pathFindingTimeout+" ): "+from+" -> "+to);
		}
		AStar.pathFindingTimeout = pathFindingTimeout;
		AStar.start = from;
		AStar.destination = to;
	}
	
	public static final void reset() 
	{
		if ( MyConstants.DEBUG_MODE) System.out.println("Path finder "+start+" -> "+destination+" reset.");
		
    	iterationCount = INTERRUPT_CHECK_INTERVAL; 
    	interruptedNode = null;
    	
    	totalElapsedRounds = 0;
    	
    	aborted = false;
        finished = false;
        started = false;	
        
        openMap = new HashMap<PathNode, PathNode>(2000);
        openList = new PriorityQueue<PathNode>(2000);
        closeList = new HashSet<PathNode>();        
	}
	
	public static void abort() 
	{
		AStar.aborted = true;
		AStar.finished = true;
	}
	
    public static final void findPath(Callback callback) throws GameActionException 
    {
    	if ( finished || started|| aborted ) {
    		throw new IllegalStateException("You need to call reset() before starting a new search (finished:"+finished+" , started: "+started+", aborted: "+aborted+")");
    	}
    	
    	INTERRUPT_CHECK_INTERVAL = DEFAULT_INTERRUPT_CHECK_INTERVAL;
    	
    	reset();
    	
        started = true;
    	startedInRound = Clock.getRoundNum();           
        AStar.callback = callback;
        
		if ( MyConstants.ASTAR_DEBUG_RUNTIME ) System.out.println("Looking for path from "+AStar.start+" to "+AStar.destination+" (timeout: "+pathFindingTimeout+")");
		
       	init();
       	
       	if ( blockedTiles[destination.x+1][destination.y+1] ) {
       		System.out.println("Destination "+destination+" blocked (map size: "+rc.getMapWidth()+"x"+rc.getMapHeight()+")");
       		searchFinished(null);
       		// printMap( null );
       		return;
       	}
        
    	final PathNode start = new PathNode( AStar.start );
    	
        assignCostToStartNode( start );
        closeList.add( start );

        mainLoop( start );
    }
    
    public static void main(String[] args) throws GameActionException {
		
    	RobotPlayer.enemyHQ = new MapLocation(10,10);
    	RobotPlayer.myHQ = new MapLocation(15,16);
    	RobotPlayer.myTeam = Team.A;
    	RobotPlayer.enemyTeam = RobotPlayer.myTeam.opponent();
    	
    	mapWidth = 27;
    	mapHeight = 27;
    	
    	final MapLocation myLocation  = new MapLocation(26,26);
    	MapLocation myDestination = new MapLocation( 5 , 5 );
    	
    	start = myLocation;
    	
    	rc = new MockRobotController() {
    		@Override
    		public MapLocation getLocation() { return myLocation; }
    		@Override
    		public GameObject senseObjectAtLocation(MapLocation loc) throws GameActionException 
    		{
    			return null;
    		}
    		
    		@Override
    		public TerrainTile senseTerrainTile(MapLocation loc) {
    			return TerrainTile.ROAD;
    		}
    		
    		@Override
    		public RobotInfo senseRobotInfo(Robot r) throws GameActionException {
    			return null;
    		}
    	};

		setRoute( myLocation , myDestination , Integer.MAX_VALUE );
    	
    	findPathNonInterruptible( new Callback() {
			
			@Override
			public void foundPath(List<MapLocation> path) {
				System.out.println("Found path");
				printMap(path);
			}
			
			@Override
			public void foundNoPath() {
				System.out.println("Found no path");				
				printMap(null);
			}
			
			@Override
			public boolean abortOnTimeout() throws GameActionException {
				return false;
			}
		});
	}
    
    public static void findPathNonInterruptible(Callback cb) throws GameActionException {
    	
    	INTERRUPT_CHECK_INTERVAL = 100000;
    	try {
	    	do {
	    		if ( started ) {
	    			continueFindPath();
	    		} else {
	    			findPath( cb );
	    		} 
	    	} while ( ! finished );
    	} finally {
    		INTERRUPT_CHECK_INTERVAL = DEFAULT_INTERRUPT_CHECK_INTERVAL;
    	}
    }
    
    private static void printMap(List<MapLocation> path) {
    	
    	for ( int y = 0 ; y < mapHeight+2 ; y++ ) 
    	{
    		for ( int x = 0 ; x < mapWidth+2 ; x++ ) 
    		{
    			if ( (x-1) == start.x && (y-1) == start.y ) {
    				System.out.print("S");
    				continue;
    			}
    			if ( (x-1) == destination.x && (y-1) == destination.y ) {
    				System.out.print("D");
    				continue;
    			}    			
    			if ( (x-1) == RobotPlayer.enemyHQ.x && (y-1) == RobotPlayer.enemyHQ.y ) {
    				System.out.print("e");
    				continue;
    			}  
    			if ( (x-1) == RobotPlayer.myHQ.x && (y-1) == RobotPlayer.myHQ.y ) {
    				System.out.print("m");
    				continue;
    			}      			
    			
    			boolean isOnPath = path != null && path.contains(new MapLocation(x-1,y-1 ) );
    			if ( blockedTiles[x][y] ) 
    			{
    				if ( isOnPath ) {
    					System.out.print("!");
    				} else {
    					System.out.print("x");    					
    				}
    			} 
    			else 
    			{
    				if ( isOnPath ) {
    					System.out.print("P");
    				} else {
    					System.out.print("o");
    				}
    			}
    		}
    		System.out.println();
    	}
    }
    
	private static void init() throws GameActionException 
	{
        int w = mapWidth+2; // +2 because we'll mark the circumference of the map as blocked
        int h = mapHeight+2;
        
        int w2 = w-1;
        int h2 = h-1;
        
		blockedTiles = new boolean[w][];
		
        for ( int i = 0 ; i < w ; i++ ) 
        {
        	blockedTiles[i]=new boolean[h];        	
        	if ( i == 0 || i == w2 ) {
        		for ( int j = 0 ; j < h ; j++ ) 
        		{
               		blockedTiles[i][j]=true;        			
        		}
        	} 
       		blockedTiles[i][0]=true;
       		blockedTiles[i][h2]=true;
        }
        
        // block my HQ
        // System.out.println("Blocking my HQ at "+RobotPlayer.myHQ);
        blockedTiles[ 1+RobotPlayer.myHQ.x ][1+RobotPlayer.myHQ.y]=true;
        
        // block area around enemy HQ
        // System.out.println("Blocking enemy HQ at "+RobotPlayer.enemyHQ);
        blockSquare( RobotPlayer.enemyHQ.x, RobotPlayer.enemyHQ.y , 3 ); // HQ has attack range 15 = 3^2
        
        int lx = rc.getLocation().x;
        int ly = rc.getLocation().y;
        
        int locXMin = lx - 4 ;
        if ( locXMin < 0 ) {
        	locXMin = 0;
        }
        
        int locYMin = ly - 4 ;
        if ( locYMin < 0 ) {
        	locYMin = 0;
        }        

        int locXMax = lx + 4;
        if ( locXMax >= mapWidth ) {
        	locXMax = mapWidth-1;
        }
        
        int locYMax = ly + 4;
        if ( locYMax >= mapHeight ) {
        	locYMax = mapHeight-1;
        }        
        for ( int x = locXMin ; x <= locXMax ; x++ ) 
        {
            for ( int y = locYMin ; y <= locYMax ; y++ ) 
            {
            	if ( x !=lx || y != ly) 
            	{
            		try {
            			GameObject object = rc.senseObjectAtLocation( new MapLocation(x,y ) );
                		if ( object != null ) 
                		{
                			RobotInfo info = rc.senseRobotInfo( (Robot) object );
                			if ( info.team == RobotPlayer.myTeam ) 
                			{
                				// something from my team
                				if( info.type == RobotType.NOISETOWER || info.type == RobotType.PASTR ) 
                				{
                			        System.out.println("Blocking my ("+x+","+y+") , occupied by my PASTR or noisetower");
               						blockedTiles[x+1][y+1]=true;
                				}            				
                			}
                		}            			
            		} catch(GameActionException e) {
            			int dist =  new MapLocation(x,y).distanceSquaredTo( rc.getLocation() );
            			System.out.println("Failed to sense location ("+x+","+y+") while at "+rc.getLocation()+" , type: "+rc.getType()+" , dist: "+dist );
            		}
            	}
            }
        }
	}
	
	private static void blockSquare(int centerX,int centerY,int radius) 
	{
		int xmin = centerX+1-radius;
		if ( xmin < 0 ) {
			xmin = 0;
		}
		
		int ymin = centerY+1-radius;
		if ( ymin < 0 ) {
			ymin = 0;
		}
		
		int xmax = centerX+1+radius;
		if ( xmax >= mapWidth+2 ) {
			xmax = mapWidth+1;
		}
		
		int ymax = centerY+1+radius;
		if ( ymax >= mapHeight+2 ) 
		{
			ymax = mapHeight+1;
		}		
		
        for (int x = xmin ; x <= xmax ; x++ ) 
        {
            for (int y = ymin ; y <= ymax ; y++ ) 
            {
                // System.out.println("Blocking enemy HQ at "+RobotPlayer.myHQ);            	
            	blockedTiles[ x ][ y ] = true;
            }
        }   
	}
    
    private static final void searchFinished( List<MapLocation> result ) 
    {
    	finished = true;
    	interruptedNode = null;
    	
    	if ( result != null ) 
    	{
			if ( MyConstants.ASTAR_DEBUG_RUNTIME ) {
				System.out.println("*** (elapsed rounds: "+totalElapsedRounds+") Path finding "+start+" -> "+destination+" finished, path length: "+result.size());
			}
    		callback.foundPath( result );
    	} 
    	else 
    	{
			if ( MyConstants.ASTAR_DEBUG_RUNTIME ) {
				System.out.println("*** (elapsed rounds: "+totalElapsedRounds+") Path finding "+start+" -> "+destination+" FAILED (aborted: "+aborted+" , elapsed rounds: "+totalElapsedRounds+")");
			}
    		callback.foundNoPath();
    	}
    }
    
    private static void mainLoop(PathNode current) throws GameActionException 
    {
    	interruptedNode = null;
        while ( true ) 
        {
        	if ( aborted ) {
        		searchFinished(null);
        		return;
        	}
        	
        	scheduleNeighbors( current );

            if ( openList.isEmpty() ) {
            	searchFinished(null);
                return;
            }

            PathNode cheapestPath = openList.remove();

            if ( cheapestPath.value.distanceSquaredTo( destination ) <= 2 ) 
            {
        		List<MapLocation> result = new ArrayList<MapLocation>();
        		do {
        			result.add( cheapestPath.value );
        			cheapestPath = cheapestPath.parent;
        		} while ( cheapestPath != null );
        		
        		Collections.reverse( result );
        		searchFinished( result );
        		return;            	
            }            
            
            openMap.remove( cheapestPath );
            closeList.add( cheapestPath );  
            
            current = cheapestPath;
            
        	if ( --iterationCount <= 0 ) 
        	{
        		iterationCount = INTERRUPT_CHECK_INTERVAL;

        		final int currentRound = Clock.getRoundNum();
        		totalElapsedRounds += ( currentRound - startedInRound);
        		startedInRound = currentRound;         			
        		
        		if ( totalElapsedRounds >= pathFindingTimeout ) 
        		{
        			if ( MyConstants.ASTAR_DEBUG_RUNTIME ) System.out.println("!!! ( elapsed: "+totalElapsedRounds+", timeout limit: "+pathFindingTimeout+") Path finding timeout *** ");
        			
        			if( callback.abortOnTimeout() ) 
        			{
        				if ( MyConstants.ASTAR_DEBUG_RUNTIME ) {
        					System.out.println("!!! (Timeout,elapsed: "+totalElapsedRounds+") Aborted at node "+current.value);
        				}        				
        				finished = true;
        				aborted = true;
        				interruptedNode = null;
        				return;
        			}
        			if ( MyConstants.ASTAR_DEBUG_RUNTIME ) System.out.println("!!! (elapsed rounds: "+totalElapsedRounds+") Path finding continues after timeout ***");
        			totalElapsedRounds = 0;        			
        		}
        		
				if ( MyConstants.ASTAR_VERBOSE ) {
					System.out.println("Interruped at node "+current.value);
				}
				interruptedNode = current;
				return;
        	}            
        }    	
    }
    
    private static void assignCostToStartNode(PathNode current) 
    {
    	//        current.f( (float) 0 + calcEstimatedCost( current ) );
    	//        current.g( (float) 0 );
        current.f( 0 + 4*Math.sqrt( destination.distanceSquaredTo(  current.value ) ) ); // movement cost + estimated cost
        current.g( 0 );
    }    
    
    public static final MapLocation getDestination() {
		return destination;
	}
    
	private static void scheduleNeighbors(team223.AStar.PathNode parent) throws GameActionException 
	{
		int x = parent.value.x;
		int y = parent.value.y;
		
		int newX=0;
		int newY=0;
		
		for ( int dx = -1 ; dx <= 1 ; dx++ ) 
		{
			for ( int dy = -1 ; dy <= 1 ; dy++ ) 
			{
				if ( dx != 0 || dy != 0 ) 
				{
					newX = x+dx;
					newY = y+dy;
					final MapLocation newLocation = new MapLocation(newX,newY);
					final TerrainTile tile = rc.senseTerrainTile( newLocation );	
					if ( ( tile == TerrainTile.NORMAL || tile == TerrainTile.ROAD ) && blockedTiles[newX+1][newY+1]==false )
					{
						maybeAddNeighbor( parent , newLocation );
					} 
				}
			}
		}
	}	
    
    private static void maybeAddNeighbor(PathNode parent, MapLocation point)
    {
        final PathNode newNode = new PathNode( point , parent );
        if ( ! closeList.contains(newNode) ) 
        {
            final PathNode existing = openMap.get(newNode);

            final double movementCost = newNode.parent.g + newNode.value.distanceSquaredTo( newNode.parent.value );
            
            if ( existing == null || movementCost < existing.g ) // prefer shorter path
            {
    			newNode.f = movementCost + 4*Math.sqrt( destination.distanceSquaredTo(  newNode.value ) ); // movementCost + calcEstimatedCost( newNode );
    			newNode.g = movementCost;    			
                openMap.put(newNode,newNode);
				openList.add( newNode );
            }
        }
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
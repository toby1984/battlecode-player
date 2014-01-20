package team223;

import java.util.*;

import battlecode.common.*;


public abstract class AStar
{
	private static final boolean VERBOSE = false;
	
	private static final boolean DEBUG_RUNTIME = true;
	
    public static final int INTERRUPT_CHECK_INTERVAL = 3;
    
    // nodes to check
    private HashMap<PathNode,PathNode> openMap = new HashMap<PathNode, PathNode>(2000);
    private PriorityQueue<PathNode> openList = new PriorityQueue<PathNode>(2000);

    // nodes ruled out
    private HashSet<PathNode> closeList = new HashSet<PathNode>();
	
    protected MapLocation start;
    protected MapLocation destination;
    
    private final RobotController rc;
    
    private int iterationCount;
    private PathNode interruptedNode;
    
    private boolean started = false;
    private boolean finished = false;
    private boolean aborted = false;
    
    private final int pathFindingTimeout;

    private int totalElapsedRounds;
    private int elapsedRounds;
    private int startedInRound;
    
    private Callback callback;
    
    // debugging
    private int missedRounds = 0;
    private long debugCounter = 0;    
    
    public static enum Result {
    	INTERRUPT,ABORT,CONTINUE;
    }
    
    public static enum TimeoutResult {
    	ABORT,CONTINUE;
    }
    
    public interface PathFindingResultCallback 
    {
    	public void foundPath(List<MapLocation> path);
    	
    	public TimeoutResult onTimeout() throws GameActionException;
    	
    	public void foundNoPath();    	
    }
    
    public interface Callback extends  PathFindingResultCallback
    {
    	public Result checkInterrupt();
    }
    
    public final static class PathNode implements Comparable<PathNode>
    {
        public PathNode parent;
        public final MapLocation value;
        
        private float f;
        private float g;
        
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
        	if ( this.f < o.f ) {
        		return -1;
        	} 
        	if ( this.f > o.f ) {
        		return 1;
        	}
        	return 0;
        }
        
        @Override
		public int hashCode() 
        {
        	return hashcode;
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean equals(Object obj) 
		{
			if ( obj instanceof PathNode) {
				return this.value.equals( ((PathNode) obj).value );
			}
			return false;
		}

		public final void f(float value) { this.f = value; }
        public final void g(float value) { this.g = value; }

        public final float f() { return f; }
        public final float g() { return g;}

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
    
	public AStar(RobotController rc,int pathFindingTimeout) {
		this.rc = rc;
		this.pathFindingTimeout = pathFindingTimeout;
	}
	
	public final boolean isInterrupted() {
		return interruptedNode != null;
	}
	
	public final boolean isFinished() {
		return finished;
	}
	
	public final boolean isStarted() {
		return started;
	}
	
	public final boolean isAborted() {
		return aborted;
	}
	
	public final void continueFindPath() throws GameActionException 
	{
		if ( interruptedNode == null || ! started || finished || aborted ) {
			throw new IllegalStateException("Cannot continue (interrupted: "+isInterrupted()+" , started: "+started+" , finished: "+finished+" , aborted: "+aborted);
		}
		
		if ( MyConstants.DEBUG_MODE) System.out.println("Continueing to find path "+start+" -> "+destination);
		
    	PathNode current = interruptedNode;
		interruptedNode = null;    		
		
		startedInRound = Clock.getRoundNum();
		mainLoop(current);
	}
	
	public final void setRoute(MapLocation from,MapLocation to) {
		if ( from == null || to == null ) {
			throw new IllegalArgumentException("from/to must not be null (from: "+from+" / to: "+to+")");
		}
		if ( started ) {
			new Exception("Cannot change route on already started search").printStackTrace();
			throw new IllegalStateException("Cannot change route on already started search");
		}
		if ( MyConstants.DEBUG_MODE ) {
			try {
				System.out.println("setRoute(): "+from+" -> "+to+" (walkable: "+isWalkable( to )+")" );
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		}
		this.start = from;
		this.destination = to;
	}
	
	public final void reset() 
	{
		if ( MyConstants.DEBUG_MODE) System.out.println("Path finder "+start+" -> "+destination+" reset.");
		
    	iterationCount = INTERRUPT_CHECK_INTERVAL; 
    	interruptedNode = null;
    	
    	totalElapsedRounds = 0;
    	elapsedRounds = 0;
    	
    	aborted = false;
        finished = false;
        started = false;	
        
        openMap = new HashMap<PathNode, PathNode>(2000);
        openList = new PriorityQueue<PathNode>(2000);
        closeList = new HashSet<PathNode>();        
	}
	
	public void abort() 
	{
		this.aborted = true;
		this.finished = true;
	}
	
    public final void findPath(Callback callback) throws GameActionException 
    {
    	if ( isFinished() || isStarted() || isAborted() ) {
    		throw new IllegalStateException("You need to call reset() before starting a new search (finished:"+finished+" , started: "+started+", aborted: "+aborted+")");
    	}
    	
    	reset();
    	
    	startedInRound = Clock.getRoundNum();
    	
        started = true;
        this.callback = callback;
        
		if ( DEBUG_RUNTIME ) System.out.println("Looking for path from "+this.start+" to "+this.destination);
		
        if ( this.start.equals(  this.destination ) ) { // trivial case
        	List<MapLocation> result = new ArrayList<MapLocation>();
        	result.add( this.start );
        	result.add( this.destination );
        	
        	searchFinished( result );
            return;
        }
        
        if ( ! isWalkable( this.destination ) ) {
    		// if ( MyConstants.DEBUG_MODE) 
        	System.out.println(">>>>>>> Destination "+this.destination+" is not walkable");
        	searchFinished( null );
        	return;
        }
        
    	final PathNode start = new PathNode( this.start );
    	
        assignCost( start );
        closeList.add( start );

		if ( VERBOSE ) System.out.println("Starting search "+this.start+" -> "+this.destination);
        mainLoop( start );
    }
    
    private final void searchFinished( List<MapLocation> result ) {
    	finished = true;
    	interruptedNode = null;
    	
    	if ( result != null ) {
			if ( DEBUG_RUNTIME ) {
				System.out.println("*** (elapsed rounds: "+totalElapsedRounds+") Path finding finished "+start+" -> "+destination+" , path length: "+result.size());
			}
    		callback.foundPath( result );
    	} 
    	else 
    	{
			if ( DEBUG_RUNTIME ) {
				System.out.println("*** (elapsed rounds: "+totalElapsedRounds+") Path finding failed (aborted: "+aborted+" , elapsed rounds: "+totalElapsedRounds+")");
			}
    		callback.foundNoPath();
    	}
    }
    
    private void mainLoop(PathNode current) throws GameActionException 
    {
        while ( true ) 
        {
        	if ( VERBOSE ) {
        		debugCounter++;
        		if ( (debugCounter%30) == 0 ) {
        			System.out.println("Searching (nodes: "+debugCounter+" , "+start+" -> "+destination+")");
        		}
        	}
        	
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

            if ( isCloseEnoughToTarget( cheapestPath ) ) 
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

        		if ( DEBUG_RUNTIME ) 
        		{
            		final int currentRound = Clock.getRoundNum();
            		int delta = ( currentRound - startedInRound);
            		
            		totalElapsedRounds += delta;
            		elapsedRounds += delta;
            		startedInRound = currentRound;        			
        		} else {
            		final int currentRound = Clock.getRoundNum();
            		elapsedRounds += ( currentRound - startedInRound);
            		startedInRound = currentRound;         			
        		}
        		
        		if ( elapsedRounds >= pathFindingTimeout ) 
        		{
        			if ( DEBUG_RUNTIME ) System.out.println("!!! (startedInRound: "+startedInRound+", elapsed: "+totalElapsedRounds+", timeout limit: "+pathFindingTimeout+") Path finding timeout *** ");
        			switch( callback.onTimeout() ) 
        			{
            			case ABORT:
            				if ( DEBUG_RUNTIME ) {
            					System.out.println("!!! (Timeout,elapsed: "+totalElapsedRounds+") Aborted at node "+current.value);
            				}        				
            				finished = true;
            				aborted = true;
            				interruptedNode = null;
            				return;
            			default:
        			}
        			if ( DEBUG_RUNTIME ) System.out.println("!!! (elapsed rounds: "+totalElapsedRounds+") Path finding continues after timeout ***");
        			elapsedRounds = 0;        			
        		}
        		
        		switch( callback.checkInterrupt() ) 
        		{
        			case ABORT:
        				if ( VERBOSE ) {
        					System.out.println("Aborted at node "+current);
        				}        				
        				finished = true;
        				aborted = true;
        				interruptedNode = null;
        				return;
        			case INTERRUPT:
        				if ( VERBOSE ) {
        					System.out.println("Interruped at node "+current);
        				}
        				interruptedNode = current;
        				return;
        			default:
        		}
        	}            
        }    	
    }
    
    protected abstract boolean isCloseEnoughToTarget( PathNode node ); 

    private void assignCost(PathNode current) 
    {
        final float movementCost = calcMovementCost(current);
        current.f( movementCost + calcEstimatedCost( current ) );
        current.g( movementCost );
    }
    
    public final MapLocation getStart() {
		return start;
	}
    
    public final MapLocation getDestination() {
		return destination;
	}

	private final float calcMovementCost(team223.AStar.PathNode current) 
	{
        if( current.parent != null ) 
        {
        	return current.parent.g + (float) Math.sqrt( current.value.distanceSquaredTo( current.parent.value ) );
        }
        return 0;
	}
	
	private final float calcEstimatedCost( team223.AStar.PathNode node) 
	{
    	// WEIGHTED A-STAR !!!
    	return (float) (4*Math.sqrt( destination.distanceSquaredTo(  node.value ) ) );
	}

	private final void scheduleNeighbors(team223.AStar.PathNode parent) throws GameActionException 
	{
		int x = parent.value.x;
		int y = parent.value.y;
		
		for ( int dx = -1 ; dx <= 1 ; dx++ ) 
		{
			for ( int dy = -1 ; dy <= 1 ; dy++ ) 
			{
				if ( dx != 0 || dy != 0 ) 
				{
					final MapLocation newLocation = new MapLocation(x+dx,y+dy);
					final TerrainTile tile = rc.senseTerrainTile( newLocation );	
					if ( ( tile == TerrainTile.NORMAL || tile == TerrainTile.ROAD ) && isWalkable( newLocation ) ) //  && newLocation.distanceSquaredTo( RobotPlayer.enemyHQ) >= MyConstants.ENEMY_HQ_SAFE_DISTANCE_SRT 
					{
						maybeAddNeighbor( parent , newLocation );							
					}
				}
			}
		}
	}	
    
    private void maybeAddNeighbor(PathNode parent, MapLocation point)
    {
        final PathNode newNode = new PathNode( point , parent );
        if ( ! closeList.contains(newNode) ) 
        {
            final PathNode existing = openMap.get(newNode);

            final float movementCost = calcMovementCost(newNode);
			newNode.f( movementCost + calcEstimatedCost( newNode ) );
			newNode.g( movementCost );

            if ( existing == null || newNode.g < existing.g ) // prefer shorter path
            {
                openMap.put(newNode,newNode);
				openList.add( newNode );
            }
        }
    }
    
	public abstract boolean isWalkable(MapLocation loc) throws GameActionException;    
}
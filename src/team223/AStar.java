package team223;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Stack;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;


public abstract class AStar
{
	private static final boolean VERBOSE = false;
	
	private static final boolean DEBUG_BYTECODES_USED = false;
	
    public static final int INTERRUPT_CHECK_INTERVAL = 3;
    
    // nodes to check
    private HashMap<PathNode<MapLocation>,PathNode<MapLocation>> openMap = new HashMap<PathNode<MapLocation>, PathNode<MapLocation>>(2000);
    private PriorityQueue<PathNode<MapLocation>> openList = new PriorityQueue<PathNode<MapLocation>>(2000);

    // nodes ruled out
    private HashSet<PathNode<MapLocation>> closeList = new HashSet<PathNode<MapLocation>>();
	
    protected MapLocation start;
    protected MapLocation destination;
    
    private final RobotController rc;
    
    private int iterationCount;
    private PathNode<MapLocation> interruptedNode;
    
    private boolean started = false;
    private boolean finished = false;
    private boolean aborted = false;
    
    private final MapLocation enemyHQ;
    
    private Callback callback;
    
    public static enum Result {
    	INTERRUPT,ABORT,CONTINUE;
    }
    
    public interface PathFindingResultCallback 
    {
    	public void foundPath(List<MapLocation> path);
    	
    	public void foundNoPath();    	
    }
    
    public interface Callback extends  PathFindingResultCallback
    {
    	public Result checkInterrupt();
    }
    
    public final static class PathNode<V> implements Comparable<PathNode<V>>
    {
        public PathNode<V> parent;
        public final V value;
        
        private float f;
        private float g;
        
        public PathNode(V value) 
        {
            this.parent=null;
            this.value = value;
        }
        
        public PathNode(V value,PathNode<V> parent) 
        {
            this.parent=parent;
            this.value = value;
        }        
        
        @Override
        public int compareTo(PathNode<V> o) 
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
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + value.hashCode();
			return result;
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean equals(Object obj) 
		{
			if ( obj instanceof PathNode) {
				return this.value.equals( ((PathNode<V>) obj).value );
			}
			return false;
		}

		public final void f(float value) { this.f = value; }
        public final void g(float value) { this.g = value; }

        public final float f() { return f; }
        public final float g() { return g;}

        public final PathNode<V> parent() { return parent; }

        public final int getNodeCount() {
            int result =1;
            PathNode<V> current = this.parent;
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
        public final List<PathNode<V>> toList() 
        {
        	List<PathNode<V>> result = new ArrayList<PathNode<V>>();
        	PathNode<V> current = this;
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
            final Stack<PathNode<V>> stack = new Stack<PathNode<V>>();

            PathNode<V> current = this;
            do {
                stack.push( current );
                current = current.parent;
            } while( current != null );

            final StringBuilder builder = new StringBuilder();
            while ( ! stack.isEmpty() ) {
                final PathNode<V> pop = stack.pop();
                builder.append( "[ "+nodeToString( pop )+" ]");
                if ( ! stack.isEmpty() ) {
                    builder.append(" -> ");
                }
            }
            return builder.toString();            
        }
        
        private String nodeToString(PathNode<V> n) {
        	return n.value == null ? "<NULL>" : n.value.toString();
        }
    }
    
	private void insert(PathNode<MapLocation> node) 
	{
        openMap.put(node,node);
        openList.add( node );
	}    
	
	public AStar(RobotController rc) {
		this.rc = rc;
		this.enemyHQ = rc.senseEnemyHQLocation();
	}
	
	public boolean isInterrupted() {
		return interruptedNode != null;
	}
	
	public boolean isFinished() {
		return finished;
	}
	
	public boolean isStarted() {
		return started;
	}
	
	public boolean isAborted() {
		return aborted;
	}
	
	public void continueFindPath() throws GameActionException 
	{
		if ( interruptedNode == null || ! started || finished || aborted ) {
			throw new IllegalStateException("Cannot continue (interrupted: "+isInterrupted()+" , started: "+started+" , finished: "+finished+" , aborted: "+aborted);
		}
		
    	PathNode<MapLocation> current = interruptedNode;
		interruptedNode = null;    		
		mainLoop(current);
		return;		
	}
	
	public void setRoute(MapLocation from,MapLocation to) {
		this.start = from;
		this.destination = to;
	}
	
	public void reset() 
	{
		if ( MyConstants.DEBUG_MODE) System.out.println("Path finder "+start+" -> "+destination+" reset.");
		
    	iterationCount = INTERRUPT_CHECK_INTERVAL; 
    	interruptedNode = null;
    	aborted = false;
        finished = false;
        started = false;	
        
        openMap = new HashMap<PathNode<MapLocation>, PathNode<MapLocation>>(2000);
        openList = new PriorityQueue<PathNode<MapLocation>>(2000);
        closeList = new HashSet<PathNode<MapLocation>>();        
	}
	
    public void findPath(Callback callback) throws GameActionException 
    {
    	if ( isFinished() || isStarted() ) {
    		throw new IllegalStateException("You need to call reset() before starting a new search");
    	}
    	
    	reset();
    	
        started = true;
        this.callback = callback;
        
		if ( MyConstants.DEBUG_MODE) System.out.println("Looking for path from "+this.start+" to "+this.destination);
		
        if ( this.start.equals(  this.destination ) ) { // trivial case
        	List<MapLocation> result = new ArrayList<MapLocation>();
        	result.add( this.start );
        	result.add( this.destination );
        	
        	searchFinished( result );
            return;
        }
    	
    	final PathNode<MapLocation> start = new PathNode<MapLocation>( this.start );
    	
        assignCost( start );
        closeList.add( start );

		if ( VERBOSE ) System.out.println("Starting search "+this.start+" -> "+this.destination);
        mainLoop( start );
    }
    
    private final void searchFinished( List<MapLocation> result ) {
    	finished = true;
    	interruptedNode = null;
    	
    	if ( result != null ) {
			if ( VERBOSE ) {
				System.out.println("Search finished , result: "+result);
			}
    		callback.foundPath( result );
    	} 
    	else 
    	{
			if ( VERBOSE ) {
				System.out.println("Search failed.");
			}
    		callback.foundNoPath();
    	}
    }
    
    private int byteCodesUsed = 0;
    private int currentRound = 0;
    private int missedRounds = 0;
    
    private void mainLoop(PathNode<MapLocation> current) throws GameActionException 
    {
		if ( VERBOSE ) {
			System.out.println("Continueing/starting search at node "+current);
		}
		
		if ( DEBUG_BYTECODES_USED ) {
			currentRound = Clock.getRoundNum();
			byteCodesUsed = Clock.getBytecodeNum();
		}
		
        while ( true ) 
        {
        	scheduleNeighbors( current );

            if ( openList.isEmpty() ) {
            	searchFinished(null);
                return;
            }

            PathNode<MapLocation> cheapestPath = openList.remove();

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
        		if ( DEBUG_BYTECODES_USED ) {
        			int newRound = Clock.getRoundNum();
        			int newByteCodesUsed = Clock.getBytecodeNum();
        			if ( newRound == currentRound ) {
        				System.out.println("checkInterrupt() after "+(newByteCodesUsed-byteCodesUsed)+" bytecodes (missed rounds: "+missedRounds+")");
        				missedRounds = 0;
        			} else {
        				missedRounds++;
        			}
        			currentRound = newRound;
        			byteCodesUsed = newByteCodesUsed;
        		}        		
        		
        		iterationCount = INTERRUPT_CHECK_INTERVAL;    
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
        		}
        	}            
        }    	
    }
    
    protected abstract boolean isCloseEnoughToTarget( PathNode<MapLocation> node ); 

    private void assignCost(PathNode<MapLocation> current) 
    {
        final float movementCost = calcMovementCost(current);
        final float estimatedCost = calcEstimatedCost( current );
        current.f( movementCost + estimatedCost );
        current.g( movementCost );
    }
    
    public MapLocation getStart() {
		return start;
	}
    
    public MapLocation getDestination() {
		return destination;
	}

	private final float calcMovementCost(team223.AStar.PathNode<MapLocation> current) 
	{
        float cost=0;
        if( current.parent != null ) 
        {
        	final float dist = (float) Math.sqrt( current.value.distanceSquaredTo( current.parent.value ) );
        	cost = current.parent.g() + dist;
        }
        return cost;
	}
	
	private final float calcEstimatedCost( team223.AStar.PathNode<MapLocation> node) 
	{
    	// WEIGHTED A-STAR !!!
    	return 4 * (float) Math.sqrt( destination.distanceSquaredTo(  node.value ) );
	}

	private final void scheduleNeighbors(team223.AStar.PathNode<MapLocation> parent) throws GameActionException 
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
					if ( newLocation.distanceSquaredTo( enemyHQ ) >= MyConstants.ENEMY_HQ_SAFE_DISTANCE_SRT ) 
					{
						final TerrainTile tile = rc.senseTerrainTile( newLocation );
						if ( ( tile == TerrainTile.NORMAL || tile == TerrainTile.ROAD ) && ! isOccupied( newLocation ) ) 
						{ 
							maybeAddNeighbor( parent , newLocation );
						}
					}
				}
			}
		}
	}	
    
    private final void maybeAddNeighbor(PathNode<MapLocation> parent, MapLocation point)
    {
        final PathNode<MapLocation> newNode = new PathNode<MapLocation>( point , parent );
        if ( ! closeList.contains(newNode) ) 
        {
            final PathNode<MapLocation> existing = openMap.get(newNode);

            assignCost( newNode );

            if ( existing == null || newNode.g < existing.g ) // prefer shorter path
            {
                insert( newNode );
            } 
        }
    }
    
	public abstract boolean isOccupied(MapLocation loc) throws GameActionException;    
}
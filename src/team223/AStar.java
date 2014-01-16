package team223;

import java.util.*;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.TerrainTile;


public abstract class AStar
{
    // nodes to check
    private final Map<PathNode<MapLocation>,PathNode<MapLocation>> openMap = new HashMap<PathNode<MapLocation>, PathNode<MapLocation>>(2000);
    private final PriorityQueue<PathNode<MapLocation>> openList = new PriorityQueue<PathNode<MapLocation>>(2000);

    // nodes ruled out
    private final Set<PathNode<MapLocation>> closeList = new HashSet<PathNode<MapLocation>>();
	
    protected MapLocation start;
    protected MapLocation destination;
    
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
        
        protected String nodeToString(PathNode<V> n) {
        	return n.value == null ? "<NULL>" : n.value.toString();
        }
    }
    
	private void insert(PathNode<MapLocation> node) 
	{
        openMap.put(node,node);
        openList.add( node );
	}    
	
	public AStar(MapLocation start,MapLocation destination) {
		this.start = start;
		this.destination = destination;
	}
	
    public PathNode<MapLocation> findPath() throws GameActionException 
    {
    	final PathNode<MapLocation> startNode = new PathNode<MapLocation>( start );
    	final PathNode<MapLocation> endNode = new PathNode<MapLocation>( destination );
		if ( MyConstants.DEBUG_MODE) System.out.println("Looking for path from "+startNode.value+" to "+endNode.value);
		return findPath( startNode ,endNode );
    }
    
    private PathNode<MapLocation> findPath(PathNode<MapLocation> start,PathNode<MapLocation> target) throws GameActionException 
    {
        if ( start.equals( target ) ) { // trivial case
            return start;
        }

        openMap.clear();
        openList.clear();
        closeList.clear();

        assignCost( start );
        
        closeList.add( start );

        PathNode<MapLocation> current = start;
        while ( true ) 
        {
        	scheduleNeighbors( current );

            if ( openList.isEmpty() ) {
                return null;
            }

            final PathNode<MapLocation> cheapestPath = openList.remove();

            if ( isCloseEnoughToTarget( cheapestPath ) ) 
            {
                return cheapestPath;
            }            
            
            openMap.remove( cheapestPath );
            closeList.add( cheapestPath );  
            
            current = cheapestPath;
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

	protected final float calcMovementCost(team223.AStar.PathNode<MapLocation> current) 
	{
        float cost=0;
        if( current.parent != null ) 
        {
        	final float dist = (float) Math.sqrt( current.value.distanceSquaredTo( current.parent.value ) );
        	cost = current.parent.g() + dist;
        }
        return cost;
	}
	
	protected final float calcEstimatedCost( team223.AStar.PathNode<MapLocation> node) 
	{
    	// WEIGHTED A-STAR !!!
    	return 4 * (float) Math.sqrt( destination.distanceSquaredTo(  node.value ) );
	}

	protected void scheduleNeighbors(team223.AStar.PathNode<MapLocation> parent) throws GameActionException 
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
					final TerrainTile tile = senseTerrainTile( newLocation );
					if ( ( tile == TerrainTile.NORMAL || tile == TerrainTile.ROAD ) && ! isOccupied( newLocation ) ) 
					{ 
						maybeAddNeighbor( parent , newLocation );
					}
				}
			}
		}
	}	
    
    protected final void maybeAddNeighbor(PathNode<MapLocation> parent, MapLocation point)
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
    
	public abstract TerrainTile senseTerrainTile(MapLocation loc);

	public abstract boolean isOccupied(MapLocation loc) throws GameActionException;    
}
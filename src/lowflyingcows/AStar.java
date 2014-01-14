package lowflyingcows;

import java.util.*;

import battlecode.common.GameActionException;


public abstract class AStar<T>
{
    // nodes to check
    private final Map<PathNode<T>,PathNode<T>> openMap = new HashMap<PathNode<T>, PathNode<T>>(2000);
    private final PriorityQueue<PathNode<T>> openList = new PriorityQueue<PathNode<T>>(2000);

    // nodes ruled out
    private final Set<PathNode<T>> closeList = new HashSet<PathNode<T>>();
	
    protected T start;
    protected T destination;
    
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
    
	private void insert(PathNode<T> node) 
	{
        openMap.put(node,node);
        openList.add( node );
	}    
	
	public AStar(T start,T destination) {
		this.start = start;
		this.destination = destination;
	}
	
    public PathNode<T> findPath() throws GameActionException 
    {
    	final PathNode<T> startNode = new PathNode<T>( start );
    	final PathNode<T> endNode = new PathNode<T>( destination );
		System.out.println("Looking for path from "+startNode.value+" to "+endNode.value);
		return findPath( startNode ,endNode );
    }
    
    private PathNode<T> findPath(PathNode<T> start,PathNode<T> target) throws GameActionException 
    {
        if ( start.equals( target ) ) { // trivial case
            return start;
        }

        openMap.clear();
        openList.clear();
        closeList.clear();

        assignCost( start );
        
        closeList.add( start );

        PathNode<T> current = start;
        while ( true ) 
        {
        	scheduleNeighbors( current );

            if ( openList.isEmpty() ) {
                return null;
            }

            final PathNode<T> cheapestPath = openList.remove();

            if ( isCloseEnoughToTarget( cheapestPath ) ) 
            {
                return cheapestPath;
            }            
            
            openMap.remove( cheapestPath );
            closeList.add( cheapestPath );  
            
            current = cheapestPath;
        }
    }
    
    protected abstract boolean isCloseEnoughToTarget( PathNode<T> node ); 

    private void assignCost(PathNode<T> current) 
    {
        final float movementCost = calcMovementCost(current);
        final float estimatedCost = calcEstimatedCost( current );
        current.f( movementCost + estimatedCost );
        current.g( movementCost );
    }

    protected abstract float calcMovementCost(PathNode<T> current);

    protected abstract float calcEstimatedCost(PathNode<T> node);

    protected abstract void scheduleNeighbors(PathNode<T> parent) throws GameActionException; 
    
    protected final void maybeAddNeighbor(PathNode<T> parent, T point)
    {
        final PathNode<T> newNode = new PathNode<T>( point , parent );
        if ( ! closeList.contains(newNode) ) 
        {
            final PathNode<T> existing = openMap.get(newNode);

            assignCost( newNode );

            if ( existing == null || newNode.g < existing.g ) // prefer shorter path
            {
                insert( newNode );
            } 
        }
    }    
}
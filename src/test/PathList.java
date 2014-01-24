package test;

import java.util.List;

import battlecode.common.MapLocation;

public final class PathList {

	private Node first;
	private Node iter;
	
	public static final class Node 
	{
		public MapLocation map;
		public Node next;
		
		public Node(MapLocation m) {
			this.map = m;
		}
	}
	
	public PathList(List<MapLocation> path) {
		
		final int l = path.size();
		if ( l == 1 ) {
			first = new Node( path.get(0) );
			return;
		} 
		
		Node previous = first = new Node( path.get(0) );
		for ( int i = 1 ; i < l ; i++ ) 
		{
			Node n = new Node( path.get(i) );
			previous.next = n;
			previous=n;
		}
		iter = first;
	}
	
	public boolean isEmpty() {
		return first==null;
	}

	public Node peek() {
		return iter;
	}
	
	public Node next() {
		Node result = iter;
		iter = iter.next;
		return result;
	}
}
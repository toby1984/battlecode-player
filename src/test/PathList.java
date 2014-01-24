package test;

import java.util.List;

import battlecode.common.MapLocation;

public final class PathList {

	public Node first;
	private Node last;
	
	private Node iter;
	
	public static final class Node 
	{
		public MapLocation map;
		public Node next;
		
		public Node(MapLocation m) {
			this.map = m;
		}
		
		@Override
		public String toString() {
			return map == null ? "<null>" : map.toString();
		}
	}
	
	public PathList() {
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
	
	public void add(MapLocation l) 
	{
		if ( first == null ) {
			first = last = new Node(l);
		} else {
			Node tmp = new Node(l);
			last.next = tmp;
			last = tmp;
		}
	}
	
	public boolean contains(MapLocation l) {
		Node current = first;
		while ( current != null ) {
			if ( current.map.equals( l ) ) {
				return true;
			}
			current = current.next;
		}
		return false;
	}
	
	public boolean remainingContains(MapLocation l) {
		Node current = iter;
		while ( current != null ) {
			if ( current.map.equals( l ) ) {
				return true;
			}
			current = current.next;
		}
		return false;
	}	
	
	public Node findNode(MapLocation l) {
		Node current = first;
		while ( current != null ) {
			if ( current.map.equals( l ) ) {
				return current;
			}
			current = current.next;
		}
		return null;
	}
	
	public Node nextStepNoAdvance(MapLocation l) 
	{
		if ( iter != null && iter.map.equals( l ) ) 
		{
			return iter.next;
		}
		
		// start all over
		Node current = first;
		while ( current != null ) 
		{
			if ( current.map.equals( l ) ) {
				return current.next;
			}
			current = current.next;
		}
		return null;
	}	
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		Node current = iter;
		while ( current != null )
		{
			builder.append( current.toString() );
			current=current.next;
			if ( current != null ) {
				builder.append(',');
			}
		} 
		return builder.toString();
	}
	
	
	public Node nextStep(MapLocation l) 
	{
		if ( iter != null && iter.map.equals( l ) ) 
		{
			iter = iter.next;
			return iter;
		}
		
		// start all over
		Node current = first;
		while ( current != null ) 
		{
			if ( current.map.equals( l ) ) {
				iter = current.next;
				return iter;
			}
			current = current.next;
		}
		return null;
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
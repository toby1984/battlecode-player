package lowflyingcows;

import java.util.List;

import battlecode.common.MapLocation;

public final class PathInfo 
{
	public final List<MapLocation> path;
	
	public PathInfo(List<MapLocation> path) {
		this.path = path;
	}
	
	public MapLocation getStepAfter(MapLocation current) 
	{
		if ( path != null ) 
		{
			final int len = path.size();
			for ( int i = 0 ; i < len ; i++ ) {
				if ( path.get(i).equals( current ) ) {
					if ( (i+1) < len ) {
						return path.get(i+1);
					}
					return null;
				}
			}
		}
		return null;
	}
	
	public boolean hasPathFor(MapLocation start,MapLocation end) {
		return start.equals( start() ) && end.equals( end() );
	}
	
	public MapLocation start() {
		return path != null ? path.get(0) : null;
	}
	
	public MapLocation end() {
		return path != null ? path.get( path.size() - 1 ) : null;
	}		
}
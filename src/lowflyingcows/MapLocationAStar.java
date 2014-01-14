package lowflyingcows;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.TerrainTile;

public abstract class MapLocationAStar extends AStar<MapLocation> {

	public MapLocationAStar(MapLocation start, MapLocation destination) {
		super(start, destination);
	}

	@Override
	protected float calcMovementCost(lowflyingcows.AStar.PathNode<MapLocation> current) 
	{
        float cost=0;
        if( current.parent != null ) 
        {
        	final float dist = (float) Math.sqrt( current.value.distanceSquaredTo( current.parent.value ) );
        	cost = current.parent.g() + dist;
        }
        return cost;
	}
	
	@Override
	protected float calcEstimatedCost( lowflyingcows.AStar.PathNode<MapLocation> node) 
	{
    	// WEIGHTED A-STAR !!!
    	float dist = (float) Math.sqrt( destination.distanceSquaredTo(  node.value ) );
		return 4 * dist;
	}
	
	public abstract TerrainTile senseTerrainTile(MapLocation loc);

	public abstract boolean isOccupied(MapLocation loc) throws GameActionException;
	
	@Override
	protected void scheduleNeighbors(lowflyingcows.AStar.PathNode<MapLocation> parent) throws GameActionException 
	{
		int x = parent.value.x;
		int y = parent.value.y;
		for ( int dx = -1 ; dx <= 1 ; dx++ ) 
		{
			for ( int dy = -1 ; dy <= 1 ; dy++ ) 
			{
				if ( dx != 0 || dy != 0 ) {
					final int newX = x+dx;
					final int newY = y+dy;
					
					final MapLocation newLocation = new MapLocation(newX,newY);
					final TerrainTile tile = senseTerrainTile( newLocation );
					switch(tile) {
						case NORMAL:
						case ROAD:
							if ( ! isOccupied( newLocation ) ) {
								maybeAddNeighbor( parent , newLocation );
							}
							break;
						default:
					}
				}
			}
		}
	}

}

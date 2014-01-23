package test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import battlecode.common.MapLocation;

public final class GameMap {

	private static final String WIDTH_KEY = "width";
	private static final String HEIGHT_KEY = "height";
	
	public static final String ROBOT_LOCATION_KEY = "robot";
	public static final String DESTINATION_LOCATION_KEY = "destination";
	
	public void save(File file) throws IOException 
	{
		Properties prop = new Properties();
		prop.put( WIDTH_KEY , ""+width );
		prop.put( HEIGHT_KEY , ""+height);
		
		putLocation( prop , ROBOT_LOCATION_KEY , robotLocation );
		putLocation( prop , DESTINATION_LOCATION_KEY , destination );
		
		for ( int x = 0 ; x < width ; x++ ) {
			for ( int y = 0 ; y < height ; y++ ) {
				if ( blockedTiles[x][y] ) {
					prop.put( x+","+y , "true" );
				}
			}
		}
		
		FileOutputStream out = new FileOutputStream( file );		
		try {
			prop.store( out , "Auto-generated, do not edit" );
		} finally {
			try { out.close(); } catch(IOException e) {}
		}
	}
	
	public static GameMap load(File f) throws IOException 
	{
		Properties p = new Properties();
		FileInputStream inputStream = new FileInputStream(f );
		try 
		{
			p.load( inputStream );
			return load( p );
		} finally {
			try { inputStream.close(); } catch(IOException e) {}
		}
	}
	
	private static GameMap load(Properties p) 
	{
		int w = Integer.parseInt( p.getProperty( WIDTH_KEY ) );
		int h = Integer.parseInt( p.getProperty( HEIGHT_KEY ) );
		
		GameMap result = new GameMap(w,h);
		result.destination = readLocation( p , DESTINATION_LOCATION_KEY );
		result.robotLocation = readLocation( p , ROBOT_LOCATION_KEY );
		
		for ( int x = 0 ; x < w ; x++ ) 
		{
			for ( int y = 0 ; y < h ; y++ ) {
				String value = p.getProperty( x+","+y );
				if ( value != null ) {
					result.blockedTiles[x][y]=true;
				}
			}
		}		
		return result;
	}

	public final int width;
	public final int height;
	
	private MapLocation robotLocation;
	private MapLocation destination;
	
	private final boolean[][] blockedTiles;
	
	public GameMap(int width, int height) 
	{
		this.width = width;
		this.height = height;
		blockedTiles = new boolean[width][];
		for ( int x = 0 ; x < width ; x++ ) {
			blockedTiles[x]=new boolean[height];
		}
	}

	public void blockTile(int x,int y) {
		blockedTiles[x][y]=true;
	}
	
	public void clearTile(int x,int y) {
		blockedTiles[x][y]=false;
	}	
	
	public boolean isBlocked(int x,int y) {
		return blockedTiles[x][y];
	}
	
	public boolean isDestination(int x,int y) {
		return destination != null && destination.x == x && destination.y == y;
	}	
	
	public boolean isRobotLocation(int x,int y) {
		return robotLocation != null && robotLocation.x == x && robotLocation.y == y;
	}		
	
	public void toggleTile(int x,int y) {
		blockedTiles[x][y]=!blockedTiles[x][y];
	}	
	
	public MapLocation getRobotLocation() {
		return robotLocation;
	}

	public boolean hasDestination() {
		return destination != null;
	}
	
	public boolean hasRobotLocation() {
		return robotLocation != null;
	}
	
	public void setRoute(MapLocation start,MapLocation destination) {
		this.robotLocation = start;
		this.destination = destination;
	}
	
	public void setRobotLocation(MapLocation robotLocation) {
		this.robotLocation = robotLocation;
	}

	public MapLocation getDestination() {
		return destination;
	}

	public void setDestination(MapLocation destination) {
		this.destination = destination;
	}
	
	private static void putLocation(Properties p,String key,MapLocation l) {
		if ( l != null ) {
			p.put( key , l.x+","+l.y );
		}
	}
	
	private static MapLocation readLocation(Properties p,String key) {
		String value = p.getProperty( key );
		if ( value == null || value.trim().length()==0 ) {
			return null;
		}
		final String[] split = value.split(",");
		return new MapLocation( Integer.parseInt( split[0] ) , Integer.parseInt( split[1] ) );
	}

	public boolean isDestination(MapLocation l) {
		return destination != null && destination.equals(l);
	}

	public boolean isRobotLocation(MapLocation l) {
		return robotLocation != null && robotLocation.equals(l);
	}
}

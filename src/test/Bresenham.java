package test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import battlecode.common.MapLocation;

public class Bresenham {

	private HashSet<MapLocation> set =new HashSet<MapLocation>();

	private MapLocation start;	
	private MapLocation destination;

	// bresenham state
	private int t;
	private int parallelDx;
	private int parallelDy;

	private int diagDx;
	private int diagDy;

	private int err;
	private int errorFast;
	private int errorSlow;	
	
	private int x;
	private int y;		
	
	public Bresenham() {
	}

	public boolean isOnLine(MapLocation l) {
		return set.contains( l );
	}
	
	public void setRoute(MapLocation start , MapLocation destination) 
	{
		set.clear();
		
		int dx = destination.x - start.x;
		int dy = destination.y - start.y;

		// determine sign of increment on each axis
		int incx = (dx > 0) ? 1 : (dx < 0) ? -1 : 0;
		int incy = (dy > 0) ? 1 : (dy < 0) ? -1 : 0;
		if (dx<0) dx = -dx;
		if (dy<0) dy = -dy;

		if (dx>dy)
		{
			/* x axis increment is largest */
			parallelDx=incx; 
			parallelDy=0;    
			diagDx=incx; 
			diagDy=incy; 
			errorFast =dy;   
			errorSlow =dx;  
		} 
		else
		{
			/* y axis increment is largest */
			parallelDx=0;    
			parallelDy=incy; 
			diagDx=incx; 
			diagDy=incy; 
			errorFast =dx;   
			errorSlow =dy;
		}

		x = start.x;
		y = start.y;
		err = errorSlow/2;			
		
		t = 0;
	}

	public List<MapLocation> line()
	{
		final ArrayList<MapLocation> list = new ArrayList<MapLocation>();
		
		MapLocation l = new MapLocation(x,y) ; 
		
		set.clear();
		set.add( l );
		list.add(l);
		
		for( t = 0 ; t < errorSlow; ++t)
		{
			err -= errorFast; // update error term
			if ( err < 0 )
			{
				/* step alongside slow axis / diagonal step*/
				x += diagDx;
				y += diagDy;

				err += errorSlow; // adjust error term					
			} 
			else
			{
				/* step alongside fast axis / parallel step*/
				x += parallelDx;
				y += parallelDy;
			}

			l = new MapLocation(x,y) ; 
			set.add( l );
			list.add( l );
		}
		return list;
	}
	
	public MapLocation onePoint() 
	{
		while( t < errorSlow )
		{
			err -= errorFast; // update error term
			if ( err < 0 )
			{
				/* step alongside slow axis / diagonal step*/
				x += diagDx;
				y += diagDy;

				err += errorSlow; // adjust error term					
			} 
			else
			{
				/* step alongside fast axis / parallel step*/
				x += parallelDx;
				y += parallelDy;
			}
			t++;
			return new MapLocation(x,y) ; 
		}
		return null;
	}
}

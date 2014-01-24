package test;

import java.util.ArrayList;
import java.util.List;

import battlecode.common.MapLocation;

public final class Bresenham {

	public static List<MapLocation> line(MapLocation start , MapLocation destination) 
	{
		final ArrayList<MapLocation> list = new ArrayList<MapLocation>();	
		
		int dx = destination.x - start.x;
		int dy = destination.y - start.y;

		// determine sign of increment on each axis
		int incx = (dx > 0) ? 1 : (dx < 0) ? -1 : 0;
		int incy = (dy > 0) ? 1 : (dy < 0) ? -1 : 0;
		if (dx<0) dx = -dx;
		if (dy<0) dy = -dy;

		int parallelDx;
		int parallelDy;
		int diagDx;
		int diagDy;
		int err;
		int errorFast;
		int errorSlow;	
				
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

		int x = start.x;
		int y = start.y;
		err = errorSlow/2;			
		
		list.add(new MapLocation(x,y) );
		
		for( int t = 0 ; t < errorSlow; ++t)
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
			list.add( new MapLocation(x,y) );
		}
		return list;
	}	
}

package test;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import battlecode.common.*;

public class BugFindingTest  
{

	public static final MockRobotController rc = new MockRobotController();
	
	public static final int MAP_WIDTH = 10;
	public static final int MAP_HEIGHT = 10;

	public static void main(String[] args) throws InterruptedException, InvocationTargetException {

		SwingUtilities.invokeAndWait( new Runnable() {

			@Override
			public void run() {
				new BugFindingTest().run();
			}} );
	}
	
	private void run() 
	{

		final JFrame frame = new JFrame();
		frame.setLayout( new BorderLayout() );

		final MyCanvas c = new MyCanvas();

		c.setPreferredSize( new Dimension(400,200 ) );

		frame.getContentPane().add(c,BorderLayout.CENTER);
		frame.pack();
		frame.setVisible( true );
		
		Bresenham.rc = rc;
	}	

	protected final class MyCanvas extends JPanel 
	{
		private float xScale;
		private float yScale;

		private final boolean[][] blockedTiles;

		private MapLocation start;
		private MapLocation destination;

		private final MouseAdapter mouse = new MouseAdapter() 
		{
			public void mouseClicked(java.awt.event.MouseEvent e) {

				if ( e.getButton() == MouseEvent.BUTTON1 ) 
				{
					MapLocation l = viewToModel( e.getPoint() );
					System.out.println("Clicked: "+l);
					blockedTiles[l.x][l.y]=!blockedTiles[l.x][l.y];	
					MyCanvas.this.repaint();
				} 
				else if ( e.getButton() == MouseEvent.BUTTON3  ) 
				{
					MapLocation l = viewToModel( e.getPoint() );
					if ( start ==  null || (start != null && destination != null ) ) {
						System.out.println("Set start "+l);
						start = l;
						destination = null;
						MyCanvas.this.repaint();
					} 
					else if ( start != null ) 
					{
						if ( ! l.equals(start) ) {
							System.out.println("Set destination "+l);
							destination = l;
							MyCanvas.this.repaint();
						} else {
							System.out.println("Start and destination must not be equal");
						}
					} 
				}
			}
		};

		public MyCanvas() {
			addMouseListener( mouse );
			blockedTiles = new boolean[ MAP_WIDTH ][];
			for ( int x = 0 ; x < MAP_WIDTH ; x++ ) 
			{
				blockedTiles[x] = new boolean[ MAP_HEIGHT ];
			}
		}

		private MapLocation viewToModel(Point p) 
		{
			int x = (int) Math.floor( p.x / xScale );
			int y = (int) Math.floor( p.y / yScale );
			return new MapLocation(x,y);
		}

		@Override
		protected void paintComponent(Graphics g) 
		{
			setBackground( Color.WHITE );
			super.paintComponent(g);

			g.setColor( Color.BLACK );

			int w = getWidth();
			int h = getHeight();

			xScale = w / (float) MAP_WIDTH;
			yScale = h / (float) MAP_HEIGHT;

			// draw grid
			for ( float x = 0 ; x < w ; x+= xScale ) 
			{
				int px = (int) Math.floor(x);
				g.drawLine( px , 0 , px , h ); 
			}
			for ( float y = 0 ; y < h ; y+= yScale ) {
				int py = (int) Math.floor(y);
				g.drawLine( 0 , py , w , py );
			}			

			Bresenham bresenham=null;
			if ( start != null && destination != null ) 
			{
				
				bresenham = new Bresenham(destination) 
				{
					
					@Override
					protected boolean canMove(Direction d) {
						// TODO Auto-generated method stub
						return false;
					}

					@Override
					protected void move(Direction d) {
						// TODO Auto-generated method stub
						
					}

					@Override
					protected boolean isBlocked(MapLocation l) {
						// TODO Auto-generated method stub
						return false;
					}
					
				};
			}

			for (int x = 0 ; x < MAP_WIDTH ; x++ ) 
			{
				for (int y = 0 ; y < MAP_HEIGHT ; y++ ) 
				{
					MapLocation l = new MapLocation(x,y );
					if ( l.equals( start ) ) {
						fillCell(l,Color.red , "S" , g );
						continue;
					} 
					else if ( l.equals( destination ) ) {
						fillCell(l,Color.red , "D" , g );			
						continue;
					} 

					if ( bresenham != null && bresenham.isOnLine( l ) ) {
						fillCell( l , Color.GREEN  , "L" , g );
					} 
					else if ( blockedTiles[x][y] ) 
					{
						fillCell( l , Color.GREEN  , g );
					}
				}
			}
		}

		private void fillCell(MapLocation l,Color color, Graphics g) {
			fillCell(l,color,null,g);
		}

		private void fillCell(MapLocation l,Color color, String message,Graphics g) 
		{
			Point p1 = modelToView( l );
			Point p2 = modelToView( new MapLocation(l.x+1,l.y+1 ) );
			g.setColor(color);
			int cellWidth = p2.x - p1.x-1;
			int cellHeight = p2.y - p1.y-1;
			g.fillRect( p1.x+1 , p1.y+1 , cellWidth , cellHeight );

			if ( message != null ) 
			{
				g.setColor(Color.BLACK);
				Rectangle2D bounds = g.getFontMetrics().getStringBounds( message , g );
				int px = (int) Math.round( p1.x + cellWidth/2 - bounds.getWidth()/2.0d );
				int py = (int) Math.round( p1.y + cellHeight/2 + bounds.getHeight()/2.0 );
				g.drawString( message , px , py );
			}
		}		


		protected Point modelToView(MapLocation l) {

			int px = (int) Math.floor(l.x*xScale);
			int py = (int) Math.floor(l.y*yScale);
			return new Point(  px , py );
		}
	}

	protected static abstract class Bresenham {

		private HashSet<MapLocation> set =new HashSet<MapLocation>();

		private static final int SENSOR_RANGE = (int) Math.sqrt(RobotType.SOLDIER.sensorRadiusSquared);
		
		private static final int INFINITY = Integer.MAX_VALUE;
		
		protected static RobotController rc;
		
		private final MapLocation destination;

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
		
		private boolean followingBoundary;
		
		private int distanceSqrtToGoalWhenObstacleWasEncountered=0;
		
		public static enum State {
			CONTINUE,
			DESTINATION_REACHED,
			SURROUNDED,
			NO_PATH;
		}

		public Bresenham(MapLocation destination) {
			this.destination=destination;
			init();
		}

		public State move(MapLocation currentLocation) {
			return null;
		}
		
		protected abstract boolean canMove(Direction d);
		
		protected abstract void move(Direction d);
		
		protected abstract boolean isBlocked(MapLocation l);

		public boolean isOnLine(MapLocation l) {
			return set.contains( l );
		}
		
		public State move() 
		{
			MapLocation current = rc.getLocation();
			if ( current.equals(destination) ) {
				return State.DESTINATION_REACHED;
			}
			
			if ( ! rc.isActive() ) {
				return State.CONTINUE;
			}			
			
			if ( followingBoundary ) {
				
			}
			
			MapLocation next = onePoint();
			Direction d = current.directionTo( next );
			if ( canMove(d) ) 
			{
				followingBoundary = false;
				move( d );
				return State.CONTINUE;
			}
			
			// reached obstacle,walk along boundary counter-clockwise
			followingBoundary = true;
			distanceSqrtToGoalWhenObstacleWasEncountered = current.distanceSquaredTo( destination ); 

			for ( int i = 7 ; i > 0 ; i-- ) 
			{
				d = d.rotateLeft();
				if ( canMove(d) )
				{
					move(d);
					return State.CONTINUE;
				}
			}
			// hmmm...surrounded by obstacles ?
			return State.SURROUNDED; 
		}
		
		public void init() 
		{
			MapLocation start = rc.getLocation();
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

		private void line()
		{
			set.clear();
			
			MapLocation l = new MapLocation(x,y) ; 
			set.add( l );

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
			}
		}
		
		public int getDistanceToClosestObstacle(int lookAhead) 
		{
			int count = 0;
			final int oldX = x;
			final int oldY = y;
			final int oldErr = err;
			final int oldT = t;
			try 
			{
				for( ; t < errorSlow && count < lookAhead ; ++t,count++)
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
					if ( isBlocked( new MapLocation(x,y) ) ) 
					{
						return (x-oldX)*(x-oldX) + (y-oldY)*(y-oldY);
					}
				}
				return INFINITY;
			} 
			finally 
			{
				x=oldX;
				y=oldY;
				err=oldErr;
				t=oldT;
			}
		}		
		
		public MapLocation onePoint() 
		{
			for( ; t < errorSlow; ++t)
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
				return new MapLocation(x,y) ; 
			}
			return null;
		}
	}
}
package test;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import team223.State;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

public class BugFindingTest  
{
	private static final File MAP_FILE = new File("/tmp/map.properties");
	
	private final MockRobotController rc = new MockRobotController();

	private MyCanvas canvas;
	private volatile State state;
	
	private volatile MapLocation start;
	
	private final SimulationEngine engine = new SimulationEngine() {

		@Override
		protected void tick() {
			BugFindingTest.this.tick();
		}
	};
	
	public static void main(String[] args) throws InterruptedException, InvocationTargetException {

		SwingUtilities.invokeAndWait( new Runnable() {

			@Override
			public void run() {
				try {
					new BugFindingTest().run();
				} 
				catch (IOException e) {
					e.printStackTrace();
				}
			}} );
	}
	
	private void run() throws IOException 
	{

		final JFrame frame = new JFrame();
		frame.setLayout( new BorderLayout() );
		
		GameMap map = loadMap();
		rc.setMap( map );
		this.start = map.getRobotLocation();
		engine.pause();
		resetState();
		canvas = new MyCanvas();

		canvas.setPreferredSize( new Dimension(800,500 ) );

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout( new FlowLayout() );
		
		final JButton resetButton = new JButton("Reset");
		final JButton saveButton = new JButton("Save");
		
		final JButton clearButton = new JButton("Clear");		
		
		final JButton startButton = new JButton("Start");
		final JButton stopButton = new JButton("Stop");		
		
		final ActionListener l = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) 
			{
				if ( e.getSource() == clearButton ) {
					clearSimulation();
				} else if ( e.getSource() == resetButton ) {
					resetSimulation();
				} else if ( e.getSource() == saveButton ) {
					saveMap();
					System.out.println("Map saved to "+MAP_FILE.getAbsolutePath());
				} else if ( e.getSource() == startButton ) 
				{
					continueSimulation();
				} else if ( e.getSource() == stopButton ) 
				{
					pauseSimulation();
				}
			}};
		
		resetButton.addActionListener( l );
		saveButton.addActionListener(l);
		
		clearButton.addActionListener(l);
		
		startButton.addActionListener( l );
		stopButton.addActionListener(l);		
		
		buttonPanel.add( resetButton );
		buttonPanel.add( saveButton );
		buttonPanel.add( clearButton );		
		
		buttonPanel.add( startButton );
		buttonPanel.add( stopButton );		
		
		frame.getContentPane().add(buttonPanel,BorderLayout.NORTH);
		frame.getContentPane().add(canvas,BorderLayout.CENTER);
		frame.pack();
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.setVisible( true );
	}
	
	public void tick() 
	{

		try {
			if ( state != null ) {
				System.out.println("-- tick --");
				state = state.perform();
			} else {
				System.out.println("-- tick (no state) --");				
			}
		} 
		catch (GameActionException e) {
			e.printStackTrace();
			System.out.println("=== Something bad happened, engine stopped");
			engine.pause();
		}
		rc.tick();
		canvas.repaint();
	}
	
	private void clearSimulation() 
	{
		engine.pause();		
		
		GameMap map = loadMap();
		map = new GameMap( map.width , map.height );
		rc.setMap( map );
		this.start = map.getRobotLocation();
		resetState();
		canvas.repaint();
	}	
	
	private void resetSimulation() 
	{
		engine.pause();		
		
		GameMap map = loadMap();
		rc.setMap( map );
		this.start = map.getRobotLocation();
		resetState();
		canvas.repaint();
	}
	
	private void setRobotLocation(MapLocation l) 
	{
		engine.pause();
		System.out.println("Setting new robot location "+l);
		this.start = l;
		rc.getMap().setRobotLocation( l );
		rc.getMap().setDestination( null );
		resetState();
	}
	
	private void resetState() {
		if ( rc.getMap().hasDestination() && rc.getMap().hasRobotLocation() ) {
			state = new BugGotoLocation(rc, rc.getMap().getDestination() );			
		} else {
			state = null;
		}
	}
	
	private void setDestination(MapLocation l) 
	{
		engine.pause();
		System.out.println("Setting new destination "+l);			
		rc.getMap().setDestination( l );
		resetState();
	}
	
	private void continueSimulation() 
	{
		System.out.println("Starting engine");
		engine.start();
	}
	
	private void pauseSimulation() {
		System.out.println("Pausing engine");
		engine.pause();
	}
	
	private GameMap loadMap() 
	{
		if ( MAP_FILE.exists() ) 
		{
			System.out.println("Trying to load map from "+MAP_FILE.getAbsolutePath());			
			try {
				return GameMap.load( MAP_FILE );
			} catch(IOException e) {
				e.printStackTrace();
			}
		} 
		System.out.println("Creating new map");
		return new GameMap(25,25);
	}
	
	public void saveMap() 
	{
		try {
			canvas.getMap().save( MAP_FILE );
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected final class MyCanvas extends JPanel 
	{
		private float xScale;
		private float yScale;

		private final MouseAdapter mouse = new MouseAdapter() 
		{
			private MapLocation lastDragged;
			
			public void mouseDragged(MouseEvent e) 
			{
				MapLocation l = viewToModel( e.getPoint() );
				if ( lastDragged == null || ! lastDragged.equals( l ) ) {
					getMap().toggleTile( l.x , l.y );
					lastDragged=l;
				}
				// getMap().blockTile( l.x , l.y );
				MyCanvas.this.repaint();
			}
			
			public void mouseClicked(java.awt.event.MouseEvent e) {

				if ( e.getButton() == MouseEvent.BUTTON1 ) 
				{
					MapLocation l = viewToModel( e.getPoint() );
					getMap().toggleTile( l.x , l.y );
					MyCanvas.this.repaint();
				} 
				else if ( e.getButton() == MouseEvent.BUTTON3  ) 
				{
					MapLocation l = viewToModel( e.getPoint() );
					if ( ! getMap().hasRobotLocation() || ( getMap().hasRobotLocation() && getMap().hasDestination() ) ) {
						setRobotLocation( l );
						MyCanvas.this.repaint();
					} 
					else if ( getMap().hasRobotLocation() ) 
					{
						if ( ! getMap().isRobotLocation( l ) ) {
							setDestination( l );
							MyCanvas.this.repaint();
						} else {
							System.out.println("robot location and destination must not be equal");
						}
					} 
				}
			}
		};

		public MyCanvas() 
		{
			addMouseListener( mouse );
			addMouseMotionListener( mouse );
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

			xScale = w / (float) getMap().width;
			yScale = h / (float) getMap().height;

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
			if ( BugFindingTest.this.start != null && getMap().hasDestination() )
			{
				bresenham = new Bresenham();
				bresenham.setRoute( BugFindingTest.this.start , getMap().getDestination() );
				bresenham.line();
			}

			for (int x = 0 ; x < getMap().width ; x++ ) 
			{
				for (int y = 0 ; y < getMap().height ; y++ ) 
				{
					MapLocation l = new MapLocation(x,y );
					if ( getMap().isRobotLocation( l ) ) {
						fillCell(l,Color.red , "R" , g );
						continue;
					} 
					else if ( getMap().isDestination(l) ) {
						fillCell(l,Color.red , "D" , g );			
						continue;
					} 

					if ( getMap().isBlocked( x , y ) ) 
					{
						fillCell( l , Color.BLACK , g );
					} 
					else if ( bresenham != null && bresenham.isOnLine( l ) ) {
						fillCell( l , Color.GREEN  , "L" , g );
					} 
				}
			}
		}

		private void fillCell(MapLocation l,Color color, Graphics g) 
		{
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

		private GameMap getMap() {
			return rc.getMap();
		}
	}
}
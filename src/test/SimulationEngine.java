package test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public abstract class SimulationEngine {

	private static final int STEP_MILLIS = 100;

	private static final AtomicReference<Clock> engineThread=new AtomicReference<Clock>();

	public SimulationEngine() 
	{
		Clock t = new Clock();
		if ( engineThread.compareAndSet( null , t ) ) {
			t.start();
		}
	}

	protected class Clock extends Thread 
	{
		private volatile boolean terminate;
		private boolean run;
		private volatile boolean clockRunning;

		private final Object LOCK = new Object();

		private volatile CountDownLatch ackLatch = new CountDownLatch(1);

		public Clock() {
			super("clock-thread");
			setDaemon(true);
		}

		@Override
		public void run() 
		{
			try {
				while ( ! terminate ) 
				{
					boolean ack = false;
					synchronized(LOCK) 
					{
						if ( ! run ) 
						{
							while ( ! run ) 
							{
								try {
									clockRunning = false;									
									LOCK.wait();
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
							ack = true;
						}
						clockRunning = true;
					}

					if ( ack ) {
						ackLatch.countDown();
					}

					try {
						tick();
					} catch(Exception e) {
						e.printStackTrace();
					}
					try {
						java.lang.Thread.sleep( STEP_MILLIS );
					} catch(Exception e) {

					}
				} 
			} finally {
				clockRunning=false;
				ackLatch.countDown();
			}
		}		
		
		public boolean isClockRunning() {
			return clockRunning;
		}

		public void startClock() 
		{
			boolean waitForAck = false;
			synchronized(LOCK) 
			{
				if ( ! run ) {
					waitForAck = true;
					ackLatch = new CountDownLatch(1);
					run = true;
					LOCK.notifyAll();
				}
			}
			if ( waitForAck ) 
			{
				try {
					ackLatch.await();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		public void pauseClock() 
		{
			boolean waitForAck = false;
			synchronized(LOCK) 
			{
				if ( run ) {
					run = false;
					waitForAck = true;
					LOCK.notifyAll();
				}
			}
			if ( waitForAck ) {
				try {
					ackLatch.await();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}			
		}		
	}
	
	public boolean isRunning() {
		return engineThread.get().isClockRunning();
	}

	public void start() 
	{
		engineThread.get().startClock();
	}

	public void pause() 
	{
		engineThread.get().pauseClock();
	}	

	protected abstract void tick();
}
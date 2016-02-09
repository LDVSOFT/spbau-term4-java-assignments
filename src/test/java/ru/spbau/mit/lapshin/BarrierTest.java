package ru.spbau.mit.lapshin;

import org.junit.Test;

import static org.junit.Assert.*;

public class BarrierTest
{
	private class Horse implements Runnable {
		private boolean completed = false;
		private Barrier barrier;

		private Horse(Barrier barrier)
		{
			this.barrier = barrier;
		}

		@Override
		public void run()
		{
			barrier.await();
			synchronized (this)
			{
				completed = true;
			}
		}

		public boolean isCompleted()
		{
			return completed;
		}
	}

	@Test
	public void testSimple() {
		Barrier barrier = new Barrier(2);
		Horse horse1 = new Horse(barrier);
		Horse horse2 = new Horse(barrier);
		new Thread(horse1).start();
		try
		{
			Thread.sleep(100);
		}
		catch (InterruptedException ignored)
		{
		}
		assertFalse(horse1.isCompleted());
		assertFalse(horse2.isCompleted());
		new Thread(horse2).start();
		try
		{
			Thread.sleep(100);
		}
		catch (InterruptedException ignored)
		{
		}
		assertTrue(horse1.isCompleted());
		assertTrue(horse2.isCompleted());
	}
}
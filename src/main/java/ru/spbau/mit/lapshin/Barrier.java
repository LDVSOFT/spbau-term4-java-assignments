package ru.spbau.mit.lapshin;

/**
 * Created by LDVSOFT on 09.02.2016.
 */
public class Barrier
{
	private int parties;
	private int waiting = 0;

	public Barrier(int parties)
	{
		this.parties = parties;
	}

	public synchronized void await()
	{
		waiting++;
		if (waiting == parties) {
			notifyAll();
		}
		else {
			while (waiting < parties) {
				try
				{
					this.wait();
				}
				catch (InterruptedException ignored)
				{
				}
			}
		}
	}
}

package org.ggp.base.player.gamer.statemachine.sample;

public class Parameters
{
	// amount of time to buffer before the timeout
	public static final long TIME_BUFFER = 2000;
	public static final int NUM_CORES = 2;
	public static final int EXPLORATION_FACTOR = 40;
	public static final int DEPTH_CHARGE_COUNT = 4;

	public static final boolean MINIMAX = true;

	// performance - use for competition
	//private static final int THREAD_PRIORITY_BUMP = 2;
	// more casual - to give your computer some breathing room
	private static final int THREAD_PRIORITY_BUMP = 1;

	public static final int WORKER_THREAD_PRIORITY = Thread.NORM_PRIORITY + THREAD_PRIORITY_BUMP;
	public static final int MAIN_THREAD_PRIORITY = Thread.MAX_PRIORITY;

	public static final String DEFAULT_PLAYER = "MonteCarloTreeSearchGamer";
}

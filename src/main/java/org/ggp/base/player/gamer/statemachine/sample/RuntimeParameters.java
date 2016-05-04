package org.ggp.base.player.gamer.statemachine.sample;

public class RuntimeParameters
{
	// amount of time to buffer before the timeout
	public static final long TIME_BUFFER = 2000;
	public static final int EXPLORATION_FACTOR = 40;
	public static final int DEPTH_CHARGE_COUNT = 4;

	public static final boolean MINIMAX = true;

	public static final String DEFAULT_PLAYER = "MonteCarloTreeSearchGamer";
}

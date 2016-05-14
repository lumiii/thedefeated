package org.ggp.base.player.gamer.statemachine.sample;

public class RuntimeParameters
{
	public static final int EXPLORATION_FACTOR = 50;
	public static final int DEPTH_CHARGE_COUNT = 4;

	public static final boolean MINIMAX = true;
	public static final boolean DIFFERENTIAL_PROPAGATION = true;

	public static final String DEFAULT_PLAYER = "MonteCarloTreeSearchGamer";

	public static final boolean LOG_MAIN_THREAD = true;
	public static final boolean LOG_THREAD = true;
	public static final boolean LOG_NODE_STATS = true;
	public static final boolean LOG_TREE_SEARCH = false;
	public static final boolean LOG_MOVE_EVALUATION = true;
	public static final boolean LOG_ERRORS = true;
	public static final boolean LOG_PROPNET = true;

	public static final boolean DISABLE_MAIN_THREAD = false;
	public static final boolean OUTPUT_GRAPH_FILE = true;
	// this value must be true if output_graph_file is true, otherwise
	// render will fail
	public static final boolean GRAPH_TOSTRING = false || OUTPUT_GRAPH_FILE;
}
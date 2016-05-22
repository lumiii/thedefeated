package org.ggp.base.player.gamer.statemachine.thedefeated;

import java.util.Date;

public class MachineParameters
{
	public static final int NUM_CORES = 1;
	public static final int META_TIME_PAD = 1000;
	public static final int MAX_NODES = 7000;
	public static final int LOW_NODE_THRESHOLD = 1000;

	public static final int MAIN_THREAD_PRIORITY = Thread.MAX_PRIORITY;
	public static final int WORKER_THREAD_PRIORITY = MAIN_THREAD_PRIORITY - 1;

	// amount of time to buffer before the timeout
	public static final long TIME_BUFFER = 2000;

	private static final String OUTPUT_FILE = "C:\\Users\\Alexander\\Documents\\CS 227B Stuff\\graph";
	private static final String OUTPUT_FILE_EXTENSION = ".dot";

	public static String outputFilename()
	{
		Date date = new Date();
		return OUTPUT_FILE + "-" + date.getTime() + OUTPUT_FILE_EXTENSION;
	}
}

package org.ggp.base.player.gamer.statemachine.thedefeated;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.MarkerFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.layout.PatternLayout.Builder;
import org.apache.logging.log4j.status.StatusLogger;

public class GLog
{
	private static LoggerContext loggerContext = null;

	public static final String S_MAIN_THREAD_ACTIVITY = "MainThreadActivity";
	public static final String S_NODE_STATS = "NodeStats";
	public static final String S_TREE_SEARCH = "TreeSearch";
	public static final String S_THREAD_ACTIVITY = "ThreadActivity";
	public static final String S_MOVE_EVALUATION = "MoveEvaluation";
	public static final String S_ERRORS = "Error";
	public static final String S_PROPNET = "PropNet";
	public static final String S_UNITTEST = "UnitTest";
	public static final String S_FACTOR = "Factor";
	public static final String S_MEMORY = "Memory";

	public static final Marker MAIN_THREAD_ACTIVITY = MarkerManager.getMarker(S_MAIN_THREAD_ACTIVITY);
	public static final Marker NODE_STATS = MarkerManager.getMarker(S_NODE_STATS);
	public static final Marker TREE_SEARCH = MarkerManager.getMarker(S_TREE_SEARCH);
	public static final Marker THREAD_ACTIVITY = MarkerManager.getMarker(S_THREAD_ACTIVITY);
	public static final Marker MOVE_EVALUATION = MarkerManager.getMarker(S_MOVE_EVALUATION);
	public static final Marker ERRORS = MarkerManager.getMarker(S_ERRORS);
	public static final Marker PROPNET = MarkerManager.getMarker(S_PROPNET);
	public static final Marker UNITTEST = MarkerManager.getMarker(S_UNITTEST);
	public static final Marker FACTOR = MarkerManager.getMarker(S_FACTOR);
	public static final Marker MEMORY = MarkerManager.getMarker(S_MEMORY);

	public static final String BANNER = "==========";

	private static final MarkerFilter F_MAIN_THREAD_ACTIVITY = MarkerFilter.createFilter(S_MAIN_THREAD_ACTIVITY,
			Filter.Result.DENY, Filter.Result.NEUTRAL);
	private static final MarkerFilter F_NODE_STATS = MarkerFilter.createFilter(S_NODE_STATS, Filter.Result.DENY,
			Filter.Result.NEUTRAL);
	private static final MarkerFilter F_TREE_SEARCH = MarkerFilter.createFilter(S_TREE_SEARCH, Filter.Result.DENY,
			Filter.Result.NEUTRAL);
	private static final MarkerFilter F_THREAD_ACTIVITY = MarkerFilter.createFilter(S_THREAD_ACTIVITY,
			Filter.Result.DENY, Filter.Result.DENY);
	private static final MarkerFilter F_MOVE_EVALUATION = MarkerFilter.createFilter(S_MOVE_EVALUATION,
			Filter.Result.DENY, Filter.Result.NEUTRAL);
	private static final MarkerFilter F_ERRORS = MarkerFilter.createFilter(S_ERRORS, Filter.Result.DENY,
			Filter.Result.NEUTRAL);
	private static final MarkerFilter F_PROPNET = MarkerFilter.createFilter(S_PROPNET, Filter.Result.DENY,
			Filter.Result.NEUTRAL);
	private static final MarkerFilter F_UNITTEST = MarkerFilter.createFilter(S_UNITTEST, Filter.Result.DENY,
			Filter.Result.NEUTRAL);
	private static final MarkerFilter F_FACTOR = MarkerFilter.createFilter(S_FACTOR, Filter.Result.DENY,
			Filter.Result.NEUTRAL);
	private static final MarkerFilter F_MEMORY = MarkerFilter.createFilter(S_MEMORY, Filter.Result.DENY,
			Filter.Result.NEUTRAL);


	static
	{
		initLogger();
	}

	private static void initLogger()
	{
		StatusLogger.getLogger().setLevel(Level.OFF);

		loggerContext = (LoggerContext) LogManager.getContext(false);

		Configuration config = loggerContext.getConfiguration();
		LoggerConfig loggerConfig = config.getRootLogger();

		// adjust this if wanting to separate logs by severity level
		loggerConfig.setLevel(Level.ALL);

		for (String key : loggerConfig.getAppenders().keySet())
		{
			loggerConfig.removeAppender(key);
		}

		Builder builder = PatternLayout.newBuilder();
		builder.withConfiguration(config);
		builder.withPattern("[%t]: %m%n");

		PatternLayout layout = builder.build();
		ConsoleAppender appender = ConsoleAppender.createDefaultAppenderForLayout(layout);
		loggerConfig.addAppender(appender, loggerConfig.getLevel(), null);

		filterLogs(loggerConfig);

		loggerContext.updateLoggers();
	}

	private static void filterLogs(LoggerConfig loggerConfig)
	{
		if (!RuntimeParameters.LOG_MAIN_THREAD)
		{
			loggerConfig.addFilter(F_MAIN_THREAD_ACTIVITY);
		}

		if (!RuntimeParameters.LOG_NODE_STATS)
		{
			loggerConfig.addFilter(F_NODE_STATS);
		}

		if (!RuntimeParameters.LOG_TREE_SEARCH)
		{
			loggerConfig.addFilter(F_TREE_SEARCH);
		}

		if (!RuntimeParameters.LOG_THREAD)
		{
			loggerConfig.addFilter(F_THREAD_ACTIVITY);
		}

		if (!RuntimeParameters.LOG_MOVE_EVALUATION)
		{
			loggerConfig.addFilter(F_MOVE_EVALUATION);
		}

		if (!RuntimeParameters.LOG_ERRORS)
		{
			loggerConfig.addFilter(F_ERRORS);
		}

		if (!RuntimeParameters.LOG_PROPNET)
		{
			loggerConfig.addFilter(F_PROPNET);
		}

		if (!RuntimeParameters.LOG_UNITTEST)
		{
			loggerConfig.addFilter(F_UNITTEST);
		}

		if (!RuntimeParameters.LOG_FACTOR)
		{
			loggerConfig.addFilter(F_FACTOR);
		}

		if (!RuntimeParameters.LOG_MEMORY)
		{
			loggerConfig.addFilter(F_MEMORY);
		}
	}

	public static Logger getLogger(Class<?> c)
	{
		return loggerContext.getLogger(c.getSimpleName());
	}

	// use this for one-off situations where you don't want to create
	// a whole logger for a class
	public static Logger getRootLogger()
	{
		return loggerContext.getRootLogger();
	}
}

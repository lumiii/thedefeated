package org.ggp.base.player.gamer.statemachine.sample;

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

	public static final Marker MAIN_THREAD_ACTIVITY = MarkerManager.getMarker(S_MAIN_THREAD_ACTIVITY);
	public static final Marker NODE_STATS = MarkerManager.getMarker(S_NODE_STATS);
	public static final Marker TREE_SEARCH = MarkerManager.getMarker(S_TREE_SEARCH);
	public static final Marker THREAD_ACTIVITY = MarkerManager.getMarker(S_THREAD_ACTIVITY);
	public static final Marker MOVE_EVALUATION = MarkerManager.getMarker(S_MOVE_EVALUATION);
	public static final Marker ERRORS = MarkerManager.getMarker(S_ERRORS);

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

	static
	{
		initLogger();
	}

	public static void initLogger()
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

		// uncomment any of these to filter out that category of logs
		// loggerConfig.addFilter(F_MAIN_THREAD_ACTIVITY);
		// loggerConfig.addFilter(F_NODE_STATS);
		// loggerConfig.addFilter(F_TREE_SEARCH);
		// loggerConfig.addFilter(F_THREAD_ACTIVITY);
		// loggerConfig.addFilter(F_MOVE_EVALUATION);
		// loggerConfig.addFilter(F_ERRORS);

		loggerContext.updateLoggers();
	}

	public static Logger getLogger(Class<?> c)
	{
		Logger log = loggerContext.getLogger(c.getSimpleName());

		return log;
	}
}

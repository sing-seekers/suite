package org.util;

import java.io.PrintWriter;

import org.apache.commons.logging.LogFactory;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import sun.reflect.Reflection;

public class LogUtil {

	private static boolean inited = false;

	public static void initLog4j() {
		if (!inited)
			initLog4j(Level.INFO);
	}

	public static void initLog4j(Level level) {
		String caller = Reflection.getCallerClass(2).getSimpleName();

		PatternLayout layout = new PatternLayout("%d %-5p [%c{1}] %m%n");

		ConsoleAppender console = new ConsoleAppender(layout);
		console.setWriter(new PrintWriter(System.out));
		console.activateOptions();

		DailyRollingFileAppender file = new DailyRollingFileAppender();
		file.setFile("logs/" + caller + ".log");
		file.setDatePattern("'.'yyyyMMdd");
		file.setLayout(layout);
		file.activateOptions();

		Logger logger = Logger.getRootLogger();
		logger.setLevel(level);
		logger.removeAllAppenders();
		logger.addAppender(console);
		logger.addAppender(file);

		inited = true;
	}

	public static void info(String cat, String message) {
		initLog4j();
		LogFactory.getLog(cat).info(message);
	}

	public static void error(String cat, Throwable th) {
		initLog4j();
		LogFactory.getLog(cat).error("", th);
	}

}

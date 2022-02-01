package org.lucee.extension.axis.log;

import org.apache.commons.logging.Log;

import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.config.Config;

public class LogImpl implements Log {

	private static final String NAME = "axis";
	private lucee.commons.io.log.Log log;

	public LogImpl(lucee.commons.io.log.Log log) {
		if (log == null) {
			getLog();
		}
		else this.log = log;

		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean isDebugEnabled() {
		return is(lucee.commons.io.log.Log.LEVEL_DEBUG);
	}

	@Override
	public boolean isErrorEnabled() {
		return is(lucee.commons.io.log.Log.LEVEL_ERROR);
	}

	@Override
	public boolean isFatalEnabled() {
		return is(lucee.commons.io.log.Log.LEVEL_FATAL);
	}

	@Override
	public boolean isInfoEnabled() {
		return is(lucee.commons.io.log.Log.LEVEL_INFO);
	}

	@Override
	public boolean isTraceEnabled() {
		return is(lucee.commons.io.log.Log.LEVEL_TRACE);
	}

	@Override
	public boolean isWarnEnabled() {
		return is(lucee.commons.io.log.Log.LEVEL_WARN);
	}

	private boolean is(int level) {
		getLog();
		return log != null ? log.getLogLevel() >= level : false;
	}

	@Override
	public void debug(Object msg) {
		log(lucee.commons.io.log.Log.LEVEL_DEBUG, msg, null);
	}

	@Override
	public void debug(Object msg, Throwable t) {
		log(lucee.commons.io.log.Log.LEVEL_DEBUG, msg, t);
	}

	@Override
	public void error(Object msg) {
		log(lucee.commons.io.log.Log.LEVEL_ERROR, msg, null);
	}

	@Override
	public void error(Object msg, Throwable t) {
		log(lucee.commons.io.log.Log.LEVEL_ERROR, msg, t);
	}

	@Override
	public void fatal(Object msg) {
		log(lucee.commons.io.log.Log.LEVEL_FATAL, msg, null);
	}

	@Override
	public void fatal(Object msg, Throwable t) {
		log(lucee.commons.io.log.Log.LEVEL_FATAL, msg, t);
	}

	@Override
	public void info(Object msg) {
		log(lucee.commons.io.log.Log.LEVEL_INFO, msg, null);
	}

	@Override
	public void info(Object msg, Throwable t) {
		log(lucee.commons.io.log.Log.LEVEL_INFO, msg, t);
	}

	@Override
	public void trace(Object msg) {
		log(lucee.commons.io.log.Log.LEVEL_TRACE, msg, null);
	}

	@Override
	public void trace(Object msg, Throwable t) {
		log(lucee.commons.io.log.Log.LEVEL_TRACE, msg, t);
	}

	@Override
	public void warn(Object msg) {
		log(lucee.commons.io.log.Log.LEVEL_WARN, msg, null);
	}

	@Override
	public void warn(Object msg, Throwable t) {
		log(lucee.commons.io.log.Log.LEVEL_WARN, msg, t);
	}

	private void log(int level, Object msg, Throwable t) {
		if (getLog() == null) {
			System.err.println(msg);
			if (t != null) t.printStackTrace();
			return;
		}
		if (t != null) {
			if (msg != null) log.log(level, NAME, msg.toString(), t);
			else log.log(level, NAME, t);
		}
		else {
			log.log(level, NAME, ("" + msg).toString(), t);
		}
	}

	private lucee.commons.io.log.Log getLog() {
		if (log == null) {
			CFMLEngine eng = CFMLEngineFactory.getInstance();
			Config c = eng.getThreadConfig();
			if (c != null) log = c.getLog("application");
		}
		return log;
	}

}

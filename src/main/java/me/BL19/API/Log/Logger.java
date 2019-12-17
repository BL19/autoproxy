package me.BL19.API.Log;

import java.util.ArrayList;
import java.util.HashMap;

import me.BL19.API.Log.modules.Module;

public class Logger {

	private String name;

	private ArrayList<Module> modules = new ArrayList<Module>();

	private static ArrayList<Module> masterModules = new ArrayList<Module>();

	public boolean DISABLEMASTER = false;

	public ArrayList<Module> getModules() {
		ArrayList<Module> res = (ArrayList<Module>) this.modules.clone();
		if(DISABLEMASTER) return res;
		for (Module module : masterModules) {
			res.add(module);
		}
		return res;
	}
	
	
	public Logger() {
		this(Thread.currentThread().getStackTrace()[1].getClass());
	}
	
	/**
	 * Create a new logger
	 * 
	 * @param name The name of the logger
	 */
	public Logger(String name) {
		this.name = name;
	}

	public Logger(Class clas) {
		this.name = clas.getSimpleName();
	}

	/**
	 * Create a new logger
	 * 
	 * @param name The name for the logger
	 * @return A new logger instance with the specified name
	 */
	public static Logger getLogger(String name) {
		return new Logger(name);
	}

	/**
	 * Create a new logger
	 * 
	 * @param clas The class BaseEntry to be the name
	 * @return A new logger instance with the name of the class
	 */
	public static Logger getLogger(Class clas) {
		return new Logger(clas);
	}

	/**
	 * Enables the specified module to log with for this instance
	 * 
	 * @param mod The {@link Module}
	 * @see Module
	 */
	public Logger enableModule(Module mod) {
		if (isModuleEnabled(mod.getModuleName()))
			return this;
		if (modules.size() != 0 || (DISABLEMASTER ? false : masterModules.size() != 0)) {
			core("Enabeling logging module " + mod.getModuleName());
		} else {
			System.out.println("Enabeling logging module " + mod.getModuleName());
		}
		modules.add(mod);
		return this;
	}

	/**
	 * Enables the specified module to log with for all instances
	 * 
	 * @param mod The {@link Module}
	 * @see Module
	 */
	public void enableMasterModule(Module mod) {
		if (modules.size() != 0 || masterModules.size() != 0) {
			core("Enabeling master logging module " + mod.getModuleName());
		} else {
			System.out.println("Enabeling master logging module " + mod.getModuleName());
		}

		masterModules.add(mod);
	}

	/**
	 * Logs with the CORE severity
	 * 
	 * @param string
	 */
	protected void core(String string) {
		triggerLog(string, Severity.CORE);

	}

	/**
	 * Checks whether the specified module is enabled
	 * 
	 * @param name The name of the {@link Module}
	 * @return {@link Boolean}
	 */
	public boolean isModuleEnabled(String name) {
		for (Module module : modules) {
			if (module.getModuleName().equals(name)) {
				return true;
			}
		}

		if (DISABLEMASTER)
			return false;
		for (Module module : masterModules) {
			if (module.getModuleName().equals(name)) {
				return true;
			}
		}
		return false;
	}

	private void triggerLog(final String msg, final Severity level) {
		if(msg.isEmpty()) return;
		if(msg.trim().isEmpty()) return;
		LogQueue.startCheck();
		LogQueue.queue(new LogQEntry(msg,level,this, name));

//		new Thread(new Runnable() {
//
//			public void run() {
//				for (Module module : modules) {
//					module.log(msg, level, name);
//				}
//
//				if (DISABLEMASTER)
//					return;
//				for (Module module : masterModules) {
//					module.log(msg, level, name);
//				}
//
//			}
//		}).start();

	}

	private void triggerLog(final BaseEntry clas, final Severity level) {
		LogQueue.startCheck();
		LogQueue.queue(new LogQEntry(clas,level,this, name));
//		new Thread(new Runnable() {
//
//			public void run() {
//				for (Module module : modules) {
//					module.log(clas, level, name);
//				}
//
//				if (DISABLEMASTER)
//					return;
//				for (Module module : masterModules) {
//					module.log(clas, level, name);
//				}
//
//			}
//		}).start();
	}
	

	public void info(String msg) {
		triggerLog(msg, Severity.INFO);
	}

	public void info(BaseEntry clas) {
		triggerLog(clas, Severity.INFO);
	}

	public void warning(String msg) {
		triggerLog(msg, Severity.WARNING);
	}

	public void warning(BaseEntry clas) {
		triggerLog(clas, Severity.WARNING);
	}

	public void error(String msg) {
		triggerLog(msg, Severity.ERROR);
	}

	public void error(BaseEntry clas) {
		triggerLog(clas, Severity.ERROR);
	}

	public void error(Throwable e, HashMap<String, Object> additionalInfo) {
		error(wrapException(e, additionalInfo));
	}
	
	public void method(Object... additional) {
		StackTraceElement st = Thread.currentThread().getStackTrace()[2];
		debug(st.getClassName() + "::" + st.getMethodName() + (additional.length != 0 ? " (" + String.join(", ", getString(additional)) + ")" : ""));
	}

	private String[] getString(Object[] additional) {
		String[] s = new String[additional.length];
		for (int i = 0; i < additional.length; i++) {
			s[i] = additional[i].toString();
		}
		return s;
	}

	private WrappedException wrapException(Throwable e, HashMap<String, Object> additionalInfo) {

		WrappedException ex = new WrappedException();
		if(additionalInfo != null)
			ex.additionalInfo = additionalInfo;
		else
			ex.additionalInfo = new HashMap<String, Object>();
		ex.originalException = e;
		ex.message = e.getMessage();
		ex.type = e.getClass().getName();
		return ex;
	}

	private class WrappedException extends BaseEntry {

		public String type;

		public Throwable originalException;

		public HashMap<String, Object> additionalInfo;

	}

	public void sevre(String msg) {
		triggerLog(msg, Severity.SEVRE);
	}

	public void sevre(BaseEntry clas) {
		triggerLog(clas, Severity.SEVRE);
	}

	public void debug(String msg) {
		triggerLog(msg, Severity.DEBUG);
	}

	public void debug(BaseEntry clas) {
		triggerLog(clas, Severity.DEBUG);
	}

	public void diagnostic(String msg) {
		triggerLog(msg, Severity.DIAGNOSTIC);
	}

	public void diagnostic(BaseEntry clas) {
		triggerLog(clas, Severity.DIAGNOSTIC);
	}

	public void metric(String msg) {
		triggerLog(msg, Severity.METRIC);
	}

	public void metric(BaseEntry clas) {
		triggerLog(clas, Severity.METRIC);
	}

	public void low(String msg) {
		triggerLog(msg, Severity.LOW);
	}

	public void low(BaseEntry clas) {
		triggerLog(clas, Severity.LOW);
	}

	public void lowest(String msg) {
		triggerLog(msg, Severity.LOWEST);
	}

	public void lowest(BaseEntry clas) {
		triggerLog(clas, Severity.LOWEST);
	}

}

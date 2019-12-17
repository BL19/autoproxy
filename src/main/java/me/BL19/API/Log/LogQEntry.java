package me.BL19.API.Log;

public class LogQEntry {
	public LogQEntry(BaseEntry clas, Severity level, Logger logger, String source) {
		messageOnly = false;
		entry = clas;
		severity = level;
		l = logger;
		this.source = source;
	}
	public LogQEntry(String msg, Severity level, Logger logger, String source) {
		messageOnly = true;
		message = msg;
		severity = level;
		l = logger;
		this.source = source;
	}
	public boolean messageOnly;
	public String message;
	public BaseEntry entry;
	public Severity severity;
	public String source;
	public Logger l;
}

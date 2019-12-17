package me.BL19.API.Log.modules;

import me.BL19.API.Log.BaseEntry;
import me.BL19.API.Log.Severity;

public interface Module {

	
	void log(String message, Severity level, String source);
	void log(BaseEntry obj, Severity level, String source);
	String getModuleName();
	
}

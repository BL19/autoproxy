package me.BL19.API.Log.modules;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.google.gson.Gson;

import me.BL19.API.Log.BaseEntry;
import me.BL19.API.Log.Severity;

public class ConsoleModule implements Module {

	SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	
	public void log(String message, Severity level, String source) {
		String msg = ("[" + format.format(new Date()) + "] [" + level + "] [" + source + "]  " + message);
		if(level == Severity.ERROR)
			System.err.println(msg);
		else 
			System.out.println(msg);
	}

	public void log(BaseEntry obj, Severity level, String source) {
		String message = "";
		try {
			message = ("[" + format.format(new Date()) + "] [" + level + "] [" + source + "]  " + new Gson().toJson(obj));
		} catch(UnsupportedOperationException e) {
			message = ("[" + format.format(new Date()) + "] [" + level + "] [" + source + "]  " + obj.message);
		}
		if(level == Severity.ERROR)
			System.err.println(message);
		else 
			System.out.println(message);
	}

	public String getModuleName() {
		return "Log.Console";
	}

	public void setLoggerName(String name) {
		
	}

}

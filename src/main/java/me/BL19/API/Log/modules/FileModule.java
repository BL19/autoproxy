package me.BL19.API.Log.modules;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.google.gson.Gson;

import me.BL19.API.Log.BaseEntry;
import me.BL19.API.Log.Severity;

public class FileModule implements Module {

	String file;
	
	
	SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	
	public FileModule(String fileName) {
		file = fileName;
	}
	
	public void setLoggerName(String name) {
		
	}

	public void log(String message, Severity level, String source) {
		wlToFile("[" + format.format(new Date()) + "] [" + level + "] [" + source + "]  " + message);
	}
	
	private void wlToFile(String line) {
		try {
			File file = new File(this.file);
			FileWriter fr = new FileWriter(file, true);
			fr.write(line + "\n");
			fr.close();
		} catch (Exception e) {
			
		}
	}

	public void log(BaseEntry obj, Severity level, String source) {
		wlToFile("[" + format.format(new Date()) + "] [" + level + "] [" + source + "]  " + new Gson().toJson(obj));
	}

	public String getModuleName() {
		return "Log.File";
	}

}

package me.BL19.API.Log;

import java.util.Date;
import java.text.SimpleDateFormat;

public class BaseEntry {

	@SuppressWarnings("deprecation")
	public String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date()); // ISO FORMAT
	public String message;
	public Severity severity;
	public String source;
	
}

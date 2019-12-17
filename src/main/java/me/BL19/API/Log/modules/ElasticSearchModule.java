package me.BL19.API.Log.modules;

import com.google.gson.Gson;

import me.BL19.API.Log.BaseEntry;
import me.BL19.API.Log.Logger;
import me.BL19.API.Log.Severity;
import me.BL19.API.Log.ElasticModule.ESUtils;

public class ElasticSearchModule implements Module {

	
	private static Logger l;
	
	public static boolean DISABLEWARNINGS = false;
	
	private String es_log;
	
	public ElasticSearchModule(String logIndex) {
		if (l == null)
			l = Logger.getLogger(ElasticSearchModule.class);
		l.DISABLEMASTER = true;
		l.enableModule(new ConsoleModule());
		es_log = logIndex;
		TryToCreate();
	}
	
	private void TryToCreate() {
		if(ESUtils.getHost() == null) {
			l.error("Elastic Search Host Was Not Set. Set the host with ESUtils.setHost");
			return;
		}
		
		String index = ESUtils.get(es_log);
		//System.out.println(index);
		if(index.equals("{}") || index.contains("\"status\":404")) {
			ESUtils.put(es_log, "{\"log\":{\"_timestamp\":{\"enabled\":true,\"path\":\"timestamp\"}}}");
			ESUtils.put(es_log + "/log/_mapping", "{\"log\":{\"properties\":{\"_timestamp\":{\"type\":\"date\",\"format\":\"MMM dd, yyyy hh:mm:ss\"}}}}");
		}
	}
	
	public void setLoggerName(String name) {
		
	}

	public void log(String message, Severity level, String source) {
		if(!DISABLEWARNINGS) {
			l.warning("Logging with a message and no object will set the specifed ElasticSearch index to the included class for a message");
			l.warning("Set DISABLEWARNINGS to TRUE to continue");
			return;
		}
		
		BaseEntry b = new BaseEntry();
		b.message = message;
		b.severity = level;
		b.source = source;
		
		log(b, level, source);
		
	}

	public void log(BaseEntry obj, Severity level, String source) {
		if(!DISABLEWARNINGS) {
			l.warning("Logging with a message and no object will set the specifed ElasticSearch index to the included class for a message");
			l.warning("Set DISABLEWARNINGS to TRUE to continue");
			return;
		}
		
		obj.source = source;
		obj.severity = level;
		
		final BaseEntry ent = obj;
		new Thread(new Runnable() {
			
			public void run() {
				ESUtils.post(es_log + "/log", new Gson().toJson(ent));				
			}
		}).start();

		
	}

	public String getModuleName() {
		return "Log.ES";
	}



}

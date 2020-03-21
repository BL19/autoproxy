package me.BL19.AutoProxy;

import java.util.HashMap;

import me.BL19.AutoProxy.Statistic.Formatting;

public class Statistics {

	public Statistic bytesSent;
	public Statistic requests;
	public HashMap<String, Statistic> requestsPerAddress;
	
	
	public void init() {
		bytesSent = new Statistic();
		bytesSent.name = "Bytes Sent";
		bytesSent.formatting = Formatting.BINARY;
		
		requests = new Statistic();
		requests.name = "Total Requests";
		
		requestsPerAddress = new HashMap<String, Statistic>();
		
	}
	
}

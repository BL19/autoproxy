package me.BL19.API.Log;

import java.util.ArrayList;

import me.BL19.API.Log.modules.Module;

public class LogQueue {

	
	
	private static ArrayList<LogQEntry> queue = new ArrayList<LogQEntry>();
	
	private static Thread t;
	
	public static void start() {
		System.out.println("Starting logqueue");
		t = new Thread(new Runnable() {
			
			public void run() {
				try {
					execute();
				} catch (Exception e) {
					System.err.println("Error in logqueue");
					e.printStackTrace();
				}
			}
		});
		t.start();
	}
	
	public static boolean isRunning() {
		if(t == null) return false;
		return t.isAlive();
	}
	
	public static void startCheck() {
		if(!isRunning()) start();
	}
	
	public static void execute() {
		
		while(true) {
			runEntry();
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}

	static LogQEntry last;
	
	private static void runEntry() {
		if(queue.size() == 0) return;
		LogQEntry e = queue.remove(0);
		if(e == last) return;
		last = e;
		for (Module m : e.l.getModules()) {
			if(e.messageOnly)
				m.log(e.message, e.severity, e.source);
			else
				m.log(e.entry, e.severity, e.source);
		}
		
	}

	public static void queue(LogQEntry logQEntry) {
		queue.add(logQEntry);
//		System.out.println("Queued " + logQEntry.source + ": " + logQEntry.message + "(" + logQEntry.entry + ")");
	}
	
}




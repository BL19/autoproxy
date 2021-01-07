package me.BL19.AutoProxy;

import java.util.Date;
import java.util.List;

import com.heroku.api.App;
import com.heroku.api.HerokuAPI;

public class Heroku {

	public static HerokuAPI getApi() {
		if(System.getenv("heroku.otheraccount") != null) {
			return new HerokuAPI(System.getenv("heroku.otheraccount"));
		}
		return null;
	}
	
	public static void startOther() {
		if(getApi() == null) return;
		List<App> apps = getApi().listApps();
		for (App app : apps) {
		    getApi().scale(app.getName(), "web", 1);
		}
	}
	
	public static void stopOther() {
		if(getApi() == null) return;
		List<App> apps = getApi().listApps();
		for (App app : apps) {
		    getApi().scale(app.getName(), "web", 0);
		}
	}
	
	public static void schedule() {
		if(getApi() == null) return;
		Date d = new Date();
		int switchDate = 16;
		int daysLeft = switchDate - d.getDate();
		boolean firstPart = System.getenv("heroku.part").equals("1");
		boolean startOther = false;
		if(firstPart) {
			if(daysLeft < 0) startOther = true;
		} else {
			if(daysLeft > 0) startOther = true;
		}
		if(startOther) {
			startOther();
		} else {
			stopOther();
		}
	}
	
}

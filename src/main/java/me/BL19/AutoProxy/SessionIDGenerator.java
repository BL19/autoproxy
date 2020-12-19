package me.BL19.AutoProxy;

import java.util.UUID;

public class SessionIDGenerator {

	public static String getNewSession() {
		return UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
	}
	
}

package me.BL19.AutoProxy;

import java.util.HashMap;
import java.util.List;

public class AutoProxyConfig {

	public int port = 8080;
	public boolean allowReplace = true;
	public HashMap<String, ProxyAddress> adresses;
	public List<String> fileTypesToIgnore;
	public CertConfig cert;
	
	
	
}

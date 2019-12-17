package me.BL19.AutoProxy;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import me.BL19.API.Log.Logger;
import me.BL19.API.Log.modules.ConsoleModule;

public class AutoProxy {

	public static List<ProxyAddress> proxiedAddresses = new ArrayList<ProxyAddress>();
	public static Logger l = new Logger(AutoProxy.class);
	public static AutoProxyConfig conf;
	
	public static void main(String[] args) {
		l.enableMasterModule(new ConsoleModule());
		try {
			loadConfig();
		} catch (IOException e) {
			l.error("Failed to load proxies");
			e.printStackTrace();
		}
		new HttpServer();
	}
	
	private static void loadConfig() throws IOException {
		Yaml yml = new Yaml(new Constructor(AutoProxyConfig.class));
		FileReader r = new FileReader("config.yml");
		conf = yml.load(r);
		r.close();
		for (ProxyAddress proxyAddress : conf.adresses.values()) {
			if(proxyAddress.enabled)
				proxiedAddresses.add(proxyAddress);
		}
	}

	public static ProxyAddress getTarget(String suburl) {
		if(suburl.equals("/"))
			return null;
		for (ProxyAddress proxyAddress : proxiedAddresses) {
			if((suburl.startsWith(proxyAddress.suburl)) && proxyAddress.enabled) 
				return proxyAddress;
		}
		return null;
	}
	
	
}

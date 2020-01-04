package me.BL19.AutoProxy;

import java.io.File;
import java.io.FileInputStream;
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
	public static String key = KeyForgery.generateKey();

	public static void main(String[] args) {
		
		l.enableMasterModule(new ConsoleModule());
		l.info("To POST a config to '/config' use the key '" + key + "'");
		try {
			loadConfig();
		} catch (IOException e) {
			l.error("Failed to load proxies. Need to post new config.");
			conf = new AutoProxyConfig(); // Null pointer
			e.printStackTrace();
		}
		if(args.length > 0) {
			if(args[0].equalsIgnoreCase("port")) {
				conf.port = Integer.parseInt(args[1]);
			}
		}
		if(!conf.allowReplace)
			l.info("Nevermind you can't replace anyway. (Config Disabled)");
		new HttpServer();
	}

	public static void loadConfig() throws IOException {
		Yaml yml = new Yaml(new Constructor(AutoProxyConfig.class));
		File file = new File("config.yml");
		FileInputStream fis = new FileInputStream(file);
		byte[] data = new byte[(int) file.length()];
		fis.read(data);
		fis.close();

		String r = new String(data, "UTF-8");

		// Try to eliminate user errors
		r = r.toLowerCase();
		r = r.replace("hardreplace", "hardReplace");
		r = r.replace("replaceinheaders", "replaceInHeaders");
		r = r.replace("filetypestoignore", "fileTypesToIgnore");
		r = r.replace("allowreplace", "allowReplace");

		conf = yml.load(r);
		proxiedAddresses.clear();
		for (ProxyAddress proxyAddress : conf.adresses.values()) {
			if (proxyAddress.enabled)
				proxiedAddresses.add(proxyAddress);
		}
	}

	public static ProxyAddress getTarget(String suburl) {
		if(suburl.equals("/"))
			return null;
		List<ProxyAddress> possibleMatches = new ArrayList<ProxyAddress>();
		for (ProxyAddress proxyAddress : proxiedAddresses) {
			if(match(suburl, proxyAddress.suburl) != null) {
				possibleMatches.add(proxyAddress);
			}
		}
		int l = 0;
		ProxyAddress s = null;
		for (ProxyAddress proxyAddress : possibleMatches) {
			if(proxyAddress.suburl.length() > l) {
//				System.out.println("new longest " + proxyAddress.suburl);
				l = proxyAddress.suburl.length();
				s = proxyAddress;
			}
		}
		return s;
	}

	static String match(String suburl1, String suburl2) {
		if(suburl2.equals("/")) {
			return suburl2;
		}
		if(suburl1.equals(suburl2)) return suburl2; // full base remapping
		suburl1 = (suburl1.startsWith("/") ? suburl1.substring(1) : suburl1);
		suburl2 = (suburl2.startsWith("/") ? suburl2.substring(1) : suburl2);
		if(!suburl1.contains(suburl2))
			return null;
//		System.out.println("Matching: " + suburl1 + " & " + suburl2);
		String[] a = suburl1.split("/");
		String[] b = suburl2.split("/");
		List<String> matches = new ArrayList<String>();
		for (int i = 0; i < a.length; i++) {
			String as = "";
			for (int j = 0; j <= i; j++) {
				as += a[j] + "/";
			}
			as = as.substring(0, as.length() - 1);
//			System.out.println("AS (" + i + "): " + as);
			for (int j = 0; j < b.length; j++) {
				String bs = "";
				for (int k = 0; k <= j; k++) {
					bs += b[k] + "/";
				}
				bs = bs.substring(0, bs.length() - 1);
//				System.out.println("BS (" + j + "): " + bs);
				if(as.equals(bs)) {
//					System.out.println("MATCH");
					matches.add(as);
				}
			}
		}
		int longest = 0;
		String longestStr = null;
		for (String string : matches) {
			if(string.length() > longest) {
				longest = string.length();
				longestStr = string;
			}
		}
//		System.out.println("Res: " + longestStr);
		return longestStr;
	}

}

package me.BL19.AutoProxy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import me.BL19.API.Log.Logger;
import me.BL19.API.Log.modules.ConsoleModule;

public class AutoProxy {

	public static List<ProxyAddress> proxiedAddresses = new ArrayList<ProxyAddress>();
	public static Logger l = new Logger(AutoProxy.class);
	public static AutoProxyConfig conf;
	public static String key = KeyForgery.generateKey();
	
	public static Thread tempRemover = new Thread(new Runnable() {
		
		public void run() {
			while (true) {
				try {
					
					for (File f : new File(".").listFiles()) {
						if(f.getName().startsWith("temp")) {
							f.delete();
						}
					}
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					Thread.sleep(600000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
		}
	}, "TempRemover");

	public static void main(String[] args) {
		Options options = new Options();

		Option port = new Option("p", "port", true, "The hosting port override");
		port.setRequired(false);
		options.addOption(port);

		Option defaultHost = new Option("d", "default", true, "The default host when a config is not found");
		defaultHost.setRequired(false);
		options.addOption(defaultHost);

		CommandLine cmd = null;

		try {
			cmd = new DefaultParser().parse(options, args);
		} catch (ParseException e) {
			System.out.println(e.getMessage());
			new HelpFormatter().printHelp("AutoProxy", options);

			System.exit(1);
		}

		l.enableMasterModule(new ConsoleModule());
		l.info("To POST a config to '/config' use the key '" + key + "'");
		try {
			loadConfig();
		} catch (IOException e) {
			if (conf == null)
				conf = new AutoProxyConfig(); // Null pointer
			if (conf.adresses == null)
				conf.adresses = new HashMap<String, ProxyAddress>();
			if (conf.fileTypesToIgnore == null)
				conf.fileTypesToIgnore = new ArrayList<String>();
			conf.fileTypesToIgnore.add("ttf");
			conf.fileTypesToIgnore.add("woff");
			conf.fileTypesToIgnore.add("ico");
			conf.fileTypesToIgnore.add("woff2");
			conf.fileTypesToIgnore.add("png");
			conf.fileTypesToIgnore.add("jpg");
			conf.fileTypesToIgnore.add("jpeg");
			conf.fileTypesToIgnore.add("ics");
			conf.allowReplace = true;
			
			conf.cert = new CertConfig();
			if(System.getenv("apcert").equals("true")) {
				conf.cert.enabled = true;
				if(System.getenv("apcert.pwd") != null) {
					conf.cert.password = System.getenv("apcert.pwd");
				}
				if(System.getenv("apcert.file") != null) {
					conf.cert.file = System.getenv("apcert.file");
				}
				if(System.getenv("apcert.url") != null) {
					conf.cert.file = System.getenv("apcert.url");
					conf.cert.url = true;
				}
			}
			
			if (cmd.hasOption("default")) {
				ProxyAddress p = new ProxyAddress();
				p.enabled = true;
				p.suburl = "/";
				p.url = cmd.getOptionValue("default");
				p.hardReplace = true;
				p.replaceInHeaders = true;
				conf.adresses.put("root", p);
				proxiedAddresses.add(p);
				conf.port = 80;
				l.info("Config not found added '" + p.url + "' as default at '/' (OPT)");
			} else if (System.getenv("apdefault") != null) {

				ProxyAddress p = new ProxyAddress();
				p.enabled = true;
				p.suburl = "/";
				p.url = System.getenv("apdefault");
				p.hardReplace = true;
				p.replaceInHeaders = true;
				conf.adresses.put("root", p);
				proxiedAddresses.add(p);
				conf.port = 80;
				l.info("Config not found added '" + p.url + "' as default at '/' (ENV)");

			} else {
				l.error("Failed to load proxies. Need to post new config.");
				e.printStackTrace();
			}
		}
		if (cmd.hasOption("port")) {
			conf.port = Integer.parseInt(cmd.getOptionValue("port"));
			l.info("Set port " + conf.port);
		}
		if (!conf.allowReplace)
			l.info("Nevermind you can't replace anyway. (Config Disabled)");
		new HttpServer();
		tempRemover.start();
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
		r = r.replace("replacewithotheradresses", "replaceWithOtherAdresses");

		conf = yml.load(r);
		proxiedAddresses.clear();
		for (ProxyAddress proxyAddress : conf.adresses.values()) {
			if (proxyAddress.enabled)
				proxiedAddresses.add(proxyAddress);
		}
	}

	public static ProxyAddress getTarget(String suburl) {
//		if (suburl.equals("/"))
//			return null;
		List<ProxyAddress> possibleMatches = new ArrayList<ProxyAddress>();
		for (ProxyAddress proxyAddress : proxiedAddresses) {
			if (match(suburl, proxyAddress.suburl) != null) {
				possibleMatches.add(proxyAddress);
			}
		}
		int l = 0;
		ProxyAddress s = null;
		for (ProxyAddress proxyAddress : possibleMatches) {
			if (proxyAddress.suburl.length() > l) {
//				System.out.println("new longest " + proxyAddress.suburl);
				l = proxyAddress.suburl.length();
				s = proxyAddress;
			}
		}
		return s;
	}

	static String match(String suburl1, String suburl2) {
		if (suburl2.equals("/")) {
			return suburl2;
		}
		if (suburl1.equals(suburl2))
			return suburl2; // full base remapping
		suburl1 = (suburl1.startsWith("/") ? suburl1.substring(1) : suburl1);
		suburl2 = (suburl2.startsWith("/") ? suburl2.substring(1) : suburl2);
		if (!suburl1.contains(suburl2))
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
				if (as.equals(bs)) {
//					System.out.println("MATCH");
					matches.add(as);
				}
			}
		}
		int longest = 0;
		String longestStr = null;
		for (String string : matches) {
			if (string.length() > longest) {
				longest = string.length();
				longestStr = string;
			}
		}
//		System.out.println("Res: " + longestStr);
		return longestStr;
	}

}

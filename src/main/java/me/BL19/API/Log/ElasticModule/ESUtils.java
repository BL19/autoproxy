package me.BL19.API.Log.ElasticModule;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import com.google.gson.Gson;


public class ESUtils {

	private static String es_host;
	
	public static boolean DEBUG = false;

	public static String get(String url) {
		if(DEBUG) {
			System.out.println(es_host + "/" + url);
		}
		try {
			HttpURLConnection c = (HttpURLConnection) new URL(es_host + "/" + url).openConnection();
			c.setRequestMethod("GET");
			c.setDoInput(true);
			c.setRequestProperty("Accept", "application/json");
			c.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.82 Safari/537.36");
			c.setRequestProperty("Cache-Control", "no-cache");

			StringBuilder sb = new StringBuilder();
			int HttpResult = c.getResponseCode();
			if (HttpResult == HttpURLConnection.HTTP_OK) {
				BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), "utf-8"));
				String line = null;
				while ((line = br.readLine()) != null) {
					sb.append(line + "\n");
				}
				br.close();
				return "" + sb.toString();
			}
		} catch (MalformedURLException e) {
//			e.printStackTrace();
		} catch (IOException e) {
//			e.printStackTrace();
			
		}
		return "{}";
	}

	public static String post(String url, String data) {
		if(DEBUG) {
			System.out.println(es_host + "/" + url);
			System.out.println("[POST] " + data);
		}
		try {
			HttpURLConnection c = (HttpURLConnection) new URL(es_host + "/" + url).openConnection();
			c.setRequestMethod("POST");
			c.setDoOutput(true);
			c.setDoInput(true);
			c.setRequestProperty("Content-Type", "application/json");
			c.setRequestProperty("Accept", "application/json");
			OutputStreamWriter wr = new OutputStreamWriter(c.getOutputStream());
			wr.write(data);
			wr.flush();

			StringBuilder sb = new StringBuilder();
			int HttpResult = c.getResponseCode();
			if (HttpResult == HttpURLConnection.HTTP_OK) {
				BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), "utf-8"));
				String line = null;
				while ((line = br.readLine()) != null) {
					sb.append(line + "\n");
				}
				br.close();
				return "" + sb.toString();
			}
		} catch (MalformedURLException e) {
//			e.printStackTrace();
		} catch (IOException e) {
//			e.printStackTrace();
		}
		return "{}";
	}

	public static String put(String url, String data) {
		if(DEBUG) {
			System.out.println(es_host + "/" + url);
			System.out.println("[PUT] " + data);
		}
		try {
			HttpURLConnection c = (HttpURLConnection) new URL(es_host + "/" + url).openConnection();
			c.setRequestMethod("PUT");
			c.setDoOutput(true);
			c.setDoInput(true);
			c.setRequestProperty("Content-Type", "application/json");
			c.setRequestProperty("Accept", "application/json");
			OutputStreamWriter wr = new OutputStreamWriter(c.getOutputStream());
			wr.write(data);
			wr.flush();

			StringBuilder sb = new StringBuilder();
			int HttpResult = c.getResponseCode();
			if (HttpResult == HttpURLConnection.HTTP_OK) {
				BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), "utf-8"));
				String line = null;
				while ((line = br.readLine()) != null) {
					sb.append(line + "\n");
				}
				br.close();
				return "" + sb.toString();
			}
		} catch (MalformedURLException e) {
//			e.printStackTrace();
		} catch (IOException e) {
//			e.printStackTrace();
		}
		return "{}";
	}

	public static void setHost(String host) {
		es_host = host;

	}

	public static String getHost() {
		return es_host;
	}

}

package me.BL19.AutoProxy;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import com.google.gson.Gson;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import me.BL19.API.Log.Logger;

public class HttpServer extends NanoHTTPD {

	public Logger l = new Logger(HttpServer.class);

	public HttpServer() {
		super(AutoProxy.conf.port);
		l.info("Trying to start http server on port " + AutoProxy.conf.port);
		try {
			start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
		} catch (IOException e) {
			l.error("Failed to start HttpServer");
			e.printStackTrace();
			return;
		}
		l.info("Started HttpServer!");
	}

	public static boolean containsHTML(String str) {
		boolean r = str.toLowerCase().trim().startsWith("<") && str.toLowerCase().trim().endsWith("</html>");
		return r;
	}

	public static String getHost(String str) {
		if (str == null) {
			return null;
		}
		String s = str.replaceFirst("(https|http):(\\/\\/)", "");
		if (s.indexOf("/") == -1)
			return s;
		s = s.substring(0, s.indexOf("/"));
		return s;
	}

	public static String normalize(String oldHost, String newHost, String theString) {
		String host = getHost(oldHost);
		String replacement = newHost;
		System.out.println(host);
		String regx = "(https|http):(\\/\\/)(www?)(\\.?)(" + host.replace(".", "\\.") + ")";
		System.out.println(regx);
		System.out.println(replacement);
		return theString.replaceAll(regx, replacement);
	}

	@Override
	public Response serve(IHTTPSession session) {
		if (session.getMethod() == Method.OPTIONS) {
			Response r = newFixedLengthResponse(Status.OK, "text", "OK");
			r.addHeader("Access-Control-Allow-Origin", "*");
			r.addHeader("Access-Control-Allow-Methods", "*");
			r.addHeader("Access-Control-Allow-Headers", "*");
			r.addHeader("Server", "AutoProxy");
			return r;
		}
		try {
			String uri = session.getUri();
			while(uri.startsWith("/")) {
				uri = uri.substring(1);
			}
			uri = "/" + uri;
			if (uri.equals("/config") && AutoProxy.key != null) {
				if (!session.getHeaders().containsKey("apkey")
						|| !session.getHeaders().get("apkey").equals(AutoProxy.key)) { // Wrong key
					new Thread(new Runnable() {
						public void run() {
							try {
								Thread.sleep((long) (Math.random() * 1000 + (int) AutoProxy.key.charAt(2)));
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							AutoProxy.key = KeyForgery.generateKey();
							l.info("Regenerated key '" + AutoProxy.key + "'");
						}
					}).start();
					return newFixedLengthResponse(Status.UNAUTHORIZED, "text", "Wrong key");
				}

				// Key is correct
				if (session.getMethod() == Method.POST && AutoProxy.conf.allowReplace) {
					Scanner sc = new Scanner(session.getInputStream());
					StringBuffer sb = new StringBuffer();
					while (sc.hasNext()) {
						sb.append(sc.nextLine() + "\n");
					}
					FileWriter fw;
					try {
						fw = new FileWriter("config.yml");
						fw.write(sb.toString());
						fw.close();
					} catch (IOException e) {
						e.printStackTrace();
						return newFixedLengthResponse("Err: " + e.getMessage());
					}
					try {
						AutoProxy.loadConfig();
					} catch (IOException e) {
						e.printStackTrace();
						return newFixedLengthResponse("Err: " + e.getMessage());
					}
					l.warning("Config has been replaced! (" + session.getRemoteIpAddress() + ")");
					return newFixedLengthResponse(sb.toString());
				} else if (session.getMethod() == Method.GET) {
					Yaml yml = new Yaml(new Constructor(AutoProxyConfig.class));
					return newFixedLengthResponse(yml.dump(AutoProxy.conf));
				}
			}

			ProxyAddress addr = AutoProxy.getTarget(uri);
			if (addr == null) {
				String ref = session.getHeaders().get("referer");

				if (ref != null) {

					String host = getHost(ref);
					String regx = "(https|http):(\\/\\/)(((www?)(\\.?))?)(" + host.replace(".", "\\.") + ")";
					ref = ref.replaceAll(regx, "");

					System.out.println("[ADVREF] Trying " + ref);
					ProxyAddress referer = AutoProxy.getTarget(ref);
					if (referer != null) {
						addr = referer;
						uri = referer.suburl + (uri.startsWith("/") ? "" : "/") + uri;
					} else {
						return newFixedLengthResponse(Status.NOT_FOUND, "text",
								"Couldn't find proxying for " + uri + "\n" + "Try adding a / before your suburl");
					}
				} else {
					return newFixedLengthResponse(Status.NOT_FOUND, "text",
							"Couldn't find proxying for " + uri + "\n" + "Try adding a / before your suburl");
				}
			}
			HttpURLConnection con = null;
			try {
				boolean runActions = true;
				for (String s : AutoProxy.conf.fileTypesToIgnore) {
					if (uri.endsWith("." + s))
						runActions = false;
				}
				System.out.println(new Gson().toJson(addr));
				String address = addr.url + uri.substring(addr.suburl.length(), uri.length());
				address = address.replace("//", "/").replace(":/", "://");
				List<NameValuePair> queryParams = URLEncodedUtils.parse(session.getQueryParameterString(),
						Charset.defaultCharset());
				List<NameValuePair> newParams = new ArrayList<NameValuePair>();
				ProxyAddress refererProxy = null;
				try {
					refererProxy = AutoProxy.getTarget(session.getHeaders().get("referer")
							.replace("http://" + session.getHeaders().get("host"), ""));
				} catch (NullPointerException e) {
				}
//			if(refererProxy != null && queryParams != null)
//				queryParams = queryParams.replace("http://" + session.getHeaders().get("host"), refererProxy.url);

				for (NameValuePair k : queryParams) {
					if (k.getName().equalsIgnoreCase("origin")) {
						newParams.add(new BasicNameValuePair(k.getName(), refererProxy.url));
					} else {
						newParams.add(k);
					}
				}

				String newAddr = address + (session.getQueryParameterString() != null
						? "?" + URLEncodedUtils.format(newParams, Charset.defaultCharset())
						: "");
				System.out.println(uri + " -> " + newAddr);
				URL url = new URL(newAddr);
				con = (HttpURLConnection) url.openConnection();
				con.setRequestMethod(session.getMethod().name());

				con.setUseCaches(false);

				{
					String host = getHost(session.getHeaders().get("host"));
					String replacement = addr.url;
					String regx = "(https|http):(\\/\\/)(www?)(\\.?)(" + host.replace(".", "\\.") + ")";
					for (String key : session.getHeaders().keySet()) {
						if (key.equalsIgnoreCase("host") || key.equalsIgnoreCase("referer")
								|| key.equalsIgnoreCase("Origin"))
							continue;
//					con.setRequestProperty(key, session.getHeaders().get(key));
						String field = String.join(";", session.getHeaders().get(key));

						if (addr.replaceInHeaders && runActions)
							field = field.replaceAll(regx, replacement);

						con.setRequestProperty(key, field);
//				System.out.println(key + ": " + session.getHeaders().get(key));
					}
				}
				if (session.getHeaders().get("referer") != null)
					con.setRequestProperty("referer", session.getHeaders().get("referer")
							.replace("http://" + session.getHeaders().get("host") + addr.suburl, addr.url));

				if (session.getHeaders().get("origin") != null && refererProxy != null)
					con.setRequestProperty("origin", refererProxy.url);
				Map<String, List<String>> requestProperties = con.getRequestProperties();
				int lslash = addr.url.indexOf("/", 8);
//			session.getHeaders().put("host", lslash == -1 ? addr.url.replace("http://", "").replace("https://", "") : addr.url.substring(0, lslash).replace("http://", "").replace("https://", ""));

				if (session.getInputStream() != null && session.getInputStream().available() >= 1) {
					con.setDoOutput(true);
					int ctl = Integer.parseInt(session.getHeaders().get("content-length"));
					byte[] data = new byte[ctl];
					session.getInputStream().read(data);
					con.getOutputStream().write(data);

//				IOUtils.copy(session.getInputStream(), con.getOutputStream());
					con.getOutputStream().close();
				}

				if (con.getResponseCode() == 404)
					return newFixedLengthResponse(Status.NOT_FOUND, "text", "Not found (404) from server");
				StringWriter writer = new StringWriter();

				InputStream is = null;
				try {
					is = con.getInputStream();
					String enc = con.getContentEncoding();
//				System.out.println(enc);
					if (enc == null || !enc.startsWith("gzip")) {
//					System.out.println("Encoding: " + enc + " (using none)");
						IOUtils.copy(is, writer);
					} else if (enc.startsWith("gzip")) {
//					System.out.println("Encoding: GZIP");
//					IOUtils.copy(is, writer, (enc == null || enc.equals("gzip") ? "UTF-8" : enc));
						writer.append(GZIPCompression.decompress(IOUtils.toByteArray(is)));
					}
				} catch (IOException ex) {
					return newFixedLengthResponse(Status.lookup(con.getResponseCode()), "text",
							"Error! Server returned " + con.getResponseCode() + " ("
									+ Status.lookup(con.getResponseCode()).name() + ")\nMethod: "
									+ con.getRequestMethod() + "\nRequest Headers: " + getHeaders(requestProperties)
									+ "\n\nResponse Headers: " + getHeaders(con.getHeaderFields()));
				}
				String theString = writer.toString();

				if (runActions) {
//				System.out.println("HTML: " + containsHTML(theString));
					if (containsHTML(theString)) {
						if (theString.contains("<base")) {
							// Has base tag needs removal
							int b = theString.indexOf("<base");
							String s = theString.substring(b);
							if (s.indexOf("/>") != -1)
								s = s.substring(0, s.indexOf("/>")); // <base href="*" / <base href="*"><base
							else
								s = s.substring(0, s.indexOf(">"));
							String baseTag = s + "";
							s = s.substring(s.indexOf("href=\""));
							s = s.replace("\"", "");
							s = s.replace("href=", "");
							s = s.trim();
							if (s.startsWith("/")) {
								s = (addr.suburl.startsWith("/") ? "" : "/") + addr.suburl + s;
							}
							if(s.startsWith("http")) {
								String uriBase = s.substring(s.indexOf('/') + 2);
								if(uriBase.contains("/"))
									uriBase = uriBase.substring(uriBase.indexOf('/') + 1);
								else
									uriBase = "";
								System.out.println(uriBase);
								String uriAddon = addr.suburl + "/" + uriBase;
								while(uriAddon.startsWith("/"))
									uriAddon = uriAddon.substring(1);
								s = "http://" + session.getHeaders().get("host") + "/" + uriAddon;
							}
							while (s.endsWith("/")) {
								s = s.substring(0, s.length() - 1);
							}
							String base = "<base href=\"" + s + "/\"";
							System.out.println("Orig Base: " + baseTag + ", New Base: " + base);
							theString = theString.replace(baseTag, base);
						} else if (theString.toLowerCase().contains("<head>")) { // localhost:8901/google
							String base = "<head><base href=\"http://" + session.getHeaders().get("host") + addr.suburl
									+ "\"/>";
							String pstring = theString;
							theString = theString.replace("<head>", base);
							if (theString.equals(pstring)) {
								theString = theString.replace("<HEAD>", base.replace("<head>", "<HEAD>"));
							}
						}
					}
					if (addr.hardReplace) {
						// (https|http):(\/\/)(www?)(\.?)(google\.com)
						// (https|http):(\/\/)(www?)(\.?)(google\.com)
						String host = getHost(addr.url);
						String replacement = "http://" + session.getHeaders().get("host") + addr.suburl;
						String regx = "(https|http):(\\/\\/)(www?)(\\.?)(" + host.replace(".", "\\.") + ")";
						theString = theString.replaceAll(regx, replacement);
					}

					if (addr.replaceWithOtherAdresses) {
						for (ProxyAddress a : AutoProxy.proxiedAddresses) {
							try {
								String host = getHost(a.url);
								String replacement = "http://" + session.getHeaders().get("host") + a.suburl;
								String regx = "(https|http):(\\/\\/)(" + host.replace(".", "\\.") + ")";
								theString = theString.replaceAll(regx, replacement);
							} catch (Exception e) {
							}
						}
					}
				}

//			System.out.println(theString);
//			if(session.getHeaders().get("accept-encoding").contains("gzip")) {
//				theString = new String(GZIPCompression.compress(theString), "UTF-8");
//			}
				int code = con.getResponseCode();
//			if(code == 302) code = 200;
//			if (runActions && theString.length() < 1000)
//				System.out.println("[" + session.getMethod().name() + "]" + code + " - "
//						+ con.getHeaderField("Content-Type") + ": " + theString);
//
//			if (!runActions || theString.length() >= 1000)
//				System.out.println("[" + session.getMethod().name() + "]" + code + " - "
//						+ con.getHeaderField("Content-Type") + " [SILENT]");

//			
//			System.out.println("Debug:");
//			System.out.println("Request Headers: ");
//			System.out.println(getHeaders(requestProperties));

				// Create response
//			if(session.getHeaders().get("accept-encoding") != null && session.getHeaders().get("accept-encoding").contains("gzip")) {
//				theString = new String(GZIPCompression.compress(theString));
//			}
//			theString = GZIPCompression.decompress(theString.getBytes());
				Response res = newFixedLengthResponse(Status.lookup(code), con.getHeaderField("Content-Type"),
						theString);

				String content = con.getHeaderField("Content-Type");
//			System.out.println(new Gson().toJson(con));

				String fileName = "";
				String disposition = con.getHeaderField("Content-Disposition");
				String contentType = con.getContentType();
				int contentLength = con.getContentLength();

				if (disposition != null) {
					// extracts file name from header field
					int index = disposition.indexOf("filename=");
					if (index > 0) {
						fileName = disposition.substring(index + 10, disposition.length() - 1);
					}
				} else {
					// extracts file name from URL
					fileName = newAddr.substring(newAddr.lastIndexOf("/") + 1, newAddr.length());
				}

//            System.out.println("Content-Type = " + contentType);
//            System.out.println("Content-Disposition = " + disposition);
//            System.out.println("Content-Length = " + contentLength);
//            System.out.println("fileName = " + fileName);

				if (is != null && !runActions) {
					URL website = new URL(newAddr);
					ReadableByteChannel rbc = Channels.newChannel(website.openStream());
					String fName = "temp_" + session.getUri().replace("/", "__");
					if (!new File(fName).exists()) {
						FileOutputStream fos = new FileOutputStream(fName);
						fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
						fos.close();
					}
					InputStream str = new FileInputStream(fName);
					long len = str.available();
					byte[] b1 = IOUtils.toByteArray(str);
					str.close();
					str = new ByteArrayInputStream(b1);
					len = b1.length;
					res = newFixedLengthResponse(Status.lookup(con.getResponseCode()),
							URLConnection.guessContentTypeFromName(fileName), str, len);
					res.addHeader("Content-Disposition", "attachment; filename=" + fileName);
					res.setRequestMethod(session.getMethod());
				}
//			if(session.getHeaders().get("accept-encoding") != null && session.getHeaders().get("accept-encoding").contains("gzip")) {
//				res.setGzipEncoding(true);
//			}
				res.setGzipEncoding(false);
				String host = getHost(addr.url);
				String replacement = "http://" + session.getHeaders().get("host") + addr.suburl;
				String regx = "(https|http):(\\/\\/)(www?)(\\.?)(" + host.replace(".", "\\.") + ")";
				for (String k : con.getHeaderFields().keySet()) {
					if (k != null && !k.equalsIgnoreCase("content-length") && !k.equalsIgnoreCase("content-type")
							&& !k.equalsIgnoreCase("content-encoding") && !k.equalsIgnoreCase("Transfer-Encoding")
							&& !k.equalsIgnoreCase("connection") && !k.startsWith("access-control")) {
						String key = k;
						String field = String.join(";", con.getHeaderFields().get(k));

						if (addr.replaceInHeaders && runActions)
							field = field.replaceAll(regx, replacement);

//					if (runActions)
//						System.out.println(key + ": " + field);

						res.addHeader(key, field);
					}
				}

				res.addHeader("Access-Control-Allow-Origin", "*");
				res.addHeader("Access-Control-Allow-Methods", "*");
				res.addHeader("Access-Control-Allow-Headers", "*");

				return res;

			} catch (Exception e) {
				l.error(e, null);
				e.printStackTrace();
				return null;
			}
		} catch (Exception ex) {
			l.error(ex, null);
			ex.printStackTrace();
			return newFixedLengthResponse(Status.INTERNAL_ERROR, "text", "It ain't my fault! :P");
		}
	}

	private String getHeaders(Map<String, List<String>> headers) {
		String s = "\n";
		for (String s1 : headers.keySet()) {
			s += "\t" + s1 + ": " + String.join(";", headers.get(s1)) + "\n";
		}
		return s;
	}

}

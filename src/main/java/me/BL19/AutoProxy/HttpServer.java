package me.BL19.AutoProxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import me.BL19.API.Log.Logger;

public class HttpServer extends NanoHTTPD {

	public Logger l = new Logger();

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
		String uri = session.getUri();
		ProxyAddress addr = AutoProxy.getTarget(uri);
		if (addr == null) {
			String ref = session.getHeaders().get("referer");

			if (ref != null) {

				String host = getHost(ref);
				System.out.println(host);
				String regx = "(https|http):(\\/\\/)(((www?)(\\.?))?)(" + host.replace(".", "\\.") + ")";
				System.out.println(regx);
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
			String address = uri.replace(addr.suburl, addr.url);
			System.out.println(uri + " -> " + address);
			URL url = new URL(address);
			con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod(session.getMethod().name());

			con.setUseCaches(false);

			{
				String host = getHost(session.getHeaders().get("host"));
				String replacement = addr.url;
				String regx = "(https|http):(\\/\\/)(www?)(\\.?)(" + host.replace(".", "\\.") + ")";
				for (String key : session.getHeaders().keySet()) {
					if (key.equalsIgnoreCase("host") || key.equalsIgnoreCase("referer"))
						continue;
//					con.setRequestProperty(key, session.getHeaders().get(key));
					String field = String.join(";", session.getHeaders().get(key));

					if (addr.replaceInHeaders && runActions)
						field = field.replaceAll(regx, replacement);

					if (runActions)
						System.out.println(key + ": " + field);

					con.setRequestProperty(key, field);
//				System.out.println(key + ": " + session.getHeaders().get(key));
				}
			}
			if (session.getHeaders().get("referer") != null)
				con.setRequestProperty("referer", session.getHeaders().get("referer")
						.replace("http://" + session.getHeaders().get("host") + addr.suburl, addr.url));
			Map<String, List<String>> requestProperties = con.getRequestProperties();
			int lslash = addr.url.indexOf("/", 8);
//			session.getHeaders().put("host", lslash == -1 ? addr.url.replace("http://", "").replace("https://", "") : addr.url.substring(0, lslash).replace("http://", "").replace("https://", ""));

			if (session.getInputStream() != null && session.getInputStream().available() >= 1) {
				IOUtils.copy(session.getInputStream(), con.getOutputStream());
				con.getOutputStream().close();
			}
			if (con.getResponseCode() == 404)
				return newFixedLengthResponse(Status.NOT_FOUND, "text", "Not found");
			StringWriter writer = new StringWriter();

			try {
				InputStream is = con.getInputStream();
				String enc = con.getContentEncoding();
//				System.out.println(enc);
				if (enc == null || !enc.startsWith("gzip")) {
					System.out.println("Encoding: " + enc + " (using none)");
					IOUtils.copy(is, writer);
				} else if (enc.startsWith("gzip")) {
					System.out.println("Encoding: GZIP");
//					IOUtils.copy(is, writer, (enc == null || enc.equals("gzip") ? "UTF-8" : enc));
					writer.append(GZIPCompression.decompress(IOUtils.toByteArray(is)));
				}
				is.close();
			} catch (IOException ex) {
				return newFixedLengthResponse(Status.lookup(con.getResponseCode()), "text",
						"Error! Server returned " + con.getResponseCode() + " ("
								+ Status.lookup(con.getResponseCode()).name() + ")\nMethod: " + con.getRequestMethod()
								+ "\nRequest Headers: " + getHeaders(requestProperties) + "\n\nResponse Headers: "
								+ getHeaders(con.getHeaderFields()));
			}
			String theString = writer.toString();

			if (runActions) {
				System.out.println("HTML: " + containsHTML(theString));
				if (containsHTML(theString)) {
					if (theString.contains("<base")) {
						// Has base tag needs removal
						System.out.println("Has basetag");
						int b = theString.indexOf("<base");
						String s = theString.substring(b);
						s = s.substring(0, s.indexOf("/>")); // <base href="*" / <base href="*"><base
						String baseTag = s + "";
						s = s.substring(s.indexOf("href=\""));
						s = s.replace("\"", "");
						s = s.replace("href=", "");
						s = s.trim();
						System.out.println(s);
						if (s.startsWith("/")) {
							s = (addr.suburl.startsWith("/") ? "" : "/") + addr.suburl + s;
						}
						if (s.endsWith("/")) {
							s = s.substring(0, s.length() - 1);
						}
						String base = "<base href=\"" + s + "/\"";
						System.out.println("Orig Base: " + baseTag + ", New Base: " + base);
						theString = theString.replace(baseTag, base);
					} else if (theString.toLowerCase().contains("</head>")) { // localhost:8901/google
						String base = "<base href=\"http://" + session.getHeaders().get("host") + addr.suburl
								+ "\"/></head>";
						String pstring = theString;
						theString = theString.replace("</head>", base);
						if (theString.equals(pstring)) {
							theString = theString.replace("</HEAD>", base.replace("</head>", "</HEAD>"));
						}
					}
				}
				if (addr.hardReplace) {
					// (https|http):(\/\/)(www?)(\.?)(google\.com)
					// (https|http):(\/\/)(www?)(\.?)(google\.com)
					String host = getHost(addr.url);
					String replacement = "http://" + session.getHeaders().get("host") + addr.suburl;
					System.out.println(host);
					String regx = "(https|http):(\\/\\/)(www?)(\\.?)(" + host.replace(".", "\\.") + ")";
					System.out.println(regx);
					System.out.println(replacement);
					theString = theString.replaceAll(regx, replacement);
				}
			}

//			System.out.println(theString);
//			if(session.getHeaders().get("accept-encoding").contains("gzip")) {
//				theString = new String(GZIPCompression.compress(theString), "UTF-8");
//			}
			int code = con.getResponseCode();
//			if(code == 302) code = 200;
			if (runActions && theString.length() < 1000)
				System.out.println("[" + session.getMethod().name() + "]" + code + " - "
						+ con.getHeaderField("Content-Type") + ": " + theString);

			if (!runActions || theString.length() >= 1000)
				System.out.println("[" + session.getMethod().name() + "]" + code + " - "
						+ con.getHeaderField("Content-Type") + " [SILENT]");

			
			System.out.println("Debug:");
			System.out.println("Request Headers: ");
			System.out.println(getHeaders(requestProperties));
			
			// Create response
//			if(session.getHeaders().get("accept-encoding") != null && session.getHeaders().get("accept-encoding").contains("gzip")) {
//				theString = new String(GZIPCompression.compress(theString));
//			}
//			theString = GZIPCompression.decompress(theString.getBytes());
			Response res = newFixedLengthResponse(Status.lookup(code), con.getHeaderField("Content-Type"), theString);
//			if(session.getHeaders().get("accept-encoding") != null && session.getHeaders().get("accept-encoding").contains("gzip")) {
//				res.setGzipEncoding(true);
//			}
			res.setGzipEncoding(false);
			String host = getHost(addr.url);
			String replacement = "http://" + session.getHeaders().get("host") + addr.suburl;
			String regx = "(https|http):(\\/\\/)(www?)(\\.?)(" + host.replace(".", "\\.") + ")";
			for (String k : con.getHeaderFields().keySet()) {
				if (k != null && !k.equalsIgnoreCase("content-length") && !k.equalsIgnoreCase("content-type")
						&& !k.equalsIgnoreCase("content-encoding") && !k.equalsIgnoreCase("Transfer-Encoding") && !k.equalsIgnoreCase("connection")) {
					String key = k;
					String field = String.join(";", con.getHeaderFields().get(k));

					if (addr.replaceInHeaders && runActions)
						field = field.replaceAll(regx, replacement);

					if (runActions)
						System.out.println(key + ": " + field);

					res.addHeader(key, field);
				}
			}
			System.out.println("Done!");
			return res;

		} catch (Exception e) {
			l.error(e, null);
			e.printStackTrace();
			return null;
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

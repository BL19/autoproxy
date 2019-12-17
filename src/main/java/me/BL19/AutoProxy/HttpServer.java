package me.BL19.AutoProxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;

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

	@Override
	public Response serve(IHTTPSession session) {
		ProxyAddress addr = AutoProxy.getTarget(session.getUri());
		if (addr == null) {
			return newFixedLengthResponse(Status.NOT_FOUND, "text",
					"Couldn't find proxying for " + session.getUri() + "\n" + "Try adding a / before your suburl");
		}
		HttpURLConnection con = null;
		try {
			String address = session.getUri().replace(addr.suburl, addr.url);
			System.out.println(session.getUri() + " -> " + address);
			URL url = new URL(address);
			con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod(session.getMethod().name());

			con.setUseCaches(false);
			
			for (String key : session.getHeaders().keySet()) {
				if(key.equalsIgnoreCase("host"))
					continue;
				con.setRequestProperty(key, session.getHeaders().get(key));
//				System.out.println(key + ": " + session.getHeaders().get(key));
			}

			if (session.getInputStream() != null && session.getInputStream().available() >= 1) {
				IOUtils.copy(session.getInputStream(), con.getOutputStream());
				con.getOutputStream().close();
			}
			if(con.getResponseCode() == 404) return newFixedLengthResponse(Status.NOT_FOUND, "text", "Not found");
			InputStream is = con.getInputStream();
			StringWriter writer = new StringWriter();
			String enc = con.getContentEncoding();
//			System.out.println(enc);
			if (enc == null || !enc.startsWith("gzip"))
				IOUtils.copy(is, writer, (enc == null ? "UTF-8" : enc));
			else {
				writer.append(GZIPCompression.decompress(IOUtils.toByteArray(is)));
			}
			String theString = writer.toString();
			is.close();
//			System.out.println(theString);
//			if(session.getHeaders().get("accept-encoding").contains("gzip")) {
//				theString = new String(GZIPCompression.compress(theString), "UTF-8");
//			}
			int code = con.getResponseCode();
//			if(code == 304) code = 200;
			System.out.println(code + " - " + con.getHeaderField("Content-Type") + ": " + theString);
			Response res = newFixedLengthResponse(Status.lookup(code),
					con.getHeaderField("Content-Type"), theString);
			for (String k : con.getHeaderFields().keySet()) {
				if (k != null && !k.equalsIgnoreCase("content-length") && !k.equalsIgnoreCase("content-type") && !k.equalsIgnoreCase("content-encoding")) {
					res.addHeader(k, String.join(";", con.getHeaderFields().get(k)));
				}
			}
			return res;

		} catch (Exception e) {
			l.error(e, null);
			e.printStackTrace();
			return null;
		}
	}

}

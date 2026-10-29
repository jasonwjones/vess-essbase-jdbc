package com.jasonwjones.hyperion.vess;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A Vess JDBC URL: the driver prefix, then the Essbase server's address as you
 * would open it in a browser, then optional settings after semicolons.
 *
 * <pre>
 * jdbc:essbase:vess:http://localhost:9000/essbase;app=Sample;cube=Basic
 * </pre>
 *
 * The application and cube, if given, are where unqualified names are looked
 * for, so that a query can say Cells rather than Sample.Basic.Cells.
 */
public final class VessUrl {

	public static final String PREFIX = "jdbc:essbase:vess:";

	private final String url;

	private final String serverUrl;

	private final Map<String, String> properties;

	private VessUrl(String url, String serverUrl, Map<String, String> properties) {
		this.url = url;
		this.serverUrl = serverUrl;
		this.properties = Collections.unmodifiableMap(properties);
	}

	public static boolean accepts(String url) {
		return url != null && url.startsWith(PREFIX);
	}

	public static VessUrl parse(String url) throws VessException {
		if (!accepts(url)) {
			throw new VessException("Not a Vess URL: " + url);
		}
		String[] parts = url.substring(PREFIX.length()).split(";");
		String serverUrl = parts[0].trim();
		if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
			throw new VessException("A Vess URL names the Essbase server's web address, as in " + PREFIX
					+ "http://localhost:9000/essbase, but this one has '" + serverUrl + "'");
		}
		while (serverUrl.endsWith("/")) {
			serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
		}
		Map<String, String> properties = new LinkedHashMap<>();
		for (int index = 1; index < parts.length; index++) {
			String part = parts[index].trim();
			if (part.isEmpty()) {
				continue;
			}
			int equals = part.indexOf('=');
			if (equals < 1) {
				throw new VessException("Expected key=value after a ';' in " + url + ", but found '" + part + "'");
			}
			properties.put(part.substring(0, equals).trim(), part.substring(equals + 1).trim());
		}
		return new VessUrl(url, serverUrl, properties);
	}

	/**
	 * @return the URL this was parsed from, as given
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @return the Essbase server's address, such as http://localhost:9000/essbase
	 */
	public String getServerUrl() {
		return serverUrl;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	@Override
	public String toString() {
		return PREFIX + serverUrl + properties;
	}

}

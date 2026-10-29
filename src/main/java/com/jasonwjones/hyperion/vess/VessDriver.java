package com.jasonwjones.hyperion.vess;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Connects to an Essbase server's REST API with URLs such as
 * {@code jdbc:essbase:vess:http://localhost:9000/essbase}, and presents the
 * whole server as SQL tables. See {@link VessUrl}.
 */
public class VessDriver implements Driver {

	/** The version of Vess, such as 1.0.0, as the build recorded it */
	public static final String VERSION = version();

	public static final int MAJOR_VERSION = versionPart(0);

	public static final int MINOR_VERSION = versionPart(1);

	static {
		try {
			DriverManager.registerDriver(new VessDriver());
		} catch (SQLException e) {
			throw new RuntimeException("Unable to register the Vess driver: " + e.getMessage());
		}
	}

	private static String version() {
		Properties properties = new Properties();
		try (InputStream in = VessDriver.class.getResourceAsStream("vess.properties")) {
			if (in != null) {
				properties.load(in);
			}
		} catch (IOException e) {
			// Reported as 0.0 below
		}
		String version = properties.getProperty("version", "0.0");
		return version.startsWith("$") ? "0.0" : version;
	}

	private static int versionPart(int index) {
		String[] parts = VERSION.split("[.-]");
		try {
			return index < parts.length ? Integer.parseInt(parts[index]) : 0;
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	@Override
	public Connection connect(String url, Properties info) throws SQLException {
		// Drivers are tried in turn for a URL; null says it isn't this one's
		if (!acceptsURL(url)) {
			return null;
		}
		return new VessConnection(VessUrl.parse(url), info == null ? new Properties() : info);
	}

	@Override
	public boolean acceptsURL(String url) throws SQLException {
		return VessUrl.accepts(url);
	}

	@Override
	public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
		DriverPropertyInfo user = new DriverPropertyInfo("user", info.getProperty("user"));
		user.required = true;
		DriverPropertyInfo password = new DriverPropertyInfo("password", null);
		password.required = true;
		return new DriverPropertyInfo[] { user, password,
				setting(info, VessConnection.APP, "The application to work in, so names needn't say it"),
				setting(info, VessConnection.CUBE, "The cube to work in, so names needn't say it"),
				setting(info, VessConnection.AUTH, "password, token for a bearer token as the password, or session for a session id as the password",
						"password", "token", "session"),
				setting(info, VessConnection.READ_ONLY, "true to refuse anything that would change the server", "false", "true"),
				setting(info, VessConnection.OUTLINE_CACHE, "Seconds to keep an outline before reading it again"),
				setting(info, VessConnection.CHUNK_CELLS, "The most cells one MDX query reads, before leaving out empty ones"),
				setting(info, VessConnection.CHUNK_MEMBERS, "The most members of a dimension one MDX query names"),
				setting(info, VessConnection.SERVER_CATALOG, "The name of the catalog holding the server's own tables"),
				setting(info, VessConnection.DIMENSION_PREFIX, "What each dimension's table is named with first; DIM_ unless given, and may be empty"),
				setting(info, VessConnection.DIMENSION_NAMES,
						"How names from Essbase are written as tables and columns: upper and lower make identifiers, as-is keeps Essbase's",
						"upper", "lower", "as-is"),
				setting(info, VessConnection.ALIAS_TABLE,
						"The alias table ALIAS columns show, such as Long Names; each cube's default unless given, and MDX uses it too"),
				setting(info, VessConnection.BROWSE_STOPPED,
						"true to list the data and dimension tables of stopped applications too, which starts them", "false", "true") };
	}

	private static DriverPropertyInfo setting(Properties info, String name, String description, String... choices) {
		DriverPropertyInfo setting = new DriverPropertyInfo(name, info.getProperty(name));
		setting.description = description + "; also ;" + name + "=... in the URL";
		if (choices.length > 0) {
			setting.choices = choices;
		}
		return setting;
	}

	@Override
	public int getMajorVersion() {
		return MAJOR_VERSION;
	}

	@Override
	public int getMinorVersion() {
		return MINOR_VERSION;
	}

	/**
	 * Vess speaks SQL well enough for the tools it's meant for, but an Essbase
	 * server isn't a relational database, so it doesn't claim full compliance.
	 */
	@Override
	public boolean jdbcCompliant() {
		return false;
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw new SQLFeatureNotSupportedException("Vess logs through SLF4J");
	}

}

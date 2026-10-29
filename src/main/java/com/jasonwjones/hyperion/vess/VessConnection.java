package com.jasonwjones.hyperion.vess;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;

import org.apache.calcite.jdbc.CalciteConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appliedolap.essbase.EssApiException;
import com.appliedolap.essbase.EssAuthentication;
import com.appliedolap.essbase.EssServer;
import com.appliedolap.essbase.impl.EssServerImpl;
import com.jasonwjones.hyperion.thriller.ThrillerDriver;
import com.jasonwjones.hyperion.vess.schema.ApplicationSchema;
import com.jasonwjones.hyperion.vess.schema.CubeSchema;
import com.jasonwjones.hyperion.vess.schema.Naming;
import com.jasonwjones.hyperion.vess.schema.OutlineFunctions;
import com.jasonwjones.hyperion.vess.schema.ServerContext;
import com.jasonwjones.hyperion.vess.schema.ServerSchema;
import com.jasonwjones.hyperion.vess.schema.VessModel;
import com.jasonwjones.hyperion.vess.schema.VessTable;

/**
 * A login to an Essbase server, presented as a database. SQL runs on an
 * embedded SQL engine whose tables are the server's applications, cubes,
 * outlines and data; MDX goes straight to Essbase, through Thriller. There are
 * no transactions: each statement takes effect as it runs.
 */
public class VessConnection implements Connection {

	static final String APP = "app";

	static final String CUBE = "cube";

	static final String AUTH = "auth";

	static final String READ_ONLY = "readonly";

	static final String OUTLINE_CACHE = "outline.cache";

	static final String CHUNK_CELLS = "chunk.cells";

	static final String CHUNK_MEMBERS = "chunk.members";

	static final String SERVER_CATALOG = "server.catalog";

	static final String BROWSE_STOPPED = "browse.stopped";

	static final String DIMENSION_NAMES = "dimension.names";

	static final String DIMENSION_PREFIX = "dimension.prefix";

	/** Shared with Thriller, which is given it too */
	static final String ALIAS_TABLE = "alias.table";

	/** A WebLogic cookie to present with a session id, as Thriller takes it */
	static final String WEBLOGIC_COOKIE = "weblogic.cookie";

	/** Settings for Vess itself, which Thriller isn't given */
	private static final Set<String> VESS_SETTINGS = Set.of(APP, CUBE, READ_ONLY, OUTLINE_CACHE, CHUNK_CELLS, CHUNK_MEMBERS, SERVER_CATALOG,
			BROWSE_STOPPED, DIMENSION_NAMES, DIMENSION_PREFIX);

	private static final Logger logger = LoggerFactory.getLogger(VessConnection.class);

	private final VessUrl url;

	private final String user;

	private final String password;

	private final Map<String, String> settings;

	private final EssServer server;

	private final String serverVersion;

	private final ServerContext context;

	private final Connection calcite;

	private final VessModel model;

	private Connection thriller;

	private boolean readOnly;

	private String catalog;

	private String schema;

	private boolean closed;

	VessConnection(VessUrl url, Properties info) throws SQLException {
		this.url = url;
		this.user = info.getProperty("user");
		this.password = info.getProperty("password");
		this.settings = settings(url, info);
		this.server = server(url, settings, user, password);
		try {
			// Asked now rather than on the first query, so that a wrong address
			// or password fails when connecting, where tools expect it
			this.serverVersion = server.getAbout().getVersion();
		} catch (EssApiException e) {
			if (unreachable(e)) {
				throw new VessException("Could not reach Essbase at " + url.getServerUrl() + ": " + e.getMessage(), e);
			}
			throw new VessException("Could not log in to " + url.getServerUrl() + (user == null ? "" : " as " + user) + ": " + e.getMessage(), e);
		}
		this.readOnly = Boolean.parseBoolean(settings.getOrDefault(READ_ONLY, "false"));
		this.context = new ServerContext(server, url.getServerUrl(), user, () -> this.readOnly, contextSettings(settings));
		this.calcite = Engine.connect();
		this.model = new VessModel(context, calcite.unwrap(CalciteConnection.class).getRootSchema());
		context.setMdxQuery(query -> thriller().createStatement().executeQuery(query));
		if (settings.containsKey(APP)) {
			setCatalog(settings.get(APP));
			if (settings.containsKey(CUBE)) {
				setSchema(settings.get(CUBE));
			}
		}
		logger.info("Connected to Essbase {} at {}", serverVersion, url.getServerUrl());
	}

	/**
	 * A server to log in to the way the auth setting says: with the user and
	 * password, the default; with the password as a bearer token; or with the
	 * password as the id of a session established elsewhere. Neither of the
	 * last two needs a user.
	 */
	static EssServer server(VessUrl url, Map<String, String> settings, String user, String password) throws VessException {
		String auth = settings.getOrDefault(AUTH, "password").trim().toLowerCase(Locale.ROOT);
		switch (auth) {
		case "password":
			if (user == null || user.isEmpty() || password == null) {
				throw new VessException("Vess needs a user and password to log in to " + url.getServerUrl());
			}
			return new EssServerImpl(url.getServerUrl(), user, password);
		case "token":
			if (password == null || password.isEmpty()) {
				throw new VessException("With auth=token, the password is the bearer token, and there isn't one");
			}
			return new EssServerImpl(url.getServerUrl(), EssAuthentication.bearerToken(password));
		case "session":
			if (password == null || password.isEmpty()) {
				throw new VessException("With auth=session, the password is the session id (JSESSIONID), and there isn't one");
			}
			return new EssServerImpl(url.getServerUrl(), EssAuthentication.sessionCookie(password, settings.get(WEBLOGIC_COOKIE)));
		default:
			throw new VessException("auth can be password, token or session, not '" + auth + "'");
		}
	}

	/**
	 * The connection's settings: its properties, which is where a tool's driver
	 * settings put them, then the URL's, which win. The user and password
	 * aren't settings.
	 */
	static Map<String, String> settings(VessUrl url, Properties info) {
		Map<String, String> settings = new LinkedHashMap<>();
		for (String name : info.stringPropertyNames()) {
			if (!name.equals("user") && !name.equals("password")) {
				settings.put(name, info.getProperty(name));
			}
		}
		settings.putAll(url.getProperties());
		return Collections.unmodifiableMap(settings);
	}

	static ServerContext.Settings contextSettings(Map<String, String> settings) throws VessException {
		ServerContext.Settings defaults = ServerContext.Settings.DEFAULTS;
		try {
			Duration outlineCache = settings.containsKey(OUTLINE_CACHE) ? Duration.ofSeconds(Long.parseLong(settings.get(OUTLINE_CACHE).trim()))
					: defaults.outlineCache();
			long chunkCells = settings.containsKey(CHUNK_CELLS) ? Long.parseLong(settings.get(CHUNK_CELLS).trim()) : defaults.chunkCells();
			int chunkMembers = settings.containsKey(CHUNK_MEMBERS) ? Integer.parseInt(settings.get(CHUNK_MEMBERS).trim())
					: defaults.chunkMembers();
			if (chunkCells < 1 || chunkMembers < 1) {
				throw new VessException(CHUNK_CELLS + " and " + CHUNK_MEMBERS + " must be at least 1");
			}
			Naming.Style style;
			try {
				style = settings.containsKey(DIMENSION_NAMES) ? Naming.Style.parse(settings.get(DIMENSION_NAMES)) : defaults.naming().style();
			} catch (IllegalArgumentException e) {
				throw new VessException(DIMENSION_NAMES + " " + e.getMessage(), e);
			}
			Naming naming = new Naming(style, settings.getOrDefault(DIMENSION_PREFIX, defaults.naming().dimensionPrefix()).trim());
			return new ServerContext.Settings(outlineCache, chunkCells, chunkMembers,
					settings.getOrDefault(SERVER_CATALOG, defaults.serverCatalog()),
					Boolean.parseBoolean(settings.getOrDefault(BROWSE_STOPPED, String.valueOf(defaults.browseStopped()))), naming,
					settings.containsKey(ALIAS_TABLE) && !settings.get(ALIAS_TABLE).isBlank() ? settings.get(ALIAS_TABLE).trim() : null);
		} catch (NumberFormatException e) {
			throw new VessException(OUTLINE_CACHE + ", " + CHUNK_CELLS + " and " + CHUNK_MEMBERS + " take whole numbers: " + e.getMessage(), e);
		}
	}

	/**
	 * Whether a failure was the server not answering at all, where the message
	 * is just an exception's name, so it helps to say what it means.
	 */
	private static boolean unreachable(Throwable failure) {
		String message = failure.getMessage();
		return message != null && (message.contains("ConnectException") || message.contains("UnknownHostException")
				|| message.contains("HttpTimeoutException"));
	}

	VessUrl getUrl() {
		return url;
	}

	String getUser() {
		return user;
	}

	String getServerVersion() {
		return serverVersion;
	}

	/**
	 * @return the embedded engine's connection, which runs the SQL
	 */
	Connection getEngine() {
		return calcite;
	}

	VessModel getModel() {
		return model;
	}

	ServerContext getContext() {
		return context;
	}

	/**
	 * @return this connection, as what INSERT, UPDATE and DELETE change
	 */
	Dml.Target dmlTarget() {
		return new Dml.Target() {

			@Override
			public Connection engine() {
				return calcite;
			}

			@Override
			public VessTable table(List<String> names) throws SQLException {
				return model.table(names, catalog, schema);
			}

			@Override
			public boolean isReadOnly() {
				return readOnly;
			}

		};
	}

	/**
	 * A Thriller connection with the same login, for MDX, made the first time
	 * MDX is run. The settings Thriller knows, such as members=alias, are
	 * passed on.
	 */
	synchronized Connection thriller() throws SQLException {
		checkOpen();
		if (thriller == null || thriller.isClosed()) {
			Properties info = new Properties();
			for (Map.Entry<String, String> setting : settings.entrySet()) {
				if (!VESS_SETTINGS.contains(setting.getKey())) {
					info.setProperty(setting.getKey(), setting.getValue());
				}
			}
			if (user != null) {
				info.setProperty("user", user);
			}
			if (password != null) {
				info.setProperty("password", password);
			}
			if (catalog != null && model.application(catalog) != null) {
				info.setProperty(APP, catalog);
				if (schema != null) {
					info.setProperty(CUBE, schema);
				}
			}
			thriller = new ThrillerDriver().connect(ThrillerDriver.START_URL + url.getServerUrl(), info);
		}
		return thriller;
	}

	private void checkOpen() throws SQLException {
		if (closed) {
			throw new VessException("This connection has been closed");
		}
	}

	@Override
	public Statement createStatement() throws SQLException {
		checkOpen();
		return new VessStatement(this);
	}

	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
		return createStatement();
	}

	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
		return createStatement();
	}

	@Override
	public PreparedStatement prepareStatement(String sql) throws SQLException {
		checkOpen();
		return new VessPreparedStatement(this, sql);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
		return prepareStatement(sql);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		return prepareStatement(sql);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
		return prepareStatement(sql);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
		return prepareStatement(sql);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
		return prepareStatement(sql);
	}

	@Override
	public CallableStatement prepareCall(String sql) throws SQLException {
		checkOpen();
		return new VessCallableStatement(this, sql);
	}

	@Override
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
		return prepareCall(sql);
	}

	@Override
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		return prepareCall(sql);
	}

	@Override
	public DatabaseMetaData getMetaData() throws SQLException {
		checkOpen();
		return new VessDatabaseMetaData(this);
	}

	@Override
	public void close() throws SQLException {
		if (closed) {
			return;
		}
		closed = true;
		OutlineFunctions.forget(context.getId());
		try {
			calcite.close();
		} finally {
			synchronized (this) {
				if (thriller != null) {
					thriller.close();
				}
			}
		}
	}

	@Override
	public boolean isClosed() throws SQLException {
		return closed;
	}

	/**
	 * Whether the connection is open and the server still answers. Tools call
	 * this to check a connection before they use it.
	 */
	@Override
	public boolean isValid(int timeout) throws SQLException {
		if (timeout < 0) {
			throw new VessException("The timeout can't be negative: " + timeout);
		}
		if (closed) {
			return false;
		}
		try {
			server.getAbout();
			return true;
		} catch (EssApiException e) {
			logger.debug("The connection is no longer valid: {}", e.getMessage());
			return false;
		}
	}

	@Override
	public String nativeSQL(String sql) throws SQLException {
		return sql;
	}

	@Override
	public boolean getAutoCommit() throws SQLException {
		return true;
	}

	/**
	 * Each statement takes effect as it runs, whatever this is set to, since
	 * Essbase has nothing to commit or roll back.
	 */
	@Override
	public void setAutoCommit(boolean autoCommit) throws SQLException {
	}

	@Override
	public void commit() throws SQLException {
	}

	@Override
	public void rollback() throws SQLException {
	}

	@Override
	public boolean isReadOnly() throws SQLException {
		return readOnly;
	}

	/**
	 * A read-only connection refuses anything that would change the server:
	 * writing to a table, running a calc, starting or stopping an application.
	 */
	@Override
	public void setReadOnly(boolean readOnly) throws SQLException {
		checkOpen();
		this.readOnly = readOnly;
	}

	@Override
	public int getTransactionIsolation() throws SQLException {
		return Connection.TRANSACTION_NONE;
	}

	@Override
	public void setTransactionIsolation(int level) throws SQLException {
	}

	@Override
	public String getCatalog() throws SQLException {
		return catalog;
	}

	/**
	 * Chooses the application whose cubes unqualified names refer to, which
	 * tools do when a schema is picked to work in.
	 */
	@Override
	public void setCatalog(String catalog) throws SQLException {
		checkOpen();
		if (catalog == null || catalog.isEmpty()) {
			this.catalog = null;
			this.schema = null;
			calcite.setSchema(null);
			return;
		}
		ApplicationSchema application = model.application(catalog);
		if (application != null) {
			this.catalog = application.getName();
		} else if (catalog.equalsIgnoreCase(context.getSettings().serverCatalog())) {
			this.catalog = context.getSettings().serverCatalog();
		} else {
			throw new VessException("There's no application named " + catalog);
		}
		this.schema = null;
		calcite.setSchema(this.catalog);
	}

	@Override
	public String getSchema() throws SQLException {
		return schema;
	}

	/**
	 * Chooses the cube unqualified names refer to, within the application
	 * chosen with setCatalog.
	 */
	@Override
	public void setSchema(String schema) throws SQLException {
		checkOpen();
		if (schema == null || schema.isEmpty()) {
			this.schema = null;
			calcite.setSchema(catalog);
			return;
		}
		if (catalog == null) {
			// An application and a cube together, as tools without catalogs give them
			int dot = schema.indexOf('.');
			if (dot > 0) {
				setCatalog(schema.substring(0, dot));
				setSchema(schema.substring(dot + 1));
				return;
			}
			throw new VessException("Choose the application (the catalog) before the cube (the schema)");
		}
		if (catalog.equalsIgnoreCase(context.getSettings().serverCatalog())) {
			if (!schema.equalsIgnoreCase(ServerSchema.NAME)) {
				throw new VessException("The " + catalog + " catalog has just the " + ServerSchema.NAME + " schema");
			}
			this.schema = ServerSchema.NAME;
			calcite.setSchema(Engine.alias(model, catalog, ServerSchema.NAME, model.getServerSchema()));
			return;
		}
		CubeSchema cube = model.application(catalog).cube(schema);
		if (cube == null) {
			throw new VessException("There's no cube named " + schema + " in " + catalog);
		}
		this.schema = cube.getSchemaName();
		calcite.setSchema(Engine.alias(model, catalog, this.schema, cube));
	}

	@Override
	public int getHoldability() throws SQLException {
		return ResultSet.CLOSE_CURSORS_AT_COMMIT;
	}

	@Override
	public void setHoldability(int holdability) throws SQLException {
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		return null;
	}

	@Override
	public void clearWarnings() throws SQLException {
	}

	@Override
	public Map<String, Class<?>> getTypeMap() throws SQLException {
		return Collections.emptyMap();
	}

	@Override
	public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
	}

	@Override
	public String getClientInfo(String name) throws SQLException {
		return null;
	}

	@Override
	public Properties getClientInfo() throws SQLException {
		return new Properties();
	}

	@Override
	public void setClientInfo(String name, String value) throws SQLClientInfoException {
	}

	@Override
	public void setClientInfo(Properties properties) throws SQLClientInfoException {
	}

	@Override
	public int getNetworkTimeout() throws SQLException {
		return 0;
	}

	@Override
	public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
	}

	@Override
	public void abort(Executor executor) throws SQLException {
		close();
	}

	@Override
	public Savepoint setSavepoint() throws SQLException {
		throw new SQLFeatureNotSupportedException("Essbase has no transactions");
	}

	@Override
	public Savepoint setSavepoint(String name) throws SQLException {
		return setSavepoint();
	}

	@Override
	public void rollback(Savepoint savepoint) throws SQLException {
		throw new SQLFeatureNotSupportedException("Essbase has no transactions");
	}

	@Override
	public void releaseSavepoint(Savepoint savepoint) throws SQLException {
		throw new SQLFeatureNotSupportedException("Essbase has no transactions");
	}

	@Override
	public Clob createClob() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Blob createBlob() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public NClob createNClob() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public SQLXML createSQLXML() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if (isWrapperFor(iface)) {
			return (T) this;
		}
		throw new VessException("Cannot cast to " + iface.getName());
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return iface.isAssignableFrom(getClass());
	}

}

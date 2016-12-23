package com.saxifrages.vess.h2vess;

import java.io.InputStream;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import org.h2.engine.ConnectionInfo;
import org.h2.engine.Database;
import org.h2.engine.Engine;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.jdbc.JdbcConnection;
import org.h2.schema.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.essbase.api.datasource.IEssOlapServer;
import com.saxifrages.vess.essbuilder.EssBuilder;
import com.saxifrages.vess.h2.EssTableFactory;
import com.saxifrages.vess.h2.SimpleEssService;
import com.saxifrages.vess.h2.essconn.EssConnectionManager;
import com.saxifrages.vess.util.EssbaseUrl;

public class VessConnection implements Connection {

	private static final Logger logger = LoggerFactory.getLogger(VessConnection.class);

	private IEssOlapServer server;

	private JdbcConnection h2Connection;

	private Properties info;

	private ConnectionInfo connInfo;

	private VessDatabaseMetaData metaData;

	private DatabaseObjectAdder objectAdder;

	public VessConnection() {
		logger.info("Simple init in VessConn");
	}

	public VessConnection(EssbaseUrl essUrl, Properties info) throws SQLException {
		logger.info("Inside VessConnection");
		System.out.println("Locale is: " + Locale.getDefault());
		try {
			this.info = info;
			// String username = info.getProperty("user");
			// String password = info.getProperty("password");
			String username = essUrl.getUsername();
			String password = essUrl.getPassword();

			Map<String, String> urlProperties = essUrl.getProperties();
			logger.info("Additional properties: {}", urlProperties);
			
			InputStream inputStream = null;
			ClassLoader classLoader = this.getClass().getClassLoader();
			inputStream = classLoader.getResourceAsStream("essbase.properties");

			System.out.println("Input stream: " + inputStream);

			System.out.println("Initializing Essbase Java API");
			EssConnectionManager connManager = EssConnectionManager.getInstance();

			System.out.println("Connecting to URL: " + essUrl);

			server = connManager.getServer(essUrl.getHostname(), username, password);

			System.out.println("Signing on to " + essUrl.getHostname() + " using " + username + "/" + password);

			System.out.println("Is connected: " + server.isConnected());

			SimpleEssService service = new SimpleEssService(server);
			connInfo = new ConnectionInfo("jdbc:h2:mem:vess", info);
			Engine engine = Engine.getInstance();

			Session session = engine.createSession(connInfo);
			// System.out.println("Autocommit: " + session.getAutoCommit());
			// Prepared drop =
			// session.prepare("DROP SCHEMA IF EXISTS VESS_SCHEMA");
			// Command dropSchema =
			// session.prepareLocal("DROP SCHEMA IF EXISTS VESS_SCHEMA");
			// dropSchema.executeUpdate();
			// session.commit(true);
			// ResultInterface result = drop.equals(obj)

			Database database = session.getDatabase();

			User user = new User(database, 1000, "ESSADMIN", true);
			user.setSaltAndHash(new byte[] { 8, 2 }, new byte[] { 1, 7 });
			Schema schema = new Schema(database, 1001, "VESS_SCHEMA", user, false);
			EssTableFactory tableFactory = new EssTableFactory(2000);

//			EssSubstitutionVariableModel variableModel = new EssSubstitutionVariableModel();
//			variableModel.setService(service);

//			DatabaseObjectAdder objectAdder = new DatabaseObjectAdder(database, session);

			// Server properties
//			EssGenericPropsModel genericProps = new EssGenericPropsModel(service, service.getServer());
//			EssTable genericPropsTable = tableFactory.createTable(genericProps, "PROPS", schema);
			// objectAdder.add(genericPropsTable);

			// if (database.getAllS)

			// database.addSchemaObject(session, genericPropsTable);

			if (database.findUser(user.getName()) == null) {
				database.addDatabaseObject(session, user);
			}

			String app = essUrl.getApplication();

			EssBuilder essBuilder = new EssBuilder(service, session, tableFactory, database);

			int schemaId = 3000;
			for (String application : Arrays.asList(app)) {
				Schema appSchema = new Schema(database, schemaId, application.toUpperCase(), user, false);
				schemaId += 1000;

				// We add the schema so that it shows as part of the database
				// (catalog) metadata
				// but we want the other stuff to be populated on demand
				if (database.findSchema(appSchema.getName()) == null) {
					logger.info("Adding app {}", app);
					database.addDatabaseObject(session, appSchema);
					essBuilder.buildApplicationSchema(appSchema, application);
					
				}

			}

			essBuilder.buildServerSchema(schema);

			// also has a constructor that takes a ConncetionInfo object
			h2Connection = new JdbcConnection(session, "sa", "foo");
			metaData = new VessDatabaseMetaData(h2Connection.getMetaData());
			
			// TODO: this should be getting called in something else to ensure cleanup happens
			server.clearActive();
		} catch (Exception e) {
			System.out.println("Issue with establishing VessConnection: " + e.getMessage());
			throw new SQLException("Error connecting to Essbase: " + e.getMessage(), e);
		}

	}

	@Override
	public Statement createStatement() throws SQLException {
		return h2Connection.createStatement();
	}

	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
		return h2Connection.createStatement(resultSetType, resultSetConcurrency);
	}

	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		return h2Connection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	@Override
	public PreparedStatement prepareStatement(String sql) throws SQLException {
		return h2Connection.prepareStatement(sql);
	}

	@Override
	public DatabaseMetaData getMetaData() throws SQLException {
		return metaData;
	}

	@Override
	public void close() throws SQLException {
		System.out.println("Vess connection being closed, not relaying to H2: " + h2Connection);
		try {
			if (server != null) {
				System.out.println("Disconnecting from Essbase server");
				server.disconnect();
				server = null;
			}

			// if (essbase != null) {
			// essbase.signOff();
			// essbase = null;
			// }

			// Engine engine = Engine.getInstance();
			// Session session = engine.createSession(connInfo);
			//
			// Database database = session.getDatabase();
			// //database.re
			// System.out.println("Dropping schema");
			// session.unlockReadLocks();

			// Command dropSchema =
			// session.prepareLocal("DROP SCHEMA VESS_SCHEMA");
			// dropSchema.executeUpdate();
			// System.out.println("Dropped schema");
			//
			// Command dropUser = session.prepareLocal("DROP USER ESSADMIN");
			// dropUser.executeUpdate();
			// System.out.println("Dropped admin user");

		} catch (Exception e) {
			System.out.println("Error signing off of Essbase: " + e.getMessage());

		}

		// Closing the H2 connection is disabled for now because it throws an
		// NPE, seemingly related to the fact that it calls a method on its
		// private member variable 'watcher'. The constructor we use to make
		// the JdbcConnection object does not initialize watcher so it's just
		// null.
		//
		// Since it's just a memory cube anyway who gives a shit if we close...

		// h2Connection.close();
	}

	///
	
	@Override
	protected void finalize() throws Throwable {
		System.out.println("Finalizer called");
		super.finalize();
	}

	@Override
	public void setAutoCommit(boolean autoCommit) throws SQLException {
		h2Connection.setAutoCommit(autoCommit);
	}

	@Override
	public boolean getAutoCommit() throws SQLException {
		return h2Connection.getAutoCommit();
	}

	@Override
	public void commit() throws SQLException {
		h2Connection.commit();
	}

	@Override
	public void rollback() throws SQLException {
		h2Connection.rollback();
	}

	@Override
	public boolean isClosed() throws SQLException {
		return h2Connection.isClosed();
	}

	@Override
	public String nativeSQL(String sql) throws SQLException {
		return h2Connection.nativeSQL(sql);
	}

	@Override
	public void setReadOnly(boolean readOnly) throws SQLException {
		h2Connection.setReadOnly(readOnly);
	}

	@Override
	public boolean isReadOnly() throws SQLException {
		return h2Connection.isReadOnly();
	}

	@Override
	public void setCatalog(String catalog) throws SQLException {
		h2Connection.setCatalog(catalog);
	}

	@Override
	public String getCatalog() throws SQLException {
		return h2Connection.getCatalog();
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		return h2Connection.getWarnings();
	}

	@Override
	public void clearWarnings() throws SQLException {
		h2Connection.clearWarnings();
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
			throws SQLException {
		return h2Connection.prepareStatement(sql, resultSetType, resultSetConcurrency);
	}

	@Override
	public void setTransactionIsolation(int level) throws SQLException {
		h2Connection.setTransactionIsolation(level);
	}

	@Override
	public int getTransactionIsolation() throws SQLException {
		return h2Connection.getTransactionIsolation();
	}

	@Override
	public void setHoldability(int holdability) throws SQLException {
		h2Connection.setHoldability(holdability);
	}

	@Override
	public int getHoldability() throws SQLException {
		return h2Connection.getHoldability();
	}

	@Override
	public Map<String, Class<?>> getTypeMap() throws SQLException {
		return h2Connection.getTypeMap();
	}

	@Override
	public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
		h2Connection.setTypeMap(map);
	}

	@Override
	public CallableStatement prepareCall(String sql) throws SQLException {
		return h2Connection.prepareCall(sql);
	}

	@Override
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
		return h2Connection.prepareCall(sql, resultSetType, resultSetConcurrency);
	}

	@Override
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
		return h2Connection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	@Override
	public Savepoint setSavepoint() throws SQLException {
		return h2Connection.setSavepoint();
	}

	@Override
	public Savepoint setSavepoint(String name) throws SQLException {
		return h2Connection.setSavepoint(name);
	}

	@Override
	public void rollback(Savepoint savepoint) throws SQLException {
		h2Connection.rollback(savepoint);
	}

	@Override
	public void releaseSavepoint(Savepoint savepoint) throws SQLException {
		h2Connection.releaseSavepoint(savepoint);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
		return h2Connection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
		return h2Connection.prepareStatement(sql, autoGeneratedKeys);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
		return h2Connection.prepareStatement(sql, columnIndexes);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
		return h2Connection.prepareStatement(sql, columnNames);
	}

	@Override
	public Clob createClob() throws SQLException {
		return h2Connection.createClob();
	}

	@Override
	public Blob createBlob() throws SQLException {
		return h2Connection.createBlob();
	}

	@Override
	public NClob createNClob() throws SQLException {
		return h2Connection.createNClob();
	}

	@Override
	public SQLXML createSQLXML() throws SQLException {
		return h2Connection.createSQLXML();
	}

	@Override
	public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
		return h2Connection.createArrayOf(typeName, elements);
	}

	@Override
	public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
		return h2Connection.createStruct(typeName, attributes);
	}

	@Override
	public boolean isValid(int timeout) {
		return h2Connection.isValid(timeout);
	}

	@Override
	public void setClientInfo(String name, String value) throws SQLClientInfoException {
		h2Connection.setClientInfo(name, value);
	}

	@Override
	public void setClientInfo(Properties properties) throws SQLClientInfoException {
		h2Connection.setClientInfo(properties);
	}

	@Override
	public Properties getClientInfo() throws SQLException {
		return h2Connection.getClientInfo();
	}

	@Override
	public String getClientInfo(String name) throws SQLException {
		return h2Connection.getClientInfo(name);
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return h2Connection.unwrap(iface);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return h2Connection.isWrapperFor(iface);
	}

	// These methods showed up in Java 1.7. H2 Database doesn't support them,
	// and trying to call them causes a compilation error.

	@Override
	public void setSchema(String schema) throws SQLException {
		// h2Connection.setSchema(schema);
	}

	@Override
	public String getSchema() throws SQLException {
		return null;
		// return h2Connection.getSchema();
	}

	@Override
	public void abort(Executor executor) throws SQLException {
		// h2Connection.abort(executor);
	}

	@Override
	public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
		// h2Connection.setNetworkTimeout(executor, milliseconds);
	}

	@Override
	public int getNetworkTimeout() throws SQLException {
		return 0;
		// return h2Connection.getNetworkTimeout();
	}

}

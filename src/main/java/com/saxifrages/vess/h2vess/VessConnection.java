package com.saxifrages.vess.h2vess;

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

import com.essbase.api.base.EssException;
import com.essbase.api.datasource.IEssOlapServer;
import com.essbase.api.session.EssbaseProperties;
import com.saxifrages.vess.h2.EssModel;
import com.saxifrages.vess.h2.EssTable;
import com.saxifrages.vess.h2.EssTableFactory;
import com.saxifrages.vess.h2.SimpleEssService;
import com.saxifrages.vess.h2.essconn.EssConnectionManager;
import com.saxifrages.vess.h2.models.EssCubeAliasTableNamesModel;
import com.saxifrages.vess.h2.models.EssSubstitutionVariableModel;
import com.saxifrages.vess.util.EssbaseUrl;

public class VessConnection implements Connection {

	private IEssOlapServer server;

	private JdbcConnection h2Connection;
	
	private Properties info;
	
	private ConnectionInfo connInfo;
	
	public VessConnection(EssbaseUrl essUrl, Properties info) throws SQLException {
		try {
			this.info = info;
			String username = info.getProperty("user");
			String password = info.getProperty("password");
			
			//System.setProperty("ESS_ES_HOME", "/Users/jasonwjones/test");
			
			System.out.println("Using server class loader: " + EssbaseProperties.useServerClassLoader());
			
			System.out.println("Initializing Essbase Java API");
			
			EssConnectionManager connManager = EssConnectionManager.getInstance();
						
			System.out.println("Connecting to URL: " + essUrl);
			
			server = connManager.getServer(essUrl.getHostname(), username, password);
			
			System.out.println("Signing on to " + essUrl.getHostname() + " using " + username + "/" + password);
			
			System.out.println("Is connected: " + server.isConnected());
			System.out.println("Is connected: " + server.isConnected());
			SimpleEssService service = new SimpleEssService(server);
			connInfo = new ConnectionInfo("jdbc:h2:mem:vess", info);
			Engine engine = Engine.getInstance();
			

			//
			Session session = engine.createSession(connInfo);
//			System.out.println("Autocommit: " + session.getAutoCommit());
			//Prepared drop = session.prepare("DROP SCHEMA IF EXISTS VESS_SCHEMA");
//			Command dropSchema = session.prepareLocal("DROP SCHEMA IF EXISTS VESS_SCHEMA");
//			dropSchema.executeUpdate();
			//session.commit(true);
			//ResultInterface result = drop.equals(obj)
			

			Database database = session.getDatabase();
			// EssService service = new TempEssService();
			// EssService service = new SimpleEssService();

			User user = new User(database, 1000, "ESSADMIN", true);
			user.setSaltAndHash(new byte[] { 8, 2 }, new byte[] { 1, 7 });
			Schema schema = new Schema(database, 1001, "VESS_SCHEMA", user, false);
			EssTableFactory tableFactory = new EssTableFactory(schema, 2000);
			EssSubstitutionVariableModel variableModel = new EssSubstitutionVariableModel();
			variableModel.setService(service);

			
			if (database.findUser(user.getName()) == null) {
				database.addDatabaseObject(session, user);
			}

			String app = essUrl.getApplication().toUpperCase();
			String db = essUrl.getDatabase().toUpperCase();
			String appDb = app + "_" + db + "_DATA";
			
			if (database.findSchema(schema.getName()) == null) {
				database.addDatabaseObject(session, schema);
			
				EssTable variableTable = tableFactory.createTable(variableModel, "VARS");
				database.addSchemaObject(session, variableTable);
	
				EssCubeAliasTableNamesModel aliasTableModel = new EssCubeAliasTableNamesModel(service, app, db);
				EssTable aliasTableTable = tableFactory.createTable(aliasTableModel, app + "_" + db + "_ALIAS_TABLES");
				database.addSchemaObject(session, aliasTableTable);
				
				try {
					EssModel dataLoadModel = service.createCubeModel(essUrl.getApplication(), essUrl.getDatabase());
					EssTable sampleFact = tableFactory.createTable(dataLoadModel, appDb);
					database.addSchemaObject(session, sampleFact);

				} catch (EssException e) {
					throw new SQLException("Essbase exception: " + e.getMessage(), e);
				}
			}
			
			Schema vessSchema = database.findSchema(schema.getName());
			//Table dataTable = vessSchema.findTableOrView(session, "VARS")
			
			//if (database.find)
			
			h2Connection = new JdbcConnection(session, "sa", "foo");
			//h2Connection = new JdbcConnection(connInfo, true);
		} catch (EssException e) {
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
		return new VessDatabaseMetaData(h2Connection.getMetaData());
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
//			if (essbase != null) {
//				essbase.signOff();
//				essbase = null;
//			}
			
			//Engine engine = Engine.getInstance();
			//Session session = engine.createSession(connInfo);
//
//			Database database = session.getDatabase();
//			//database.re
			//System.out.println("Dropping schema");
			//session.unlockReadLocks();
		
//			Command dropSchema = session.prepareLocal("DROP SCHEMA VESS_SCHEMA");
//			dropSchema.executeUpdate();
//			System.out.println("Dropped schema");
//					
//			Command dropUser = session.prepareLocal("DROP USER ESSADMIN");
//			dropUser.executeUpdate();
//			System.out.println("Dropped admin user");

			
		} catch (EssException e) {
			System.out.println("Error signing off of Essbase: " + e.getMessage());
			
		}
		
		// Closing the H2 connection is disabled for now because it throws an
		// NPE, seemingly related to the fact that it calls a method on its
		// private member variable 'watcher'. The constructor we use to make
		// the JdbcConnection object does not initialize watcher so it's just
		// null.
		//
		// Since it's just a memory cube anyway who gives a shit if we close...
		
		//h2Connection.close();
	}
	
	

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

	@Override
	public void setSchema(String schema) throws SQLException {
		h2Connection.setSchema(schema);
	}

	@Override	
	public String getSchema() throws SQLException {
		return h2Connection.getSchema();
	}

	@Override
	public void abort(Executor executor) throws SQLException {
		h2Connection.abort(executor);
	}

	@Override
	public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
		h2Connection.setNetworkTimeout(executor, milliseconds);
	}

	@Override
	public int getNetworkTimeout() throws SQLException {
		return h2Connection.getNetworkTimeout();
	}

}

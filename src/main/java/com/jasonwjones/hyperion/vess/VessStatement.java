package com.jasonwjones.hyperion.vess;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLTimeoutException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A statement on a Vess connection. What it runs decides who runs it: a query
 * goes to the SQL engine, MDX to Essbase through Thriller, a CALL to a
 * procedure, and INSERT, UPDATE and DELETE to the tables they change.
 */
public class VessStatement implements Statement {

	/** How the engine wraps its errors, which says more than people want */
	private static final Pattern WRAPPED = Pattern.compile("^Error while executing SQL \".*?\": (.*)$", Pattern.DOTALL);

	protected final VessConnection connection;

	private Statement delegate;

	private ResultSet resultSet;

	private int updateCount = -1;

	private int maxRows;

	private int queryTimeout;

	private int fetchSize;

	private boolean closed;

	private boolean closeOnCompletion;

	VessStatement(VessConnection connection) {
		this.connection = connection;
	}

	protected void checkOpen() throws SQLException {
		if (closed) {
			throw new VessException("This statement has been closed");
		}
		if (connection.isClosed()) {
			throw new VessException("This statement's connection has been closed");
		}
	}

	/**
	 * Clears what the last statement left, before another runs.
	 */
	protected void reset() throws SQLException {
		if (resultSet != null) {
			resultSet.close();
			resultSet = null;
		}
		if (delegate != null) {
			delegate.close();
			delegate = null;
		}
		updateCount = -1;
	}

	/**
	 * Records what a statement answered with: rows, or a count.
	 */
	protected void setResults(ResultSet resultSet, int updateCount) {
		this.resultSet = resultSet;
		this.updateCount = updateCount;
	}

	/**
	 * An error from the engine, with the engine's wrapping taken off its
	 * message, and a timeout reported as one.
	 */
	static SQLException translate(SQLException e) {
		if (e instanceof VessException || e instanceof SQLTimeoutException) {
			return e;
		}
		String message = e.getMessage();
		Throwable cause = e;
		while (cause != null) {
			if (cause instanceof VessException) {
				return (VessException) cause;
			}
			if (cause instanceof VessException.Unchecked && cause.getCause() instanceof SQLException) {
				return (SQLException) cause.getCause();
			}
			cause = cause.getCause();
		}
		if (message != null) {
			Matcher matcher = WRAPPED.matcher(message);
			if (matcher.matches()) {
				message = matcher.group(1);
			}
			if (message.contains("cancelled") || message.contains("timeout")) {
				return new SQLTimeoutException(message, e);
			}
		}
		return new VessException(message, e);
	}

	@Override
	public ResultSet executeQuery(String sql) throws SQLException {
		if (!execute(sql)) {
			throw new VessException("The statement didn't answer with rows: " + sql);
		}
		return resultSet;
	}

	@Override
	public int executeUpdate(String sql) throws SQLException {
		if (execute(sql)) {
			return 0;
		}
		return updateCount;
	}

	@Override
	public boolean execute(String sql) throws SQLException {
		checkOpen();
		reset();
		return run(sql, List.of());
	}

	/**
	 * Runs a statement with the given parameter values.
	 *
	 * @return true if it answered with rows, false if with a count
	 */
	protected boolean run(String sql, List<Object> parameters) throws SQLException {
		switch (Sql.classify(sql)) {
		case MDX:
			return runMdx(sql, parameters);
		case CALL:
			resultSet = Calls.call(connection, sql, parameters);
			return true;
		case DML:
			updateCount = Dml.execute(connection.dmlTarget(), sql, parameters);
			return false;
		default:
			return runQuery(Sql.withoutSemicolon(sql), parameters, true);
		}
	}

	private boolean runMdx(String sql, List<Object> parameters) throws SQLException {
		Connection thriller = connection.thriller();
		if (parameters.isEmpty()) {
			Statement statement = thriller.createStatement();
			configure(statement);
			delegate = statement;
			resultSet = statement.executeQuery(sql);
		} else {
			java.sql.PreparedStatement statement = thriller.prepareStatement(sql);
			configure(statement);
			for (int index = 0; index < parameters.size(); index++) {
				statement.setObject(index + 1, parameters.get(index));
			}
			delegate = statement;
			resultSet = statement.executeQuery();
		}
		return true;
	}

	private boolean runQuery(String sql, List<Object> parameters, boolean retry) throws SQLException {
		try {
			if (parameters.isEmpty()) {
				Statement statement = connection.getEngine().createStatement();
				configure(statement);
				delegate = statement;
				if (statement.execute(sql)) {
					resultSet = statement.getResultSet();
					return true;
				}
				updateCount = statement.getUpdateCount();
				return false;
			}
			java.sql.PreparedStatement statement = connection.getEngine().prepareStatement(sql);
			configure(statement);
			Parameters.bind(statement, parameters);
			delegate = statement;
			if (statement.execute()) {
				resultSet = statement.getResultSet();
				return true;
			}
			updateCount = statement.getUpdateCount();
			return false;
		} catch (SQLException e) {
			SQLException translated = translate(e);
			// An application made since connecting isn't known yet
			if (retry && translated.getMessage() != null && translated.getMessage().contains("not found")) {
				reset();
				connection.getModel().refresh();
				return runQuery(sql, parameters, false);
			}
			throw translated;
		}
	}

	private void configure(Statement statement) throws SQLException {
		if (maxRows > 0) {
			statement.setMaxRows(maxRows);
		}
		if (queryTimeout > 0) {
			statement.setQueryTimeout(queryTimeout);
		}
		if (fetchSize > 0) {
			statement.setFetchSize(fetchSize);
		}
	}

	@Override
	public void close() throws SQLException {
		if (closed) {
			return;
		}
		reset();
		closed = true;
	}

	@Override
	public int getMaxFieldSize() throws SQLException {
		return 0;
	}

	@Override
	public void setMaxFieldSize(int max) throws SQLException {
	}

	@Override
	public int getMaxRows() throws SQLException {
		return maxRows;
	}

	@Override
	public void setMaxRows(int max) throws SQLException {
		if (max < 0) {
			throw new VessException("The most rows can't be negative: " + max);
		}
		this.maxRows = max;
	}

	@Override
	public void setEscapeProcessing(boolean enable) throws SQLException {
	}

	@Override
	public int getQueryTimeout() throws SQLException {
		return queryTimeout;
	}

	@Override
	public void setQueryTimeout(int seconds) throws SQLException {
		if (seconds < 0) {
			throw new VessException("The timeout can't be negative: " + seconds);
		}
		this.queryTimeout = seconds;
	}

	@Override
	public void cancel() throws SQLException {
		Statement running = delegate;
		if (running != null) {
			running.cancel();
		}
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		return null;
	}

	@Override
	public void clearWarnings() throws SQLException {
	}

	@Override
	public void setCursorName(String name) throws SQLException {
		throw new SQLFeatureNotSupportedException("Vess has no named cursors");
	}

	@Override
	public ResultSet getResultSet() throws SQLException {
		return resultSet;
	}

	@Override
	public int getUpdateCount() throws SQLException {
		return updateCount;
	}

	@Override
	public boolean getMoreResults() throws SQLException {
		if (resultSet != null) {
			resultSet.close();
			resultSet = null;
		}
		updateCount = -1;
		return false;
	}

	@Override
	public void setFetchDirection(int direction) throws SQLException {
		if (direction != ResultSet.FETCH_FORWARD) {
			throw new SQLFeatureNotSupportedException("Vess reads results forward only");
		}
	}

	@Override
	public int getFetchDirection() throws SQLException {
		return ResultSet.FETCH_FORWARD;
	}

	@Override
	public void setFetchSize(int rows) throws SQLException {
		this.fetchSize = Math.max(0, rows);
	}

	@Override
	public int getFetchSize() throws SQLException {
		return fetchSize;
	}

	@Override
	public int getResultSetConcurrency() throws SQLException {
		return ResultSet.CONCUR_READ_ONLY;
	}

	@Override
	public int getResultSetType() throws SQLException {
		return ResultSet.TYPE_FORWARD_ONLY;
	}

	@Override
	public void addBatch(String sql) throws SQLException {
		throw new SQLFeatureNotSupportedException("Vess runs statements one at a time");
	}

	@Override
	public void clearBatch() throws SQLException {
	}

	@Override
	public int[] executeBatch() throws SQLException {
		throw new SQLFeatureNotSupportedException("Vess runs statements one at a time");
	}

	@Override
	public Connection getConnection() throws SQLException {
		return connection;
	}

	@Override
	public boolean getMoreResults(int current) throws SQLException {
		return getMoreResults();
	}

	@Override
	public ResultSet getGeneratedKeys() throws SQLException {
		return Calls.resultSet(List.of(), List.of());
	}

	@Override
	public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
		return executeUpdate(sql);
	}

	@Override
	public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
		return executeUpdate(sql);
	}

	@Override
	public int executeUpdate(String sql, String[] columnNames) throws SQLException {
		return executeUpdate(sql);
	}

	@Override
	public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
		return execute(sql);
	}

	@Override
	public boolean execute(String sql, int[] columnIndexes) throws SQLException {
		return execute(sql);
	}

	@Override
	public boolean execute(String sql, String[] columnNames) throws SQLException {
		return execute(sql);
	}

	@Override
	public int getResultSetHoldability() throws SQLException {
		return ResultSet.CLOSE_CURSORS_AT_COMMIT;
	}

	@Override
	public boolean isClosed() throws SQLException {
		return closed;
	}

	@Override
	public void setPoolable(boolean poolable) throws SQLException {
	}

	@Override
	public boolean isPoolable() throws SQLException {
		return false;
	}

	@Override
	public void closeOnCompletion() throws SQLException {
		closeOnCompletion = true;
	}

	@Override
	public boolean isCloseOnCompletion() throws SQLException {
		return closeOnCompletion;
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

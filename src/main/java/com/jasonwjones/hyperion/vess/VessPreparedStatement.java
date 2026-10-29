package com.jasonwjones.hyperion.vess;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * A statement with ? parameters. A query is prepared by the SQL engine at once,
 * so tools can ask what it will answer with before running it; MDX, CALL and
 * changes to tables take their parameters when they run.
 */
public class VessPreparedStatement extends VessStatement implements PreparedStatement {

	private final String sql;

	private final Sql.Kind kind;

	private final List<Object> parameters = new ArrayList<>();

	/** The engine's statement, for a query */
	private PreparedStatement prepared;

	VessPreparedStatement(VessConnection connection, String sql) throws SQLException {
		super(connection);
		this.sql = sql;
		this.kind = Sql.classify(sql);
		if (kind == Sql.Kind.QUERY) {
			try {
				prepared = connection.getEngine().prepareStatement(Sql.withoutSemicolon(sql));
			} catch (SQLException e) {
				throw translate(e);
			}
		}
	}

	private void set(int index, Object value) throws SQLException {
		checkOpen();
		if (index < 1) {
			throw new VessException("Parameters are numbered from 1, not " + index);
		}
		while (parameters.size() < index) {
			parameters.add(null);
		}
		parameters.set(index - 1, value);
	}

	@Override
	public ResultSet executeQuery() throws SQLException {
		if (!execute()) {
			throw new VessException("The statement didn't answer with rows: " + sql);
		}
		return getResultSet();
	}

	@Override
	public int executeUpdate() throws SQLException {
		if (execute()) {
			return 0;
		}
		return getUpdateCount();
	}

	@Override
	public boolean execute() throws SQLException {
		checkOpen();
		reset();
		if (prepared != null) {
			try {
				Parameters.bind(prepared, parameters);
				if (getMaxRows() > 0) {
					prepared.setMaxRows(getMaxRows());
				}
				if (getQueryTimeout() > 0) {
					prepared.setQueryTimeout(getQueryTimeout());
				}
				boolean rows = prepared.execute();
				setResults(rows ? prepared.getResultSet() : null, rows ? -1 : prepared.getUpdateCount());
				return rows;
			} catch (SQLException e) {
				throw translate(e);
			}
		}
		return run(sql, new ArrayList<>(parameters));
	}

	@Override
	public void close() throws SQLException {
		try {
			super.close();
		} finally {
			if (prepared != null) {
				prepared.close();
			}
		}
	}

	@Override
	public void clearParameters() throws SQLException {
		parameters.clear();
	}

	@Override
	public ResultSetMetaData getMetaData() throws SQLException {
		checkOpen();
		if (prepared != null) {
			return prepared.getMetaData();
		}
		ResultSet results = getResultSet();
		return results == null ? null : results.getMetaData();
	}

	@Override
	public ParameterMetaData getParameterMetaData() throws SQLException {
		checkOpen();
		if (prepared != null) {
			return prepared.getParameterMetaData();
		}
		int count = 0;
		for (char c : Sql.withoutQuoted(sql).toCharArray()) {
			if (c == '?') {
				count++;
			}
		}
		return new TextParameters(count);
	}

	/**
	 * Parameters whose types aren't known until the statement runs, described
	 * as text, which any value can be given as.
	 */
	private static final class TextParameters implements ParameterMetaData {

		private final int count;

		TextParameters(int count) {
			this.count = count;
		}

		@Override
		public int getParameterCount() {
			return count;
		}

		@Override
		public int isNullable(int param) {
			return parameterNullableUnknown;
		}

		@Override
		public boolean isSigned(int param) {
			return false;
		}

		@Override
		public int getPrecision(int param) {
			return 0;
		}

		@Override
		public int getScale(int param) {
			return 0;
		}

		@Override
		public int getParameterType(int param) {
			return Types.VARCHAR;
		}

		@Override
		public String getParameterTypeName(int param) {
			return "VARCHAR";
		}

		@Override
		public String getParameterClassName(int param) {
			return String.class.getName();
		}

		@Override
		public int getParameterMode(int param) {
			return parameterModeIn;
		}

		@Override
		public <T> T unwrap(Class<T> iface) throws SQLException {
			throw new VessException("Cannot cast to " + iface.getName());
		}

		@Override
		public boolean isWrapperFor(Class<?> iface) {
			return false;
		}

	}

	@Override
	public void addBatch() throws SQLException {
		throw new SQLFeatureNotSupportedException("Vess runs statements one at a time");
	}

	@Override
	public void setNull(int parameterIndex, int sqlType) throws SQLException {
		set(parameterIndex, null);
	}

	@Override
	public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
		set(parameterIndex, null);
	}

	@Override
	public void setBoolean(int parameterIndex, boolean x) throws SQLException {
		set(parameterIndex, x);
	}

	@Override
	public void setByte(int parameterIndex, byte x) throws SQLException {
		set(parameterIndex, (int) x);
	}

	@Override
	public void setShort(int parameterIndex, short x) throws SQLException {
		set(parameterIndex, (int) x);
	}

	@Override
	public void setInt(int parameterIndex, int x) throws SQLException {
		set(parameterIndex, x);
	}

	@Override
	public void setLong(int parameterIndex, long x) throws SQLException {
		set(parameterIndex, x);
	}

	@Override
	public void setFloat(int parameterIndex, float x) throws SQLException {
		set(parameterIndex, (double) x);
	}

	@Override
	public void setDouble(int parameterIndex, double x) throws SQLException {
		set(parameterIndex, x);
	}

	@Override
	public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
		set(parameterIndex, x);
	}

	@Override
	public void setString(int parameterIndex, String x) throws SQLException {
		set(parameterIndex, x);
	}

	@Override
	public void setNString(int parameterIndex, String value) throws SQLException {
		set(parameterIndex, value);
	}

	@Override
	public void setBytes(int parameterIndex, byte[] x) throws SQLException {
		throw new SQLFeatureNotSupportedException("Vess has no binary values");
	}

	@Override
	public void setDate(int parameterIndex, Date x) throws SQLException {
		set(parameterIndex, x);
	}

	@Override
	public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
		set(parameterIndex, x);
	}

	@Override
	public void setTime(int parameterIndex, Time x) throws SQLException {
		set(parameterIndex, x);
	}

	@Override
	public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
		set(parameterIndex, x);
	}

	@Override
	public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
		set(parameterIndex, x);
	}

	@Override
	public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
		set(parameterIndex, x);
	}

	@Override
	public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
		set(parameterIndex, x);
	}

	@Override
	public void setObject(int parameterIndex, Object x) throws SQLException {
		set(parameterIndex, x);
	}

	@Override
	public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
		set(parameterIndex, x);
	}

	@Override
	public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
		throw new SQLFeatureNotSupportedException("Vess takes text as strings");
	}

	@Override
	public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException("Vess takes text as strings");
	}

	@Override
	public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
		throw new SQLFeatureNotSupportedException("Vess takes text as strings");
	}

	@Override
	@Deprecated
	public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
		throw new SQLFeatureNotSupportedException("Vess takes text as strings");
	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
		throw new SQLFeatureNotSupportedException("Vess has no binary values");
	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException("Vess has no binary values");
	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
		throw new SQLFeatureNotSupportedException("Vess has no binary values");
	}

	@Override
	public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
		set(parameterIndex, read(reader));
	}

	@Override
	public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
		set(parameterIndex, read(reader));
	}

	@Override
	public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
		set(parameterIndex, read(reader));
	}

	@Override
	public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
		set(parameterIndex, read(value));
	}

	@Override
	public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
		set(parameterIndex, read(value));
	}

	@Override
	public void setClob(int parameterIndex, Clob x) throws SQLException {
		set(parameterIndex, x == null ? null : x.getSubString(1, (int) x.length()));
	}

	@Override
	public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
		set(parameterIndex, read(reader));
	}

	@Override
	public void setClob(int parameterIndex, Reader reader) throws SQLException {
		set(parameterIndex, read(reader));
	}

	@Override
	public void setNClob(int parameterIndex, NClob value) throws SQLException {
		setClob(parameterIndex, value);
	}

	@Override
	public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
		set(parameterIndex, read(reader));
	}

	@Override
	public void setNClob(int parameterIndex, Reader reader) throws SQLException {
		set(parameterIndex, read(reader));
	}

	private static String read(Reader reader) throws SQLException {
		if (reader == null) {
			return null;
		}
		StringBuilder text = new StringBuilder();
		char[] buffer = new char[4096];
		try {
			int read;
			while ((read = reader.read(buffer)) > 0) {
				text.append(buffer, 0, read);
			}
		} catch (java.io.IOException e) {
			throw new VessException("Could not read the parameter's text: " + e.getMessage(), e);
		}
		return text.toString();
	}

	@Override
	public void setRef(int parameterIndex, Ref x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setBlob(int parameterIndex, Blob x) throws SQLException {
		throw new SQLFeatureNotSupportedException("Vess has no binary values");
	}

	@Override
	public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException("Vess has no binary values");
	}

	@Override
	public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
		throw new SQLFeatureNotSupportedException("Vess has no binary values");
	}

	@Override
	public void setArray(int parameterIndex, Array x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setURL(int parameterIndex, URL x) throws SQLException {
		set(parameterIndex, x == null ? null : x.toString());
	}

	@Override
	public void setRowId(int parameterIndex, RowId x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public ResultSet executeQuery(String sql) throws SQLException {
		throw new VessException("A prepared statement runs the statement it was prepared with");
	}

	@Override
	public int executeUpdate(String sql) throws SQLException {
		throw new VessException("A prepared statement runs the statement it was prepared with");
	}

	@Override
	public boolean execute(String sql) throws SQLException {
		throw new VessException("A prepared statement runs the statement it was prepared with");
	}

}

package com.jasonwjones.hyperion.vess;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

/**
 * A CALL prepared with prepareCall. Vess's procedures answer with result sets
 * rather than OUT parameters, so this runs a call as a prepared statement does,
 * and the methods for OUT and named parameters say they aren't supported.
 */
public class VessCallableStatement extends VessPreparedStatement implements CallableStatement {

	VessCallableStatement(VessConnection connection, String sql) throws SQLException {
		super(connection, sql);
	}

	private static SQLFeatureNotSupportedException outParameters() {
		return new SQLFeatureNotSupportedException("Vess's procedures answer with a result set, not OUT or named parameters");
	}

	@Override
	public void registerOutParameter(int arg0, int arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public void registerOutParameter(int arg0, int arg1, int arg2) throws SQLException {
		throw outParameters();
	}

	@Override
	public boolean wasNull() throws SQLException {
		throw outParameters();
	}

	@Override
	public String getString(int arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public boolean getBoolean(int arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public byte getByte(int arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public short getShort(int arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public int getInt(int arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public long getLong(int arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public float getFloat(int arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public double getDouble(int arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	@Deprecated
	public BigDecimal getBigDecimal(int arg0, int arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public byte[] getBytes(int arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public Date getDate(int arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public Time getTime(int arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public Timestamp getTimestamp(int arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public Object getObject(int arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public BigDecimal getBigDecimal(int arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public Object getObject(int arg0, Map<String, Class<?>> arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public Ref getRef(int arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public Blob getBlob(int arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public Clob getClob(int arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public Array getArray(int arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public Date getDate(int arg0, Calendar arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public Time getTime(int arg0, Calendar arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public Timestamp getTimestamp(int arg0, Calendar arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public void registerOutParameter(int arg0, int arg1, String arg2) throws SQLException {
		throw outParameters();
	}

	@Override
	public void registerOutParameter(String arg0, int arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public void registerOutParameter(String arg0, int arg1, int arg2) throws SQLException {
		throw outParameters();
	}

	@Override
	public void registerOutParameter(String arg0, int arg1, String arg2) throws SQLException {
		throw outParameters();
	}

	@Override
	public URL getURL(int arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setURL(String arg0, URL arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setNull(String arg0, int arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setBoolean(String arg0, boolean arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setByte(String arg0, byte arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setShort(String arg0, short arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setInt(String arg0, int arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setLong(String arg0, long arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setFloat(String arg0, float arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setDouble(String arg0, double arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setBigDecimal(String arg0, BigDecimal arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setString(String arg0, String arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setBytes(String arg0, byte[] arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setDate(String arg0, Date arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setTime(String arg0, Time arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setTimestamp(String arg0, Timestamp arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setAsciiStream(String arg0, InputStream arg1, int arg2) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setBinaryStream(String arg0, InputStream arg1, int arg2) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setObject(String arg0, Object arg1, int arg2, int arg3) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setObject(String arg0, Object arg1, int arg2) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setObject(String arg0, Object arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setCharacterStream(String arg0, Reader arg1, int arg2) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setDate(String arg0, Date arg1, Calendar arg2) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setTime(String arg0, Time arg1, Calendar arg2) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setTimestamp(String arg0, Timestamp arg1, Calendar arg2) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setNull(String arg0, int arg1, String arg2) throws SQLException {
		throw outParameters();
	}

	@Override
	public String getString(String arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public boolean getBoolean(String arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public byte getByte(String arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public short getShort(String arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public int getInt(String arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public long getLong(String arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public float getFloat(String arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public double getDouble(String arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public byte[] getBytes(String arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public Date getDate(String arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public Time getTime(String arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public Timestamp getTimestamp(String arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public Object getObject(String arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public BigDecimal getBigDecimal(String arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public Object getObject(String arg0, Map<String, Class<?>> arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public Ref getRef(String arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public Blob getBlob(String arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public Clob getClob(String arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public Array getArray(String arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public Date getDate(String arg0, Calendar arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public Time getTime(String arg0, Calendar arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public Timestamp getTimestamp(String arg0, Calendar arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public URL getURL(String arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public RowId getRowId(int arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public RowId getRowId(String arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setRowId(String arg0, RowId arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setNString(String arg0, String arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setNCharacterStream(String arg0, Reader arg1, long arg2) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setNClob(String arg0, NClob arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setClob(String arg0, Reader arg1, long arg2) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setBlob(String arg0, InputStream arg1, long arg2) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setNClob(String arg0, Reader arg1, long arg2) throws SQLException {
		throw outParameters();
	}

	@Override
	public NClob getNClob(int arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public NClob getNClob(String arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setSQLXML(String arg0, SQLXML arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public SQLXML getSQLXML(int arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public SQLXML getSQLXML(String arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public String getNString(int arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public String getNString(String arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public Reader getNCharacterStream(int arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public Reader getNCharacterStream(String arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public Reader getCharacterStream(int arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public Reader getCharacterStream(String arg0) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setBlob(String arg0, Blob arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setClob(String arg0, Clob arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setAsciiStream(String arg0, InputStream arg1, long arg2) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setBinaryStream(String arg0, InputStream arg1, long arg2) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setCharacterStream(String arg0, Reader arg1, long arg2) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setAsciiStream(String arg0, InputStream arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setBinaryStream(String arg0, InputStream arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setCharacterStream(String arg0, Reader arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setNCharacterStream(String arg0, Reader arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setClob(String arg0, Reader arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setBlob(String arg0, InputStream arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public void setNClob(String arg0, Reader arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public <T> T getObject(int arg0, Class<T> arg1) throws SQLException {
		throw outParameters();
	}

	@Override
	public <T> T getObject(String arg0, Class<T> arg1) throws SQLException {
		throw outParameters();
	}

}

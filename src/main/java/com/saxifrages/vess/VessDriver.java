package com.saxifrages.vess;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import com.jasonwjones.jolo.ResultSetTablePrinter;
import com.saxifrages.vess.h2vess.VessConnection;
import com.saxifrages.vess.util.EssbaseUrl;

public class VessDriver implements Driver {

	static {
		System.out.println("Static driver registration (Vess)");
		try {
			java.sql.DriverManager.registerDriver(new VessDriver());
		} catch (SQLException E) {
			throw new RuntimeException("Can't register driver!");
		}

		DriverPropertyInfo mode = new DriverPropertyInfo("mode", "embedded");
		mode.choices = new String[] { "embedded", "server" };

		DriverPropertyInfo essHome = new DriverPropertyInfo("essHome", null);
		
		driverProperties = new DriverPropertyInfo[] {
			mode,
			essHome
		};
	}

	public static void main(String[] args) throws SQLException, Exception {
		System.out.println(null + "Foo");

		//String url = "jdbc:essbase://192.168.1.231:9000/Sample.Basic;map=Sample.Basic";
		String url = "jdbc:essbase://172.16.28.136:9000/Sample.Basic;map=Sample.Basic";
		Properties props = new Properties();
		props.setProperty("user", "admin");
		props.setProperty("password", "Way2GoGuys");
		props.setProperty("Fooo", "Bar");
//		props.setProperty("MVCC", "TRUE");
		props.setProperty("LOCK_TIMEOUT", "5000");
		
		// Fuck locks!
		//props.setProperty("LOCK_MODE", "0");

		//props.setProperty("MULTI_THREADED", "0");
		EssbaseUrl essUrl = EssbaseUrl.parse(url);

		VessConnection conn = new VessConnection(essUrl, props);
		
		ResultSet rs = conn.getMetaData().getPrimaryKeys(null, null, "USERS");
		ResultSetTablePrinter printer = new ResultSetTablePrinter();
		
		printer.printResultSet(rs);
		
		printer.printResultSet(conn.getMetaData().getTables(null, null, null, null));
		
		printer.printResultSet(conn.getMetaData().getIndexInfo(null, null, "USERS", false, true));
		
		System.out.println("# SCHEMAS");
		printer.printResultSet(conn.getMetaData().getSchemas());
		//conn.close();
		
		ResultSet rs2 = conn.createStatement().executeQuery("SELECT * FROM SAMPLE.BASIC_DIM_PRODUCT");
		printer.printResultSet(rs2);
		
		conn = null;
		System.out.println("Signed off one");

		
		
//		VessConnection conn2 = new VessConnection(essUrl, props);
//		conn2.close();
		

		
//		System.out.println("Stub");
//		VessDriver driver = new VessDriver();
//
//		Connection connection = driver.connect(null, null);
//		DatabaseMetaData md = connection.getMetaData();
//
//		ResultSetTablePrinter printer = new ResultSetTablePrinter();
//		printer.printResultSet(md.getCatalogs());
//
//		System.out.println();
//		printer.printResultSet(md.getTables(null, null, null, null));
//		System.out.println();
//
//		printer.printResultSet(md.getColumns("VESS", null, null, null));
//		// md.getCatalogs();
//		System.out.println();
//		printer.printResultSet(md.getTableTypes());

	}

	private static DriverPropertyInfo[] driverProperties;

	public VessDriver() throws SQLException {
		
	}

	public Connection connect(String url, Properties info) throws SQLException {
		// JDBC drivers are cycled through to find a winner. Return null if
		// we don't support it
		if (!acceptsURL(url)) {
			return null;
		}
		System.out.println("VessDriver connecting to: " + url);
		EssbaseUrl essUrl = EssbaseUrl.parse(url);
		return new VessConnection(essUrl, info);
	}

	public boolean acceptsURL(String url) throws SQLException {
        if (url != null) {
            if (url.startsWith(VessConstants.START_URL)) {
                return true;
            } 
        }
        return false;
	}

	public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
		return driverProperties;
	}

	public int getMajorVersion() {
		return 0;
	}

	public int getMinorVersion() {
		return 9;
	}

	public boolean jdbcCompliant() {
		return false;
	}

	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw new SQLFeatureNotSupportedException("getParentLogger not supported");
	}

}

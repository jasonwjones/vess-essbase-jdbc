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

	//private static final org.slf4j.Logger logger = LoggerFactory.getLogger(VessDriver.class);
	
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
		
		DriverPropertyInfo persistentConfiguration = new DriverPropertyInfo("persistentConfiguration", null);
		
		driverProperties = new DriverPropertyInfo[] {
			mode,
			essHome,
			persistentConfiguration
		};
		
		System.out.println("Current thread class loader: " + Thread.currentThread().getContextClassLoader());
	}

	private static boolean hasAddedClassLoader = false;
	
	
	public static void main(String[] args) {
		System.out.println("Vess Virtual Essbase JDBC Driver");
	}
	
	public static void main2(String[] args) throws SQLException, Exception {
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

		VessConnection conn = null; //new VessConnection(essUrl, props);
		
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
		//logger.info("VessDriver initializing");
		System.out.println("Vess init");
		System.out.println("VD Current thread class loader: " + Thread.currentThread().getContextClassLoader());
		System.out.println("VD Current class loader: " + getClass().getClassLoader());
		
		synchronized (this) {
			System.out.println("Have added CL? " + hasAddedClassLoader);
			if (!hasAddedClassLoader) {
				hasAddedClassLoader = true;
				//addClassLoader2(getClass().getClassLoader());
		
//		if (!hasAddedClassLoader) {
//			addClassLoader2(getClass().getClassLoader());
//			hasAddedClassLoader = true;
//			System.out.println("Have added CL? " + hasAddedClassLoader);
//		} else {
//			System.out.println("Have already added custom class loader");
//		}
//		
//		
//		try {
//			getClass().getClassLoader().loadClass("com.saxifrages.vess.h2vess.VessConnection");
//		} catch (ClassNotFoundException e) {
//			System.out.println("Couldn't load class: " + e.getMessage());
//		}
			}
		}
			
	}

//	public void addClassLoader2(ClassLoader parent) {
//		System.out.println("adding CL to: " + parent);
//		try {
//			URL url[] = new URL[]{
//					new URL("file:///Users/jasonwjones/Development/Hyperion/vess-essbase-jdbc/target/dist/essbase-es-server-11.1.2.3.jar"),
//					new URL("file:///Users/jasonwjones/Development/Hyperion/vess-essbase-jdbc/target/dist/essbase-japi-11.1.2.3.jar"),
//					new URL("file:///Users/jasonwjones/Development/Hyperion/vess-essbase-jdbc/target/dist/odl-logging-11.1.2.3.jar"),
//					new URL("file:///Users/jasonwjones/Development/Hyperion/vess-essbase-jdbc/target/dist/vess-essbase-jdbc-0.0.1-SNAPSHOT.jar"),
//					new URL("file:///Users/jasonwjones/Development/Hyperion/vess-essbase-jdbc/target/dist/slf4j-api-1.7.9.jar"),
//					new URL("file:///Users/jasonwjones/Development/Hyperion/vess-essbase-jdbc/target/dist/logback-core-1.1.3.jar"),
//					new URL("file:///Users/jasonwjones/Development/Hyperion/vess-essbase-jdbc/target/dist/logback-classic-1.1.3.jar"),
//					new URL("file:///Users/jasonwjones/Development/Hyperion/vess-essbase-jdbc/target/dist/log4j-over-slf4j-1.7.8.jar"),
//					new URL("file:///Users/jasonwjones/Development/Hyperion/vess-essbase-jdbc/target/dist/h2-1.4.187.jar"),
//
//					///Users/jasonwjones/Development/Hyperion/vess-essbase-jdbc/target/dist/h2-1.4.187.jar
//					///Users/jasonwjones/Development/Hyperion/vess-essbase-jdbc/target/dist/log4j-over-slf4j-1.7.8.jar
//			};
//			//Thread.currentThread().getContextClassLoader()
//			EssClassLoader ecl = new EssClassLoader(url, parent);
//			//getClass().getClassLoader().
//			System.out.println("Setting thread class loader");
//			//Thread.currentThread().setContextClassLoader(ecl);
//			System.out.println("Loading ECL");
//			//Class<?> clazz = ecl.loadClass("com.essbase.server.framework.EssServerFramework");
//			
//		} catch (MalformedURLException e) {
//			System.out.println("Bad URL");
////		} catch (ClassNotFoundException e) {
////			// TODO Auto-generated catch block
////			e.printStackTrace();
//		}
//		
//	}
	
	/*
	public void addClassCloader(ClassLoader parent) {
		System.out.println("Current thread class loader: " + Thread.currentThread().getContextClassLoader());
		System.out.println("Current class loader: " + getClass().getClassLoader());

		
		try {
			//EssClassLoader ecl = new EssClassLoader()
			
//			Class<?> foo = Thread.currentThread().getContextClassLoader().loadClass("com.saxifrages.vess.h2vess.VessConnection");
//			Class<?> iep = Thread.currentThread().getContextClassLoader().loadClass("com.essbase.api.base.IEssProperties");
		
//			System.out.println("Getting constructors");
//			foo.getDeclaredConstructors();
//			System.out.println("Got constructors");
//			
//			System.out.println("Class for IEssProps: " + iep.getClassLoader());
//			System.out.println("My class loader: " + getClass().getClassLoader());
//			System.out.println("Class for IEssProps: " + IEssProperties.class.getClassLoader());
//			
//			VessConnection conn = (VessConnection) foo.newInstance();
//			System.out.println("Conn: " + conn);
		} catch (ClassNotFoundException e) {
			System.out.println("Class not found: " + e.getMessage());
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			System.out.println("Illegal access");
		} catch (InstantiationException e) {
			System.out.println("IE");
		}
		
	}
	*/
	
	public Connection connect(String url, Properties info) throws SQLException {
		// JDBC drivers are cycled through to find a winner. Return null if
		// we don't support it
		if (!acceptsURL(url)) {
			return null;
		}
		EssbaseUrl essUrl = EssbaseUrl.parse(url, info);
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

package com.saxifrages.vess;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import com.jasonwjones.jolo.ResultSetTablePrinter;



public class TestSuite {

	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		
		//String embeddedSampleBasic = "jdbc:essbase:embedded://admin:password@10.0.0.212/Sample.Basic;app=Sample;cube=Basic";
		// jdbc:essbase:embedded://localhost/Sample.Basic;app=Sample;cube=Basic
		String embeddedSampleBasic = "jdbc:essbase:embedded://epm11124/Sample.Basic;persistentStorage=/Users/foo";
		Class.forName("com.saxifrages.vess.VessDriver");

		Connection conn = DriverManager.getConnection(embeddedSampleBasic, "admin", "password");
		
		DatabaseMetaData md = conn.getMetaData();
		//ResultSet rs = md.getTables(null, "SAMPLE", "%", null);
		//print(rs);
		
		//print(md.getTables(null, "VESS_SCHEMA", "%", null));
		
		ResultSet variables = md.getColumns(null, null, "VARS", null);
		//new ResultSetTablePrinter().printResultSet(variables);
		
		DataSource dataSource = new SingleConnectionDataSource(conn, false);
		JdbcTemplate template = new JdbcTemplate(dataSource);
		List<Map<String, Object>> vars = template.queryForList("SELECT * FROM VESS_SCHEMA.VARS");
		
		for (Map<String, Object> var : vars) {
			System.out.println("Var: " + var);
		}
	
		
		System.out.println("Inserting");
		int rows = template.update("INSERT INTO VESS_SCHEMA.VARS VALUES ('CurYear', 'FY15')");
		System.out.println("inserted: " + rows);

		int rows2 = template.update("INSERT INTO VESS_SCHEMA.VARS VALUES ('NextYear', 'FY16')");
		System.out.println("inserted: " + rows2);


		int rows3 = template.update("INSERT INTO SAMPLE.BASIC_VARS (NAME, VALUE) VALUES ('CurScen', 'Actual')");
		System.out.println("inserted: " + rows3);

		template.update("UPDATE SAMPLE.BASIC_VARS SET VALUE = 'Budget' WHERE NAME = 'CurScen'");
		template.update("UPDATE SAMPLE.BASIC_VARS SET VALUE = 'FY98' WHERE NAME = 'CurYear' AND APPLICATION = 'Sample' AND CUBE = 'Basic'");
		template.update("UPDATE SAMPLE.BASIC_VARS SET VALUE = 'FY97' WHERE NAME = 'CurYear'");
		
		// AND APPLICATION = 'Sample' AND CUBE = 'Basic'
		
		System.out.println("Deleting");
		//int deleted = template.update("DELETE FROM VESS_SCHEMA.VARS WHERE NAME = 'CurYear'");
		//System.out.println("Deleted: " + deleted);
	
		
		//List<Map<String, Object>> vars = template.queryForList("SELECT * FROM SAMPLE.PROPS");
			
		//print(conn, "SELECT * FROM SAMPLE.PROPS");
		print(conn, "SELECT * FROM SAMPLE.BASIC_DIM_YEAR");
		
		Connection conn2 = DriverManager.getConnection(embeddedSampleBasic, "admin", "password");
		print(conn2, "SELECT * FROM VESS_SCHEMA.VARS");
		
		Connection conn3 = DriverManager.getConnection(embeddedSampleBasic, "admin", "password");
		print(conn3, "SELECT * FROM VESS_SCHEMA.VARS");
		print(conn3, "SELECT * FROM SAMPLE.BASIC_VARS");
		
		
		print(conn3, "SELECT * FROM SAMPLE.SAMPLE_DATA_TEST LIMIT 5 OFFSET 3");
		print(conn3, "SELECT * FROM SAMPLE.SAMPLE_DATA_TEST WHERE SCENARIO = 'Actual'");
		print(conn3, "SELECT * FROM SAMPLE.SAMPLE_DATA_TEST WHERE SCENARIO = 'Budget'");
		
		print(conn3, "SELECT * FROM SAMPLE.SAMPLE_DATA_TEST WHERE SCENARIO IN ('Budget', 'Forecast') AND TIME = 'Bar'");
		
		//print(conn3, "SELECT * FROM VESS_SCHEMA.CUSTOM_FUNCTIONS");
		
		//print(conn3, "SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'BASIC_DIM_PRODUCT'");
		
		//print(conn3, "SELECT * FROM SAMPLE.BASIC_DATAX");
		
//		print(conn3, "SELECT * FROM SAMPLE.BASIC_DATA");
	}

//	private static void print(ResultSet rs) throws SQLException {
//		new ResultSetTablePrinter().printResultSet(rs);
//	}
	
	private static void print(Connection connection, String sql) throws SQLException {
		System.out.println("Query: " + sql);
		Statement statement = connection.createStatement();
		ResultSet resultSet = statement.executeQuery(sql);
		new ResultSetTablePrinter().printResultSet(resultSet);
		System.out.println();
	}
	
}

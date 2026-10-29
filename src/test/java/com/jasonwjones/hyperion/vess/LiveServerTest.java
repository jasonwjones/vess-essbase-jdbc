package com.jasonwjones.hyperion.vess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Vess against a real Essbase server with Sample.Basic, when given one:
 *
 * <pre>
 * VESS_URL=jdbc:essbase:vess:http://localhost:9000/essbase \
 * VESS_USER=admin VESS_PASSWORD=... mvn test
 * </pre>
 *
 * These only read, unless VESS_WRITE_TESTS=true as well, when they also make
 * and remove a substitution variable and a calc script, and change three
 * values of Sample.Basic's budget and put them back.
 */
public class LiveServerTest {

	private static final String URL = System.getenv("VESS_URL");

	private static final String USER = System.getenv("VESS_USER");

	private static final String PASSWORD = System.getenv("VESS_PASSWORD");

	private static final boolean WRITES = "true".equals(System.getenv("VESS_WRITE_TESTS"));

	private Connection connection;

	@BeforeAll
	public static void needsAServer() {
		assumeTrue(URL != null && USER != null && PASSWORD != null, "VESS_URL, VESS_USER and VESS_PASSWORD are not set");
	}

	@BeforeEach
	public void connect() throws SQLException {
		connection = DriverManager.getConnection(URL + ";app=Sample;cube=Basic", USER, PASSWORD);
	}

	@AfterEach
	public void disconnect() throws SQLException {
		connection.close();
	}

	private List<List<Object>> query(String sql, Object... parameters) throws SQLException {
		List<List<Object>> rows = new ArrayList<>();
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			for (int index = 0; index < parameters.length; index++) {
				statement.setObject(index + 1, parameters[index]);
			}
			try (ResultSet results = statement.executeQuery()) {
				while (results.next()) {
					List<Object> row = new ArrayList<>();
					for (int column = 1; column <= results.getMetaData().getColumnCount(); column++) {
						row.add(results.getObject(column));
					}
					rows.add(row);
				}
			}
		}
		return rows;
	}

	private int update(String sql, Object... parameters) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			for (int index = 0; index < parameters.length; index++) {
				statement.setObject(index + 1, parameters[index]);
			}
			return statement.executeUpdate();
		}
	}

	@Test
	public void connectsAndDescribesTheServer() throws SQLException {
		assertInstanceOf(VessConnection.class, connection);
		DatabaseMetaData metaData = connection.getMetaData();
		assertEquals("Essbase", metaData.getDatabaseProductName());
		List<String> catalogs = new ArrayList<>();
		try (ResultSet results = metaData.getCatalogs()) {
			while (results.next()) {
				catalogs.add(results.getString("TABLE_CAT"));
			}
		}
		assertTrue(catalogs.contains("ESSBASE") && catalogs.contains("Sample"), catalogs.toString());
		List<String> keys = new ArrayList<>();
		try (ResultSet results = metaData.getImportedKeys("Sample", "Basic", "CELLS")) {
			while (results.next()) {
				keys.add(results.getString("FKCOLUMN_NAME") + "->" + results.getString("PKTABLE_NAME"));
			}
		}
		assertTrue(keys.contains("PRODUCT->DIM_PRODUCT"), keys.toString());
		try (ResultSet results = metaData.getProcedures("Sample", "Basic", "CalcAll")) {
			assertTrue(results.next(), "CalcAll is a procedure");
			assertEquals(DatabaseMetaData.procedureNoResult, results.getShort("PROCEDURE_TYPE"), "a procedure, not a function");
			assertTrue(results.getString("REMARKS").contains("CALL Sample.Basic.CalcAll()"), results.getString("REMARKS"));
		}
		try (ResultSet results = metaData.getProcedureColumns("Sample", "Basic", "CalcAll", "%")) {
			assertTrue(!results.next(), "CalcAll takes nothing and returns nothing");
		}
		try (ResultSet results = metaData.getProcedureColumns("Sample", "Basic", "RUN_CALC", "%")) {
			assertTrue(results.next());
			assertEquals(DatabaseMetaData.procedureColumnIn, results.getShort("COLUMN_TYPE"));
			assertTrue(!results.next());
		}
		assertTrue(connection.isValid(5));
	}

	@Test
	public void readsTheServersOwnTables() throws SQLException {
		assertTrue(query("select NAME from ESSBASE.SERVER.APPLICATIONS").contains(List.of("Sample")));
		assertTrue(query("select NAME from SERVER.CUBES where APPLICATION = 'Sample'").contains(List.of("Basic")));
		assertTrue(query("select count(*) from server.mdx_functions").get(0).get(0) instanceof Long);
	}

	@Test
	public void readsTheOutline() throws SQLException {
		assertEquals(List.of(List.of("100", "Colas")), query("select PARENT, MEMBER_ALIAS(PARENT) from DIM_PRODUCT where NAME = ?", "100-10"));
		assertEquals(List.of(List.of(13L)), query("select count(*) from DIM_PRODUCT where LEVEL = 0"));
		assertTrue(query("select MEMBER from UDAS where DIMENSION = 'Market' and UDA = 'Major Market'").contains(List.of("New York")));
	}

	@Test
	public void readsCells() throws SQLException {
		List<List<Object>> rows = query("select Market, Value from Cells where Year = 'Jan' and Measures = 'Sales' and Product = '100-10'"
				+ " and Scenario = 'Actual' and IS_CHILD(Market, 'East') order by Market");
		assertEquals(5, rows.size());
		assertEquals("Connecticut", rows.get(0).get(0));
		List<List<Object>> total = query("select Value from Cells where Year = 'Jan' and Measures = 'Sales' and Product = '100-10'"
				+ " and Scenario = 'Actual' and Market = 'East'");
		double sum = 0;
		for (List<Object> row : rows) {
			sum += (Double) row.get(1);
		}
		assertEquals(sum, (Double) total.get(0).get(0), 0.001, "East is the sum of its states");
	}

	@Test
	public void runsMdx() throws SQLException {
		try (Statement statement = connection.createStatement();
				ResultSet results = statement.executeQuery("SELECT {[Jan]} ON COLUMNS, [Market].Children ON ROWS FROM Sample.Basic")) {
			int rows = 0;
			while (results.next()) {
				rows++;
			}
			assertEquals(4, rows);
		}
	}

	@Test
	public void refusesWritesWhenReadOnly() throws SQLException {
		connection.setReadOnly(true);
		assertThrows(SQLException.class, () -> update("delete from Variables where NAME = 'VESS_NOTHING'"));
		assertThrows(SQLException.class, () -> query("call RUN_CALC('CALC ALL;')"));
	}

	@Test
	public void changesVariablesAndScripts() throws SQLException {
		assumeTrue(WRITES, "VESS_WRITE_TESTS isn't true");
		try {
			assertEquals(1, update("insert into Variables (SCOPE, NAME, VALUE) values ('Cube', 'VESS_LIVE_TEST', 'one')"));
			assertEquals(1, update("update Variables set VALUE = ? where NAME = 'VESS_LIVE_TEST'", "two"));
			assertEquals(List.of(List.of("two")), query("select VALUE from Variables where NAME = 'VESS_LIVE_TEST'"));
		} finally {
			update("delete from Variables where NAME = 'VESS_LIVE_TEST'");
		}
		try {
			assertEquals(1, update("insert into Scripts (TYPE, NAME, CONTENT) values ('Calc', 'VessLive', '/* one */')"));
			assertEquals(1, update("update Scripts set CONTENT = '/* two */' where NAME = 'VessLive'"));
			assertEquals(List.of(List.of("/* two */")), query("select CONTENT from Scripts where NAME = 'VessLive'"));
		} finally {
			update("delete from Scripts where NAME = 'VessLive'");
		}
		assertEquals(List.of(), query("select NAME from Scripts where NAME = 'VessLive'"));
	}

	@Test
	public void writesValuesAndPutsThemBack() throws SQLException {
		assumeTrue(WRITES, "VESS_WRITE_TESTS isn't true");
		String slice = " from Facts where Scenario = 'Budget' and Product = '100-10' and Market = 'New York' and Measures = 'Sales'"
				+ " and Year in ('Jan', 'Feb', 'Mar')";
		List<List<Object>> original = query("select Year, Value" + slice + " order by Year");
		try {
			assertEquals(3, update("update Facts set Value = Value + 1" + slice.substring(" from Facts".length())));
			List<List<Object>> changed = query("select Year, Value" + slice + " order by Year");
			for (int index = 0; index < original.size(); index++) {
				assertEquals((Double) original.get(index).get(1) + 1, (Double) changed.get(index).get(1), 0.001);
			}
		} finally {
			for (List<Object> row : original) {
				update("insert into Facts values (?, 'Sales', '100-10', 'New York', 'Budget', ?)", row.get(0), row.get(1));
			}
		}
		assertEquals(original, query("select Year, Value" + slice + " order by Year"));
	}

}

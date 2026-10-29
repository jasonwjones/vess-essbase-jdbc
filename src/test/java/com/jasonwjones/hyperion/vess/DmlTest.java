package com.jasonwjones.hyperion.vess;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.jasonwjones.hyperion.vess.schema.Column;
import com.jasonwjones.hyperion.vess.schema.Conditions;
import com.jasonwjones.hyperion.vess.schema.VessTable;

/**
 * INSERT, UPDATE and DELETE, turned into queries and then into changes.
 */
class DmlTest {

	/**
	 * A table held in memory, as substitution variables might be.
	 */
	static final class Variables extends VessTable {

		final List<Object[]> rows = new ArrayList<>();

		Variables() {
			super("Vars", "Variables for tests");
			rows.add(new Object[] { "CurMonth", "Jan", 1 });
			rows.add(new Object[] { "CurYear", "FY26", 2 });
		}

		@Override
		public List<Column> getColumns() {
			return List.of(Column.text("NAME", 320).notNull(), Column.text("VALUE", 256).writable(), Column.integer("POSITION").writable());
		}

		@Override
		public List<String> getPrimaryKey() {
			return List.of("NAME");
		}

		@Override
		protected Iterable<Object[]> rows(Conditions conditions) {
			return new ArrayList<>(rows);
		}

		@Override
		public boolean canInsert() {
			return true;
		}

		@Override
		public boolean canUpdate() {
			return true;
		}

		@Override
		public boolean canDelete() {
			return true;
		}

		@Override
		public void insert(List<Object[]> inserted) {
			rows.addAll(inserted);
		}

		@Override
		public void update(List<Object[]> before, List<Object[]> after) throws SQLException {
			for (int index = 0; index < before.size(); index++) {
				List<String> changed = changedReadOnly(before.get(index), after.get(index));
				if (!changed.isEmpty()) {
					throw new VessException("Can't change " + changed);
				}
				for (int row = 0; row < rows.size(); row++) {
					if (rows.get(row)[0].equals(before.get(index)[0])) {
						rows.set(row, after.get(index));
					}
				}
			}
		}

		@Override
		public void delete(List<Object[]> deleted) {
			for (Object[] row : deleted) {
				rows.removeIf(each -> each[0].equals(row[0]));
			}
		}

	}

	private TestEngine engine;

	private Variables variables;

	@BeforeEach
	void setUp() throws SQLException {
		FakeCube cube = new FakeCube().with(678, "Jan", "Sales", "100-10", "New York", "Actual").with(494, "Jan", "Sales", "100-10",
				"Massachusetts", "Actual");
		variables = new Variables();
		engine = new TestEngine(cube).add(variables);
	}

	@AfterEach
	void tearDown() throws SQLException {
		engine.close();
	}

	@Test
	void insertsValues() throws SQLException {
		assertEquals(2, engine.dml("insert into Vars (NAME, VALUE) values ('Next', 'Feb'), ('Last', upper('dec'))"));
		assertEquals(4, variables.rows.size());
		assertArrayEquals(new Object[] { "Next", "Feb", null }, variables.rows.get(2));
		assertArrayEquals(new Object[] { "Last", "DEC", null }, variables.rows.get(3));
	}

	@Test
	void insertsParameters() throws SQLException {
		assertEquals(1, engine.dml("insert into Sample.Basic.Vars values (?, ?, ?)", "Next", "Feb", "3"));
		assertArrayEquals(new Object[] { "Next", "Feb", 3 }, variables.rows.get(2));
	}

	@Test
	void insertsWhatAQueryAnswers() throws SQLException {
		assertEquals(2, engine.dml("insert into Vars (NAME, VALUE) select 'Prior' || NAME, VALUE from Vars"));
		assertEquals("PriorCurMonth", variables.rows.get(2)[0]);
	}

	@Test
	void updatesTheRowsTheConditionsPick() throws SQLException {
		assertEquals(1, engine.dml("update Vars set VALUE = 'Feb' where NAME = 'CurMonth'"));
		assertEquals("Feb", variables.rows.get(0)[1]);
		assertEquals("FY26", variables.rows.get(1)[1]);
	}

	@Test
	void updatesWithExpressionsAndParameters() throws SQLException {
		assertEquals(2, engine.dml("update Vars v set VALUE = v.VALUE || ?, POSITION = POSITION * 10 where v.NAME like ?", "!", "Cur%"));
		assertEquals("Jan!", variables.rows.get(0)[1]);
		assertEquals(20, variables.rows.get(1)[2]);
	}

	@Test
	void refusesToChangeWhatCantBe() {
		SQLException e = assertThrows(SQLException.class, () -> engine.dml("update Vars set NAME = 'Other' where NAME = 'CurMonth'"));
		assertTrue(e.getMessage().contains("NAME"), e.getMessage());
	}

	@Test
	void deletesTheRowsTheConditionsPick() throws SQLException {
		assertEquals(1, engine.dml("delete from Vars where VALUE = 'FY26'"));
		assertEquals(1, variables.rows.size());
		assertEquals(0, engine.dml("delete from Vars where 1 = 0"));
	}

	@Test
	void refusesOnAReadOnlyConnection() {
		engine.readOnly();
		SQLException e = assertThrows(SQLException.class, () -> engine.dml("delete from Vars"));
		assertTrue(e.getMessage().contains("read-only"), e.getMessage());
		assertEquals(2, variables.rows.size());
	}

	@Test
	void saysWhichColumnIsWrong() {
		SQLException e = assertThrows(SQLException.class, () -> engine.dml("insert into Vars (NAME, COLOUR) values ('A', 'B')"));
		assertTrue(e.getMessage().contains("COLOUR"), e.getMessage());
	}

	@Test
	void writesCells() throws SQLException {
		assertEquals(1, engine.dml("insert into Cells values ('Feb', 'Sales', '100-10', 'New York', 'Budget', 700)"));
		assertArrayEquals(new Object[] { "Feb", "Sales", "100-10", "New York", "Budget", 700.0 }, engine.cube.written.get(0));
		assertEquals(List.of(List.of(700.0)), engine.query("select Value from Cells where Scenario = 'Budget' and Year = 'Feb'"));
	}

	@Test
	void updatesCellsByFormula() throws SQLException {
		assertEquals(2, engine.dml("update Facts set Value = Value * 1.5 where Year = 'Jan' and Scenario = 'Actual'"));
		assertEquals(List.of(List.of(1758.0)), engine.query("select sum(Value) from Facts"));
	}

	@Test
	void copiesOneScenarioToAnother() throws SQLException {
		assertEquals(2, engine.dml("insert into Cells select Year, Measures, Product, Market, 'Budget', Value * 1.1 from Facts"
				+ " where Scenario = 'Actual'"));
		assertEquals(List.of(List.of(1289.2)), engine.query("select round(sum(Value), 1) from Cells where Scenario = 'Budget'"));
	}

	@Test
	void movesCellsClearingWhereTheyWere() throws SQLException {
		engine.dml("update Cells set Scenario = 'Budget' where Market = 'New York' and Year = 'Jan' and Measures = 'Sales'"
				+ " and Product = '100-10' and Scenario = 'Actual'");
		// The old cell is cleared first, then the new one written
		assertEquals(2, engine.cube.written.size());
		assertEquals("Actual", engine.cube.written.get(0)[4]);
		assertEquals(null, engine.cube.written.get(0)[5]);
		assertEquals("Budget", engine.cube.written.get(1)[4]);
		assertEquals(List.of(List.of("Budget", 678.0)), engine.query("select Scenario, Value from Cells where Market = 'New York'"));
	}

	@Test
	void clearsCells() throws SQLException {
		assertEquals(1, engine.dml("delete from Cells where Market = 'Massachusetts'"));
		assertEquals(List.of(List.of(1L)), engine.query("select count(*) from Cells"));
	}

	@Test
	void checksMembersBeforeWriting() {
		SQLException unknown = assertThrows(SQLException.class,
				() -> engine.dml("insert into Cells values ('Jan', 'Sales', '100-10', 'Atlantis', 'Actual', 1)"));
		assertTrue(unknown.getMessage().contains("Atlantis"), unknown.getMessage());
		SQLException upper = assertThrows(SQLException.class,
				() -> engine.dml("insert into Facts values ('Qtr1', 'Sales', '100-10', 'New York', 'Actual', 1)"));
		assertTrue(upper.getMessage().contains("Qtr1"), upper.getMessage());
		SQLException dynamic = assertThrows(SQLException.class,
				() -> engine.dml("insert into Cells values ('Qtr1', 'Sales', '100-10', 'New York', 'Actual', 1)"));
		assertTrue(dynamic.getMessage().contains("Dynamic Calc"), dynamic.getMessage());
		assertEquals(0, engine.cube.written.size());
	}

	@Test
	void refusesToWriteCellsWhenReadOnly() {
		engine.cube.readOnly();
		assertThrows(SQLException.class, () -> engine.dml("delete from Cells"));
		assertEquals(0, engine.cube.written.size());
	}

}

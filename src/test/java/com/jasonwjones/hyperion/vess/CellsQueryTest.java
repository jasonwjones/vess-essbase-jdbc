package com.jasonwjones.hyperion.vess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Queries of the cells, through the SQL engine, against a fake Sample.Basic:
 * what they answer, and what MDX they take to answer it.
 */
class CellsQueryTest {

	private TestEngine engine;

	@BeforeEach
	void setUp() throws SQLException {
		FakeCube cube = new FakeCube()
				.with(678, "Jan", "Sales", "100-10", "New York", "Actual")
				.with(494, "Jan", "Sales", "100-10", "Massachusetts", "Actual")
				.with(200, "Jan", "Sales", "100-20", "New York", "Actual")
				.with(645, "Feb", "Sales", "100-10", "New York", "Actual")
				.with(271, "Jan", "COGS", "100-10", "New York", "Actual")
				.with(640, "Jan", "Sales", "100-10", "New York", "Budget")
				.with(1812, "Jan", "Sales", "100", "East", "Actual")
				.with(1998, "Qtr1", "Sales", "100-10", "New York", "Actual");
		engine = new TestEngine(cube);
	}

	@AfterEach
	void tearDown() throws SQLException {
		engine.close();
	}

	private List<String> lastSet(int index) {
		List<String> queries = engine.cube.queries;
		return FakeCube.sets(queries.get(queries.size() - 1)).get(index);
	}

	/**
	 * The members a query read of one dimension, wherever in the MDX they
	 * were: on an axis, or in the slicer.
	 */
	private boolean read(String member) {
		return engine.cube.queries.stream().anyMatch(query -> query.contains("[" + member + "]"));
	}

	@Test
	void readsOneSlice() throws SQLException {
		List<List<Object>> rows = engine.query(
				"select Market, Value from Cells where Year = 'Jan' and Measures = 'Sales' and Product = '100-10' and Scenario = 'Actual'");
		assertEquals(List.of(List.of("New York", 678.0), List.of("Massachusetts", 494.0)), rows);
		assertEquals(1, engine.cube.queries.size());
	}

	@Test
	void readsEveryValueAtEveryLevel() throws SQLException {
		assertEquals(List.of(List.of(8L)), engine.query("select count(*) from Cells"));
	}

	@Test
	void factsHasOnlyStoredLevelZero() throws SQLException {
		assertEquals(List.of(List.of(6L)), engine.query("select count(*) from Facts"));
		assertFalse(read("Qtr1"), "Facts doesn't ask for dynamic members");
		assertFalse(read("East"), "nor for upper-level ones");
	}

	@Test
	void addsUpFacts() throws SQLException {
		assertEquals(List.of(List.of("Actual", 2017.0), List.of("Budget", 640.0)),
				engine.query("select Scenario, sum(Value) from Facts where Measures = 'Sales' group by Scenario order by Scenario"));
	}

	@Test
	void readsOnlyTheMembersAnOutlineFunctionKeeps() throws SQLException {
		List<List<Object>> rows = engine.query("select Market, Value from Cells where Year = 'Jan' and Measures = 'Sales' and Product = '100-10'"
				+ " and Scenario = 'Actual' and IS_DESCENDANT(Market, 'East')");
		assertEquals(List.of(List.of("New York", 678.0), List.of("Massachusetts", 494.0)), rows);
		assertEquals(List.of("New York", "Massachusetts", "Florida", "Connecticut", "New Hampshire"), lastSet(0));
	}

	@Test
	void readsOnlyTheMembersLikeKeeps() throws SQLException {
		engine.query("select * from Cells where Market like 'New%' and Year = 'Jan' and Measures = 'Sales' and Scenario = 'Actual'");
		assertTrue(read("New York") && read("New Hampshire") && read("New Mexico"));
		assertFalse(read("Massachusetts"));
	}

	@Test
	void readsOnlyMembersWithAnAttribute() throws SQLException {
		engine.query("select * from Facts where MEMBER_ATTRIBUTE(Product, 'Caffeinated') = 'False' and Year = 'Jan' and Measures = 'Sales'");
		assertTrue(read("100-30"));
		assertFalse(read("100-10"));
	}

	@Test
	void answersConditionsAcrossDimensionsCorrectly() throws SQLException {
		// Neither dimension can be narrowed alone, so both are read whole and
		// the engine sorts out which rows hold
		List<List<Object>> rows = engine.query("select Product, Market, Value from Cells where (Market = 'East' or Product = '100-20')"
				+ " and Year = 'Jan' and Measures = 'Sales' and Scenario = 'Actual' order by Value");
		assertEquals(List.of(List.of("100-20", "New York", 200.0), List.of("100", "East", 1812.0)), rows);
	}

	@Test
	void joinsToADimensionTable() throws SQLException {
		List<List<Object>> rows = engine.query("select c.PRODUCT, p.ALIAS, c.VALUE from CELLS c join DIM_PRODUCT p on c.PRODUCT = p.NAME"
				+ " where c.Year = 'Jan' and c.Measures = 'Sales' and c.Market = 'New York' and c.Scenario = 'Actual' order by c.Value");
		assertEquals(List.of(List.of("100-20", "Diet Cola", 200.0), List.of("100-10", "Cola", 678.0)), rows);
	}

	@Test
	void asksNothingWhenNoMemberCanMatch() throws SQLException {
		assertEquals(List.of(), engine.query("select * from Cells where Market = 'Nowhere'"));
		assertEquals(0, engine.cube.queries.size());
	}

	@Test
	void comparesNamesExactly() throws SQLException {
		// SQL compares text exactly, so a name in the wrong case matches nothing
		assertEquals(List.of(), engine.query("select * from Cells where Market = 'new york'"));
	}

	@Test
	void readsInPiecesWithTheSameAnswer() throws SQLException {
		engine.cube.chunks(40, 3);
		assertEquals(List.of(List.of(8L, 6738.0)), engine.query("select count(*), sum(Value) from Cells"));
		assertTrue(engine.cube.queries.size() > 10, "read in many pieces: " + engine.cube.queries.size());
		for (String query : engine.cube.queries) {
			for (List<String> set : FakeCube.sets(query)) {
				assertTrue(set.size() <= 3, "at most three members a dimension: " + query);
			}
		}
	}

	@Test
	void stopsReadingWhenTheQueryHasEnough() throws SQLException {
		engine.cube.chunks(40, 3);
		assertEquals(1, engine.query("select * from Cells fetch first 1 rows only").size());
		int some = engine.cube.queries.size();
		engine.cube.queries.clear();
		engine.query("select count(*) from Cells");
		assertTrue(some < engine.cube.queries.size(), "a query wanting one row reads less: " + some + " of " + engine.cube.queries.size());
	}

	@Test
	void showsDimensionsAsTables() throws SQLException {
		List<List<Object>> rows = engine.query("select NAME, ALIAS, CATEGORY, PRODUCT_SKU, CAFFEINATED, OUNCES, INTRO_DATE, LEVEL"
				+ " from DIM_PRODUCT where NAME = '100-30'");
		assertEquals(List.of(List.of("100-30", "Caffeine Free Cola", "100", "100-30", false, 16.0, java.sql.Date.valueOf("1996-04-01"), 0)), rows);
		assertEquals(List.of(List.of(19L)), engine.query("select count(*) from dim_product"), "shared members aren't repeated");
		assertEquals(List.of(List.of("Central"), List.of("East"), List.of("South"), List.of("West")),
				engine.query("select NAME from DIM_MARKET where IS_CHILD(NAME, 'Market') order by NAME"));
	}

	@Test
	void hasOneAliasColumn() throws SQLException {
		assertEquals(List.of("NAME", "ALIAS", "PARENT", "LEVEL", "GENERATION", "STORAGE", "CONSOLIDATION", "FORMULA", "UDAS", "CHILDREN",
				"OUTLINE_ORDER", "CATEGORY", "PRODUCT_SKU", "CAFFEINATED", "OUNCES", "PKG_TYPE", "INTRO_DATE"),
				engine.columns("select * from DIM_PRODUCT"));
		assertEquals(List.of(List.of("Cola")), engine.query("select ALIAS from DIM_PRODUCT where NAME = '100-10'"));
	}

	@Test
	void showsTheAliasTableChosen() throws SQLException {
		try (TestEngine longNames = new TestEngine(new FakeCube(), "long names")) {
			assertEquals(List.of(List.of("Quarter1", "Quarter1", "Januar")),
					longNames.query("select ALIAS, MEMBER_ALIAS('Qtr1'), MEMBER_ALIAS('Jan', 'GermanNames') from DIM_YEAR where NAME = 'Qtr1'"));
		}
		try (TestEngine unknown = new TestEngine(new FakeCube(), "Klingon")) {
			assertEquals(List.of(List.of("Cola")), unknown.query("select ALIAS from DIM_PRODUCT where NAME = '100-10'"),
					"a cube without that alias table shows its default");
		}
	}

	@Test
	void answersOutlineFunctions() throws SQLException {
		assertEquals(List.of(List.of(true, false, 3, 1, "100", "Cola", "Product", "Can", true)),
				engine.query("select IS_DESCENDANT('100-20', 'Diet'), IS_CHILD('100-20', 'Product'), MEMBER_GENERATION('100-10'),"
						+ " MEMBER_LEVEL('100'), MEMBER_PARENT('100-10'), MEMBER_ALIAS('100-10'), MEMBER_DIMENSION('100-10'),"
						+ " MEMBER_ATTRIBUTE('100-10', 'Pkg Type'), HAS_UDA('New York', 'Major Market')"));
		assertEquals(List.of(List.of("Cola")), engine.query("select MEMBER_ALIAS('100-10', 'GermanNames')"));
	}

}

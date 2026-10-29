package com.jasonwjones.hyperion.vess.cells;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.jasonwjones.hyperion.vess.Fixtures;
import com.jasonwjones.hyperion.vess.outline.Dimension;
import com.jasonwjones.hyperion.vess.outline.Member;
import com.jasonwjones.hyperion.vess.outline.Outline;

class CellPlanTest {

	private final Outline outline = Fixtures.sampleBasic();

	private final List<Dimension> dimensions = outline.getStandardDimensions();

	private List<Member> members(String... names) {
		List<Member> members = new ArrayList<>();
		for (String name : names) {
			members.add(outline.getMember(name));
		}
		return members;
	}

	private List<List<Member>> all() {
		List<List<Member>> sets = new ArrayList<>();
		for (Dimension dimension : dimensions) {
			sets.add(dimension.getBaseMembers());
		}
		return sets;
	}

	private static List<CellPlan.Chunk> chunks(Iterator<CellPlan.Chunk> iterator) {
		List<CellPlan.Chunk> chunks = new ArrayList<>();
		iterator.forEachRemaining(chunks::add);
		return chunks;
	}

	@Test
	void putsSingleMembersInTheSlicerAndADenseDimensionOnTheColumns() {
		List<List<Member>> sets = all();
		sets.set(1, members("Sales"));
		sets.set(4, members("Actual"));
		sets.set(3, members("New York", "Florida"));
		List<CellPlan.Chunk> chunks = chunks(CellPlan.chunks(dimensions, sets, 1_000_000, 2_000));
		assertEquals(1, chunks.size());
		String mdx = chunks.get(0).mdx("Sample", "Basic");
		assertEquals("SELECT {[Year], [Qtr1], [Jan], [Feb], [Mar], [Qtr2], [Apr], [May], [Jun], [Qtr3], [Jul], [Aug], [Sep], [Qtr4], [Oct], [Nov], [Dec]}"
				+ " ON COLUMNS,\nNON EMPTY CROSSJOIN({" + String.join(", ", names(sets.get(2))) + "}, {[New York], [Florida]}) ON ROWS"
				+ "\nFROM [Sample].[Basic]\nWHERE ([Sales], [Actual])", mdx);
	}

	private static List<String> names(List<Member> members) {
		return members.stream().map(member -> "[" + member.getName() + "]").toList();
	}

	@Test
	void alwaysPutsSomethingOnTheRows() {
		List<List<Member>> sets = all();
		sets.set(0, members("Jan"));
		sets.set(1, members("Sales"));
		sets.set(2, members("100-10"));
		sets.set(4, members("Actual"));
		CellPlan.Chunk chunk = chunks(CellPlan.chunks(dimensions, sets, 1_000_000, 2_000)).get(0);
		assertEquals(3, chunk.columnDimension(), "Market, the only dimension of several members");
		assertEquals(List.of(0), chunk.rowDimensions(), "Year, moved out of the slicer");
		assertTrue(chunk.mdx("Sample", "Basic").contains("NON EMPTY {[Jan]} ON ROWS"));
	}

	@Test
	void splitsTheOutermostDimensionFirst() {
		List<CellPlan.Chunk> chunks = chunks(CellPlan.chunks(dimensions, all(), 10_000, 2_000));
		assertTrue(chunks.size() > 1);
		long cells = 0;
		for (CellPlan.Chunk chunk : chunks) {
			assertTrue(chunk.cells() <= 10_000, "no more than asked: " + chunk.cells());
			cells += chunk.cells();
		}
		assertEquals(17L * 17 * 19 * 25 * 5, cells, "every cell, once");
		// In outline order: the first piece starts at the first member of Measures
		assertEquals("Measures", chunks.get(0).rows().get(0).get(0).getName());
	}

	@Test
	void namesNoMoreMembersThanAllowed() {
		for (CellPlan.Chunk chunk : chunks(CellPlan.chunks(dimensions, all(), 1_000_000, 4))) {
			assertTrue(chunk.columns().size() <= 4);
			for (List<Member> row : chunk.rows()) {
				assertTrue(row.size() <= 4);
			}
		}
	}

	@Test
	void plansNothingForAnEmptySet() {
		List<List<Member>> sets = all();
		sets.set(2, List.of());
		assertFalse(CellPlan.chunks(dimensions, sets, 1_000_000, 2_000).hasNext());
	}

	@Test
	void quotesNames() {
		assertEquals("[Sales]", CellPlan.name("Sales"));
		assertEquals("[a]]b]", CellPlan.name("a]b"));
	}

}

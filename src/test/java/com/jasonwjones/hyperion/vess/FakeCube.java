package com.jasonwjones.hyperion.vess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.appliedolap.essbase.EssMdxGrid;
import com.jasonwjones.hyperion.vess.cells.CellSource;
import com.jasonwjones.hyperion.vess.outline.Member;
import com.jasonwjones.hyperion.vess.outline.Outline;

/**
 * A cube for tests: Sample.Basic's outline, and the values put in it. It
 * answers the MDX that Vess writes for the cells, as Essbase would, and keeps
 * the queries and what's written to it, for tests to look at.
 */
public final class FakeCube implements CellSource {

	private static final Pattern SET = Pattern.compile("\\{([^}]*)\\}");

	private static final Pattern SLICER = Pattern.compile("WHERE \\((.*)\\)\\s*$", Pattern.DOTALL);

	private static final Pattern MEMBER = Pattern.compile("\\[((?:[^\\]]|\\]\\])*)\\]");

	private final Outline outline = Fixtures.sampleBasic();

	private final Map<List<String>, Double> values = new HashMap<>();

	public final List<String> queries = new ArrayList<>();

	public final List<Object[]> written = new ArrayList<>();

	private long chunkCells = 500_000;

	private int chunkMembers = 2_000;

	private boolean readOnly;

	/**
	 * Puts a value in a cell, given a member of each dimension in outline
	 * order: Year, Measures, Product, Market, Scenario.
	 */
	public FakeCube with(double value, String... members) {
		values.put(Arrays.asList(members), value);
		return this;
	}

	public FakeCube chunks(long cells, int members) {
		this.chunkCells = cells;
		this.chunkMembers = members;
		return this;
	}

	public FakeCube readOnly() {
		this.readOnly = true;
		return this;
	}

	@Override
	public Outline outline() {
		return outline;
	}

	@Override
	public long chunkCells() {
		return chunkCells;
	}

	@Override
	public int chunkMembers() {
		return chunkMembers;
	}

	@Override
	public void checkWritable(String what) throws VessException {
		if (readOnly) {
			throw new VessException("The connection is read-only, so Vess won't " + what);
		}
	}

	@Override
	public void write(List<Object[]> records) {
		for (Object[] record : records) {
			written.add(record.clone());
			List<String> key = new ArrayList<>();
			for (int index = 0; index < record.length - 1; index++) {
				key.add((String) record[index]);
			}
			if (record[record.length - 1] == null) {
				values.remove(key);
			} else {
				values.put(key, ((Number) record[record.length - 1]).doubleValue());
			}
		}
	}

	/**
	 * The members a query names, by dimension: the columns' set, each set on
	 * the rows, and the slicer.
	 */
	public static List<List<String>> sets(String mdx) {
		List<List<String>> sets = new ArrayList<>();
		Matcher matcher = SET.matcher(mdx);
		while (matcher.find()) {
			sets.add(members(matcher.group(1)));
		}
		return sets;
	}

	private static List<String> members(String text) {
		List<String> members = new ArrayList<>();
		Matcher matcher = MEMBER.matcher(text);
		while (matcher.find()) {
			members.add(matcher.group(1).replace("]]", "]"));
		}
		return members;
	}

	@Override
	public EssMdxGrid mdx(String query) {
		queries.add(query);
		List<List<String>> sets = sets(query);
		List<String> columns = sets.get(0);
		List<List<String>> rows = sets.subList(1, sets.size());
		Map<Integer, String> fixed = new HashMap<>();
		Matcher slicer = SLICER.matcher(query);
		if (slicer.find()) {
			for (String name : members(slicer.group(1))) {
				fixed.put(outline.getMember(name).getDimension().getNumber() - 1, name);
			}
		}
		int columnDimension = dimensionOf(columns.get(0));
		List<Integer> rowDimensions = new ArrayList<>();
		for (List<String> set : rows) {
			rowDimensions.add(dimensionOf(set.get(0)));
		}
		List<List<String>> grid = new ArrayList<>();
		List<String> header = new ArrayList<>();
		for (int index = 0; index < rowDimensions.size(); index++) {
			header.add("");
		}
		header.addAll(columns);
		grid.add(header);
		for (List<String> tuple : product(rows)) {
			List<String> line = new ArrayList<>(tuple);
			boolean any = false;
			for (String column : columns) {
				String[] key = new String[5];
				fixed.forEach((dimension, name) -> key[dimension] = name);
				for (int index = 0; index < tuple.size(); index++) {
					key[rowDimensions.get(index)] = tuple.get(index);
				}
				key[columnDimension] = column;
				Double value = values.get(Arrays.asList(key));
				any |= value != null;
				line.add(value == null ? "#Missing" : value.toString());
			}
			if (any || !query.contains("NON EMPTY")) {
				grid.add(line);
			}
		}
		return new Grid(grid, columns.size() + rowDimensions.size());
	}

	private int dimensionOf(String member) {
		Member found = outline.getMember(member);
		return found.getDimension().getNumber() - 1;
	}

	private static List<List<String>> product(List<List<String>> sets) {
		List<List<String>> tuples = new ArrayList<>();
		tuples.add(new ArrayList<>());
		for (List<String> set : sets) {
			List<List<String>> next = new ArrayList<>();
			for (List<String> tuple : tuples) {
				for (String member : set) {
					List<String> longer = new ArrayList<>(tuple);
					longer.add(member);
					next.add(longer);
				}
			}
			tuples = next;
		}
		return tuples;
	}

	private record Grid(List<List<String>> cells, int width) implements EssMdxGrid {

		@Override
		public int getRows() {
			return cells.size();
		}

		@Override
		public int getColumns() {
			return width;
		}

		@Override
		public String getCell(int row, int col) {
			return cells.get(row).get(col);
		}

		@Override
		public List<String> getRowDimensions() {
			return List.of();
		}

		@Override
		public List<String> getColumnDimensions() {
			return List.of("columns");
		}

		@Override
		public List<String> getPageDimensions() {
			return List.of();
		}

		@Override
		public List<String> getPageMembers() {
			return List.of();
		}

	}

}

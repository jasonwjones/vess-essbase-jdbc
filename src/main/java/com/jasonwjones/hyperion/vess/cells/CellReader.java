package com.jasonwjones.hyperion.vess.cells;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appliedolap.essbase.EssMdxGrid;
import com.jasonwjones.hyperion.vess.VessException;
import com.jasonwjones.hyperion.vess.outline.Member;

/**
 * The cells a plan reads, a row for each value, running each of its queries
 * only when the rows before it have been used up.
 */
final class CellReader implements Iterator<Object[]> {

	private static final Logger logger = LoggerFactory.getLogger(CellReader.class);

	/**
	 * Runs one MDX query.
	 */
	@FunctionalInterface
	interface MdxRunner {

		EssMdxGrid run(String mdx) throws SQLException;

	}

	private final Iterator<CellPlan.Chunk> chunks;

	private final int dimensions;

	private final MdxRunner runner;

	private final String application;

	private final String cube;

	private final AtomicBoolean cancelled;

	private Iterator<Object[]> current = List.<Object[]>of().iterator();

	CellReader(Iterator<CellPlan.Chunk> chunks, int dimensions, MdxRunner runner, String application, String cube, AtomicBoolean cancelled) {
		this.chunks = chunks;
		this.dimensions = dimensions;
		this.runner = runner;
		this.application = application;
		this.cube = cube;
		this.cancelled = cancelled;
	}

	@Override
	public boolean hasNext() {
		while (!current.hasNext() && chunks.hasNext()) {
			if (cancelled != null && cancelled.get()) {
				return false;
			}
			current = read(chunks.next()).iterator();
		}
		return current.hasNext();
	}

	@Override
	public Object[] next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		return current.next();
	}

	private List<Object[]> read(CellPlan.Chunk chunk) {
		String mdx = chunk.mdx(application, cube);
		long started = System.currentTimeMillis();
		EssMdxGrid grid;
		try {
			grid = runner.run(mdx);
		} catch (SQLException e) {
			throw new VessException.Unchecked(e);
		}
		List<Object[]> rows = rows(chunk, grid, dimensions);
		logger.debug("{} cells asked for, {} values in {} ms:\n{}", chunk.cells(), rows.size(), System.currentTimeMillis() - started, mdx);
		return rows;
	}

	/**
	 * The values in a grid, a row each, with the members of every dimension in
	 * the table's order and the value last. Missing values, and those that
	 * aren't numbers, are left out.
	 */
	static List<Object[]> rows(CellPlan.Chunk chunk, EssMdxGrid grid, int dimensions) {
		List<Object[]> rows = new ArrayList<>();
		int header = grid.getHeaderRows();
		int rowMembers = chunk.rowDimensions().size();
		if (grid.getRows() <= header) {
			return rows;
		}
		for (int row = header; row < grid.getRows(); row++) {
			for (int column = rowMembers; column < grid.getColumns(); column++) {
				Double value = value(grid.getCell(row, column));
				if (value == null) {
					continue;
				}
				Object[] values = new Object[dimensions + 1];
				for (Map.Entry<Integer, Member> fixed : chunk.fixed().entrySet()) {
					values[fixed.getKey()] = fixed.getValue().getName();
				}
				for (int index = 0; index < rowMembers; index++) {
					values[chunk.rowDimensions().get(index)] = grid.getCell(row, index);
				}
				values[chunk.columnDimension()] = grid.getCell(header - 1, column);
				values[dimensions] = value;
				rows.add(values);
			}
		}
		return rows;
	}

	/**
	 * A cell's number, or null for #Missing, #NoAccess and anything else that
	 * isn't one.
	 */
	static Double value(String text) {
		if (text == null || text.isEmpty() || text.charAt(0) == '#') {
			return null;
		}
		try {
			return Double.valueOf(text);
		} catch (NumberFormatException e) {
			return null;
		}
	}

}

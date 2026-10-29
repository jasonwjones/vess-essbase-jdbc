package com.jasonwjones.hyperion.vess.cells;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.jasonwjones.hyperion.vess.outline.Dimension;
import com.jasonwjones.hyperion.vess.outline.Member;

/**
 * How to read a set of cells with MDX: which dimension goes on the columns,
 * which on the rows and which in the slicer, and how to split the reading into
 * queries of a sensible size.
 *
 * <p>
 * A dimension down to one member goes in the slicer. Of the rest, the largest
 * dense dimension goes on the columns, where Essbase reads it within each block,
 * and the others are crossed on the rows, with NON EMPTY so that combinations
 * holding nothing are left out. When that would be too much for one query, the
 * outermost dimension on the rows is split, so the results still come out in
 * outline order, a piece at a time.
 */
final class CellPlan {

	/**
	 * One MDX query's share of the cells.
	 *
	 * @param columnDimension the position of the dimension on the columns
	 * @param columns its members
	 * @param rowDimensions the positions of the dimensions on the rows
	 * @param rows each one's members
	 * @param fixed the members of the dimensions in the slicer, by position
	 */
	record Chunk(int columnDimension, List<Member> columns, List<Integer> rowDimensions, List<List<Member>> rows,
			Map<Integer, Member> fixed) {

		long cells() {
			long cells = columns.size();
			for (List<Member> members : rows) {
				cells *= members.size();
			}
			return cells;
		}

		String mdx(String application, String cube) {
			StringBuilder mdx = new StringBuilder("SELECT ").append(set(columns)).append(" ON COLUMNS");
			if (!rows.isEmpty()) {
				String crossed = set(rows.get(0));
				for (int index = 1; index < rows.size(); index++) {
					crossed = "CROSSJOIN(" + crossed + ", " + set(rows.get(index)) + ")";
				}
				mdx.append(",\nNON EMPTY ").append(crossed).append(" ON ROWS");
			}
			mdx.append("\nFROM ").append(name(application)).append('.').append(name(cube));
			if (!fixed.isEmpty()) {
				mdx.append("\nWHERE (");
				boolean first = true;
				for (Member member : fixed.values()) {
					if (!first) {
						mdx.append(", ");
					}
					mdx.append(name(member.getName()));
					first = false;
				}
				mdx.append(')');
			}
			return mdx.toString();
		}

		private Chunk with(int rowIndex, List<Member> members) {
			List<List<Member>> split = new ArrayList<>(rows);
			split.set(rowIndex, members);
			return new Chunk(columnDimension, columns, rowDimensions, split, fixed);
		}

		private Chunk withColumns(List<Member> members) {
			return new Chunk(columnDimension, members, rowDimensions, rows, fixed);
		}

	}

	private CellPlan() {
	}

	static String name(String name) {
		return "[" + name.replace("]", "]]") + "]";
	}

	private static String set(List<Member> members) {
		StringBuilder set = new StringBuilder("{");
		for (int index = 0; index < members.size(); index++) {
			if (index > 0) {
				set.append(", ");
			}
			set.append(name(members.get(index).getName()));
		}
		return set.append('}').toString();
	}

	/**
	 * The queries that read the given members of each dimension, made only as
	 * they're wanted.
	 *
	 * @param dimensions the dimensions, in the table's order
	 * @param sets each dimension's members
	 * @param maxCells the most cells one query may ask for, before NON EMPTY
	 * @param maxMembers the most members of one dimension a query may name
	 */
	static Iterator<Chunk> chunks(List<Dimension> dimensions, List<List<Member>> sets, long maxCells, int maxMembers) {
		for (List<Member> set : sets) {
			if (set.isEmpty()) {
				return List.<Chunk>of().iterator();
			}
		}
		Map<Integer, Member> fixed = new LinkedHashMap<>();
		List<Integer> several = new ArrayList<>();
		for (int index = 0; index < sets.size(); index++) {
			if (sets.get(index).size() == 1) {
				fixed.put(index, sets.get(index).get(0));
			} else {
				several.add(index);
			}
		}
		int columnDimension;
		if (several.isEmpty()) {
			// Every dimension is down to one member: one cell, with the first
			// dimension on the columns, since a query needs something there
			columnDimension = 0;
			fixed.remove(0);
		} else {
			columnDimension = columnDimension(dimensions, sets, several);
		}
		List<Integer> rowDimensions = new ArrayList<>();
		List<List<Member>> rows = new ArrayList<>();
		for (int index : several) {
			if (index != columnDimension) {
				rowDimensions.add(index);
				rows.add(sets.get(index));
			}
		}
		if (rowDimensions.isEmpty() && !fixed.isEmpty()) {
			// Without anything on the rows, Essbase answers with the column
			// headers alone and no values, so a dimension from the slicer goes
			// there instead
			int moved = fixed.keySet().iterator().next();
			fixed.remove(moved);
			rowDimensions.add(moved);
			rows.add(sets.get(moved));
		}
		Deque<Chunk> pending = new ArrayDeque<>();
		pending.add(new Chunk(columnDimension, sets.get(columnDimension), rowDimensions, rows, fixed));
		return new Iterator<>() {

			@Override
			public boolean hasNext() {
				return !pending.isEmpty();
			}

			@Override
			public Chunk next() {
				if (pending.isEmpty()) {
					throw new NoSuchElementException();
				}
				while (true) {
					Chunk chunk = pending.removeFirst();
					List<Chunk> pieces = split(chunk, maxCells, maxMembers);
					if (pieces == null) {
						return chunk;
					}
					for (int index = pieces.size() - 1; index >= 0; index--) {
						pending.addFirst(pieces.get(index));
					}
				}
			}

		};
	}

	/**
	 * The dimension for the columns: the largest dense one, or if none of
	 * those has more than one member, the smallest of the others, to keep each
	 * row short.
	 */
	private static int columnDimension(List<Dimension> dimensions, List<List<Member>> sets, List<Integer> several) {
		int best = -1;
		for (int index : several) {
			if ("Dense".equals(dimensions.get(index).getDensity())
					&& (best < 0 || sets.get(index).size() > sets.get(best).size())) {
				best = index;
			}
		}
		if (best >= 0) {
			return best;
		}
		for (int index : several) {
			if (best < 0 || sets.get(index).size() < sets.get(best).size()) {
				best = index;
			}
		}
		return best;
	}

	/**
	 * A chunk split into smaller ones, in order, or null if it's small enough
	 * as it is.
	 */
	static List<Chunk> split(Chunk chunk, long maxCells, int maxMembers) {
		if (chunk.columns().size() > maxMembers) {
			return splitColumns(chunk, maxMembers);
		}
		for (int index = 0; index < chunk.rows().size(); index++) {
			if (chunk.rows().get(index).size() > maxMembers) {
				return splitRows(chunk, index, maxMembers);
			}
		}
		long cells = chunk.cells();
		if (cells <= maxCells) {
			return null;
		}
		for (int index = 0; index < chunk.rows().size(); index++) {
			int size = chunk.rows().get(index).size();
			if (size > 1) {
				long perMember = cells / size;
				int block = (int) Math.max(1, Math.min(size - 1, maxCells / Math.max(1, perMember)));
				return splitRows(chunk, index, block);
			}
		}
		if (chunk.columns().size() > 1) {
			return splitColumns(chunk, (int) Math.max(1, Math.min(chunk.columns().size() - 1, maxCells)));
		}
		return null;
	}

	private static List<Chunk> splitRows(Chunk chunk, int rowIndex, int block) {
		List<Chunk> pieces = new ArrayList<>();
		List<Member> members = chunk.rows().get(rowIndex);
		for (int start = 0; start < members.size(); start += block) {
			pieces.add(chunk.with(rowIndex, members.subList(start, Math.min(members.size(), start + block))));
		}
		return pieces;
	}

	private static List<Chunk> splitColumns(Chunk chunk, int block) {
		List<Chunk> pieces = new ArrayList<>();
		List<Member> members = chunk.columns();
		for (int start = 0; start < members.size(); start += block) {
			pieces.add(chunk.withColumns(members.subList(start, Math.min(members.size(), start + block))));
		}
		return pieces;
	}

}

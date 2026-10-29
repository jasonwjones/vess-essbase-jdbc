package com.jasonwjones.hyperion.vess.cells;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.apache.calcite.DataContext;

import com.jasonwjones.hyperion.vess.VessException;
import com.jasonwjones.hyperion.vess.outline.Dimension;
import com.jasonwjones.hyperion.vess.outline.Member;
import com.jasonwjones.hyperion.vess.outline.Outline;
import com.jasonwjones.hyperion.vess.schema.Column;
import com.jasonwjones.hyperion.vess.schema.Conditions;
import com.jasonwjones.hyperion.vess.schema.ForeignKey;
import com.jasonwjones.hyperion.vess.schema.Naming;
import com.jasonwjones.hyperion.vess.schema.VessTable;

/**
 * A cube's data as a table: a column for each dimension, holding a member of
 * it, and the value, a row for every value the cube has.
 *
 * <p>
 * CELLS has every member of every dimension, at every level, so it holds the
 * cube's totals and dynamic calculations as well as what was loaded. FACTS has
 * only the stored members at level 0 of each, which is what was loaded: a fact
 * table, whose values add up the way a SQL GROUP BY expects. Shared members
 * aren't repeated in either, and a cell that's #Missing isn't a row.
 *
 * <p>
 * Inserting a row writes its value into the cube, overwriting what was there;
 * updating one writes the new value; deleting one clears it.
 */
public final class CellsTable extends VessTable {

	private final CellSource source;

	private final String application;

	private final String cube;

	private final boolean facts;

	private final Function<Dimension, String> dimensionTables;

	private final Naming naming;

	private List<Column> columns;

	private List<Dimension> dimensions;

	/**
	 * @param facts true for only the stored members at level 0, false for
	 *            every member
	 * @param dimensionTables the name of each dimension's table, for the keys
	 *            from this one to them
	 * @param naming how the dimensions' names become the columns' names
	 */
	public CellsTable(String name, CellSource source, String application, String cube, boolean facts,
			Function<Dimension, String> dimensionTables, Naming naming) {
		super(name, facts ? "The values loaded into the cube, at level 0 of each dimension: the cube as a fact table"
				: "Every value in the cube, at any level, totals and dynamic calculations included; a row each");
		this.source = source;
		this.application = application;
		this.cube = cube;
		this.facts = facts;
		this.dimensionTables = dimensionTables;
		this.naming = naming;
	}

	private synchronized List<Dimension> dimensions() throws SQLException {
		if (dimensions == null) {
			dimensions = source.outline().getStandardDimensions();
		}
		return dimensions;
	}

	@Override
	public synchronized List<Column> getColumns() {
		if (columns == null) {
			List<Column> list = new ArrayList<>();
			Set<String> taken = new HashSet<>();
			try {
				for (Dimension dimension : dimensions()) {
					list.add(Column.text(naming.unique(taken, dimension.getName(), "Dimension"), Column.MEMBER_NAME).notNull().writable()
							.remarks("A member of " + dimension.getName() + (facts ? ", at level 0" : "")));
				}
			} catch (SQLException e) {
				throw new VessException.Unchecked(e);
			}
			list.add(Column.number(valueColumn(list)).writable().remarks("The cell's value"));
			columns = list;
		}
		return columns;
	}

	/**
	 * VALUE, unless a dimension already has that name.
	 */
	private static String valueColumn(List<Column> dimensions) {
		for (Column column : dimensions) {
			if (column.getName().equalsIgnoreCase("VALUE")) {
				return "CELL_VALUE";
			}
		}
		return "VALUE";
	}

	@Override
	public List<String> getPrimaryKey() {
		List<String> key = new ArrayList<>();
		List<Column> all = getColumns();
		for (int index = 0; index < all.size() - 1; index++) {
			key.add(all.get(index).getName());
		}
		return key;
	}

	@Override
	public List<ForeignKey> getForeignKeys() {
		List<ForeignKey> keys = new ArrayList<>();
		try {
			List<Dimension> dimensions = dimensions();
			List<Column> columns = getColumns();
			for (int index = 0; index < dimensions.size(); index++) {
				Dimension dimension = dimensions.get(index);
				String column = columns.get(index).getName();
				keys.add(ForeignKey.of(getName() + "_" + column, column, application, cube, dimensionTables.apply(dimension), "NAME"));
			}
		} catch (SQLException e) {
			throw new VessException.Unchecked(e);
		}
		return keys;
	}

	/**
	 * Each dimension's members before any conditions: all of them that aren't
	 * shared, or for FACTS, those stored at level 0.
	 */
	private List<List<Member>> domains(List<Dimension> dimensions) {
		List<List<Member>> domains = new ArrayList<>();
		for (Dimension dimension : dimensions) {
			List<Member> members = new ArrayList<>();
			for (Member member : dimension.getBaseMembers()) {
				if (!facts || (member.isLeaf() && member.isStored())) {
					members.add(member);
				}
			}
			domains.add(members);
		}
		return domains;
	}

	@Override
	protected Iterable<Object[]> rows(Conditions conditions) throws SQLException {
		Outline outline = source.outline();
		List<Dimension> dimensions = outline.getStandardDimensions();
		List<List<Member>> sets = MemberSets.narrow(dimensions, domains(dimensions), conditions);
		AtomicBoolean cancelled = cancelFlag(conditions.getRoot());
		String app = outline.getApplication();
		String cubeName = outline.getCube();
		long chunkCells = source.chunkCells();
		int chunkMembers = source.chunkMembers();
		return () -> new CellReader(CellPlan.chunks(dimensions, sets, chunkCells, chunkMembers), dimensions.size(), source::mdx, app,
				cubeName, cancelled);
	}

	private static AtomicBoolean cancelFlag(DataContext root) {
		if (root == null) {
			return null;
		}
		Object flag = root.get(DataContext.Variable.CANCEL_FLAG.camelName);
		return flag instanceof AtomicBoolean ? (AtomicBoolean) flag : null;
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
	public void insert(List<Object[]> rows) throws SQLException {
		source.checkWritable("write to " + application + "." + cube);
		List<Object[]> records = new ArrayList<>();
		for (Object[] row : rows) {
			records.add(record(row));
		}
		source.write(records);
	}

	@Override
	public void update(List<Object[]> before, List<Object[]> after) throws SQLException {
		source.checkWritable("write to " + application + "." + cube);
		// Cells that a row moved away from are cleared first, so that a cell
		// both cleared and written ends up written
		Map<List<Object>, Object[]> writes = new LinkedHashMap<>();
		for (Object[] row : after) {
			Object[] record = record(row);
			writes.put(key(record), record);
		}
		List<Object[]> records = new ArrayList<>();
		for (Object[] row : before) {
			Object[] record = record(row);
			if (!writes.containsKey(key(record))) {
				record[record.length - 1] = null;
				records.add(record);
			}
		}
		records.addAll(writes.values());
		source.write(records);
	}

	@Override
	public void delete(List<Object[]> rows) throws SQLException {
		source.checkWritable("clear cells of " + application + "." + cube);
		List<Object[]> records = new ArrayList<>();
		for (Object[] row : rows) {
			Object[] record = record(row);
			record[record.length - 1] = null;
			records.add(record);
		}
		source.write(records);
	}

	/**
	 * A row as a record to load, checking it names a member of every
	 * dimension that this table has.
	 */
	private Object[] record(Object[] row) throws SQLException {
		Outline outline = source.outline();
		List<Dimension> dimensions = dimensions();
		Object[] record = new Object[dimensions.size() + 1];
		for (int index = 0; index < dimensions.size(); index++) {
			Dimension dimension = dimensions.get(index);
			Object value = row[index];
			if (value == null) {
				throw new VessException("Every row written to " + getName() + " needs a member of " + dimension.getName());
			}
			Member member = outline.getMember(value.toString());
			if (member == null || member.getDimension() != dimension) {
				throw new VessException("There's no member " + value + " in " + dimension.getName());
			}
			if (facts && !(member.isLeaf() && member.isStored())) {
				throw new VessException(member.getName() + " isn't a stored member at level 0 of " + dimension.getName()
						+ ", so it isn't in " + getName() + "; write it through CELLS");
			}
			if (!member.isStored()) {
				// Essbase would take the value and keep nothing
				throw new VessException(member.getName() + " is " + member.getStorage() + ", so Essbase works out its values rather than"
						+ " storing them, and a value can't be written to it");
			}
			record[index] = member.getName();
		}
		record[dimensions.size()] = row[dimensions.size()];
		return record;
	}

	private static List<Object> key(Object[] record) {
		List<Object> key = new ArrayList<>();
		for (int index = 0; index < record.length - 1; index++) {
			key.add(record[index].toString().toLowerCase(java.util.Locale.ROOT));
		}
		return key;
	}

}

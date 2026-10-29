package com.jasonwjones.hyperion.vess.schema;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.calcite.DataContext;
import org.apache.calcite.linq4j.Enumerable;
import org.apache.calcite.linq4j.Linq4j;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.schema.ProjectableFilterableTable;
import org.apache.calcite.schema.impl.AbstractTable;

import com.jasonwjones.hyperion.vess.VessException;

/**
 * A table modelling something on an Essbase server. A table gives its rows as
 * plain Java values; it's handed the query's conditions, which it can use to
 * read less, but needn't, because the engine applies them to whatever it
 * gives. A table that can be changed says so, and turns rows that statements
 * insert, update or delete into changes on the server.
 */
public abstract class VessTable extends AbstractTable implements ProjectableFilterableTable {

	private final String name;

	private final String remarks;

	protected VessTable(String name, String remarks) {
		this.name = name;
		this.remarks = remarks;
	}

	public final String getName() {
		return name;
	}

	/**
	 * @return what the table holds, for tools that show descriptions
	 */
	public String getRemarks() {
		return remarks;
	}

	public abstract List<Column> getColumns();

	/**
	 * @return the columns that identify a row, or none if nothing does
	 */
	public List<String> getPrimaryKey() {
		return List.of();
	}

	/**
	 * @return the columns that name a row of another table
	 */
	public List<ForeignKey> getForeignKeys() {
		return List.of();
	}

	/**
	 * @param name a column's name, in any case
	 * @return the column's position, from 0, or -1 if there's no such column
	 */
	public int indexOf(String name) {
		List<Column> columns = getColumns();
		for (int index = 0; index < columns.size(); index++) {
			if (columns.get(index).getName().equalsIgnoreCase(name)) {
				return index;
			}
		}
		return -1;
	}

	@Override
	public RelDataType getRowType(RelDataTypeFactory factory) {
		RelDataTypeFactory.Builder builder = factory.builder();
		for (Column column : getColumns()) {
			builder.add(column.getName(), column.toRelType(factory));
		}
		return builder.build();
	}

	@Override
	public Enumerable<Object[]> scan(DataContext root, List<RexNode> filters, int[] projects) {
		List<Column> columns = getColumns();
		Iterable<Object[]> rows;
		try {
			rows = rows(new Conditions(filters, root, projects));
		} catch (SQLException e) {
			throw new VessException.Unchecked(e);
		}
		return Linq4j.asEnumerable(rows).select(row -> internal(columns, row, projects));
	}

	private static Object[] internal(List<Column> columns, Object[] row, int[] projects) {
		if (projects == null) {
			Object[] values = new Object[columns.size()];
			for (int index = 0; index < values.length; index++) {
				values[index] = columns.get(index).toInternal(index < row.length ? row[index] : null);
			}
			return values;
		}
		Object[] values = new Object[projects.length];
		for (int index = 0; index < projects.length; index++) {
			int column = projects[index];
			values[index] = columns.get(column).toInternal(column < row.length ? row[column] : null);
		}
		return values;
	}

	/**
	 * The table's rows, one value for each column. They're read as the engine
	 * asks for them, so a table that reads in pieces needn't read the rest of
	 * them when a query stops early.
	 *
	 * @param conditions the query's conditions, which say which rows it wants
	 */
	protected abstract Iterable<Object[]> rows(Conditions conditions) throws SQLException;

	public boolean canInsert() {
		return false;
	}

	public boolean canUpdate() {
		return false;
	}

	public boolean canDelete() {
		return false;
	}

	/**
	 * @param rows each row to add, with a value for every column, null for
	 *            those the statement didn't give
	 */
	public void insert(List<Object[]> rows) throws SQLException {
		throw readOnly("insert into");
	}

	/**
	 * @param before each row as it was
	 * @param after the same rows as the statement leaves them
	 */
	public void update(List<Object[]> before, List<Object[]> after) throws SQLException {
		throw readOnly("update");
	}

	public void delete(List<Object[]> rows) throws SQLException {
		throw readOnly("delete from");
	}

	protected VessException readOnly(String what) {
		return new VessException("Vess can't " + what + " " + name);
	}

	/**
	 * @return the names of the columns a statement may not set, of those it
	 *         changed between two rows
	 */
	protected List<String> changedReadOnly(Object[] before, Object[] after) {
		List<String> changed = new ArrayList<>();
		List<Column> columns = getColumns();
		for (int index = 0; index < columns.size(); index++) {
			if (!columns.get(index).isWritable() && !java.util.Objects.equals(before[index], after[index])) {
				changed.add(columns.get(index).getName());
			}
		}
		return changed;
	}

	@Override
	public String toString() {
		return name;
	}

}

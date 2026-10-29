package com.jasonwjones.hyperion.vess.schema;

import java.sql.SQLException;
import java.util.List;

import com.appliedolap.essbase.EssApiException;
import com.jasonwjones.hyperion.vess.VessException;

/**
 * A read-only table whose rows come from one function, which is most of them.
 */
public class ListTable extends VessTable {

	/**
	 * Reads a table's rows.
	 */
	@FunctionalInterface
	public interface Rows {

		Iterable<Object[]> read(Conditions conditions) throws SQLException;

	}

	private final List<Column> columns;

	private final List<String> primaryKey;

	private final Rows rows;

	public ListTable(String name, String remarks, List<Column> columns, List<String> primaryKey, Rows rows) {
		super(name, remarks);
		this.columns = columns;
		this.primaryKey = primaryKey;
		this.rows = rows;
	}

	@Override
	public List<Column> getColumns() {
		return columns;
	}

	@Override
	public List<String> getPrimaryKey() {
		return primaryKey;
	}

	@Override
	protected Iterable<Object[]> rows(Conditions conditions) throws SQLException {
		try {
			return rows.read(conditions);
		} catch (EssApiException e) {
			throw new VessException("Could not read " + getName() + ": " + e.getMessage(), e);
		}
	}

}

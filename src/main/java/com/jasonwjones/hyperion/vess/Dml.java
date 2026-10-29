package com.jasonwjones.hyperion.vess;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlDelete;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlInsert;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlUpdate;
import org.apache.calcite.sql.dialect.CalciteSqlDialect;

import com.jasonwjones.hyperion.vess.schema.Column;
import com.jasonwjones.hyperion.vess.schema.VessTable;

/**
 * Runs INSERT, UPDATE and DELETE statements against Vess's tables.
 *
 * <p>
 * Each is turned into a query the SQL engine answers, with the same
 * conditions and parameters: the rows an UPDATE or DELETE touches, with an
 * UPDATE's new values beside them, or the rows an INSERT gives. The table then
 * turns those rows into changes on the server. So a WHERE clause can say
 * anything a query can, and the rows changed are exactly the rows a SELECT
 * with it would show.
 */
final class Dml {

	private static final SqlDialect DIALECT = CalciteSqlDialect.DEFAULT;

	/**
	 * What a statement changes: the tables it can name, and the engine that
	 * finds their rows.
	 */
	interface Target {

		Connection engine();

		/**
		 * @param names the table's name, in up to three parts
		 */
		VessTable table(List<String> names) throws SQLException;

		boolean isReadOnly() throws SQLException;

	}

	private Dml() {
	}

	/**
	 * @param parameters the values for the statement's ? parameters, in order
	 * @return how many rows were changed
	 */
	static int execute(Target connection, String sql, List<Object> parameters) throws SQLException {
		SqlNode node = Parameters.inline(Engine.parse(Sql.withoutSemicolon(sql)), parameters);
		parameters = List.of();
		if (connection.isReadOnly()) {
			throw new VessException("The connection is read-only, so Vess won't run " + Sql.firstWord(Sql.withoutLeadingComments(sql)));
		}
		switch (node.getKind()) {
		case INSERT:
			return insert(connection, (SqlInsert) node, parameters);
		case UPDATE:
			return update(connection, (SqlUpdate) node, parameters);
		case DELETE:
			return delete(connection, (SqlDelete) node, parameters);
		default:
			throw new VessException("Vess runs INSERT, UPDATE and DELETE, but not " + node.getKind());
		}
	}

	private static VessTable table(Target connection, SqlNode target) throws SQLException {
		if (!(target instanceof SqlIdentifier)) {
			throw new VessException("Name the table to change: " + target);
		}
		return connection.table(((SqlIdentifier) target).names);
	}

	private static int insert(Target connection, SqlInsert insert, List<Object> parameters) throws SQLException {
		VessTable table = table(connection, insert.getTargetTable());
		if (!table.canInsert()) {
			throw new VessException("Rows can't be inserted into " + table.getName());
		}
		List<Column> columns = table.getColumns();
		int[] targets = targets(table, insert.getTargetColumnList());
		SqlNode source = insert.getSource();
		String query;
		if (source.getKind() == SqlKind.VALUES) {
			// Each value is cast to its column's type, so that a parameter has a
			// type for the engine to give it
			List<String> rows = new ArrayList<>();
			for (SqlNode row : ((SqlBasicCall) source).getOperandList()) {
				List<SqlNode> values = row.getKind() == SqlKind.ROW ? ((SqlCall) row).getOperandList() : List.of(row);
				if (values.size() != targets.length) {
					throw new VessException("The INSERT gives " + values.size() + " values for " + targets.length + " columns");
				}
				List<String> casts = new ArrayList<>();
				for (int index = 0; index < values.size(); index++) {
					casts.add(cast(values.get(index), columns.get(targets[index])));
				}
				rows.add("(" + String.join(", ", casts) + ")");
			}
			query = "VALUES " + String.join(", ", rows);
		} else {
			query = source.toSqlString(DIALECT).getSql();
		}
		List<Object[]> inserted = new ArrayList<>();
		for (Object[] values : run(connection.engine(), query, parameters)) {
			if (values.length != targets.length) {
				throw new VessException("The INSERT gives " + values.length + " values for " + targets.length + " columns");
			}
			Object[] row = new Object[columns.size()];
			for (int index = 0; index < targets.length; index++) {
				row[targets[index]] = convert(columns.get(targets[index]), values[index]);
			}
			inserted.add(row);
		}
		if (!inserted.isEmpty()) {
			table.insert(inserted);
		}
		return inserted.size();
	}

	private static int update(Target connection, SqlUpdate update, List<Object> parameters) throws SQLException {
		VessTable table = table(connection, update.getTargetTable());
		if (!table.canUpdate()) {
			throw new VessException("Rows of " + table.getName() + " can't be updated");
		}
		List<Column> columns = table.getColumns();
		int[] targets = targets(table, update.getTargetColumnList());
		List<SqlNode> expressions = update.getSourceExpressionList().getList();
		List<String> select = new ArrayList<>();
		for (Column column : columns) {
			select.add(quoted(column.getName()));
		}
		for (int index = 0; index < targets.length; index++) {
			select.add(cast(expressions.get(index), columns.get(targets[index])));
		}
		String query = "SELECT " + String.join(", ", select) + " FROM " + from(update.getTargetTable(), update.getAlias())
				+ where(update.getCondition());
		List<Object[]> before = new ArrayList<>();
		List<Object[]> after = new ArrayList<>();
		for (Object[] values : run(connection.engine(), query, parameters)) {
			Object[] old = new Object[columns.size()];
			for (int index = 0; index < columns.size(); index++) {
				old[index] = convert(columns.get(index), values[index]);
			}
			Object[] changed = Arrays.copyOf(old, old.length);
			for (int index = 0; index < targets.length; index++) {
				changed[targets[index]] = convert(columns.get(targets[index]), values[columns.size() + index]);
			}
			before.add(old);
			after.add(changed);
		}
		if (!before.isEmpty()) {
			table.update(before, after);
		}
		return before.size();
	}

	private static int delete(Target connection, SqlDelete delete, List<Object> parameters) throws SQLException {
		VessTable table = table(connection, delete.getTargetTable());
		if (!table.canDelete()) {
			throw new VessException("Rows can't be deleted from " + table.getName());
		}
		List<Column> columns = table.getColumns();
		List<String> select = new ArrayList<>();
		for (Column column : columns) {
			select.add(quoted(column.getName()));
		}
		String query = "SELECT " + String.join(", ", select) + " FROM " + from(delete.getTargetTable(), delete.getAlias())
				+ where(delete.getCondition());
		List<Object[]> rows = new ArrayList<>();
		for (Object[] values : run(connection.engine(), query, parameters)) {
			Object[] row = new Object[columns.size()];
			for (int index = 0; index < columns.size(); index++) {
				row[index] = convert(columns.get(index), values[index]);
			}
			rows.add(row);
		}
		if (!rows.isEmpty()) {
			table.delete(rows);
		}
		return rows.size();
	}

	/**
	 * The positions of the columns a statement names, or of them all if it
	 * names none.
	 */
	private static int[] targets(VessTable table, SqlNodeList columnList) throws VessException {
		List<Column> columns = table.getColumns();
		if (columnList == null || columnList.isEmpty()) {
			int[] all = new int[columns.size()];
			for (int index = 0; index < all.length; index++) {
				all[index] = index;
			}
			return all;
		}
		int[] targets = new int[columnList.size()];
		for (int index = 0; index < targets.length; index++) {
			SqlIdentifier identifier = (SqlIdentifier) columnList.get(index);
			String name = identifier.names.get(identifier.names.size() - 1);
			targets[index] = table.indexOf(name);
			if (targets[index] < 0) {
				throw new VessException(table.getName() + " has no column " + name);
			}
		}
		return targets;
	}

	private static String from(SqlNode target, SqlIdentifier alias) {
		return target.toSqlString(DIALECT).getSql() + (alias == null ? "" : " AS " + alias.toSqlString(DIALECT).getSql());
	}

	private static String where(SqlNode condition) {
		return condition == null ? "" : " WHERE " + condition.toSqlString(DIALECT).getSql();
	}

	private static String cast(SqlNode value, Column column) {
		String type = column.getTypeName();
		if (type.equals("TIMESTAMP")) {
			type = "TIMESTAMP(3)";
		}
		return "CAST((" + value.toSqlString(DIALECT).getSql() + ") AS " + type + ")";
	}

	private static String quoted(String name) {
		return '"' + name.replace("\"", "\"\"") + '"';
	}

	private static Object convert(Column column, Object value) throws VessException {
		try {
			return column.fromStatement(value);
		} catch (IllegalArgumentException e) {
			throw new VessException(e.getMessage(), e);
		}
	}

	private static List<Object[]> run(Connection engine, String query, List<Object> parameters) throws SQLException {
		List<Object[]> rows = new ArrayList<>();
		try (PreparedStatement statement = engine.prepareStatement(query)) {
			Parameters.bind(statement, parameters);
			try (ResultSet results = statement.executeQuery()) {
				int width = results.getMetaData().getColumnCount();
				while (results.next()) {
					Object[] row = new Object[width];
					for (int index = 0; index < width; index++) {
						row[index] = results.getObject(index + 1);
					}
					rows.add(row);
				}
			}
		} catch (SQLException e) {
			throw VessStatement.translate(e);
		}
		return rows;
	}

}

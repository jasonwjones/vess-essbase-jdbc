package com.jasonwjones.hyperion.vess;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.calcite.sql.dialect.CalciteSqlDialect;

import com.jasonwjones.hyperion.thriller.ObjectResultSet;
import com.jasonwjones.hyperion.thriller.ThrillerResultSetMetaData;
import com.jasonwjones.hyperion.thriller.table.ThrillerColumn;
import com.jasonwjones.hyperion.vess.schema.Column;
import com.jasonwjones.hyperion.vess.schema.Procedure;
import com.jasonwjones.hyperion.vess.schema.ServerSchema;
import com.jasonwjones.hyperion.vess.schema.VessSchema;

/**
 * Runs CALL statements: a cube's calc scripts, and the procedures Vess adds,
 * such as RUN_CALC. They're written CALL Sample.Basic.CalcAll(), or with the
 * JDBC escape {call ...}, or EXEC.
 */
final class Calls {

	private static final Pattern CALL = Pattern.compile("^\\{?\\s*(?:\\?\\s*=\\s*)?(?:CALL|EXEC|EXECUTE)\\s+(.+?)\\s*\\}?$",
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	private Calls() {
	}

	/**
	 * A CALL statement, taken apart.
	 *
	 * @param names the procedure's name, in up to three parts
	 * @param arguments the text of its arguments, or empty if it has none
	 */
	record Parsed(List<String> names, String arguments) {
	}

	static Parsed parse(String sql) throws VessException {
		String text = Sql.withoutSemicolon(Sql.withoutLeadingComments(sql));
		Matcher matcher = CALL.matcher(text);
		if (!matcher.matches()) {
			throw new VessException("Expected CALL procedure(arguments), but found: " + sql);
		}
		String rest = matcher.group(1).strip();
		List<String> names = new ArrayList<>();
		int index = 0;
		while (index < rest.length()) {
			StringBuilder name = new StringBuilder();
			if (rest.charAt(index) == '"') {
				index++;
				while (index < rest.length()) {
					char c = rest.charAt(index++);
					if (c == '"') {
						if (index < rest.length() && rest.charAt(index) == '"') {
							name.append('"');
							index++;
						} else {
							break;
						}
					} else {
						name.append(c);
					}
				}
			} else {
				while (index < rest.length() && rest.charAt(index) != '.' && rest.charAt(index) != '(' && !Character.isWhitespace(rest.charAt(index))) {
					name.append(rest.charAt(index++));
				}
			}
			names.add(name.toString());
			while (index < rest.length() && Character.isWhitespace(rest.charAt(index))) {
				index++;
			}
			if (index < rest.length() && rest.charAt(index) == '.') {
				index++;
				while (index < rest.length() && Character.isWhitespace(rest.charAt(index))) {
					index++;
				}
				continue;
			}
			break;
		}
		String arguments = "";
		if (index < rest.length()) {
			String tail = rest.substring(index).strip();
			if (!tail.startsWith("(") || !tail.endsWith(")")) {
				throw new VessException("Expected the arguments in parentheses after " + String.join(".", names) + ", but found: " + tail);
			}
			arguments = tail.substring(1, tail.length() - 1).strip();
		}
		if (names.isEmpty() || names.stream().anyMatch(String::isEmpty) || names.size() > 3) {
			throw new VessException("Expected a procedure's name after CALL, but found: " + rest);
		}
		return new Parsed(names, arguments);
	}

	static ResultSet call(VessConnection connection, String sql, List<Object> parameters) throws SQLException {
		Parsed parsed = parse(sql);
		Procedure procedure = find(connection, parsed.names());
		if (procedure.changesServer() && connection.isReadOnly()) {
			throw new VessException("The connection is read-only, so Vess won't run " + procedure.getName());
		}
		List<Object> arguments = arguments(connection, parsed.arguments(), parameters);
		if (arguments.size() != procedure.getParameters().size()) {
			throw new VessException(procedure.getName() + " takes " + procedure.getParameters().size() + " argument"
					+ (procedure.getParameters().size() == 1 ? "" : "s") + ", not " + arguments.size());
		}
		Procedure.Result result = procedure.run(arguments);
		if (result.resultSet() != null) {
			return result.resultSet();
		}
		return resultSet(result.columns(), result.rows());
	}

	/**
	 * The procedure a name means: in full, Application.Cube.Procedure or
	 * Essbase.Server.Procedure; or within the cube or application the
	 * connection is working in; or the server's.
	 */
	static Procedure find(VessConnection connection, List<String> names) throws SQLException {
		String name = names.get(names.size() - 1);
		List<VessSchema> candidates = new ArrayList<>();
		String catalog = connection.getCatalog();
		String schema = connection.getSchema();
		if (names.size() == 3) {
			candidates.add(connection.getModel().schema(names.get(0), names.get(1)));
		} else if (names.size() == 2) {
			if (names.get(0).equalsIgnoreCase(ServerSchema.NAME)) {
				candidates.add(connection.getModel().getServerSchema());
			} else {
				candidates.add(connection.getModel().schema(catalog, names.get(0)));
			}
		} else {
			candidates.add(connection.getModel().schema(catalog, schema));
			candidates.add(connection.getModel().getServerSchema());
		}
		for (VessSchema candidate : candidates) {
			if (candidate == null) {
				continue;
			}
			for (Procedure procedure : candidate.getProcedures()) {
				if (procedure.getName().equalsIgnoreCase(name)) {
					return procedure;
				}
			}
		}
		throw new VessException("There's no procedure " + String.join(".", names) + (names.size() < 3 && schema == null
				? "; name it in full, as Application.Cube." + name + ", or choose a cube to work in first" : ""));
	}

	/**
	 * The arguments' values, worked out by the SQL engine so they can be any
	 * expression, ? parameters included.
	 */
	private static List<Object> arguments(VessConnection connection, String text, List<Object> parameters) throws SQLException {
		List<Object> arguments = new ArrayList<>();
		if (text.isEmpty()) {
			return arguments;
		}
		String query = Parameters.inline(Engine.parse("SELECT " + text), parameters).toSqlString(CalciteSqlDialect.DEFAULT).getSql();
		try (PreparedStatement statement = connection.getEngine().prepareStatement(query)) {
			try (ResultSet results = statement.executeQuery()) {
				if (results.next()) {
					for (int index = 1; index <= results.getMetaData().getColumnCount(); index++) {
						arguments.add(results.getObject(index));
					}
				}
			}
		} catch (SQLException e) {
			throw VessStatement.translate(e);
		}
		return arguments;
	}

	static ResultSet resultSet(List<Column> columns, List<Object[]> rows) throws SQLException {
		ThrillerColumn[] described = new ThrillerColumn[columns.size()];
		for (int index = 0; index < described.length; index++) {
			Column column = columns.get(index);
			described[index] = new ThrillerColumn(column.getType(), column.getSize());
			described[index].setColumnName(column.getName());
		}
		return new ObjectResultSet(rows, new ThrillerResultSetMetaData(described));
	}

}

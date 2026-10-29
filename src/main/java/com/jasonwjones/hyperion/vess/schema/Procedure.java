package com.jasonwjones.hyperion.vess.schema;

import java.sql.SQLException;
import java.util.List;

/**
 * Something a CALL statement runs, such as a calc script. A procedure takes
 * values for its parameters and answers with a table of its own, often a
 * single row saying what happened.
 */
public final class Procedure {

	/**
	 * Runs a procedure.
	 */
	@FunctionalInterface
	public interface Body {

		Result run(List<Object> arguments) throws SQLException;

	}

	/**
	 * What a procedure answers with: columns and rows, or a result set of its
	 * own, which an MDX script gives.
	 *
	 * @param columns the columns of the rows
	 * @param rows the rows
	 * @param resultSet a result set to answer with instead, or null
	 */
	public record Result(List<Column> columns, List<Object[]> rows, java.sql.ResultSet resultSet) {

		public static Result of(List<Column> columns, List<Object[]> rows) {
			return new Result(columns, rows, null);
		}

		public static Result of(java.sql.ResultSet resultSet) {
			return new Result(List.of(), List.of(), resultSet);
		}

	}

	private final String name;

	private final String remarks;

	private final List<Column> parameters;

	private final boolean changesServer;

	private final Body body;

	public Procedure(String name, String remarks, List<Column> parameters, boolean changesServer, Body body) {
		this.name = name;
		this.remarks = remarks;
		this.parameters = parameters;
		this.changesServer = changesServer;
		this.body = body;
	}

	public String getName() {
		return name;
	}

	public String getRemarks() {
		return remarks;
	}

	public List<Column> getParameters() {
		return parameters;
	}

	/**
	 * @return whether running it changes the server, which a read-only
	 *         connection won't do
	 */
	public boolean changesServer() {
		return changesServer;
	}

	public Result run(List<Object> arguments) throws SQLException {
		return body.run(arguments);
	}

}

package com.jasonwjones.hyperion.vess;

import java.math.BigDecimal;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Locale;

import org.apache.calcite.sql.SqlDynamicParam;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.util.SqlShuttle;

/**
 * Values for a statement's ? parameters.
 *
 * <p>
 * A statement Vess rewrites has its parameters written into it as literals,
 * since a parameter inside an expression, such as VALUE || ?, has no type for
 * the engine to give it. A query the engine prepares itself is given its
 * values converted to the types it worked out, so text typed into a tool's
 * prompt still serves where a number is wanted.
 */
final class Parameters {

	private Parameters() {
	}

	/**
	 * A statement with each parameter replaced by its value.
	 */
	static SqlNode inline(SqlNode node, List<Object> values) throws VessException {
		try {
			return node.accept(new SqlShuttle() {

				@Override
				public SqlNode visit(SqlDynamicParam parameter) {
					int index = parameter.getIndex();
					if (index >= values.size()) {
						throw new IllegalArgumentException("There's no value for parameter " + (index + 1));
					}
					return literal(values.get(index));
				}

			});
		} catch (IllegalArgumentException e) {
			throw new VessException(e.getMessage(), e);
		}
	}

	static SqlNode literal(Object value) {
		SqlParserPos position = SqlParserPos.ZERO;
		if (value == null) {
			return SqlLiteral.createNull(position);
		}
		if (value instanceof Boolean) {
			return SqlLiteral.createBoolean((Boolean) value, position);
		}
		if (value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte) {
			return SqlLiteral.createExactNumeric(value.toString(), position);
		}
		if (value instanceof BigDecimal) {
			return SqlLiteral.createExactNumeric(((BigDecimal) value).toPlainString(), position);
		}
		if (value instanceof Double || value instanceof Float) {
			double number = ((Number) value).doubleValue();
			if (Double.isNaN(number) || Double.isInfinite(number)) {
				throw new IllegalArgumentException("A parameter can't be " + number);
			}
			return SqlLiteral.createExactNumeric(BigDecimal.valueOf(number).toPlainString(), position);
		}
		return SqlLiteral.createCharString(value.toString(), position);
	}

	/**
	 * Gives a prepared statement its values, converting text to the type the
	 * engine wants where it can.
	 */
	static void bind(PreparedStatement statement, List<Object> values) throws SQLException {
		ParameterMetaData described = null;
		try {
			described = statement.getParameterMetaData();
		} catch (SQLException | RuntimeException e) {
			// Bound as given, then
		}
		for (int index = 0; index < values.size(); index++) {
			Object value = values.get(index);
			int type = Types.OTHER;
			if (described != null) {
				try {
					type = described.getParameterType(index + 1);
				} catch (SQLException | RuntimeException e) {
					type = Types.OTHER;
				}
			}
			statement.setObject(index + 1, convert(value, type));
		}
	}

	static Object convert(Object value, int type) throws VessException {
		if (!(value instanceof String)) {
			return value;
		}
		String text = ((String) value).trim();
		try {
			switch (type) {
			case Types.INTEGER:
			case Types.SMALLINT:
			case Types.TINYINT:
				return Integer.valueOf(text);
			case Types.BIGINT:
				return Long.valueOf(text);
			case Types.DOUBLE:
			case Types.FLOAT:
			case Types.REAL:
				return Double.valueOf(text);
			case Types.DECIMAL:
			case Types.NUMERIC:
				return new BigDecimal(text);
			case Types.BOOLEAN:
				return Boolean.valueOf(text.toLowerCase(Locale.ROOT).matches("true|t|yes|y|1"));
			case Types.DATE:
				return java.sql.Date.valueOf(text);
			case Types.TIMESTAMP:
				return java.sql.Timestamp.valueOf(text);
			default:
				return value;
			}
		} catch (IllegalArgumentException e) {
			throw new VessException("The parameter '" + value + "' isn't a " + typeName(type), e);
		}
	}

	private static String typeName(int type) {
		switch (type) {
		case Types.DATE:
			return "date, as yyyy-mm-dd";
		case Types.TIMESTAMP:
			return "timestamp, as yyyy-mm-dd hh:mm:ss";
		case Types.BOOLEAN:
			return "true or false";
		default:
			return "number";
		}
	}

}

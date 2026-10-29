package com.jasonwjones.hyperion.vess.schema;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.sql.type.SqlTypeName;

/**
 * A column of a Vess table, described once for both the SQL engine and the
 * JDBC metadata tools read.
 *
 * <p>
 * Tables give their values as plain Java: String, Integer, Long, Double,
 * Boolean, BigDecimal, Instant for a timestamp and LocalDate for a date. Each
 * column turns its values into what the engine expects, and back again for the
 * values a statement writes.
 */
public final class Column {

	/** The longest member name or alias Essbase allows */
	public static final int MEMBER_NAME = 1024;

	/** Application and cube names */
	public static final int OBJECT_NAME = 30;

	/** Substitution variable names and values */
	public static final int VARIABLE_NAME = 320;

	public static final int VARIABLE_VALUE = 256;

	/** Text with no particular limit, such as a formula or a script */
	public static final int TEXT = 0;

	private final String name;

	private final int type;

	private final int size;

	private final boolean nullable;

	private final boolean writable;

	private final String remarks;

	private Column(String name, int type, int size, boolean nullable, boolean writable, String remarks) {
		this.name = name;
		this.type = type;
		this.size = size;
		this.nullable = nullable;
		this.writable = writable;
		this.remarks = remarks;
	}

	public static Column text(String name, int size) {
		return new Column(name, Types.VARCHAR, size, true, false, null);
	}

	public static Column integer(String name) {
		return new Column(name, Types.INTEGER, 10, true, false, null);
	}

	public static Column bigint(String name) {
		return new Column(name, Types.BIGINT, 19, true, false, null);
	}

	public static Column number(String name) {
		return new Column(name, Types.DOUBLE, 15, true, false, null);
	}

	public static Column bool(String name) {
		return new Column(name, Types.BOOLEAN, 1, true, false, null);
	}

	public static Column timestamp(String name) {
		return new Column(name, Types.TIMESTAMP, 23, true, false, null);
	}

	public static Column date(String name) {
		return new Column(name, Types.DATE, 10, true, false, null);
	}

	/**
	 * @return this column, never null
	 */
	public Column notNull() {
		return new Column(name, type, size, false, writable, remarks);
	}

	/**
	 * @return this column, which statements may set
	 */
	public Column writable() {
		return new Column(name, type, size, nullable, true, remarks);
	}

	/**
	 * @return this column, described for tools that show descriptions
	 */
	public Column remarks(String remarks) {
		return new Column(name, type, size, nullable, writable, remarks);
	}

	/**
	 * @return this column under another name
	 */
	public Column named(String name) {
		return new Column(name, type, size, nullable, writable, remarks);
	}

	public String getName() {
		return name;
	}

	/**
	 * @return the type, from {@link java.sql.Types}
	 */
	public int getType() {
		return type;
	}

	public String getTypeName() {
		switch (type) {
		case Types.VARCHAR:
			return "VARCHAR";
		case Types.INTEGER:
			return "INTEGER";
		case Types.BIGINT:
			return "BIGINT";
		case Types.DOUBLE:
			return "DOUBLE";
		case Types.BOOLEAN:
			return "BOOLEAN";
		case Types.TIMESTAMP:
			return "TIMESTAMP";
		case Types.DATE:
			return "DATE";
		default:
			return "OTHER";
		}
	}

	/**
	 * @return the length of a text column, 0 for text of any length; the
	 *         precision of anything else
	 */
	public int getSize() {
		return size;
	}

	public boolean isNullable() {
		return nullable;
	}

	public boolean isWritable() {
		return writable;
	}

	public String getRemarks() {
		return remarks;
	}

	RelDataType toRelType(RelDataTypeFactory factory) {
		RelDataType relType;
		switch (type) {
		case Types.VARCHAR:
			relType = size > 0 ? factory.createSqlType(SqlTypeName.VARCHAR, size) : factory.createSqlType(SqlTypeName.VARCHAR);
			break;
		case Types.INTEGER:
			relType = factory.createSqlType(SqlTypeName.INTEGER);
			break;
		case Types.BIGINT:
			relType = factory.createSqlType(SqlTypeName.BIGINT);
			break;
		case Types.DOUBLE:
			relType = factory.createSqlType(SqlTypeName.DOUBLE);
			break;
		case Types.BOOLEAN:
			relType = factory.createSqlType(SqlTypeName.BOOLEAN);
			break;
		case Types.TIMESTAMP:
			relType = factory.createSqlType(SqlTypeName.TIMESTAMP, 3);
			break;
		case Types.DATE:
			relType = factory.createSqlType(SqlTypeName.DATE);
			break;
		default:
			throw new IllegalStateException("No SQL type for " + type);
		}
		return factory.createTypeWithNullability(relType, nullable);
	}

	/**
	 * A table's value, as the engine holds it: a timestamp as milliseconds and
	 * a date as days, both counted from 1970.
	 */
	Object toInternal(Object value) {
		if (value == null) {
			return null;
		}
		switch (type) {
		case Types.VARCHAR:
			return value.toString();
		case Types.INTEGER:
			return value instanceof Integer ? value : Integer.valueOf(((Number) value).intValue());
		case Types.BIGINT:
			return value instanceof Long ? value : Long.valueOf(((Number) value).longValue());
		case Types.DOUBLE:
			return value instanceof Double ? value : Double.valueOf(((Number) value).doubleValue());
		case Types.TIMESTAMP:
			// The engine's timestamps have no time zone, and are shown as they
			// are; so an instant is held as the time it was here
			long millis = ((Instant) value).toEpochMilli();
			return millis + TimeZone.getDefault().getOffset(millis);
		case Types.DATE:
			return (int) ((LocalDate) value).toEpochDay();
		default:
			return value;
		}
	}

	/**
	 * A value a statement gives this column, as a table takes it.
	 *
	 * @throws IllegalArgumentException if it can't be one
	 */
	public Object fromStatement(Object value) {
		if (value == null) {
			return null;
		}
		try {
			switch (type) {
			case Types.VARCHAR:
				return value.toString();
			case Types.INTEGER:
				return value instanceof Number ? ((Number) value).intValue() : Integer.parseInt(value.toString().trim());
			case Types.BIGINT:
				return value instanceof Number ? ((Number) value).longValue() : Long.parseLong(value.toString().trim());
			case Types.DOUBLE:
				return value instanceof Number ? ((Number) value).doubleValue() : Double.parseDouble(value.toString().trim());
			case Types.BOOLEAN:
				if (value instanceof Boolean) {
					return value;
				}
				return Boolean.valueOf(value.toString().trim().toLowerCase(Locale.ROOT).matches("true|t|yes|y|1"));
			case Types.TIMESTAMP:
				return value instanceof Timestamp ? ((Timestamp) value).toInstant() : value;
			case Types.DATE:
				return value instanceof java.sql.Date ? ((java.sql.Date) value).toLocalDate() : value;
			default:
				return value instanceof BigDecimal ? ((BigDecimal) value).doubleValue() : value;
			}
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(name + " takes a number, not '" + value + "'", e);
		}
	}

	@Override
	public String toString() {
		return name + " " + getTypeName() + (type == Types.VARCHAR && size > 0 ? "(" + size + ")" : "");
	}

}

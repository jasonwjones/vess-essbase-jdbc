package com.jasonwjones.hyperion.vess;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.calcite.sql.SqlJdbcFunctionCall;
import org.apache.calcite.sql.parser.SqlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jasonwjones.hyperion.thriller.ObjectResultSet;
import com.jasonwjones.hyperion.thriller.ThrillerResultSetMetaData;
import com.jasonwjones.hyperion.thriller.table.ThrillerColumn;
import com.jasonwjones.hyperion.vess.schema.ApplicationSchema;
import com.jasonwjones.hyperion.vess.schema.Column;
import com.jasonwjones.hyperion.vess.schema.CubeSchema;
import com.jasonwjones.hyperion.vess.schema.ForeignKey;
import com.jasonwjones.hyperion.vess.schema.OutlineFunction;
import com.jasonwjones.hyperion.vess.schema.Procedure;
import com.jasonwjones.hyperion.vess.schema.VessSchema;
import com.jasonwjones.hyperion.vess.schema.VessTable;

/**
 * What a tool sees when it browses a Vess connection: a catalog for each
 * application, and one for the server's own tables; a schema for each cube;
 * tables with their columns, keys and descriptions; calc scripts as
 * procedures; and the outline functions.
 */
public class VessDatabaseMetaData implements DatabaseMetaData {

	private static final Logger logger = LoggerFactory.getLogger(VessDatabaseMetaData.class);

	/** How long the text in a column of no particular length is said to be */
	private static final int LONG_TEXT = 65_535;

	private final VessConnection connection;

	VessDatabaseMetaData(VessConnection connection) {
		this.connection = connection;
	}

	/**
	 * A result with the columns JDBC defines for a metadata query: each named,
	 * and typed by the letter after its name's colon, I for a number, B for a
	 * flag, and text otherwise.
	 */
	private static ResultSet result(List<Object[]> rows, String... columns) throws SQLException {
		ThrillerColumn[] described = new ThrillerColumn[columns.length];
		for (int index = 0; index < columns.length; index++) {
			String[] parts = columns[index].split(":");
			int type = parts.length < 2 ? Types.VARCHAR : parts[1].equals("I") ? Types.INTEGER : parts[1].equals("S") ? Types.SMALLINT
					: parts[1].equals("B") ? Types.BOOLEAN : Types.VARCHAR;
			described[index] = new ThrillerColumn(type, type == Types.VARCHAR ? 1024 : 10);
			described[index].setColumnName(parts[0]);
		}
		return new ObjectResultSet(rows, new ThrillerResultSetMetaData(described));
	}

	/**
	 * Whether a name matches a JDBC search pattern, where % is any run of
	 * characters and _ any one. A null pattern matches everything. Essbase
	 * names aren't case sensitive, so neither is this.
	 */
	static boolean matches(String pattern, String name) {
		if (pattern == null || pattern.equals("%")) {
			return true;
		}
		StringBuilder regex = new StringBuilder();
		boolean escaped = false;
		for (char c : pattern.toCharArray()) {
			if (escaped) {
				regex.append(Pattern.quote(String.valueOf(c)));
				escaped = false;
			} else if (c == '\\') {
				escaped = true;
			} else if (c == '%') {
				regex.append(".*");
			} else if (c == '_') {
				regex.append('.');
			} else {
				regex.append(Pattern.quote(String.valueOf(c)));
			}
		}
		return Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(name).matches();
	}

	/**
	 * The schemas in a catalog, or in every catalog for null. A schema that
	 * can't be read, such as a cube of an application that won't answer, is
	 * left out rather than failing the whole tree.
	 */
	private List<VessSchema> schemas(String catalog, String schemaPattern) {
		List<VessSchema> schemas = new ArrayList<>();
		if (catalog != null && catalog.isEmpty()) {
			return schemas;
		}
		String serverCatalog = connection.getContext().getSettings().serverCatalog();
		if (catalog == null || catalog.equalsIgnoreCase(serverCatalog)) {
			VessSchema server = connection.getModel().getServerSchema();
			if (matches(schemaPattern, server.getSchemaName())) {
				schemas.add(server);
			}
		}
		for (ApplicationSchema application : connection.getModel().getApplications()) {
			if (catalog != null && !catalog.equalsIgnoreCase(application.getName())) {
				continue;
			}
			try {
				for (CubeSchema cube : application.getCubeSchemas()) {
					if (matches(schemaPattern, cube.getSchemaName())) {
						schemas.add(cube);
					}
				}
			} catch (RuntimeException e) {
				logger.warn("Left out the cubes of {}: {}", application.getName(), e.getMessage());
			}
		}
		return schemas;
	}

	private static List<VessTable> tables(VessSchema schema) {
		try {
			return schema.getBrowsableTables();
		} catch (RuntimeException e) {
			logger.warn("Left out the tables of {}.{}: {}", schema.getCatalogName(), schema.getSchemaName(), e.getMessage());
			return List.of();
		}
	}

	private VessTable table(String catalog, String schema, String table) {
		for (VessSchema each : schemas(catalog, schema == null ? null : escape(schema))) {
			for (VessTable candidate : tables(each)) {
				if (candidate.getName().equalsIgnoreCase(table)) {
					return candidate;
				}
			}
		}
		return null;
	}

	private VessSchema schemaOf(String catalog, String schema, String table) {
		for (VessSchema each : schemas(catalog, schema == null ? null : escape(schema))) {
			for (VessTable candidate : tables(each)) {
				if (candidate.getName().equalsIgnoreCase(table)) {
					return each;
				}
			}
		}
		return null;
	}

	/**
	 * A name as a pattern that matches only itself.
	 */
	private static String escape(String name) {
		return name.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private static int size(Column column) {
		return column.getType() == Types.VARCHAR && column.getSize() == 0 ? LONG_TEXT : column.getSize();
	}

	@Override
	public ResultSet getCatalogs() throws SQLException {
		List<Object[]> rows = new ArrayList<>();
		rows.add(new Object[] { connection.getContext().getSettings().serverCatalog() });
		for (ApplicationSchema application : connection.getModel().getApplications()) {
			rows.add(new Object[] { application.getName() });
		}
		rows.sort(Comparator.comparing(row -> ((String) row[0]).toLowerCase()));
		return result(rows, "TABLE_CAT");
	}

	@Override
	public ResultSet getSchemas() throws SQLException {
		return getSchemas(null, null);
	}

	@Override
	public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
		List<Object[]> rows = new ArrayList<>();
		for (VessSchema schema : schemas(catalog, schemaPattern)) {
			rows.add(new Object[] { schema.getSchemaName(), schema.getCatalogName() });
		}
		rows.sort(Comparator.comparing((Object[] row) -> ((String) row[1]).toLowerCase()).thenComparing(row -> ((String) row[0]).toLowerCase()));
		return result(rows, "TABLE_SCHEM", "TABLE_CATALOG");
	}

	@Override
	public ResultSet getTableTypes() throws SQLException {
		List<Object[]> rows = new ArrayList<>();
		rows.add(new Object[] { "TABLE" });
		return result(rows, "TABLE_TYPE");
	}

	@Override
	public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types) throws SQLException {
		List<Object[]> rows = new ArrayList<>();
		boolean wantsTables = types == null;
		if (types != null) {
			for (String type : types) {
				wantsTables |= "TABLE".equalsIgnoreCase(type);
			}
		}
		if (wantsTables) {
			for (VessSchema schema : schemas(catalog, schemaPattern)) {
				for (VessTable table : tables(schema)) {
					if (matches(tableNamePattern, table.getName())) {
						rows.add(new Object[] { schema.getCatalogName(), schema.getSchemaName(), table.getName(), "TABLE", table.getRemarks(),
								null, null, null, null, null });
					}
				}
			}
		}
		return result(rows, "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "TABLE_TYPE", "REMARKS", "TYPE_CAT", "TYPE_SCHEM", "TYPE_NAME",
				"SELF_REFERENCING_COL_NAME", "REF_GENERATION");
	}

	@Override
	public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
		List<Object[]> rows = new ArrayList<>();
		for (VessSchema schema : schemas(catalog, schemaPattern)) {
			for (VessTable table : tables(schema)) {
				if (!matches(tableNamePattern, table.getName())) {
					continue;
				}
				List<Column> columns;
				try {
					columns = table.getColumns();
				} catch (RuntimeException e) {
					logger.warn("Left out the columns of {}: {}", table.getName(), e.getMessage());
					continue;
				}
				for (int index = 0; index < columns.size(); index++) {
					Column column = columns.get(index);
					if (!matches(columnNamePattern, column.getName())) {
						continue;
					}
					boolean text = column.getType() == Types.VARCHAR;
					rows.add(new Object[] { schema.getCatalogName(), schema.getSchemaName(), table.getName(), column.getName(), column.getType(),
							column.getTypeName(), size(column), null, column.getType() == Types.DOUBLE ? 15 : 0,
							text ? null : 10, column.isNullable() ? columnNullable : columnNoNulls, column.getRemarks(), null, null, null,
							text ? size(column) * 4 : null, index + 1, column.isNullable() ? "YES" : "NO", null, null, null, null, "NO", "NO" });
				}
			}
		}
		return result(rows, "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "COLUMN_NAME", "DATA_TYPE:I", "TYPE_NAME", "COLUMN_SIZE:I",
				"BUFFER_LENGTH:I", "DECIMAL_DIGITS:I", "NUM_PREC_RADIX:I", "NULLABLE:I", "REMARKS", "COLUMN_DEF", "SQL_DATA_TYPE:I",
				"SQL_DATETIME_SUB:I", "CHAR_OCTET_LENGTH:I", "ORDINAL_POSITION:I", "IS_NULLABLE", "SCOPE_CATALOG", "SCOPE_SCHEMA",
				"SCOPE_TABLE", "SOURCE_DATA_TYPE:S", "IS_AUTOINCREMENT", "IS_GENERATEDCOLUMN");
	}

	@Override
	public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
		List<Object[]> rows = new ArrayList<>();
		VessSchema owner = schemaOf(catalog, schema, table);
		VessTable found = table(catalog, schema, table);
		if (owner != null && found != null) {
			List<String> key = found.getPrimaryKey();
			for (int index = 0; index < key.size(); index++) {
				rows.add(new Object[] { owner.getCatalogName(), owner.getSchemaName(), found.getName(), key.get(index), index + 1,
						"PK_" + found.getName() });
			}
		}
		return result(rows, "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "COLUMN_NAME", "KEY_SEQ:S", "PK_NAME");
	}

	private static final String[] KEY_COLUMNS = { "PKTABLE_CAT", "PKTABLE_SCHEM", "PKTABLE_NAME", "PKCOLUMN_NAME", "FKTABLE_CAT",
			"FKTABLE_SCHEM", "FKTABLE_NAME", "FKCOLUMN_NAME", "KEY_SEQ:S", "UPDATE_RULE:S", "DELETE_RULE:S", "FK_NAME", "PK_NAME",
			"DEFERRABILITY:S" };

	private static void addKey(List<Object[]> rows, VessSchema schema, VessTable table, ForeignKey key) {
		for (int index = 0; index < key.columns().size(); index++) {
			rows.add(new Object[] { key.catalog(), key.schema(), key.table(), key.referenced().get(index), schema.getCatalogName(),
					schema.getSchemaName(), table.getName(), key.columns().get(index), index + 1, (int) importedKeyNoAction,
					(int) importedKeyNoAction, key.name(), "PK_" + key.table(), (int) importedKeyNotDeferrable });
		}
	}

	private static List<ForeignKey> keys(VessTable table) {
		try {
			return table.getForeignKeys();
		} catch (RuntimeException e) {
			return List.of();
		}
	}

	@Override
	public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException {
		List<Object[]> rows = new ArrayList<>();
		VessSchema owner = schemaOf(catalog, schema, table);
		VessTable found = table(catalog, schema, table);
		if (owner != null && found != null) {
			for (ForeignKey key : keys(found)) {
				addKey(rows, owner, found, key);
			}
		}
		rows.sort(Comparator.comparing((Object[] row) -> (String) row[2]).thenComparing(row -> (Integer) row[8]));
		return result(rows, KEY_COLUMNS);
	}

	@Override
	public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException {
		List<Object[]> rows = new ArrayList<>();
		VessSchema owner = schemaOf(catalog, schema, table);
		if (owner != null) {
			for (VessTable each : tables(owner)) {
				for (ForeignKey key : keys(each)) {
					if (key.table().equalsIgnoreCase(table) && key.schema().equalsIgnoreCase(owner.getSchemaName())) {
						addKey(rows, owner, each, key);
					}
				}
			}
		}
		rows.sort(Comparator.comparing((Object[] row) -> (String) row[6]).thenComparing(row -> (Integer) row[8]));
		return result(rows, KEY_COLUMNS);
	}

	@Override
	public ResultSet getCrossReference(String parentCatalog, String parentSchema, String parentTable, String foreignCatalog,
			String foreignSchema, String foreignTable) throws SQLException {
		List<Object[]> rows = new ArrayList<>();
		VessSchema owner = schemaOf(foreignCatalog, foreignSchema, foreignTable);
		VessTable found = table(foreignCatalog, foreignSchema, foreignTable);
		if (owner != null && found != null) {
			for (ForeignKey key : keys(found)) {
				if (key.table().equalsIgnoreCase(parentTable)) {
					addKey(rows, owner, found, key);
				}
			}
		}
		return result(rows, KEY_COLUMNS);
	}

	@Override
	public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate) throws SQLException {
		List<Object[]> rows = new ArrayList<>();
		VessSchema owner = schemaOf(catalog, schema, table);
		VessTable found = table(catalog, schema, table);
		if (owner != null && found != null) {
			List<String> key = found.getPrimaryKey();
			for (int index = 0; index < key.size(); index++) {
				rows.add(new Object[] { owner.getCatalogName(), owner.getSchemaName(), found.getName(), false, null, "PK_" + found.getName(),
						(int) tableIndexOther, index + 1, key.get(index), "A", null, null, null });
			}
		}
		return result(rows, "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "NON_UNIQUE:B", "INDEX_QUALIFIER", "INDEX_NAME", "TYPE:S",
				"ORDINAL_POSITION:S", "COLUMN_NAME", "ASC_OR_DESC", "CARDINALITY:I", "PAGES:I", "FILTER_CONDITION");
	}

	@Override
	public ResultSet getBestRowIdentifier(String catalog, String schema, String table, int scope, boolean nullable) throws SQLException {
		List<Object[]> rows = new ArrayList<>();
		VessTable found = table(catalog, schema, table);
		if (found != null) {
			for (String name : found.getPrimaryKey()) {
				Column column = found.getColumns().get(found.indexOf(name));
				rows.add(new Object[] { (int) bestRowSession, column.getName(), column.getType(), column.getTypeName(), size(column), null, 0,
						(int) bestRowNotPseudo });
			}
		}
		return result(rows, "SCOPE:S", "COLUMN_NAME", "DATA_TYPE:I", "TYPE_NAME", "COLUMN_SIZE:I", "BUFFER_LENGTH:I", "DECIMAL_DIGITS:S",
				"PSEUDO_COLUMN:S");
	}

	private List<Object[]> procedures(String catalog, String schemaPattern, String namePattern) {
		List<Object[]> found = new ArrayList<>();
		for (VessSchema schema : schemas(catalog, schemaPattern)) {
			List<Procedure> procedures;
			try {
				procedures = schema.getProcedures();
			} catch (RuntimeException e) {
				continue;
			}
			for (Procedure procedure : procedures) {
				if (matches(namePattern, procedure.getName())) {
					found.add(new Object[] { schema, procedure });
				}
			}
		}
		return found;
	}

	@Override
	public ResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern) throws SQLException {
		List<Object[]> rows = new ArrayList<>();
		for (Object[] each : procedures(catalog, schemaPattern, procedureNamePattern)) {
			VessSchema schema = (VessSchema) each[0];
			Procedure procedure = (Procedure) each[1];
			rows.add(new Object[] { schema.getCatalogName(), schema.getSchemaName(), procedure.getName(), null, null, null,
					procedure.getRemarks(), (int) procedureReturnsResult, procedure.getName() });
		}
		return result(rows, "PROCEDURE_CAT", "PROCEDURE_SCHEM", "PROCEDURE_NAME", "RESERVED1", "RESERVED2", "RESERVED3", "REMARKS",
				"PROCEDURE_TYPE:S", "SPECIFIC_NAME");
	}

	@Override
	public ResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern, String columnNamePattern)
			throws SQLException {
		List<Object[]> rows = new ArrayList<>();
		for (Object[] each : procedures(catalog, schemaPattern, procedureNamePattern)) {
			VessSchema schema = (VessSchema) each[0];
			Procedure procedure = (Procedure) each[1];
			int position = 1;
			for (Column column : procedure.getParameters()) {
				if (matches(columnNamePattern, column.getName())) {
					rows.add(procedureColumn(schema, procedure, column, procedureColumnIn, position));
				}
				position++;
			}
			position = 1;
			for (Column column : procedure.getResults()) {
				if (matches(columnNamePattern, column.getName())) {
					rows.add(procedureColumn(schema, procedure, column, procedureColumnResult, position));
				}
				position++;
			}
		}
		return result(rows, "PROCEDURE_CAT", "PROCEDURE_SCHEM", "PROCEDURE_NAME", "COLUMN_NAME", "COLUMN_TYPE:S", "DATA_TYPE:I", "TYPE_NAME",
				"PRECISION:I", "LENGTH:I", "SCALE:S", "RADIX:S", "NULLABLE:S", "REMARKS", "COLUMN_DEF", "SQL_DATA_TYPE:I", "SQL_DATETIME_SUB:I",
				"CHAR_OCTET_LENGTH:I", "ORDINAL_POSITION:I", "IS_NULLABLE", "SPECIFIC_NAME");
	}

	private static Object[] procedureColumn(VessSchema schema, Procedure procedure, Column column, int kind, int position) {
		boolean text = column.getType() == Types.VARCHAR;
		return new Object[] { schema.getCatalogName(), schema.getSchemaName(), procedure.getName(), column.getName(), kind, column.getType(),
				column.getTypeName(), size(column), size(column), 0, text ? null : 10, (int) procedureNullable, column.getRemarks(), null, null,
				null, text ? size(column) * 4 : null, kind == procedureColumnIn ? position : 0, "YES", procedure.getName() };
	}

	private List<Object[]> functions(String catalog, String schemaPattern, String namePattern) {
		List<Object[]> found = new ArrayList<>();
		for (VessSchema schema : schemas(catalog, schemaPattern)) {
			for (OutlineFunction function : schema.getOutlineFunctions()) {
				if (matches(namePattern, function.getName())) {
					found.add(new Object[] { schema, function });
				}
			}
		}
		return found;
	}

	private static String specificName(OutlineFunction function) {
		return function.getName() + "_" + function.getParameterColumns().size();
	}

	@Override
	public ResultSet getFunctions(String catalog, String schemaPattern, String functionNamePattern) throws SQLException {
		List<Object[]> rows = new ArrayList<>();
		for (Object[] each : functions(catalog, schemaPattern, functionNamePattern)) {
			VessSchema schema = (VessSchema) each[0];
			OutlineFunction function = (OutlineFunction) each[1];
			rows.add(new Object[] { schema.getCatalogName(), schema.getSchemaName(), function.getName(), function.getRemarks(),
					(int) functionNoTable, specificName(function) });
		}
		return result(rows, "FUNCTION_CAT", "FUNCTION_SCHEM", "FUNCTION_NAME", "REMARKS", "FUNCTION_TYPE:S", "SPECIFIC_NAME");
	}

	@Override
	public ResultSet getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern, String columnNamePattern)
			throws SQLException {
		List<Object[]> rows = new ArrayList<>();
		for (Object[] each : functions(catalog, schemaPattern, functionNamePattern)) {
			VessSchema schema = (VessSchema) each[0];
			OutlineFunction function = (OutlineFunction) each[1];
			Column result = function.getResult();
			rows.add(functionColumn(schema, function, result.named(""), functionReturn, 0));
			int position = 1;
			for (Column column : function.getParameterColumns()) {
				if (matches(columnNamePattern, column.getName())) {
					rows.add(functionColumn(schema, function, column, functionColumnIn, position));
				}
				position++;
			}
		}
		return result(rows, "FUNCTION_CAT", "FUNCTION_SCHEM", "FUNCTION_NAME", "COLUMN_NAME", "COLUMN_TYPE:S", "DATA_TYPE:I", "TYPE_NAME",
				"PRECISION:I", "LENGTH:I", "SCALE:S", "RADIX:S", "NULLABLE:S", "REMARKS", "CHAR_OCTET_LENGTH:I", "ORDINAL_POSITION:I",
				"IS_NULLABLE", "SPECIFIC_NAME");
	}

	private static Object[] functionColumn(VessSchema schema, OutlineFunction function, Column column, int kind, int position) {
		boolean text = column.getType() == Types.VARCHAR;
		return new Object[] { schema.getCatalogName(), schema.getSchemaName(), function.getName(), column.getName(), kind, column.getType(),
				column.getTypeName(), size(column), size(column), 0, text ? null : 10, (int) functionNullable, column.getRemarks(),
				text ? size(column) * 4 : null, position, "YES", specificName(function) };
	}

	@Override
	public ResultSet getTypeInfo() throws SQLException {
		List<Object[]> rows = new ArrayList<>();
		rows.add(typeInfo("BOOLEAN", Types.BOOLEAN, 1, null, null, null));
		rows.add(typeInfo("TINYINT", Types.TINYINT, 3, null, null, 10));
		rows.add(typeInfo("BIGINT", Types.BIGINT, 19, null, null, 10));
		rows.add(typeInfo("VARCHAR", Types.VARCHAR, LONG_TEXT, "'", "length", null));
		rows.add(typeInfo("CHAR", Types.CHAR, LONG_TEXT, "'", "length", null));
		rows.add(typeInfo("DECIMAL", Types.DECIMAL, 38, null, "precision,scale", 10));
		rows.add(typeInfo("INTEGER", Types.INTEGER, 10, null, null, 10));
		rows.add(typeInfo("SMALLINT", Types.SMALLINT, 5, null, null, 10));
		rows.add(typeInfo("FLOAT", Types.FLOAT, 15, null, null, 2));
		rows.add(typeInfo("REAL", Types.REAL, 7, null, null, 2));
		rows.add(typeInfo("DOUBLE", Types.DOUBLE, 15, null, null, 2));
		rows.add(typeInfo("DATE", Types.DATE, 10, "DATE '", null, null));
		rows.add(typeInfo("TIME", Types.TIME, 8, "TIME '", null, null));
		rows.add(typeInfo("TIMESTAMP", Types.TIMESTAMP, 23, "TIMESTAMP '", null, null));
		rows.sort(Comparator.comparing(row -> (Integer) row[1]));
		return result(rows, "TYPE_NAME", "DATA_TYPE:I", "PRECISION:I", "LITERAL_PREFIX", "LITERAL_SUFFIX", "CREATE_PARAMS", "NULLABLE:S",
				"CASE_SENSITIVE:B", "SEARCHABLE:S", "UNSIGNED_ATTRIBUTE:B", "FIXED_PREC_SCALE:B", "AUTO_INCREMENT:B", "LOCAL_TYPE_NAME",
				"MINIMUM_SCALE:S", "MAXIMUM_SCALE:S", "SQL_DATA_TYPE:I", "SQL_DATETIME_SUB:I", "NUM_PREC_RADIX:I");
	}

	private static Object[] typeInfo(String name, int type, int precision, String prefix, String createParams, Integer radix) {
		boolean text = "'".equals(prefix);
		int maximumScale = type == Types.DECIMAL ? 38 : 0;
		return new Object[] { name, type, precision, prefix, prefix == null ? null : "'", createParams, (int) typeNullable, text,
				(int) typeSearchable, false, false, false, name, 0, maximumScale, null, null, radix };
	}

	@Override
	public ResultSet getClientInfoProperties() throws SQLException {
		return result(List.of(), "NAME", "MAX_LEN:I", "DEFAULT_VALUE", "DESCRIPTION");
	}

	@Override
	public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern) throws SQLException {
		return result(List.of(), "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "COLUMN_NAME", "GRANTOR", "GRANTEE", "PRIVILEGE", "IS_GRANTABLE");
	}

	@Override
	public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
		return result(List.of(), "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "GRANTOR", "GRANTEE", "PRIVILEGE", "IS_GRANTABLE");
	}

	@Override
	public ResultSet getVersionColumns(String catalog, String schema, String table) throws SQLException {
		return result(List.of(), "SCOPE:S", "COLUMN_NAME", "DATA_TYPE:I", "TYPE_NAME", "COLUMN_SIZE:I", "BUFFER_LENGTH:I", "DECIMAL_DIGITS:S",
				"PSEUDO_COLUMN:S");
	}

	@Override
	public ResultSet getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[] types) throws SQLException {
		return result(List.of(), "TYPE_CAT", "TYPE_SCHEM", "TYPE_NAME", "CLASS_NAME", "DATA_TYPE:I", "REMARKS", "BASE_TYPE:S");
	}

	@Override
	public ResultSet getSuperTypes(String catalog, String schemaPattern, String typeNamePattern) throws SQLException {
		return result(List.of(), "TYPE_CAT", "TYPE_SCHEM", "TYPE_NAME", "SUPERTYPE_CAT", "SUPERTYPE_SCHEM", "SUPERTYPE_NAME");
	}

	@Override
	public ResultSet getSuperTables(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
		return result(List.of(), "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "SUPERTABLE_NAME");
	}

	@Override
	public ResultSet getAttributes(String catalog, String schemaPattern, String typeNamePattern, String attributeNamePattern)
			throws SQLException {
		return result(List.of(), "TYPE_CAT", "TYPE_SCHEM", "TYPE_NAME", "ATTR_NAME", "DATA_TYPE:I", "ATTR_TYPE_NAME", "ATTR_SIZE:I",
				"DECIMAL_DIGITS:I", "NUM_PREC_RADIX:I", "NULLABLE:I", "REMARKS", "ATTR_DEF", "SQL_DATA_TYPE:I", "SQL_DATETIME_SUB:I",
				"CHAR_OCTET_LENGTH:I", "ORDINAL_POSITION:I", "IS_NULLABLE", "SCOPE_CATALOG", "SCOPE_SCHEMA", "SCOPE_TABLE", "SOURCE_DATA_TYPE:S");
	}

	@Override
	public ResultSet getPseudoColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern)
			throws SQLException {
		return result(List.of(), "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "COLUMN_NAME", "DATA_TYPE:I", "COLUMN_SIZE:I", "DECIMAL_DIGITS:I",
				"NUM_PREC_RADIX:I", "COLUMN_USAGE", "REMARKS", "CHAR_OCTET_LENGTH:I", "IS_NULLABLE");
	}

	@Override
	public String getDatabaseProductName() throws SQLException {
		return "Essbase";
	}

	@Override
	public String getDatabaseProductVersion() throws SQLException {
		return connection.getServerVersion();
	}

	@Override
	public int getDatabaseMajorVersion() throws SQLException {
		return versionPart(connection.getServerVersion(), 0);
	}

	@Override
	public int getDatabaseMinorVersion() throws SQLException {
		return versionPart(connection.getServerVersion(), 1);
	}

	private static int versionPart(String version, int index) {
		if (version == null) {
			return 0;
		}
		String[] parts = version.split("[.-]");
		try {
			return index < parts.length ? Integer.parseInt(parts[index].replaceAll("\\D", "")) : 0;
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	@Override
	public String getDriverName() throws SQLException {
		return "Vess";
	}

	@Override
	public String getDriverVersion() throws SQLException {
		return VessDriver.VERSION;
	}

	@Override
	public int getDriverMajorVersion() {
		return VessDriver.MAJOR_VERSION;
	}

	@Override
	public int getDriverMinorVersion() {
		return VessDriver.MINOR_VERSION;
	}

	@Override
	public int getJDBCMajorVersion() throws SQLException {
		return 4;
	}

	@Override
	public int getJDBCMinorVersion() throws SQLException {
		return 3;
	}

	@Override
	public String getURL() throws SQLException {
		return connection.getUrl().getUrl();
	}

	@Override
	public String getUserName() throws SQLException {
		return connection.getUser();
	}

	@Override
	public boolean isReadOnly() throws SQLException {
		return connection.isReadOnly();
	}

	@Override
	public Connection getConnection() throws SQLException {
		return connection;
	}

	@Override
	public String getIdentifierQuoteString() throws SQLException {
		return "\"";
	}

	/**
	 * The words the parser reserves that SQL:2003 doesn't, which tools quote
	 * when they appear as names.
	 */
	@Override
	public String getSQLKeywords() throws SQLException {
		return SqlParser.create("", Engine.parserConfig()).getMetadata().getJdbcKeywords();
	}

	@Override
	public String getNumericFunctions() throws SQLException {
		return SqlJdbcFunctionCall.getNumericFunctions();
	}

	@Override
	public String getStringFunctions() throws SQLException {
		return SqlJdbcFunctionCall.getStringFunctions();
	}

	@Override
	public String getSystemFunctions() throws SQLException {
		return SqlJdbcFunctionCall.getSystemFunctions();
	}

	@Override
	public String getTimeDateFunctions() throws SQLException {
		return SqlJdbcFunctionCall.getTimeDateFunctions();
	}

	@Override
	public String getSearchStringEscape() throws SQLException {
		return "\\";
	}

	@Override
	public String getExtraNameCharacters() throws SQLException {
		return "";
	}

	@Override
	public String getSchemaTerm() throws SQLException {
		return "cube";
	}

	@Override
	public String getProcedureTerm() throws SQLException {
		return "procedure";
	}

	@Override
	public String getCatalogTerm() throws SQLException {
		return "application";
	}

	@Override
	public boolean isCatalogAtStart() throws SQLException {
		return true;
	}

	@Override
	public String getCatalogSeparator() throws SQLException {
		return ".";
	}

	@Override
	public boolean allProceduresAreCallable() throws SQLException {
		return true;
	}

	@Override
	public boolean allTablesAreSelectable() throws SQLException {
		return true;
	}

	@Override
	public boolean nullsAreSortedHigh() throws SQLException {
		return true;
	}

	@Override
	public boolean nullsAreSortedLow() throws SQLException {
		return false;
	}

	@Override
	public boolean nullsAreSortedAtStart() throws SQLException {
		return false;
	}

	@Override
	public boolean nullsAreSortedAtEnd() throws SQLException {
		return false;
	}

	@Override
	public boolean usesLocalFiles() throws SQLException {
		return false;
	}

	@Override
	public boolean usesLocalFilePerTable() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsMixedCaseIdentifiers() throws SQLException {
		return false;
	}

	@Override
	public boolean storesUpperCaseIdentifiers() throws SQLException {
		return false;
	}

	@Override
	public boolean storesLowerCaseIdentifiers() throws SQLException {
		return false;
	}

	@Override
	public boolean storesMixedCaseIdentifiers() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException {
		return false;
	}

	@Override
	public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {
		return false;
	}

	@Override
	public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {
		return false;
	}

	@Override
	public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsAlterTableWithAddColumn() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsAlterTableWithDropColumn() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsColumnAliasing() throws SQLException {
		return true;
	}

	@Override
	public boolean nullPlusNonNullIsNull() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsConvert() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsConvert(int fromType, int toType) throws SQLException {
		return true;
	}

	@Override
	public boolean supportsTableCorrelationNames() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsDifferentTableCorrelationNames() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsExpressionsInOrderBy() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsOrderByUnrelated() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsGroupBy() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsGroupByUnrelated() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsGroupByBeyondSelect() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsLikeEscapeClause() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsMultipleResultSets() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsMultipleTransactions() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsNonNullableColumns() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsMinimumSQLGrammar() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsCoreSQLGrammar() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsExtendedSQLGrammar() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsANSI92EntryLevelSQL() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsANSI92IntermediateSQL() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsANSI92FullSQL() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsIntegrityEnhancementFacility() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsOuterJoins() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsFullOuterJoins() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsLimitedOuterJoins() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsSchemasInDataManipulation() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsSchemasInProcedureCalls() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsSchemasInTableDefinitions() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsSchemasInIndexDefinitions() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsCatalogsInDataManipulation() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsCatalogsInProcedureCalls() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsCatalogsInTableDefinitions() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsCatalogsInIndexDefinitions() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsPositionedDelete() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsPositionedUpdate() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsSelectForUpdate() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsStoredProcedures() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsSubqueriesInComparisons() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsSubqueriesInExists() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsSubqueriesInIns() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsSubqueriesInQuantifieds() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsCorrelatedSubqueries() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsUnion() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsUnionAll() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsOpenCursorsAcrossCommit() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsOpenCursorsAcrossRollback() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsOpenStatementsAcrossCommit() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsOpenStatementsAcrossRollback() throws SQLException {
		return true;
	}

	@Override
	public int getMaxBinaryLiteralLength() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxCharLiteralLength() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxColumnNameLength() throws SQLException {
		return Column.MEMBER_NAME;
	}

	@Override
	public int getMaxColumnsInGroupBy() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxColumnsInIndex() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxColumnsInOrderBy() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxColumnsInSelect() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxColumnsInTable() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxConnections() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxCursorNameLength() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxIndexLength() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxSchemaNameLength() throws SQLException {
		return Column.OBJECT_NAME;
	}

	@Override
	public int getMaxProcedureNameLength() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxCatalogNameLength() throws SQLException {
		return Column.OBJECT_NAME;
	}

	@Override
	public int getMaxRowSize() throws SQLException {
		return 0;
	}

	@Override
	public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
		return false;
	}

	@Override
	public int getMaxStatementLength() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxStatements() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxTableNameLength() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxTablesInSelect() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxUserNameLength() throws SQLException {
		return 0;
	}

	@Override
	public int getDefaultTransactionIsolation() throws SQLException {
		return Connection.TRANSACTION_NONE;
	}

	@Override
	public boolean supportsTransactions() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsTransactionIsolationLevel(int level) throws SQLException {
		return level == Connection.TRANSACTION_NONE;
	}

	@Override
	public boolean supportsDataDefinitionAndDataManipulationTransactions() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsDataManipulationTransactionsOnly() throws SQLException {
		return false;
	}

	@Override
	public boolean dataDefinitionCausesTransactionCommit() throws SQLException {
		return false;
	}

	@Override
	public boolean dataDefinitionIgnoredInTransactions() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsResultSetType(int type) throws SQLException {
		return type == ResultSet.TYPE_FORWARD_ONLY;
	}

	@Override
	public boolean supportsResultSetConcurrency(int type, int concurrency) throws SQLException {
		return type == ResultSet.TYPE_FORWARD_ONLY && concurrency == ResultSet.CONCUR_READ_ONLY;
	}

	@Override
	public boolean ownUpdatesAreVisible(int type) throws SQLException {
		return false;
	}

	@Override
	public boolean ownDeletesAreVisible(int type) throws SQLException {
		return false;
	}

	@Override
	public boolean ownInsertsAreVisible(int type) throws SQLException {
		return false;
	}

	@Override
	public boolean othersUpdatesAreVisible(int type) throws SQLException {
		return false;
	}

	@Override
	public boolean othersDeletesAreVisible(int type) throws SQLException {
		return false;
	}

	@Override
	public boolean othersInsertsAreVisible(int type) throws SQLException {
		return false;
	}

	@Override
	public boolean updatesAreDetected(int type) throws SQLException {
		return false;
	}

	@Override
	public boolean deletesAreDetected(int type) throws SQLException {
		return false;
	}

	@Override
	public boolean insertsAreDetected(int type) throws SQLException {
		return false;
	}

	@Override
	public boolean supportsBatchUpdates() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsSavepoints() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsNamedParameters() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsMultipleOpenResults() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsGetGeneratedKeys() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsResultSetHoldability(int holdability) throws SQLException {
		return holdability == ResultSet.CLOSE_CURSORS_AT_COMMIT;
	}

	@Override
	public int getResultSetHoldability() throws SQLException {
		return ResultSet.CLOSE_CURSORS_AT_COMMIT;
	}

	@Override
	public int getSQLStateType() throws SQLException {
		return sqlStateSQL;
	}

	@Override
	public boolean locatorsUpdateCopy() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsStatementPooling() throws SQLException {
		return false;
	}

	@Override
	public RowIdLifetime getRowIdLifetime() throws SQLException {
		return RowIdLifetime.ROWID_UNSUPPORTED;
	}

	@Override
	public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException {
		return false;
	}

	@Override
	public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
		return false;
	}

	@Override
	public boolean generatedKeyAlwaysReturned() throws SQLException {
		return false;
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if (isWrapperFor(iface)) {
			return iface.cast(this);
		}
		throw new VessException("Cannot cast to " + iface.getName());
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return iface.isInstance(this);
	}

}

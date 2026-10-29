package com.jasonwjones.hyperion.vess;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.schema.Function;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.jasonwjones.hyperion.vess.cells.CellsTable;
import com.jasonwjones.hyperion.vess.outline.Dimension;
import com.jasonwjones.hyperion.vess.schema.DimensionTable;
import com.jasonwjones.hyperion.vess.schema.OutlineFunction;
import com.jasonwjones.hyperion.vess.schema.OutlineFunctions;
import com.jasonwjones.hyperion.vess.schema.VessTable;

/**
 * The SQL engine as Vess sets it up, with a fake Sample.Basic in it: its
 * cells, its dimension tables and its outline functions, and any other tables
 * a test adds.
 */
final class TestEngine implements AutoCloseable, Dml.Target {

	private static int counter;

	final FakeCube cube;

	final Connection connection;

	private final Map<String, VessTable> tables = new LinkedHashMap<>();

	private boolean readOnly;

	TestEngine(FakeCube cube) throws SQLException {
		this.cube = cube;
		this.connection = Engine.connect();
		String key = "test" + (++counter) + "/Sample/Basic";
		OutlineFunctions.register(key, cube::outline);
		List<OutlineFunction> functions = OutlineFunction.forCube(key);
		tables.put("Cells", new CellsTable("Cells", cube, "Sample", "Basic", false, Dimension::getName));
		tables.put("Facts", new CellsTable("Facts", cube, "Sample", "Basic", true, Dimension::getName));
		for (Dimension dimension : cube.outline().getDimensions()) {
			tables.put(dimension.getName(), new DimensionTable(dimension.getName(), cube::outline, "Sample", "Basic", dimension, Dimension::getName));
		}
		Schema basic = new AbstractSchema() {

			@Override
			protected Map<String, Table> getTableMap() {
				return new LinkedHashMap<>(tables);
			}

			@Override
			protected Multimap<String, Function> getFunctionMultimap() {
				ImmutableMultimap.Builder<String, Function> builder = ImmutableMultimap.builder();
				for (OutlineFunction function : functions) {
					builder.put(function.getName(), function);
				}
				return builder.build();
			}

		};
		SchemaPlus root = connection.unwrap(CalciteConnection.class).getRootSchema();
		root.add("Sample", new AbstractSchema() {

			@Override
			protected Map<String, Schema> getSubSchemaMap() {
				return Map.of("Basic", basic);
			}

		});
		root.add("Sample.Basic", basic);
		connection.setSchema("Sample.Basic");
	}

	TestEngine add(VessTable table) {
		tables.put(table.getName(), table);
		return this;
	}

	TestEngine readOnly() {
		this.readOnly = true;
		return this;
	}

	/**
	 * A query's rows, each value as JDBC gives it.
	 */
	List<List<Object>> query(String sql) throws SQLException {
		List<List<Object>> rows = new ArrayList<>();
		try (Statement statement = connection.createStatement(); ResultSet results = statement.executeQuery(sql)) {
			int width = results.getMetaData().getColumnCount();
			while (results.next()) {
				List<Object> row = new ArrayList<>();
				for (int index = 1; index <= width; index++) {
					row.add(results.getObject(index));
				}
				rows.add(row);
			}
		} catch (SQLException e) {
			throw VessStatement.translate(e);
		}
		return rows;
	}

	int dml(String sql, Object... parameters) throws SQLException {
		return Dml.execute(this, sql, List.of(parameters));
	}

	@Override
	public Connection engine() {
		return connection;
	}

	@Override
	public VessTable table(List<String> names) throws SQLException {
		String name = names.get(names.size() - 1);
		for (Map.Entry<String, VessTable> table : tables.entrySet()) {
			if (table.getKey().equalsIgnoreCase(name)) {
				return table.getValue();
			}
		}
		throw new VessException("There's no table " + String.join(".", names));
	}

	@Override
	public boolean isReadOnly() {
		return readOnly;
	}

	@Override
	public void close() throws SQLException {
		connection.close();
	}

}

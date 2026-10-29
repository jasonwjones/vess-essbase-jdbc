package com.jasonwjones.hyperion.vess.schema;

import java.util.List;

/**
 * Columns of one table that name a row of another, which tools draw as a line
 * between them: from each of a cube's members to its dimension's table, for
 * one.
 *
 * @param name the key's name
 * @param columns the columns that do the naming
 * @param catalog the other table's catalog, its application
 * @param schema the other table's schema, its cube
 * @param table the other table
 * @param referenced the other table's columns, which identify its rows
 */
public record ForeignKey(String name, List<String> columns, String catalog, String schema, String table, List<String> referenced) {

	public static ForeignKey of(String name, String column, String catalog, String schema, String table, String referenced) {
		return new ForeignKey(name, List.of(column), catalog, schema, table, List.of(referenced));
	}

}

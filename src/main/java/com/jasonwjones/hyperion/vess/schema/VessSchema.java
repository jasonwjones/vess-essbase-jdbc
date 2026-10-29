package com.jasonwjones.hyperion.vess.schema;

import java.util.List;

/**
 * A schema as Vess describes it to tools: where it sits, and what's in it.
 */
public interface VessSchema {

	/**
	 * @return the catalog it's in: an application, or the server's own catalog
	 */
	String getCatalogName();

	/**
	 * @return its name: a cube, or the server's own schema
	 */
	String getSchemaName();

	String getRemarks();

	List<VessTable> getVessTables();

	/**
	 * @return the tables to show someone browsing, which can leave out those
	 *         that would start a stopped application just to describe them
	 */
	default List<VessTable> getBrowsableTables() {
		return getVessTables();
	}

	List<Procedure> getProcedures();

	/**
	 * @return the SQL functions it adds
	 */
	List<OutlineFunction> getOutlineFunctions();

}

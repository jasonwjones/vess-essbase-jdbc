package com.jasonwjones.hyperion.vess.cells;

import java.sql.SQLException;
import java.util.List;

import com.appliedolap.essbase.EssApiException;
import com.appliedolap.essbase.EssCube;
import com.appliedolap.essbase.EssMdxGrid;
import com.appliedolap.essbase.EssMdxOptions;
import com.jasonwjones.hyperion.vess.VessException;
import com.jasonwjones.hyperion.vess.outline.Outline;
import com.jasonwjones.hyperion.vess.schema.ServerContext;

/**
 * Where a cube's cells come from and go to: its outline, MDX to read them, and
 * a data load to write them.
 */
public interface CellSource {

	Outline outline() throws SQLException;

	/**
	 * Runs an MDX query, naming members by their names.
	 */
	EssMdxGrid mdx(String query) throws SQLException;

	/**
	 * Writes records of a member of each dimension and a value, null to clear
	 * the cell.
	 */
	void write(List<Object[]> records) throws SQLException;

	/**
	 * @throws VessException if the connection may not change the server
	 */
	void checkWritable(String what) throws VessException;

	/**
	 * @return how many cells one MDX query asks for at most
	 */
	long chunkCells();

	/**
	 * @return how many members of one dimension an MDX query names at most
	 */
	int chunkMembers();

	/**
	 * A cube's cells on the server a connection logs in to.
	 */
	static CellSource of(ServerContext context, String application, String cube) {
		return new CellSource() {

			@Override
			public Outline outline() throws SQLException {
				return context.outline(application, cube);
			}

			@Override
			public EssMdxGrid mdx(String query) throws SQLException {
				EssCube essCube = context.cube(application, cube);
				try {
					return essCube.executeMdx(query, new EssMdxOptions().memberIdentifierType(EssCube.MdxIdentifierType.NAME));
				} catch (EssApiException e) {
					throw new VessException("Essbase couldn't read the cells of " + application + "." + cube + ": " + e.getMessage(), e);
				}
			}

			@Override
			public void write(List<Object[]> records) throws SQLException {
				CellWriter.write(context, application, cube, records);
			}

			@Override
			public void checkWritable(String what) throws VessException {
				context.checkWritable(what);
			}

			@Override
			public long chunkCells() {
				return context.getSettings().chunkCells();
			}

			@Override
			public int chunkMembers() {
				return context.getSettings().chunkMembers();
			}

		};
	}

}

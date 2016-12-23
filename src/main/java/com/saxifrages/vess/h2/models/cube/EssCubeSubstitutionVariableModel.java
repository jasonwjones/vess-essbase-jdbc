package com.saxifrages.vess.h2.models.cube;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.h2.engine.Session;
import org.h2.result.Row;
import org.h2.value.Value;
import org.h2.value.ValueString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.essbase.api.base.EssException;
import com.essbase.api.datasource.IEssCube;
import com.saxifrages.vess.h2.ColumnBuilder;
import com.saxifrages.vess.h2.EssService;
import com.saxifrages.vess.h2.StringUtil;
import com.saxifrages.vess.h2.essresults.ArrayListEssResultSet;
import com.saxifrages.vess.h2.essresults.EssResultSet;
import com.saxifrages.vess.h2.models.AbstractEssbaseCubeModel;
import com.saxifrages.vess.h2.models.ColConstants;

public class EssCubeSubstitutionVariableModel extends AbstractEssbaseCubeModel {

	private static final Logger logger = LoggerFactory.getLogger(EssCubeSubstitutionVariableModel.class);

	public EssCubeSubstitutionVariableModel(EssService service, String application, String cube) {
		super(service, application, cube);
		logger.info("Building sub var mapper for {}/{}", application, cube);
		columns = new ColumnBuilder()
				.addString("NAME")
				.addString("VALUE")
				.add(ColConstants.APPLICATION)
				.add(ColConstants.CUBE)
				.build();
	}

	@Override
	public void addRealRow(Session session, Row row) throws SQLException {
		String name = row.getValue(0).getString();
		String value = row.getValue(1).getString();
		logger.info("Got var: {}={}", name, value);
		try {
			service.addSubstitutionVariable(application, cube, name, value);
		} catch (EssException e) {
			throw new SQLException("Unable to create variable", e);
		}
	}

	// TODO: Implement some sort of generified class that has BeanMapper
	// or something so that we get methods that pass in a transformed Row
	// for us
	@Override
	public void removeRealRow(Session session, Row row) throws SQLException {
		String name = row.getValue(0).getString();
		String database = row.getValue(3).getString();
		
		logger.info("Searching for variable to delete {} from {}/{}", name, application, database);
		try {
			if (database != null && !database.isEmpty()) {
				// it's a database specific variable
				logger.info("Deleting cube variable {} from {}/{}", name, application, database);
				service.deleteVariable(application, database, name);
			} else {
				// it's an app or server level variable
				logger.info("Deleting app variable {}", name);
				service.deleteVariable(application, name);
			}
		} catch (EssException e) {
			logger.error("Couldn't delete variable");
			throw new SQLException("Couldn't delete variable", e);
		}
	}
	
	/**
	 * You might think that you should filter variables out here so that we
	 * ignore variables that are server/global scoped, but technically that'd be
	 * wrong. It makes sense that the application/database specific variables
	 * you get back from the cube include globals because those do indeed affect
	 * the variable namespace of the cube.
	 */
	@Override
	public EssResultSet get(IEssCube cube) throws EssException {
		List<Row> rows = new ArrayList<Row>();
		for (String[] variable : cube.getSubstitutionVariables()) {
			Value[] var = new Value[getColumnCount()];
			var[0] = ValueString.get(StringUtil.emptyIfNull(variable[0]));
			var[1] = ValueString.get(StringUtil.emptyIfNull(variable[1]));
			var[2] = ValueString.get(StringUtil.emptyIfNull(variable[2]));
			var[3] = ValueString.get(StringUtil.emptyIfNull(variable[3]));
			rows.add(new Row(var, 0));
		}
		return new ArrayListEssResultSet(rows, getColumnCount());
	}

}

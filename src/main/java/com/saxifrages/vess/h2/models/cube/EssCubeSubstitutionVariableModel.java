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
	
	@Override
	public EssResultSet get(IEssCube cube) throws EssException {
		List<Row> rows = new ArrayList<Row>(); 
		for (String[] variable : cube.getSubstitutionVariables()) {
			Value[] var = new Value[getColumnCount()];
			var[0] = ValueString.get(StringUtil.emptyIfNull(variable[0]));
			var[1] = ValueString.get(StringUtil.emptyIfNull(variable[1]));
			var[2] = ValueString.get(StringUtil.emptyIfNull(variable[2]));
			var[3] = ValueString.get(StringUtil.emptyIfNull(variable[3]));
			
			
//			if (variable[2].equalsIgnoreCase(application)) {
				rows.add(new Row(var, 0));
//			}
		}
		return new ArrayListEssResultSet(rows, getColumnCount());
	}

}

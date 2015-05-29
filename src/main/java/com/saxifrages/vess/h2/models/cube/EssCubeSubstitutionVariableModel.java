package com.saxifrages.vess.h2.models.cube;

import java.util.ArrayList;
import java.util.List;

import org.h2.result.Row;
import org.h2.value.Value;
import org.h2.value.ValueString;

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

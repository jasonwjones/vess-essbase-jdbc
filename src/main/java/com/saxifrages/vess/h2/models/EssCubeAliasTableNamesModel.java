package com.saxifrages.vess.h2.models;

import java.util.ArrayList;
import java.util.List;

import org.h2.result.Row;
import org.h2.value.Value;
import org.h2.value.ValueString;

import com.essbase.api.base.EssException;
import com.essbase.api.datasource.IEssCube;
import com.saxifrages.vess.h2.ColumnBuilder;
import com.saxifrages.vess.h2.EssService;
import com.saxifrages.vess.h2.EssTable;
import com.saxifrages.vess.h2.StringUtil;
import com.saxifrages.vess.h2.essresults.ArrayListEssResultSet;
import com.saxifrages.vess.h2.essresults.EssResultSet;

public class EssCubeAliasTableNamesModel extends AbstractEssbaseReadOnlyModel {

	private EssService service;

	private String app;
	
	private String cube;
	
	public EssCubeAliasTableNamesModel(EssService service, String app, String cube) {
		this.service = service;
		this.app = app;
		this.cube = cube;
		
		columns = new ColumnBuilder()
				.add(ColConstants.ALIAS_TABLE_NAME)
				.build();
	}

	public EssResultSet get() {
		try {
			IEssCube essCube = service.getCube(app, cube);
			
			List<Row> rows = new ArrayList<Row>();
			for (String aliasTableName : essCube.getAliasTableNames()) {
				Value[] var = new Value[1];
				var[0] = ValueString.get(StringUtil.emptyIfNull(aliasTableName));
				Row row = new Row(var, 0);
				rows.add(row);
			}
			return new ArrayListEssResultSet(rows, 4);
		} catch (EssException e) {
			throw EssTable.wrapException("Essbase error", e);
		}
	}

}

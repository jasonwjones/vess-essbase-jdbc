package com.saxifrages.vess.h2.models;

import java.util.ArrayList;
import java.util.List;

import org.h2.result.Row;
import org.h2.value.Value;
import org.h2.value.ValueString;

import com.essbase.api.base.EssException;
import com.essbase.api.datasource.IEssOlapServer;
import com.saxifrages.vess.h2.ColumnBuilder;
import com.saxifrages.vess.h2.EssService;
import com.saxifrages.vess.h2.StringUtil;
import com.saxifrages.vess.h2.essresults.ArrayListEssResultSet;
import com.saxifrages.vess.h2.essresults.EssResultSet;

public class EssGenericPropsModel extends AbstractEssbaseServerModel {

	public EssGenericPropsModel(EssService service) {
		super(service);
		columns = new ColumnBuilder()
				.addString("PROPERTY_NAME")
				.build();
	}

	@Override
	protected EssResultSet get(IEssOlapServer server) throws EssException {
		String[] propNames = server.getPropertyNames();

		List<Row> rows = new ArrayList<Row>();
		for (String aliasTableName : propNames) {
			Value[] var = new Value[1];
			var[0] = ValueString.get(StringUtil.emptyIfNull(aliasTableName));
			Row row = new Row(var, 0);
			rows.add(row);
		}
		return new ArrayListEssResultSet(rows, 1);
	}

}

package com.saxifrages.vess.h2.models.server;

import java.util.ArrayList;
import java.util.List;

import org.h2.engine.Session;
import org.h2.result.Row;
import org.h2.table.Column;
import org.h2.value.Value;
import org.h2.value.ValueString;

import com.essbase.api.base.EssException;
import com.saxifrages.vess.h2.EssModel;
import com.saxifrages.vess.h2.EssService;
import com.saxifrages.vess.h2.EssTable;
import com.saxifrages.vess.h2.StringUtil;
import com.saxifrages.vess.h2.essresults.ArrayListEssResultSet;
import com.saxifrages.vess.h2.essresults.EssResultSet;

public class EssSubstitutionVariableModel implements EssModel {

	private EssService service;

	public Column[] getColumns() {
		Column name = new Column("NAME", Value.STRING, 320, -1, 20);
		name.setPrimaryKey(true);
		Column value = new Column("VALUE", Value.STRING, 256, -1, 20);
		//Column app = new Column("APPLICATION", Value.STRING, 30, -1, 10);
		//Column database = new Column("DATABASE", Value.STRING, 30, -1, 10);
		return new Column[] { name, value};
	}

	public EssResultSet get() {
		try {
			List<Row> rows = new ArrayList<Row>();
			for (String[] variable : service.getSubstitutionVariables()) {
				Value[] var = new Value[2];
				var[0] = ValueString.get(StringUtil.emptyIfNull(variable[0]));
				var[1] = ValueString.get(StringUtil.emptyIfNull(variable[1]));
				//var[2] = ValueString.get(StringUtil.emptyIfNull(variable[2]));
				//var[3] = ValueString.get(StringUtil.emptyIfNull(variable[3]));
				Row row = new Row(var, 0);
				rows.add(row);
			}
			return new ArrayListEssResultSet(rows, 4);
		} catch (EssException e) {
			throw EssTable.wrapException("Unable to get substitution variables", e);
		}
	}

	public void addRealRow(Session session, Row row) {
		System.out.println("Row to add: " + row);
		for (int col = 0; col < row.getColumnCount(); col++) {
			System.out.println(row.getValue(col));
		}
		String name = row.getValue(0).getString();
		String value = row.getValue(1).getString();
		//String application = row.getValue(2).getString();
		//String database = row.getValue(3).getString();
		try { 
			service.addSubstitutionVariable(name, value);
		} catch (EssException e) {
			throw EssTable.wrapException("Unable to set substitution variable", e);
		}
	}
	
	public void removeRealRow(Session session, Row row) {
		
	}

	public EssService getService() {
		return service;
	}

	public void setService(EssService service) {
		this.service = service;
	}

	@Override
	public void endDataLoad() {
		// TODO Auto-generated method stub

	}

}

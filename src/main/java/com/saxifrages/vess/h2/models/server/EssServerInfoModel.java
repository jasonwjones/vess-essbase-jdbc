package com.saxifrages.vess.h2.models.server;

import com.essbase.api.base.EssException;
import com.essbase.api.datasource.IEssOlapServer;
import com.saxifrages.vess.h2.ColumnBuilder;
import com.saxifrages.vess.h2.EssService;
import com.saxifrages.vess.h2.essresults.ArrayListEssResultSet;
import com.saxifrages.vess.h2.essresults.EssResultSet;
import com.saxifrages.vess.h2.models.AbstractEssbaseServerModel;

public class EssServerInfoModel extends AbstractEssbaseServerModel {

	public EssServerInfoModel(EssService service) {
		super(service);
		columns = new ColumnBuilder()
			.addString("NAME")
			.addString("LOCALE")
			.build();
	}

	@Override
	protected EssResultSet get(IEssOlapServer server) throws EssException {
		//return new ArrayListEssResultSet(rows, getColumnCount());
		// TODO Auto-generated method stub
		return null;
	}

}

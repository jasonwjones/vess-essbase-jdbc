package com.saxifrages.vess.h2.models;

import java.sql.SQLException;

import com.saxifrages.vess.h2.EssService;
import com.saxifrages.vess.h2.essresults.EssResultSet;

public abstract class AbstractEssbaseReadOnlyModel extends AbstractEssbaseModel {

	public AbstractEssbaseReadOnlyModel(EssService service) {
		super(service);
	}
	
	@Override
	public void endDataLoad() {
		// TODO Auto-generated method stub

	}

	@Override
	public EssResultSet get() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

}

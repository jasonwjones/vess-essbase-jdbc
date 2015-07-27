package com.saxifrages.vess.h2.models;

import java.sql.SQLException;

import com.essbase.api.base.EssException;
import com.essbase.api.datasource.IEssOlapApplication;
import com.saxifrages.vess.h2.EssService;
import com.saxifrages.vess.h2.essresults.EssResultSet;

public abstract class AbstractEssbaseAppModel extends AbstractEssbaseModel {

	protected String application;
	
	public AbstractEssbaseAppModel(EssService service, String application) {
		super(service);
		this.application = application;
	}
	
	@Override
	public void endDataLoad() {
		// TODO Auto-generated method stub
	}

	@Override
	public EssResultSet get() throws SQLException {
		try {
			return get(service.getApplication(application));
		} catch (EssException e) {
			throw new SQLException("Error performing cube operation", e);
		}
	}
	
	public abstract EssResultSet get(IEssOlapApplication application) throws EssException;
	
}

package com.saxifrages.vess.h2.models;

import java.sql.SQLException;

import com.essbase.api.base.EssException;
import com.essbase.api.datasource.IEssCube;
import com.saxifrages.vess.h2.EssService;
import com.saxifrages.vess.h2.essresults.EssResultSet;

public abstract class AbstractEssbaseCubeModel extends AbstractEssbaseModel {

	protected String application;
	
	protected String cube;

	public AbstractEssbaseCubeModel(EssService service, String application, String cube) {
		super(service);
		this.application = application;
		this.cube = cube;
	}
	
	@Override
	public void endDataLoad() {
		// TODO Auto-generated method stub

	}

	@Override
	public EssResultSet get() throws SQLException {
		try {
			return get(service.getCube(application, cube));
		} catch (EssException e) {
			throw new SQLException("Error performing cube operation", e);
		}
	}
	
	public abstract EssResultSet get(IEssCube cube) throws EssException;
	

}

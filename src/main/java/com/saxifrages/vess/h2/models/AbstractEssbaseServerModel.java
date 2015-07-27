package com.saxifrages.vess.h2.models;

import java.sql.SQLException;

import com.essbase.api.base.EssException;
import com.essbase.api.datasource.IEssOlapServer;
import com.saxifrages.vess.h2.EssService;
import com.saxifrages.vess.h2.essresults.EssResultSet;

public abstract class AbstractEssbaseServerModel extends AbstractEssbaseModel {

	public AbstractEssbaseServerModel(EssService service) {
		super(service);
	}
	
	@Override
	public void endDataLoad() {
		// TODO Auto-generated method stub

	}

	@Override
	public EssResultSet get() throws SQLException {
		try {
			return get(service.getServer());
		} catch (EssException e) {
			throw new SQLException("Problem retrieving sessions/requests from Essbase server", e);
		}
	}
	
	protected abstract EssResultSet get(IEssOlapServer server) throws EssException;

}

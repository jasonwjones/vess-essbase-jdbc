package com.saxifrages.vess.h2.models;

import com.essbase.api.base.EssException;
import com.essbase.api.datasource.IEssOlapServer;
import com.saxifrages.vess.h2.EssService;
import com.saxifrages.vess.h2.essresults.EssResultSet;

public class EssServerPropsModel extends AbstractEssbaseServerModel {

	public EssServerPropsModel(EssService service) {
		super(service);
	}
	
	@Override
	protected EssResultSet get(IEssOlapServer server) throws EssException {
		server.getName();
		server.getLocale();
		server.getDescription();
		server.getOlapSecurityMode();
		server.getOlapServerVersion();
		server.isUnicodeEnabled();
		// TODO Auto-generated method stub
		return null;
	}

}

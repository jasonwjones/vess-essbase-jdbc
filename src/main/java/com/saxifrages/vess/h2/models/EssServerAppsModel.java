package com.saxifrages.vess.h2.models;

import org.h2.value.Value;
import org.h2.value.ValueInt;
import org.h2.value.ValueString;

import com.essbase.api.base.EssException;
import com.essbase.api.datasource.IEssOlapApplication;
import com.essbase.api.datasource.IEssOlapServer;
import com.saxifrages.vess.h2.ColumnBuilder;
import com.saxifrages.vess.h2.EssService;
import com.saxifrages.vess.h2.essresults.EssResultSet;

public class EssServerAppsModel extends AbstractEssbaseServerModel {
	
	private static final AppMapper APP_MAPPER = new AppMapper();
	
	public EssServerAppsModel(EssService service) {
		super(service);
		columns = new ColumnBuilder()
			.add(ColConstants.APPLICATION)
			.key()
			.addInteger("CUBE_COUNT")
			.addInteger("USERS_CONNECTED")
			.addInteger("ELAPSED_TIME")
			.addInteger("AUTOLOAD")
			.build();
	}
	
	@Override
	protected EssResultSet get(IEssOlapServer server) throws EssException {
		return mapper(server.getApplications(), APP_MAPPER);
	}

	public static class AppMapper implements IterMapper<IEssOlapApplication> {
		@Override
		public void convert(IEssOlapApplication app, Value[] values) throws EssException {
			values[0] = ValueString.get(app.getName());
			values[1] = ValueInt.get(app.getCountCubes());
			values[2] = ValueInt.get(app.getCountUsersConnected());
			values[3] = ValueInt.get(app.getElapsedAppTime());
			values[4] = ValueInt.get(app.isAutoLoad() ? 1 : 0);
		}
	}

}

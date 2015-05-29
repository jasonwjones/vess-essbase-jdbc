package com.saxifrages.vess.h2.models;

import java.sql.SQLException;

import org.h2.value.Value;
import org.h2.value.ValueString;

import com.essbase.api.base.EssException;
import com.essbase.api.datasource.IEssOlapServer.IEssOlapConnectionInfo;
import com.saxifrages.vess.h2.ColumnBuilder;
import com.saxifrages.vess.h2.EssService;
import com.saxifrages.vess.h2.essresults.EssResultSet;

/**
 * Models the requests as seen in the Sessions pane of EAS
 * 
 * @author jasonwjones
 *
 */
public class EssOlapRequestModel extends AbstractEssbaseModel {
	
	private static final SessionMapper SESSION_MAPPER = new SessionMapper();
	
	public EssOlapRequestModel(EssService service) {
		super(service);
		columns = new ColumnBuilder()
			.add(ColConstants.APPLICATION)
			.add(ColConstants.CUBE)
			.addColumn("USER_NAME")
			.addColumn("LOGIN_TIME")
			.addString("CONNECT_TIME")
			.build();
	}

	@Override
	public void endDataLoad() {
		// do nothing
	}

	// TODO: Could abstract an Essbase class above this and below the current
	// parent and have it wrap EssExceptions for us to clean this up a little
	@Override
	public EssResultSet get() throws SQLException {
		try {
			//System.out.println("FOUND SESSION COUNT: " + service.getServer().getConnections().getCount());
			return mapper(service.getServer().getConnections(), SESSION_MAPPER);
		} catch (EssException e) {
			throw new SQLException("Problem retrieving sessions/requests from Essbase server", e);
		}
	}
	
	public static class SessionMapper implements IterMapper<IEssOlapConnectionInfo> {
		@Override
		public void convert(IEssOlapConnectionInfo request, Value[] values) throws EssException {
			values[0] = ValueString.get(request.getConnectedApplicationName());
			values[1] = ValueString.get(request.getConnectedCubeName());
			values[2] = ValueString.get(request.getConnectedUserName());
			values[3] = ValueString.get(request.getConnectedLoginTime());
			values[4] = ValueString.get(request.getDatabaseConnectTime());
		}
	}
	
}

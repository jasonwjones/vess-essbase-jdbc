package com.saxifrages.vess.h2.models;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.h2.engine.Session;
import org.h2.result.Row;
import org.h2.table.Column;
import org.h2.value.Value;
import org.h2.value.ValueString;

import com.essbase.api.base.EssException;
import com.essbase.api.base.IEssIterator;
import com.essbase.api.datasource.IEssOlapRequest;
import com.essbase.api.datasource.IEssOlapServer;
import com.saxifrages.vess.h2.ColumnBuilder;
import com.saxifrages.vess.h2.essresults.ArrayListEssResultSet;
import com.saxifrages.vess.h2.essresults.EssResultSet;

/**
 * Models the requests as seen in the Sessions pane of EAS
 * 
 * @author jasonwjones
 *
 */
public class EssOlapRequestModel extends AbstractEssbaseModel {

	private IEssOlapServer server;
	
	public EssOlapRequestModel() {
		columns = new ColumnBuilder()
			.add(ColConstants.APPLICATION)
			.add(ColConstants.CUBE)
			.addColumn("USER_NAME")
			.addColumn("REQUEST_SOURCE")
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
			return mapper(server.getRequests(null, null, null), new OlapRequestMapper());
		} catch (EssException e) {
			throw new SQLException("Problem retrieving sessions/requests from Essbase server", e);
		}
	}

	// user, app, cube; all can be null (no filtering)
	public static class OlapRequestMapper implements IterMapper<IEssOlapRequest> {
		@Override
		public void convert(IEssOlapRequest request, Value[] values) throws EssException {
			values[0] = ValueString.get(request.getOlapApplicationName());
			values[1] = ValueString.get(request.getCubeName());
			values[2] = ValueString.get(request.getUserName());
			values[3] = ValueString.get(request.getRequestSource());
		}
	}
		
}

package com.saxifrages.vess.h2.models;

import java.sql.SQLException;

import org.h2.value.Value;
import org.h2.value.ValueInt;
import org.h2.value.ValueString;

import com.essbase.api.base.EssException;
import com.essbase.api.datasource.IEssCube;
import com.essbase.api.datasource.IEssOlapApplication;
import com.essbase.api.datasource.IEssOlapRequest;
import com.essbase.api.datasource.IEssOlapServer;
import com.essbase.api.metadata.IEssDimension;
import com.saxifrages.vess.h2.ColumnBuilder;
import com.saxifrages.vess.h2.essresults.EssResultSet;
import com.saxifrages.vess.h2.models.AbstractEssbaseModel.IterMapper;

/**
 * Models the requests as seen in the Sessions pane of EAS
 * 
 * @author jasonwjones
 *
 */
public class EssCubeInfoModel extends AbstractEssbaseCubeReadOnlyModel {

	private IEssOlapServer server;
	
	private static final IterMapper<IEssDimension> CUBE_MAPPER = new CubeMapper();
	
	public EssCubeInfoModel() {
		columns = new ColumnBuilder()
			.addString("STARTUP")
			.build();
	}

	public void fetch(IEssCube cube) throws EssException {
		cube.isAutoLoad();
		cube.isLoadable();
		cube.getDataCacheSizeEx();
		cube.getDataFileCacheSize();
		cube.getIndexCacheSize();
		cube.getIndexPageSize();
		cube.getElapsedCubeTime();
		cube.getCountUsersConnected();
		cube.getCountDimensions();
	}
	
	// TODO: Could abstract an Essbase class above this and below the current
	// parent and have it wrap EssExceptions for us to clean this up a little
	@Override
	public EssResultSet get() throws SQLException {
		try {
			return mapper(server.getRequests(null, null, null), CUBE_MAPPER);
		} catch (EssException e) {
			throw new SQLException("Problem retrieving sessions/requests from Essbase server", e);
		}
	}
		
	public static class CubeMapper implements IterMapper<IEssDimension> {
		@Override
		public void convert(IEssDimension dimension, Value[] values) throws EssException {
			values[0] = ValueString.get(dimension.getName());
			values[1] = ValueInt.get(-1);
			values[2] = ValueInt.get(dimension.getDeclaredSize());
			values[3] = ValueInt.get(dimension.getActualSize());
		}
	}
	
}

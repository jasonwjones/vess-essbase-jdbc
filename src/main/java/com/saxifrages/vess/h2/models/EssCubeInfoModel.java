package com.saxifrages.vess.h2.models;

import java.sql.SQLException;

import org.h2.value.Value;
import org.h2.value.ValueInt;
import org.h2.value.ValueLong;
import org.h2.value.ValueString;

import com.essbase.api.base.EssException;
import com.essbase.api.datasource.IEssCube;
import com.essbase.api.datasource.IEssOlapServer;
import com.saxifrages.vess.h2.ColumnBuilder;
import com.saxifrages.vess.h2.EssService;
import com.saxifrages.vess.h2.essresults.EssResultSet;

/**
 * Models the requests as seen in the Sessions pane of EAS
 * 
 * @author jasonwjones
 *
 */
public class EssCubeInfoModel extends AbstractEssbaseCubeReadOnlyModel {
	
	private static final IterMapper<IEssCube> CUBE_MAPPER = new CubeInfoMapper();
	
	public EssCubeInfoModel(EssService service) {
		super(service);
		columns = new ColumnBuilder()
			.addString("NAME")
			.addInteger("INDEX_CACHE_SIZE")
			.addLong("DATA_CACHE_SIZE")
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
	
	@Override
	public EssResultSet get() throws SQLException {
		try {
			return mapper(service.getServer().getApplication("Sample").getCubes(), CUBE_MAPPER);
		} catch (EssException e) {
			throw new SQLException("Problem retrieving sessions/requests from Essbase server", e);
		}
	}
		
	public static class CubeInfoMapper implements IterMapper<IEssCube> {
		@Override
		public void convert(IEssCube cube, Value[] values) throws EssException {
			values[0] = ValueString.get(cube.getName());
			values[1] = ValueInt.get(cube.getIndexCacheSize());
			values[2] = ValueLong.get(cube.getDataCacheSizeEx());
		}
	}
	
}

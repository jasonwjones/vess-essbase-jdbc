package com.saxifrages.vess.h2.models;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.h2.result.Row;
import org.h2.value.Value;
import org.h2.value.ValueInt;
import org.h2.value.ValueLong;
import org.h2.value.ValueString;

import com.essbase.api.base.EssException;
import com.essbase.api.base.IEssIterator;
import com.essbase.api.datasource.IEssCube;
import com.essbase.api.datasource.IEssOlapApplication;
import com.saxifrages.vess.h2.ColumnBuilder;
import com.saxifrages.vess.h2.EssService;
import com.saxifrages.vess.h2.essresults.ArrayListEssResultSet;
import com.saxifrages.vess.h2.essresults.EssResultSet;

/**
 * Models the requests as seen in the Sessions pane of EAS
 * 
 * @author jasonwjones
 *
 */
public class EssCubeInfoModel extends AbstractEssbaseModel {
	
	private static final IterMapper<IEssCube> CUBE_MAPPER = new CubeInfoMapper();
	
	public EssCubeInfoModel(EssService service) {
		super(service);
		columns = new ColumnBuilder()
			.add(ColConstants.APPLICATION)
			.add(ColConstants.CUBE)
			.addInteger("INDEX_CACHE_SIZE")
			.addLong("DATA_CACHE_SIZE")
			.build();
	}

//	public void fetch(IEssCube cube) throws EssException {
//		cube.isAutoLoad();
//		cube.isLoadable();
//		cube.getDataCacheSizeEx();
//		cube.getDataFileCacheSize();
//		cube.getIndexCacheSize();
//		cube.getIndexPageSize();
//		cube.getElapsedCubeTime();
//		cube.getCountUsersConnected();
//		cube.getCountDimensions();
//	}
	
	@Override
	public EssResultSet get() throws SQLException {
		try {
			List<Row> rows = new ArrayList<Row>();
			IEssIterator apps = service.getServer().getApplications();
			for (int appIndex = 0; appIndex < apps.getCount(); appIndex++) {
				IEssIterator iterator = ((IEssOlapApplication) apps.getAt(appIndex)).getCubes();
				
				for (int i = 0; i < iterator.getCount(); i++) {
					Value[] values = new Value[getColumnCount()];
					CUBE_MAPPER.convert((IEssCube) iterator.getAt(i), values);
					rows.add(new Row(values, 0));
				}
			}
			return new ArrayListEssResultSet(rows, getColumnCount());
		} catch (EssException e) {
			throw new SQLException("Problem retrieving sessions/requests from Essbase server", e);
		}
	}
		
	public static class CubeInfoMapper implements IterMapper<IEssCube> {
		@Override
		public void convert(IEssCube cube, Value[] values) throws EssException {
			values[0] = ValueString.get(cube.getApplicationName());
			values[1] = ValueString.get(cube.getName());
			values[2] = ValueInt.get(cube.getIndexCacheSize());
			values[3] = ValueLong.get(cube.getDataCacheSizeEx());
		}
	}

	@Override
	public void endDataLoad() {
		// TODO Auto-generated method stub
		
	}
	
}

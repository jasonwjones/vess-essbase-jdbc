package com.saxifrages.vess.h2.models;

import java.sql.SQLException;

import org.h2.value.Value;
import org.h2.value.ValueInt;
import org.h2.value.ValueString;

import com.essbase.api.base.EssException;
import com.essbase.api.datasource.IEssCube;
import com.essbase.api.datasource.IEssOlapServer;
import com.essbase.api.metadata.IEssDimension;
import com.saxifrages.vess.h2.ColumnBuilder;
import com.saxifrages.vess.h2.EssService;
import com.saxifrages.vess.h2.essresults.EssResultSet;

/**
 * Models the requests as seen in the Sessions pane of EAS
 * 
 * @author jasonwjones
 *
 */
public class EssCubeStatsModel extends AbstractEssbaseServerModel {

	//private IEssOlapServer server;
	
	private static final IterMapper<IEssDimension> DIMENSION_MAPPER = new DimensionMapper();
	
	public EssCubeStatsModel(EssService service) {
		super(service);
		columns = new ColumnBuilder()
			.add(ColConstants.DIMENSION_NAME)
			.addInteger("TYPE")
			.addInteger("DECLARED_SIZE")
			.addInteger("ACTUAL_SIZE")
			.build();
	}
	
	@Override
	public void endDataLoad() {
		// do nothing
	}
		
	public static class DimensionMapper implements IterMapper<IEssDimension> {
		@Override
		public void convert(IEssDimension dimension, Value[] values) throws EssException {
			values[0] = ValueString.get(dimension.getName());
			values[1] = ValueInt.get(-1);
			values[2] = ValueInt.get(dimension.getDeclaredSize());
			values[3] = ValueInt.get(dimension.getActualSize());
		}
	}

	@Override
	protected EssResultSet get(IEssOlapServer server) throws EssException {
		return mapper(server.getRequests(null, null, null), DIMENSION_MAPPER);
	}
	
}

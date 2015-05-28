package com.saxifrages.vess.h2.models;

import org.h2.value.Value;
import org.h2.value.ValueBoolean;
import org.h2.value.ValueInt;
import org.h2.value.ValueLong;
import org.h2.value.ValueString;

import com.essbase.api.base.EssException;
import com.essbase.api.dataquery.IEssDrillThrough;
import com.essbase.api.dataquery.IEssDrillThroughReport;
import com.essbase.api.dataquery.IEssURLDrillThrough;
import com.essbase.api.datasource.IEssCube;
import com.essbase.api.datasource.IEssOlapUser;
import com.saxifrages.vess.h2.ColumnBuilder;
import com.saxifrages.vess.h2.EssService;
import com.saxifrages.vess.h2.essresults.EssResultSet;
import com.saxifrages.vess.h2.models.AbstractEssbaseModel.IterMapper;

public class EssCubeDrillthroughModel extends AbstractEssbaseCubeReadOnlyModel {

	public EssCubeDrillthroughModel(EssService service, String application, String cube) {
		super(service, application, cube);
		columns = new ColumnBuilder()
			.addString("URL_NAME")
			.addString("URL_LINK")
			.addInteger("COUNT_OF_DRILL_REGIONS")
			.addBoolean("IS_LEVEL0")
			.build();
	}

	@Override
	public EssResultSet get(IEssCube cube) throws EssException {
		cube.listDrillThroughURLs();
		return null;
	}

	public static class DrillthroughMapper implements IterMapper<IEssURLDrillThrough> {
		@Override
		public void convert(IEssURLDrillThrough dt, Value[] values) throws EssException {
			values[0] = ValueString.get(dt.getURLName());
			values[1] = ValueString.get(dt.getURLLink());
			values[2] = ValueInt.get(dt.getCountOfDrillRegions());
			values[3] = ValueBoolean.get(dt.isLevel0());
		}
	}

	
}

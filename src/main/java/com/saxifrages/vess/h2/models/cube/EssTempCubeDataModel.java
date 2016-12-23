package com.saxifrages.vess.h2.models.cube;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.h2.result.Row;
import org.h2.value.Value;
import org.h2.value.ValueDouble;
import org.h2.value.ValueString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.essbase.api.base.EssException;
import com.essbase.api.datasource.IEssCube;
import com.saxifrages.vess.h2.ColumnBuilder;
import com.saxifrages.vess.h2.EssService;
import com.saxifrages.vess.h2.essresults.ArrayListEssResultSet;
import com.saxifrages.vess.h2.essresults.EssResultSet;
import com.saxifrages.vess.h2.models.AbstractEssbaseCubeModel;

public class EssTempCubeDataModel extends AbstractEssbaseCubeModel {

	private static final Logger logger = LoggerFactory.getLogger(EssTempCubeDataModel.class);
	
	private List<Object[]> data;

	public EssTempCubeDataModel(EssService service, String application, String cube) {
		super(service, application, cube);

		columns = new ColumnBuilder()
				.addString("SCENARIO")
				.addString("YEAR_")
				.addString("MARKET")
				.addString("MEASURE")
				.addString("PRODUCT")
				.addDouble("DATA")
				.build();

		InputStream is = getClass().getResourceAsStream("/sample-basic.csv");
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));

		try {
			data = new ArrayList<Object[]>();
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				System.out.println("Line: " + line);
				String[] arr = line.split(",");
				Object[] currLine = new Object[6];
				for (int i = 0; i < 5; i++) {
					currLine[i] = arr[i];
				}
				currLine[5] = Double.valueOf(arr[5]);
				data.add(currLine);
			}
			logger.info("Have {} data points", data.size());
		} catch (IOException e) {
			logger.error("Couldn't read: {}", e);
		}

	}

	@Override
	public EssResultSet get(IEssCube cube) throws EssException {
		/*
		 * 		List<Row> rows = new ArrayList<Row>();
		for (String[] variable : cube.getSubstitutionVariables()) {
			Value[] var = new Value[getColumnCount()];
			var[0] = ValueString.get(StringUtil.emptyIfNull(variable[0]));
			var[1] = ValueString.get(StringUtil.emptyIfNull(variable[1]));
			var[2] = ValueString.get(StringUtil.emptyIfNull(variable[2]));
			var[3] = ValueString.get(StringUtil.emptyIfNull(variable[3]));
			rows.add(new Row(var, 0));
		}
		return new ArrayListEssResultSet(rows, getColumnCount());
		 */
		// TODO Auto-generated method stub
		
		List<Row> rows = new ArrayList<Row>();
		for (Object[] dataPoint : data) {
			Value[] var = new Value[getColumnCount()];
			for (int i = 0; i < 5; i++) {
				var[i] = ValueString.get((String)dataPoint[i]);
			}
			var[5] = ValueDouble.get((Double) dataPoint[5]);
			rows.add(new Row(var, 0));
		}
		return new ArrayListEssResultSet(rows, getColumnCount());
	}

}

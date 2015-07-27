package com.saxifrages.vess.h2.models.app;

import java.util.ArrayList;
import java.util.List;

import org.h2.result.Row;
import org.h2.value.Value;
import org.h2.value.ValueInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.essbase.api.base.EssException;
import com.essbase.api.datasource.IEssOlapApplication;
import com.saxifrages.vess.h2.ColumnBuilder;
import com.saxifrages.vess.h2.EssService;
import com.saxifrages.vess.h2.essresults.ArrayListEssResultSet;
import com.saxifrages.vess.h2.essresults.EssResultSet;
import com.saxifrages.vess.h2.models.AbstractEssbaseAppModel;

public class EssAppPropsModel extends AbstractEssbaseAppModel {
	
	private static final Logger logger = LoggerFactory.getLogger(EssAppPropsModel.class);
	
	public EssAppPropsModel(EssService service, String application) {
		super(service, application);
		columns = new ColumnBuilder()
			.addInteger("COUNT_OF_CUBES")
			.build();
	}
	
	@Override
	public void endDataLoad() {
		// do nothing
	}

	@Override
	public EssResultSet get(IEssOlapApplication application) throws EssException {
		
		logger.info("App has properties: {}", application.getCountProperties());
		for (String propName : application.getPropertyNames()) {
			logger.info("Prop: {}", propName);
		}
		
		List<Row> rows = new ArrayList<Row>();
		Value[] var = new Value[getColumnCount()];
		var[0] = ValueInt.get(application.getCountCubes());
		rows.add(new Row(var, 0));
		return new ArrayListEssResultSet(rows, getColumnCount());
	}

}

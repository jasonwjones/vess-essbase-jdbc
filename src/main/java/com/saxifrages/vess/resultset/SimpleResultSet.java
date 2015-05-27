package com.saxifrages.vess.resultset;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SimpleResultSet extends VessAbstractResultSet {

	private int row = -1;
	
	private String[] columnNames;
	
	private Class<?>[] types;
	
	private List<Object[]> data;
	
	SimpleResultSet(ResultSetBuilder builder) {
		this.columnNames = builder.getColumnNames();
		this.types = builder.getTypes();
		
		data = new ArrayList<Object[]>();
		for (Object[] row : builder.getData()) {
			data.add(row);
		}
	}
	
	public boolean next() throws SQLException {
		return ++row < data.size();
	}

}

package com.saxifrages.vess.resultset;

import java.sql.ResultSet;

public class ResultSetBuilder {

	private String[] columnNames;
	
	private Class<?>[] types;
	
	private Iterable<Object[]> data;
	
	public ResultSetBuilder columnNames(String[] columnNames) {
		this.columnNames = columnNames;
		return this;
	}
	
	public ResultSetBuilder columnTypes(Class<?>[] types) {
		this.types = types;
		return this;
	}
	
	public ResultSetBuilder data(Iterable<Object[]> data) {
		this.data = data;
		return this;
	}
	
	public ResultSet build() {
		return new SimpleResultSet(this);
	}

	public String[] getColumnNames() {
		return columnNames;
	}

	public Class<?>[] getTypes() {
		return types;
	}

	public Iterable<Object[]> getData() {
		return data;
	}
	
}

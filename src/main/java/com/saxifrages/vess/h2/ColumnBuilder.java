package com.saxifrages.vess.h2;

import java.util.ArrayList;
import java.util.List;

import org.h2.table.Column;
import org.h2.value.Value;

public class ColumnBuilder {
	
	private List<Column> columns;
	
	private Column recent;
	
	private int defaultStringLength = 50;
	
	public ColumnBuilder() {
		columns = new ArrayList<>();
	}
	
	public ColumnBuilder add(Column column) {
		Column c = new Column(column.getName(), column.getType());
		c.copy(column);
		columns.add(c);
		recent = c;
		return this;
	}
	
	public ColumnBuilder addColumn(String columnName) {
		Column c = new Column(columnName, Value.STRING);
		columns.add(c);
		recent = c;
		return this;
	}
	
	public ColumnBuilder addString(String columnName, int length) {
		Column c = new Column(columnName, Value.STRING);
		c.setPrecision(length);
		columns.add(c);
		recent = c;
		return this;
	}

	public ColumnBuilder addBoolean(String columnName) {
		Column c = new Column(columnName, Value.BOOLEAN);
		columns.add(c);
		recent = c;
		return this;
	}
	
	public ColumnBuilder addString(String columnName) {
		Column c = new Column(columnName.toUpperCase(), Value.STRING);
		columns.add(c);
		recent = c;
		return this;
	}
	
	public ColumnBuilder addStrings(Iterable<String> columnNames) {
		for (String columnName : columnNames) {
			addString(columnName);
		}
		return this;
	}
	
	public ColumnBuilder addStrings(Iterable<String> columnNames, int length) {
		for (String columnName : columnNames) {
			addString(columnName, length);
		}
		return this;
	}
	
	public ColumnBuilder addDouble(String columnName) {
		Column column = new Column(columnName, Value.DOUBLE);
		columns.add(column);
		recent = column;
		return this;
	}
	
	public ColumnBuilder addInteger(String columnName) {
		Column column = new Column(columnName, Value.INT);
		columns.add(column);
		recent = column;
		return this;
	}
	
	public ColumnBuilder notNullable() {
		recent.setNullable(false);
		return this;
	}
	
	public Column[] build() {
		return columns.toArray(new Column[]{});
	}
	
}
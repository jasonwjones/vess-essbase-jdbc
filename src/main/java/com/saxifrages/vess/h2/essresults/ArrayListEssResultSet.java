package com.saxifrages.vess.h2.essresults;

import java.util.List;

import org.h2.result.Row;
import org.h2.value.Value;

public class ArrayListEssResultSet implements EssResultSet {

	private int row = -1;
	
	private List<Row> rows;
	
	private int columnCount;
	
	public ArrayListEssResultSet(List<Row> rows, int columnCount) {
		System.out.println("New ArrayListResultSet with " + rows.size() + " rows");
		this.rows = rows;
		this.columnCount = columnCount;
	}
	
	@Override
	public boolean next() {
		return ++row < rows.size();
	}

	@Override
	public int getColumnCount() {
		return columnCount;
	}

	@Override
	public Value getValue(int column) {
		return rows.get(row).getValue(column);
	}

	@Override
	public Row getRow() {
		return rows.get(row);
	}
	
}

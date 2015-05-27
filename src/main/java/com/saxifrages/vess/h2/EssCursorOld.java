package com.saxifrages.vess.h2;

import java.util.ArrayList;

import org.h2.index.Cursor;
import org.h2.result.Row;
import org.h2.result.SearchRow;

public class EssCursorOld implements Cursor {

	private ArrayList<Row> rows;
	
	private int index = -1;
	
	public EssCursorOld(ArrayList<Row> rows) {
		this.rows = rows;
	}
	
	@Override
	public Row get() {
		System.out.println("Getting row");
		return rows.get(index);
	}

	@Override
	public SearchRow getSearchRow() {
		System.out.println("Getting search row");
		return rows.get(index);
	}

	@Override
	public boolean next() {
		System.out.println("Ask next");
		return ++index < rows.size();
	}

	@Override
	public boolean previous() {
		System.out.println("Ask prev");
		// TODO Auto-generated method stub
		return false;
	}

}

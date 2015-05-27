package com.saxifrages.vess.h2;

import org.h2.engine.Session;
import org.h2.index.Cursor;
import org.h2.result.Row;
import org.h2.result.SearchRow;

import com.saxifrages.vess.h2.essresults.EssResultSet;

public class EssCursor implements Cursor {

	private EssTable table;
	private EssResultSet resultSet;
	private Session session;
	
	public EssCursor(EssTable table, EssResultSet resultSet, Session session) {
		this.table = table;
		this.resultSet = resultSet;
		this.session = session;
	}
	
	@Override
	public Row get() {
		return resultSet.getRow();
	}

	@Override
	public SearchRow getSearchRow() {
		return get();
	}

	@Override
	public boolean next() {
		return resultSet.next();
	}

	@Override
	public boolean previous() {
		return false;
	}

}

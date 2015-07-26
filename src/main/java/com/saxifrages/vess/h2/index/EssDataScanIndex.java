package com.saxifrages.vess.h2.index;

import org.h2.engine.Session;
import org.h2.index.BaseIndex;
import org.h2.index.Cursor;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.result.SortOrder;
import org.h2.table.TableFilter;

public class EssDataScanIndex extends BaseIndex {

	@Override
	public void close(Session session) {
		// TODO Auto-generated method stub

	}

	@Override
	public void add(Session session, Row row) {
		// TODO Auto-generated method stub

	}

	@Override
	public void remove(Session session, Row row) {
		// TODO Auto-generated method stub

	}

	@Override
	public Cursor find(Session session, SearchRow first, SearchRow last) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getCost(Session session, int[] masks, TableFilter filter, SortOrder sortOrder) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void remove(Session session) {
		// TODO Auto-generated method stub

	}

	@Override
	public void truncate(Session session) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean canGetFirstOrLast() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Cursor findFirstOrLast(Session session, boolean first) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean needRebuild() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public long getRowCount(Session session) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getRowCountApproximation() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getDiskSpaceUsed() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void checkRename() {
		// TODO Auto-generated method stub

	}

}

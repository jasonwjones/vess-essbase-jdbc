package com.saxifrages.vess.h2;

import org.h2.engine.Session;
import org.h2.index.BaseIndex;
import org.h2.index.Cursor;
import org.h2.index.IndexType;
import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.result.SortOrder;
import org.h2.table.IndexColumn;
import org.h2.table.TableFilter;

/**
 * In retrospect, this is a pretty thin implementation, the only method with any
 * meat in it is the addRow method that just dumps it back in the lap of the
 * associated EssTable.
 * 
 * @author jasonwjones
 *
 */
public class EssIndex extends BaseIndex {

	private long rowCount = 100;

	private EssTable essTable;

	// public LinkedIndex(TableLink table, int id, IndexColumn[] columns,
	// IndexType indexType)

	public EssIndex(EssTable table, int id, IndexColumn[] columns, IndexType type) {
		initBaseIndex(table, id, null, columns, type);
		essTable = table;
	}

	public EssIndex(EssTable table, int id, String name, IndexColumn[] columns, IndexType type) {
		initBaseIndex(table, id, name, columns, type);
		essTable = table;
	}

	/**
	 * Same as LinkedIndex
	 */
	@Override
	public String getCreateSQL() {
		return "EssIndex create SQL";
	}

	/**
	 * Same as LinkedIndex
	 */
	@Override
	public void close(Session session) {
		System.out.println("Index close");
		// nothing to do
	}

	public void add(Session session, Row row) {
		System.out.println("Index add");
		essTable.addRealRow(session, row);
		rowCount++;
	}

	public void remove(Session session, Row row) {
		System.out.println("Index remove");
		// TODO Auto-generated method stub

	}

	public Cursor find(Session session, SearchRow first, SearchRow last) {
		System.out.println("Index find");
		try {
		// In the LinkedIndex, a statement is generated and executed,
		// the result set is passed to the LinkedCursor to iterate over
			return new EssCursor(essTable, essTable.getModel().get(), session);
		} catch (Exception e) {
			throw EssTable.wrapException("fake SQL", e);
		}
	}

	public double getCost(Session session, int[] masks, TableFilter filter, SortOrder sortOrder) {
		System.out.println("Index cost");
		// TODO Auto-generated method stub
		return 0;
	}

	
	
	@Override
	public IndexColumn[] getIndexColumns() {
		System.out.println("EssIndex getIndexColumns()");
		// TODO Auto-generated method stub
		return super.getIndexColumns();
	}

	@Override
	public IndexType getIndexType() {
		System.out.println("EssIndex getIndexType()");
		// TODO Auto-generated method stub
		return super.getIndexType();
	}

	@Override
	public boolean isRowIdIndex() {
		System.out.println("EssIndex isRowIdIndex()");
		// TODO Auto-generated method stub
		return super.isRowIdIndex();
	}

	/**
	 * Copied from LinkedIndex
	 */
	@Override
	public void remove(Session session) {
		System.out.println("Index remove (session)");
		// nothing to do
	}

	/**
	 * Copied from LinkedIndex
	 */
	@Override
	public void truncate(Session session) {
		System.out.println("Index truncate");
		// nothing to do
	}

	public boolean canGetFirstOrLast() {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * Copied from LinkedIndex
	 */
	public Cursor findFirstOrLast(Session session, boolean first) {
		throw DbException.getUnsupportedException("LINKED");
	}

	public boolean needRebuild() {
		return false;
	}

	public long getRowCount(Session session) {
		System.out.println("Index rowcount");
		return rowCount;
	}

	public long getRowCountApproximation() {
		System.out.println("Index rowcount approx");
		return rowCount;
	}

	@Override
	public long getDiskSpaceUsed() {
		return 0;
	}

	/**
	 * Copied from LinkedIndex
	 */
	@Override
	public void checkRename() {
		throw DbException.getUnsupportedException("LINKED");
	}

}

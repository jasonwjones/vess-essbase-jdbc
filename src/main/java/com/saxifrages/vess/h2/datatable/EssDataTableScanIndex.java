package com.saxifrages.vess.h2.datatable;

import org.h2.engine.Session;
import org.h2.index.BaseIndex;
import org.h2.index.Cursor;
import org.h2.index.IndexType;
import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.result.SortOrder;
import org.h2.table.Column;
import org.h2.table.IndexColumn;
import org.h2.table.TableFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jasonwjones.jacombi.CartesianIndex;

/**
 * Consult ScanIndex for method parity (methods to "copy" from)...
 * 
 * @author jasonwjones
 *
 */
public class EssDataTableScanIndex extends BaseIndex {

	private static final Logger logger = LoggerFactory.getLogger(EssDataTableScanIndex.class);

	private EssDataTable essDataTable;
	
	// public EssIndex(EssTable table, int id, String name, IndexColumn[]
	// columns, IndexType type) {
	// initBaseIndex(table, id, name, columns, type);
	// essTable = table;
	// }

	public EssDataTableScanIndex(EssDataTable table, int id, String name, IndexColumn[] columns, IndexType type) {
		// index = new EssIndex(this, id, IndexColumn.wrap(columns),
		// IndexType.createNonUnique(false));
		initBaseIndex(table, id, name, columns, type);
		this.essDataTable = table;
	}

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
		logger.info("Called find with first/last {}/{}", first, last);
		
		CartesianIndex<String> ci = new CartesianIndex<String>(essDataTable.getDataTableDefinition().getMembers());
		return new EssDataTableCursor(ci);
		//return new TempEssTableCursor();
	}

	@Override
	public double getCost(Session session, int[] masks, TableFilter filter, SortOrder sortOrder) {
		// TODO Auto-generated method stub
		return 10;
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

	/**
	 * Copied from ScanIndex
	 */
	@Override
	public Cursor findFirstOrLast(Session session, boolean first) {
		throw DbException.getUnsupportedException("SCAN");
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

	/**
	 * Copied from ScanIndex
	 */
	@Override
	public void checkRename() {
		throw DbException.getUnsupportedException("SCAN");
	}

	/**
	 * Copied from ScanIndex
	 */
	@Override
	public int getColumnIndex(Column col) {
		// the scan index cannot use any columns
		return -1;
	}

}

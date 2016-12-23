package com.saxifrages.vess.h2.datatable;

import org.h2.engine.Session;
import org.h2.index.BaseIndex;
import org.h2.index.Cursor;
import org.h2.index.IndexType;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.result.SortOrder;
import org.h2.table.IndexColumn;
import org.h2.table.TableFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jasonwjones.jacombi.CartesianIndex;

public class EssDimensionIndex extends BaseIndex {

	private static final Logger logger = LoggerFactory.getLogger(EssDimensionIndex.class);

	private EssDataTable essDataTable;
	
	public EssDimensionIndex(EssDataTable table, int id, String name, IndexColumn[] newIndexColumns) {
		initBaseIndex(table, id, name, newIndexColumns, IndexType.createNonUnique(false));
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
		logger.info("Asked to find on dim index {}/{}", first, last);
		CartesianIndex<String> ci = new CartesianIndex<String>(essDataTable.getDataTableDefinition().getMembers());
		return new EssDataTableCursor(ci);
	}
	
	@Override
	public Cursor find(TableFilter filter, SearchRow first, SearchRow last) {
		// TODO Auto-generated method stub
		//Cursor find(TableFilter filter, SearchRow first, SearchRow last);
		// TODO: I think this method is our chance to really chop down on the search set
		// when we have known dimensions. The default parent implementation just delegates
		// straight over to the previous method (meaning the default is naive).
		return super.find(filter, first, last);
	}
	

	@Override
	public double getCost(Session session, int[] masks, TableFilter filter, SortOrder sortOrder) {
		logger.info("Estimating cost on dimension index with filter: {}", filter);
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
		logger.info("Can get first or last");
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

package com.saxifrages.vess.h2.datatable;

import java.util.ArrayList;

import org.h2.engine.Session;
import org.h2.index.Index;
import org.h2.index.IndexType;
import org.h2.result.Row;
import org.h2.schema.Schema;
import org.h2.table.Column;
import org.h2.table.IndexColumn;
import org.h2.table.Table;
import org.h2.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EssDataTable extends Table {

	private static final Logger logger = LoggerFactory.getLogger(EssDataTable.class);
	
	private static final String TABLE_TYPE = "TABLE";
	
	private Index index;
	
	private ArrayList<Index> indexes;
	
	private EssDataTableDefinition dataTableDefinition;
	
    public EssDataTable(Schema schema, int id, String tableName, EssDataTableDefinition model) {
    	super(schema, id, tableName, false, false);

    	Column[] columns = createColumns(model);
    	
//		Column name = new Column("NAME", Value.STRING, 320, -1, 20);
//		name.setPrimaryKey(true);
//		Column value = new Column("VALUE", Value.STRING, 256, -1, 20);
//		Column[] columns = new Column[] { name, value};
		setColumns(columns);
    	
		//index = new EssIndex(this, id, IndexColumn.wrap(columns), IndexType.createNonUnique(false));
		
		EssDataTableScanIndex dataIndex = new EssDataTableScanIndex(this, id, "Some index", IndexColumn.wrap(columns), IndexType.createNonUnique(false));
		this.index = dataIndex;
		
		EssDimensionIndex scenarioIndex = new EssDimensionIndex(this, id, "SCEN_INDEX", IndexColumn.wrap(new Column[]{columns[0]}));
		
		indexes = new ArrayList<Index>();
		indexes.add(index);
		indexes.add(scenarioIndex);
		
		this.dataTableDefinition = model;
    }

    private Column[] createColumns(EssDataTableDefinition model) {
    	Column[] columns = new Column[model.getDimensionNames().size() + 2];
    	columns[0] = new Column("KEY", Value.LONG);
    	columns[0].setPrimaryKey(true);
    	
    	int colIndex = 1;
    	for (String dimension : model.getDimensionNames()) {
    		columns[colIndex] = new Column(dimension.toUpperCase(), Value.STRING, 320, -1, 20);
    		colIndex++;
    	}
    	columns[colIndex] = new Column("DATA", Value.DOUBLE, 20, -1, -1);
    	
    	return columns;
    }
    
    public EssDataTableDefinition getDataTableDefinition() {
    	return dataTableDefinition;
    }
    
	@Override
	public boolean lock(Session session, boolean exclusive, boolean forceLockEvenInMvcc) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void close(Session session) {
		// TODO Auto-generated method stub

	}

	@Override
	public void unlock(Session s) {
		// TODO Auto-generated method stub

	}

	@Override
	public Index addIndex(Session session, String indexName, int indexId, IndexColumn[] cols, IndexType indexType,
			boolean create, String indexComment) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeRow(Session session, Row row) {
		// TODO Auto-generated method stub
	}

	@Override
	public void truncate(Session session) {
		// TODO Auto-generated method stub
	}

	@Override
	public void addRow(Session session, Row row) {
		// TODO Auto-generated method stub
	}

	@Override
	public void checkSupportAlter() {
		// TODO Auto-generated method stub
	}

	@Override
	public String getTableType() {
		return TABLE_TYPE;
	}

	@Override
	public Index getScanIndex(Session session) {
		logger.info("Asked for scan index on data table");
		return index;
	}

	@Override
	public Index getUniqueIndex() {
		logger.info("Asked for unique index on data table");
		return index;
	}

	@Override
	public ArrayList<Index> getIndexes() {
		logger.info("Asked for all indexes on data table");
		return this.indexes;
	}

	@Override
	public boolean isLockedExclusively() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public long getMaxDataModificationId() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isDeterministic() {
		return false;
	}

	@Override
	public boolean canGetRowCount() {
		return false;
	}

	@Override
	public boolean canDrop() {
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
	public String getCreateSQL() {
		return "Fake Create Data Table SQL";
	}

	@Override
	public String getDropSQL() {
		return "Fake Drop Data Table SQL";
	}

	@Override
	public void checkRename() {
		// TODO Auto-generated method stub

	}

}

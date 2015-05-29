package com.saxifrages.vess.h2;

import java.sql.SQLException;
import java.util.ArrayList;

import org.h2.api.ErrorCode;
import org.h2.engine.Session;
import org.h2.index.Index;
import org.h2.index.IndexType;
import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.schema.Schema;
import org.h2.table.Column;
import org.h2.table.IndexColumn;
import org.h2.table.Table;
import org.h2.util.New;

public class EssTable extends Table {

	private static final String TABLE_TYPE = "TABLE";
	
    private final ArrayList<Index> indexes = New.arrayList();

    private EssModel model;
    
    private EssIndex index;
 
	private boolean needsFlush = false;
	
    public EssTable(Schema schema, int id, String name, EssModel model) {
    	super(schema, id, name, false, false);
    	setColumns(model.getColumns());
		index = new EssIndex(this, id, IndexColumn.wrap(columns), IndexType.createNonUnique(false));
		System.out.println("Created EssTable: " + name);
		
		Column[] pkColumns = getPrimaryKeyColumns(model.getColumns());
		if (pkColumns != null) {
			// we have uniques we can create a PK with
			System.out.println("Added PK index to EssTable");
			EssIndex pkIndex = new EssIndex(this, id, "PK_" + name, IndexColumn.wrap(pkColumns), IndexType.createPrimaryKey(true, true));
			this.indexes.add(pkIndex);
		} else {
			System.out.println("No PK columns on table " + name);
		}
		
		indexes.add(index);
		this.model = model;  
    }
    
    public static Column[] getPrimaryKeyColumns(Column[] columns) {
    	int countPrimaryKeyColumns = 0;
    	System.out.println("Checking column for PK");
    	for (int i = 0; i < columns.length; i++) {
    		if (columns[i].isPrimaryKey()) {
    			countPrimaryKeyColumns++;
    			System.out.println("Found at " + i);
    		}
    	}
    	
    	if (countPrimaryKeyColumns > 0) {
	    	Column[] pkColumns = new Column[countPrimaryKeyColumns];
	    	countPrimaryKeyColumns = 0;
	    	for (int i = 0; i < columns.length; i++) {
	    		if (columns[i].isPrimaryKey()) {
	    			pkColumns[countPrimaryKeyColumns++] = columns[i];
	    		}	    
	    	}
	    	return pkColumns;
    	} else {
    		return null;
    	}
    }
    
    public EssModel getModel() {
    	return model;
    }
    
	public void addRealRow(Session session, Row row) {
//		System.out.println("Add real row");
//		//row.
//		for (int i = 0; i < row.getColumnCount(); i++) {
//			System.out.println(row.getValue(i));
//		}
		try {
			model.addRealRow(session, row);
		} catch (Exception e) {
			throw DbException.convert(e);
			//throw new SQLException("Error adding data to cube", e);
		}
		needsFlush = true;
	}
	
	/**
	 * Commit gets called once for each row that is inserted. For performance
	 * reasons we want to buffer up rows and only commit when needed.
	 * 
	 * We can track the number of rows added and expect that many number of
	 * commits to come in, it seems. In any case, we could track if
	 * there were any changes since the last commit and only do something
	 * with Essbase if necessary.
	 */
	@Override
	public void commit(short operation, Row row) {
		if (needsFlush) {
			System.out.println("Needed flush; flushing");
			model.endDataLoad();
			needsFlush = false;
		}
		//System.out.println("Commit, operation: " + operation + " : " + row);
		// TODO Auto-generated method stub
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
		System.out.println("EssTable removeRow(..): " + row);
		// TODO Auto-generated method stub

	}

	@Override
	public void truncate(Session session) {
		// TODO Auto-generated method stub
	}

	@Override
	public void addRow(Session session, Row row) {
		System.out.println("Add row to magic table");
		getScanIndex(session).add(session, row);
	}

	@Override
	public void checkSupportAlter() {
		// TODO Auto-generated method stub

	}

	@Override
	public String getTableType() {
		System.out.println("Ask table type");
		return TABLE_TYPE;
	}

	@Override
	public Index getScanIndex(Session session) {
		System.out.println("Asked for scan index");
		return index;
	}

	/** 
	 * Copied from LinkTable
	 * @return the unique index if any, null otherwise
	 */
    @Override
    public Index getUniqueIndex() {
    	System.out.println("Asked for unique index");
        for (Index idx : indexes) {
            if (idx.getIndexType().isUnique()) {
                return idx;
            }
        }
        return null;
    }
    
	@Override
	public ArrayList<Index> getIndexes() {
		System.out.println("EssTable.getIndexes()");
		return indexes;
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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canGetRowCount() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canDrop() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public long getRowCount(Session session) {
		// TODO Auto-generated method stub
		return 10;
	}

	@Override
	public long getRowCountApproximation() {
		// TODO Auto-generated method stub
		return 20;
	}

	@Override
	public long getDiskSpaceUsed() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getCreateSQL() {
		return "Fake SQL";
	}

	@Override
	public String getDropSQL() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void checkRename() {
		// TODO Auto-generated method stub
	}

	public static DbException wrapException(String sql, Exception ex) {
        SQLException e = DbException.toSQLException(ex);
        return DbException.get(ErrorCode.GENERAL_ERROR_1,
                e, sql, e.toString());
	}
	
}

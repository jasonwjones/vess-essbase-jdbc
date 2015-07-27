package com.saxifrages.vess.h2;

import java.util.ArrayList;

import org.h2.engine.Session;
import org.h2.index.Index;
import org.h2.index.IndexType;
import org.h2.result.Row;
import org.h2.schema.Schema;
import org.h2.table.IndexColumn;
import org.h2.table.Table;

public abstract class AbstractEssTable extends Table {

	public AbstractEssTable(Schema schema, int id, String name, boolean persistIndexes, boolean persistData) {
		super(schema, id, name, persistIndexes, persistData);
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Index getScanIndex(Session session) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Index getUniqueIndex() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<Index> getIndexes() {
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub
		return null;
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

}

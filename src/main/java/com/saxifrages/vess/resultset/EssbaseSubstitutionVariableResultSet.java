package com.saxifrages.vess.resultset;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class EssbaseSubstitutionVariableResultSet extends VessAbstractResultSet {
	
	/**
	 * Comes in as variable, value, app, cube
	 * @param variables
	 */
	public EssbaseSubstitutionVariableResultSet(String[][] variables) {
		
	}
		
	@Override
	public ResultSetMetaData getMetaData() throws SQLException {
		// TODO Auto-generated method stub
		
		// should determine values for the impl subclass here:
		// CatalogName -- App
		// SchemaName --  DB
		// TableName -- 
		return super.getMetaData();
	}
	
	public boolean next() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

}

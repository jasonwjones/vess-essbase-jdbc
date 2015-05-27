package com.saxifrages.vess.resultset;


public class EssbaseSubstitutionVariableResultSetMetaData extends AbstractResultSetMetaData {

	private static final MetaDataColumn[] columns;
	
	static {
		columns = new MetaDataColumn[4];
	}
	
	 // Comes in as variable, value, app, cube

	public int getColumnCount() {
		return 4;
	}
	
	
	
}

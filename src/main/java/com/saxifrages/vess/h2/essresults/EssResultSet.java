package com.saxifrages.vess.h2.essresults;

import org.h2.result.Row;
import org.h2.value.Value;

public interface EssResultSet {

	public boolean next();
	
	public int getColumnCount();

	public Row getRow();
	
	public Value getValue(int column);   
	
}

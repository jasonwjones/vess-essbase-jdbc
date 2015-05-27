package com.saxifrages.vess.h2.models;

import org.h2.table.Column;
import org.h2.value.Value;

public class ColConstants {

	public static final Column APPLICATION;
	
	public static final Column CUBE;
	
	public static final Column ALIAS_TABLE_NAME;
	
	public static final Column DIMENSION_NAME;
	
	static {
		APPLICATION = new Column("APPLICATION", Value.STRING, 30, -1, 10);
		CUBE = new Column("CUBE", Value.STRING, 30, -1, 10);
		ALIAS_TABLE_NAME = new Column("ALIAS_TABLE_NAME", Value.STRING, 30, -1, 10);
		DIMENSION_NAME = new Column("DIMENSION_NAME", Value.STRING, 30, -1, 20);
	}
	
}

package com.saxifrages.vess.h2;

import org.h2.schema.Schema;

public class EssTableFactory {

	private int startId;
	
	public EssTableFactory(int startId) {
		this.startId = startId;
	}
	
	public EssTable createTable(EssModel model, String tableName, Schema schema) {
		EssTable table = new EssTable(schema, startId++, tableName, model);
		return table;
	}
	
}

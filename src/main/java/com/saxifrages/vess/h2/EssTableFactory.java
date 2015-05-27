package com.saxifrages.vess.h2;

import org.h2.schema.Schema;

public class EssTableFactory {

	private Schema schema;
	private int startId;
	
	public EssTableFactory(Schema schema, int startId) {
		this.schema = schema;
		this.startId = startId;
	}
	
	public EssTable createTable(EssModel model, String tableName) {
		EssTable table = new EssTable(schema, startId++, tableName, model);
		return table;
	}
	
}

package com.saxifrages.vess.h2vess;

import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.schema.SchemaObject;
import org.h2.schema.SchemaObjectBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseObjectAdder {

	private static final Logger logger = LoggerFactory.getLogger(DatabaseObjectAdder.class);
	
	private Database database;

	private Session session;
	
	public DatabaseObjectAdder(Database database, Session session) {
		this.database = database;
		this.session = session;
	}
	
	public void addDatabaseObject(SchemaObjectBase object) {
		for (SchemaObject schemaObject : database.getAllSchemaObjects()) {
			if (schemaObject.getName().equals(object.getName())) {
				logger.warn("Schema already contains object named {}, skipping", object.getName());
				break;
			}
		}
		database.addSchemaObject(session, object);
	}

	public void addSchemaObject(SchemaObjectBase object) {
		for (SchemaObject schemaObject : database.getAllSchemaObjects()) {
			if (schemaObject.getName().equals(object.getName())) {
				logger.warn("Schema already contains object named {}, skipping", object.getName());
				break;
			}
		}
		database.addSchemaObject(session, object);
	}
	
}

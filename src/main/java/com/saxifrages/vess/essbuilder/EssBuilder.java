package com.saxifrages.vess.essbuilder;

import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.schema.Schema;

import com.saxifrages.vess.h2.EssService;
import com.saxifrages.vess.h2.EssTable;
import com.saxifrages.vess.h2.EssTableFactory;
import com.saxifrages.vess.h2.models.cube.EssCubeSubstitutionVariableModel;

public class EssBuilder {

	private EssService service;
	private Session session;
	private EssTableFactory tableFactory;
	private Database database;
	
	public EssBuilder(EssService service, Session session, EssTableFactory tableFactory, Database database) {
		this.service = service;
		this.session = session;
		this.tableFactory = tableFactory;
		this.database = database;
	}

	/**
	 * Builds a schema for a particular Essbase cube. Schemas for cubes have a
	 * certain set of tables and may vary based on the cube being ASO or BSO.
	 * 
	 * These tables include the following cube (app)-specific tables:
	 * 
	 * VARS
	 * SESSIONS (?)
	 * CUBES (?)
	 * REQUESTS (?)
	 * _DATA
	 * _REJECTS
	 * DIMS
	 * 
	 * SAMPLE
	 *   SAMPLE_BASIC_DATA
	 *   SAMPLE_CUBES
	 * 
	 * @param application
	 * @return
	 */
	public void buildApplicationSchema(Schema schema, String application) {
		System.out.println("Building out schema for " + application);
		
		EssCubeSubstitutionVariableModel variables = new EssCubeSubstitutionVariableModel(service, application, "Basic");
		EssTable variableTable = tableFactory.createTable(variables, "VARS", schema);
		database.addSchemaObject(session, variableTable);
	}

}

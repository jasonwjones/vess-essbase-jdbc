package com.saxifrages.vess.essbuilder;

import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.schema.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.essbase.api.base.EssException;
import com.saxifrages.vess.h2.EssService;
import com.saxifrages.vess.h2.EssTable;
import com.saxifrages.vess.h2.EssTableFactory;
import com.saxifrages.vess.h2.models.EssGenericPropsModel;
import com.saxifrages.vess.h2.models.app.EssAppPropsModel;
import com.saxifrages.vess.h2.models.cube.EssCubeSubstitutionVariableModel;
import com.saxifrages.vess.h2.models.cube.outline.EssCubeDimensionModel;

public class EssBuilder {

	private static final Logger logger = LoggerFactory.getLogger(EssBuilder.class);

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

	public void buildApplicationSchemas() throws EssException {

	}

	/**
	 * Builds a schema for a particular Essbase cube. Schemas for cubes have a
	 * certain set of tables and may vary based on the cube being ASO or BSO.
	 * 
	 * These tables include the following cube (app)-specific tables:
	 * 
	 * VARS SESSIONS (?) CUBES (?) REQUESTS (?) _DATA _REJECTS DIMS
	 * 
	 * SAMPLE SAMPLE_BASIC_DATA SAMPLE_CUBES
	 * 
	 * @param application
	 * @return
	 */
	public void buildApplicationSchema(Schema schema, String application) throws EssException {
		logger.info("Building application schema for {}", application);
		
//		EssAppPropsModel appModel = new EssAppPropsModel(service, application);
//		EssTable appTable = tableFactory.createTable(appModel, "PROPS", schema);
//		database.addSchemaObject(session, appTable);

		EssGenericPropsModel genericProps = new EssGenericPropsModel(service, service.getApplication(application));
		EssTable genericPropsTable = tableFactory.createTable(genericProps, "PROPS", schema);
		database.addSchemaObject(session, genericPropsTable);

		
		for (String cube : service.getCubes(application)) {
			// need to have a way to build a lazy cube schema
			buildCubeSchema(schema, application, cube.toUpperCase());
		}

	}

	/**
	 * Builds a schema for the given cube. A different schema will be built
	 * depending on whether the cube is ASO or BSO. For example, some member attributes are not
	 * applicable to ASO, such as TwoPassCalc, so its dimension table does not have this column
	 * 
	 * @param schema
	 * @param application
	 * @param cube
	 * @throws EssException
	 */
	public void buildCubeSchema(Schema schema, String application, String cube) throws EssException {
		logger.info("Building cube schema for {}.{}", application, cube);

		EssCubeSubstitutionVariableModel variables = new EssCubeSubstitutionVariableModel(service, application, cube);
		EssTable variableTable = tableFactory.createTable(variables, cube + "_VARS", schema);
		database.addSchemaObject(session, variableTable);

		EssGenericPropsModel genericProps = new EssGenericPropsModel(service, service.getApplication(application).getCube(cube));
		EssTable genericPropsTable = tableFactory.createTable(genericProps, cube + "_PROPS", schema);
		database.addSchemaObject(session, genericPropsTable);
		
		for (String dimension : service.getDimensions(application, cube)) {
			EssCubeDimensionModel dimModel = new EssCubeDimensionModel(service, application, cube, dimension);
			String tableName = cube + "_DIM_" + dimension.toUpperCase();
			EssTable dimTable = tableFactory.createTable(dimModel, tableName, schema);
			database.addSchemaObject(session, dimTable);
		}

	}

	public void buildBsoCubeSchema() {
	}

	public void buildAsoCubeSchema() {
	}

}

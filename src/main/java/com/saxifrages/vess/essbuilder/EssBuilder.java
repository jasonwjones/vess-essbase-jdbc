package com.saxifrages.vess.essbuilder;

import java.util.Arrays;
import java.util.List;

import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.schema.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.essbase.api.base.EssException;
import com.jasonwjones.jacombi.CartesianIndex;
import com.saxifrages.vess.h2.EssModel;
import com.saxifrages.vess.h2.EssService;
import com.saxifrages.vess.h2.EssTable;
import com.saxifrages.vess.h2.EssTableFactory;
import com.saxifrages.vess.h2.datatable.EssDataTable;
import com.saxifrages.vess.h2.datatable.EssDataTableDefinition;
import com.saxifrages.vess.h2.models.EssCubeAliasTableNamesModel;
import com.saxifrages.vess.h2.models.EssCubeInfoModel;
import com.saxifrages.vess.h2.models.EssGenericPropsModel;
import com.saxifrages.vess.h2.models.EssOlapRequestModel;
import com.saxifrages.vess.h2.models.EssServerAppsModel;
import com.saxifrages.vess.h2.models.EssServerUsersModel;
import com.saxifrages.vess.h2.models.cube.EssCubeSubstitutionVariableModel;
import com.saxifrages.vess.h2.models.cube.outline.EssCubeDimensionModel;
import com.saxifrages.vess.h2.models.server.EssServerCustomFunctions;
import com.saxifrages.vess.h2.models.server.EssServerInfoModel;
import com.saxifrages.vess.h2.models.server.EssSubstitutionVariableModel;

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

	public void buildServerSchema(Schema schema) throws EssException {
		if (database.findSchema(schema.getName()) == null) {
			database.addDatabaseObject(session, schema);

			EssSubstitutionVariableModel variableModel = new EssSubstitutionVariableModel();
			variableModel.setService(service);
			EssTable variableTable = tableFactory.createTable(variableModel, "VARS", schema);
			database.addSchemaObject(session, variableTable);

			EssOlapRequestModel requestModel = new EssOlapRequestModel(service);
			EssTable requestTable = tableFactory.createTable(requestModel, "REQUESTS", schema);
			database.addSchemaObject(session, requestTable);

			EssServerAppsModel appsModel = new EssServerAppsModel(service);
			EssTable appsTable = tableFactory.createTable(appsModel, "APPLICATIONS", schema);
			database.addSchemaObject(session, appsTable);

			EssServerUsersModel usersModel = new EssServerUsersModel(service);
			EssTable usersTable = tableFactory.createTable(usersModel, "USERS", schema);
			database.addSchemaObject(session, usersTable);
			
			EssCubeInfoModel cubeInfoModel = new EssCubeInfoModel(service);
			EssTable cubeInfoTable = tableFactory.createTable(cubeInfoModel, "CUBES", schema);
			database.addSchemaObject(session, cubeInfoTable);

			EssServerInfoModel serverInfoModel = new EssServerInfoModel(service);
			EssTable serverInfoTable = tableFactory.createTable(serverInfoModel, "SERVER", schema);
			database.addSchemaObject(session, serverInfoTable);

			EssServerCustomFunctions functions = new EssServerCustomFunctions(service);
			EssTable serverFunctionsTable = tableFactory.createTable(functions, "CUSTOM_FUNCTIONS", schema);
			database.addSchemaObject(session, serverFunctionsTable);
		}
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
			buildCubeSchema(schema, application, cube);
		}
		
		List<String> scenario;
		List<String> time;
		List<String> years;
		
		scenario = Arrays.asList("Actual", "Budget");
		time = Arrays.asList("Jan", "Feb", "Mar");
		years = Arrays.asList("FY15", "FY16");
		
		List<List<String>> lists = Arrays.asList(scenario, time, years);
		//CartesianIndex<String> ci = new CartesianIndex<String>(lists);

		List<String> dimNames = Arrays.asList("Scenario", "Time", "Fiscal_Year");
		
		EssDataTableDefinition def = new EssDataTableDefinition();
		def.setMembers(lists);
		def.setDimensionNames(dimNames);
		
		System.out.println("Creating " + application + "_DATA_TEST");
		EssDataTable dataTable = new EssDataTable(schema, 20000, application.toUpperCase() + "_DATA_TEST", def);
		database.addSchemaObject(session, dataTable);

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
		logger.info("Building cube schema for {}/{}", application, cube);

		String cubeUpper = cube.toUpperCase();
		
		EssCubeSubstitutionVariableModel variables = new EssCubeSubstitutionVariableModel(service, application, cube);
		EssTable variableTable = tableFactory.createTable(variables, cubeUpper + "_VARS", schema);
		database.addSchemaObject(session, variableTable);

		EssGenericPropsModel genericProps = new EssGenericPropsModel(service, service.getApplication(application).getCube(cube));
		EssTable genericPropsTable = tableFactory.createTable(genericProps, cubeUpper + "_PROPS", schema);
		database.addSchemaObject(session, genericPropsTable);
		
		for (String dimension : service.getDimensions(application, cube)) {
			EssCubeDimensionModel dimModel = new EssCubeDimensionModel(service, application, cube, dimension);
			String tableName = cubeUpper + "_DIM_" + dimension.toUpperCase();
			EssTable dimTable = tableFactory.createTable(dimModel, tableName, schema);
			database.addSchemaObject(session, dimTable);
		}
		
//		EssTempCubeDataModel dataModel = new EssTempCubeDataModel(service, application, cubeUpper);
//		EssTable dataTable = tableFactory.createTable(dataModel, cubeUpper + "_DATA", schema);
//		database.addSchemaObject(session, dataTable);
		
//		EssTempCubeDataModel2 dataModel = new EssTempCubeDataModel2(service, application, cubeUpper);
//		EssTable dataTable = tableFactory.createTable(dataModel, cubeUpper + "_DATAX", schema);
//		database.addSchemaObject(session, dataTable);

//		logger.info("XXXX looking for {}", schema.getName());
		
		// none of this runs right now because the schema exists...
		// Furthermore, this ISN'T CUBE LEVEL STUFF WHY IS IT HERE???
		if (database.findSchema(schema.getName()) == null) {
			logger.info("YYYY");
			database.addDatabaseObject(session, schema);

			EssCubeAliasTableNamesModel aliasTableModel = new EssCubeAliasTableNamesModel(service, application, cube);
			EssTable aliasTableTable = tableFactory.createTable(aliasTableModel, application + "_" + cube + "_ALIAS_TABLES",
					schema);
			database.addSchemaObject(session, aliasTableTable);

			EssOlapRequestModel requestModel = new EssOlapRequestModel(service);
			EssTable requestTable = tableFactory.createTable(requestModel, "REQUESTS", schema);
			database.addSchemaObject(session, requestTable);

			EssServerAppsModel appsModel = new EssServerAppsModel(service);
			EssTable appsTable = tableFactory.createTable(appsModel, "APPLICATIONS", schema);
			database.addSchemaObject(session, appsTable);

			EssServerUsersModel usersModel = new EssServerUsersModel(service);
			EssTable usersTable = tableFactory.createTable(usersModel, "USERS", schema);
			database.addSchemaObject(session, usersTable);

			EssCubeInfoModel cubeInfoModel = new EssCubeInfoModel(service);
			EssTable cubeInfoTable = tableFactory.createTable(cubeInfoModel, "CUBES", schema);
			database.addSchemaObject(session, cubeInfoTable);

			EssServerInfoModel serverInfoModel = new EssServerInfoModel(service);
			EssTable serverInfoTable = tableFactory.createTable(serverInfoModel, "SERVER", schema);
			database.addSchemaObject(session, serverInfoTable);

			EssServerCustomFunctions functions = new EssServerCustomFunctions(service);
			EssTable serverFunctionsTable = tableFactory.createTable(functions, "CUSTOM_FUNCTIONS", schema);
			database.addSchemaObject(session, serverFunctionsTable);

			String appDb = application.toUpperCase() + "_" + cube + "_DATA";
			
			EssModel dataLoadModel = service.createCubeModel(application, cube);
			EssTable sampleFact = tableFactory.createTable(dataLoadModel, appDb, schema);
			database.addSchemaObject(session, sampleFact);
		}
		
	}

}

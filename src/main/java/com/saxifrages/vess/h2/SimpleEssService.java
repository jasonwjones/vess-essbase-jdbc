package com.saxifrages.vess.h2;

import java.util.ArrayList;
import java.util.List;

import org.h2.table.Column;

import com.essbase.api.base.EssException;
import com.essbase.api.base.IEssIterator;
import com.essbase.api.datasource.IEssCube;
import com.essbase.api.datasource.IEssOlapApplication;
import com.essbase.api.datasource.IEssOlapServer;
import com.essbase.api.metadata.IEssCubeOutline;
import com.essbase.api.metadata.IEssDimension;
import com.essbase.api.metadata.IEssDimension.EEssDimensionCategory;
import com.essbase.api.session.IEssbase;
import com.saxifrages.essbase.util.ConversionDelegate;
import com.saxifrages.essbase.util.IteratorUtil;

public class SimpleEssService implements EssService {

	private IEssOlapServer server;

	private static ConversionDelegate<IEssOlapApplication, String> APP_TO_NAME = new AppToName();

	private static ConversionDelegate<IEssCube, String> CUBE_NAME = new CubeNames();

	private static ConversionDelegate<IEssDimension, String> DIM_NAMES = new DimNames();

	public SimpleEssService(IEssOlapServer server) {
		this.server = server;
	}

	@Override
	public void addSubstitutionVariable(String name, String value) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addSubstitutionVariable(String application, String database, String name, String value) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<String[]> getSubstitutionVariables() throws EssException {
		System.out.println("Getting sub vars from server");
		List<String[]> subVars = new ArrayList<String[]>();
		String[][] allVars = server.getSubstitutionVariables();
		System.out.println("Got " + allVars.length + " vars back");
		for (String[] var : allVars) {
			subVars.add(var);
		}
		return subVars;
	}

	@Override
	public void deleteVariable(String name) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<String> getApplications() throws EssException {
		IEssIterator appIterator = server.getApplications();
		return IteratorUtil.iteratorToList(appIterator, APP_TO_NAME);
	}

	@Override
	public List<String> getCubes(String application) throws EssException {
		IEssOlapApplication app = server.getApplication(application);
		IEssIterator cubeIterator = app.getCubes();
		return IteratorUtil.iteratorToList(cubeIterator, CUBE_NAME);
	}

	@Override
	public List<String> getDimensions(String application, String cubeName) throws EssException {
		IEssOlapApplication app = server.getApplication(application);
		IEssCube cube = app.getCube(cubeName);
		IEssIterator dimIterator = cube.getDimensions();
		return IteratorUtil.iteratorToList(dimIterator, DIM_NAMES);
	}

	@Override
	public EssModel createCubeModel(String application, String database) throws EssException {
		List<String> dimensionNames = getStoredDimensions(application, database);
		ColumnBuilder builder = new ColumnBuilder();
		for (String dimName : dimensionNames) {
			builder
					.addString(dimName, EssService.MAX_MEMBER_NAME_LENGTH)
					.notNullable();
		}
		Column[] columns = builder
				.addDouble("DATA")
				.build();
		return new EssDataLoadModel(columns, this, application, database);
	}

	/**
	 * Creates a rejected record model for a given cube (set of dimensionality).
	 * The format of the table is as follows:
	 * 
	 * dimension names, fact, rejection reason (varchar)
	 * 
	 * @param dimensionNames
	 * @return
	 */
	public EssModel createCubeRejectedDataModel(List<String> dimensionNames) {

		return null;
	}

	@Override
	public EssModel createDimensionModel(String application, String database) {
		// TODO Auto-generated method stub
		return null;
	}

	private List<String> getStoredDimensions(String app, String cube) throws EssException {
		IEssCubeOutline outline = server.getApplication(app).getCube(cube).openOutline();
		IEssIterator dimensionIterator = outline.getDimensions();

		List<String> storedDimNames = new ArrayList<String>();

		for (int i = 0; i < dimensionIterator.getCount(); i++) {
			IEssDimension dimension = (IEssDimension) dimensionIterator.getAt(i);

			if (dimension.getCategory() != EEssDimensionCategory.ATTRIBUTE
					&& dimension.getCategory() != EEssDimensionCategory.ATTRIBUTE_CALC) {
				storedDimNames.add(dimension.getName());
			}
		}
		return storedDimNames;
	}

	public IEssCube getCube(String application, String cube) throws EssException {
		return server.getApplication(application).getCube(cube);
	}

	private static class AppToName implements ConversionDelegate<IEssOlapApplication, String> {
		@Override
		public String convert(IEssOlapApplication from) throws EssException {
			return from.getName();
		}
	}

	private static class CubeNames implements ConversionDelegate<IEssCube, String> {
		@Override
		public String convert(IEssCube from) throws EssException {
			return from.getName();
		}
	}

	private static class DimNames implements ConversionDelegate<IEssDimension, String> {
		@Override
		public String convert(IEssDimension dimension) throws EssException {
			// if (dimension.getCategory() != EEssDimensionCategory.ATTRIBUTE
			// && dimension.getCategory() !=
			// EEssDimensionCategory.ATTRIBUTE_CALC) {
			return dimension.getName();
			// } else {
			// return null;
			// }
		}
	}

	public static void main(String[] args) throws EssException {
		IEssbase essbase = IEssbase.Home.create(IEssbase.JAPI_VERSION);
		IEssOlapServer server = essbase.signOn("admin", "Way2GoGuys", false, null, "embedded", "192.168.1.231");
		SimpleEssService service = new SimpleEssService(server);

		String[][] vars = server.getSubstitutionVariables();
		System.out.println("Vars: " + vars.length);

		List<String> apps = service.getApplications();
		for (String app : apps) {
			System.out.println("App: " + app);
		}

		for (String app : apps) {
			List<String> cubes = service.getCubes(app);
			for (String cube : cubes) {
				System.out.println("Cube: " + cube);
			}
		}

	}

	@Override
	public IEssOlapServer getServer() throws EssException {
		return server;
	}

}

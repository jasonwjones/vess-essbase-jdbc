package com.saxifrages.vess.h2;

import java.util.ArrayList;
import java.util.List;

import org.h2.table.Column;

import com.essbase.api.base.EssException;
import com.essbase.api.datasource.IEssCube;
import com.essbase.api.datasource.IEssOlapServer;
import com.saxifrages.vess.h2.models.data.EssDataLoadModel;

public class TempEssService implements EssService {

	private List<String[]> variables; 
	
	public TempEssService() {
		variables = new ArrayList<>();
	}
	
	@Override
	public void addSubstitutionVariable(String name, String value) {
		//JSubstitutionVariable variable = new JSubstitutionVariable(name, value);
		//System.out.println("Addin substitution variable: " + variable);
		//variables.add(variable);
	}

	@Override
	public void addSubstitutionVariable(String application, String database, String name, String value) {
		//JSubstitutionVariable variable = new JSubstitutionVariable(application, database, name, value);
		//System.out.println("Addin substitution variable: " + variable);
		//variables.add(variable);
	}
	
	@Override
	public List<String[]> getSubstitutionVariables() {
		return variables;
	}

	@Override
	public void deleteVariable(String name) {
		System.out.println("Delete variable: " + name);
	}
	
	@Override
	public EssModel createCubeModel(String application, String database) throws EssException {
		ColumnBuilder builder = new ColumnBuilder();
		Column[] columns = builder
			.addString("Time")
			.addString("Year")
			.addString("Product")
			.addDouble("Data")
			.build();
		
		return new EssDataLoadModel(columns, this, application, database);
	}

	@Override
	public EssModel createDimensionModel(String application, String database) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getApplications() throws EssException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getCubes(String application) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getDimensions(String application, String cube) throws EssException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public IEssCube getCube(String application, String cube) throws EssException {
		return null;
	}

	@Override
	public IEssOlapServer getServer() throws EssException {
		throw new RuntimeException("getServer() unimplemented");
	}
	
}

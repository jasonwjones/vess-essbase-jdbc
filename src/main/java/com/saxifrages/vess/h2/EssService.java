package com.saxifrages.vess.h2;

import java.util.List;

import com.essbase.api.base.EssException;
import com.essbase.api.datasource.IEssCube;
import com.essbase.api.datasource.IEssOlapServer;

public interface EssService {

	public static final int MAX_MEMBER_NAME_LENGTH = 80;
	
	public void addSubstitutionVariable(String name, String value);

	public void addSubstitutionVariable(String name, String value, String application, String database);

	public List<String[]> getSubstitutionVariables() throws EssException;

	public void deleteVariable(String name);

	public List<String> getApplications() throws EssException;
	
	public List<String> getCubes(String application) throws EssException;
	
	public List<String> getDimensions(String application, String cube) throws EssException;
	
	public IEssCube getCube(String application, String cube) throws EssException;
	
	public IEssOlapServer getServer() throws EssException;
	
	//public void loadData()
	
	/**
	 * Dynamically create a data model of an Essbase cube.
	 * 
	 * @param application
	 * @param database
	 * @return
	 */
	public EssModel createCubeModel(String application, String database) throws EssException;

	/**
	 * Create a model for a particular dimension of a cube.
	 * 
	 * @param application
	 * @param database
	 * @return
	 */
	public EssModel createDimensionModel(String application, String database);

}

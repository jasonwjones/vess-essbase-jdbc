package com.saxifrages.vess.core;

import java.util.List;

public interface VessEss {

	public List<String> getApplications();
	
	public List<String> getDatabasesInApplication(String application);
	
	public List<SubstitutionVariable> getSubstitutionVariables();
	
	public List<SubstitutionVariable> getSubstitutionVariables(String application, String datbase);
	
}

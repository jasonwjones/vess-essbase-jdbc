package com.saxifrages.vess.h2.datatable;

import java.util.List;

public class EssDataTableDefinition {

	private List<String> dimensionNames;

	private List<List<String>> members;

	public EssDataTableDefinition() {}

	public void setMembers(List<List<String>> members) {
		this.members = members;
	}

	public List<List<String>> getMembers() {
		return this.members;
	}

	public List<String> getDimensionNames() {
		return dimensionNames;
	}

	public void setDimensionNames(List<String> dimensionNames) {
		this.dimensionNames = dimensionNames;
	}

}

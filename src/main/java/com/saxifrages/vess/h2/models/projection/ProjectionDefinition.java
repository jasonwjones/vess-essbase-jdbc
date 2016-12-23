package com.saxifrages.vess.h2.models.projection;

import java.util.List;
import java.util.Map;

public class ProjectionDefinition {

	private boolean includeMissing;
	
	private Map<String, String> fixedPov;
	
	private String factDimension;
	
	private List<String> facts;
	
	private boolean onlyLeafData;

	public boolean isIncludeMissing() {
		return includeMissing;
	}

	public void setIncludeMissing(boolean includeMissing) {
		this.includeMissing = includeMissing;
	}

	public Map<String, String> getFixedPov() {
		return fixedPov;
	}

	public void setFixedPov(Map<String, String> fixedPov) {
		this.fixedPov = fixedPov;
	}

	public String getFactDimension() {
		return factDimension;
	}

	public void setFactDimension(String factDimension) {
		this.factDimension = factDimension;
	}

	public List<String> getFacts() {
		return facts;
	}

	public void setFacts(List<String> facts) {
		this.facts = facts;
	}

	public boolean isOnlyLeafData() {
		return onlyLeafData;
	}

	public void setOnlyLeafData(boolean onlyLeafData) {
		this.onlyLeafData = onlyLeafData;
	}
	
}

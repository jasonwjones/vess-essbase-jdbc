package com.saxifrages.vess.core;

import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;

public class VessResultSetFetcher {

	private List<String> variableColumns = Arrays.asList("app", "cube", "name", "value");
	// possible tables:
	//     variables
	
	public ResultSet fetch(String[] columnNames, String table) {
		return null;
	}
	
}

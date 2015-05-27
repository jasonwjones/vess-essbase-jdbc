package com.saxifrages.vess.core;

import com.essbase.api.base.EssException;
import com.essbase.api.datasource.IEssOlapServer;

public class Subvars {

	// name, value, app, cube, analytic server (0-4)
	// Limits: app/cube: 8/30 (unicode)
	// server: 29/50
	// subvarname: 320
	// value: 256
	public void variables(IEssOlapServer server) throws EssException {
		String[][] vars = server.getSubstitutionVariables();
	}
	
}

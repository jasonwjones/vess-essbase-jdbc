package com.saxifrages.vess.h2.procs;

import java.sql.SQLException;

import org.h2.engine.FunctionAlias;

import com.essbase.api.base.EssException;
import com.essbase.api.datasource.IEssCube;
import com.essbase.api.datasource.IEssOlapServer;
import com.saxifrages.vess.h2.essconn.EssConnectionManager;

public class VessFuncs {

	public void loadCube(String connUser, String app, String cube, String calcName) {
		FunctionAlias func;
		// new FunctionAlias()
	}

	public static int runCalc(String connUser, String app, String cube, String calcName) throws SQLException {
		try {
			IEssOlapServer server = EssConnectionManager.getInstance().getForUser(connUser);
			IEssCube essCube = server.getApplication(app).getCube(cube);
			essCube.calculate(false, calcName);
			return 1;
		} catch (EssException e) {
			throw new SQLException("Error running calculation", e);
		}
	}

}

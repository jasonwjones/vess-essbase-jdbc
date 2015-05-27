package com.saxifrages.vess.h2.procs;

import java.sql.Connection;
import java.sql.SQLException;

import com.essbase.api.base.EssException;
import com.essbase.api.datasource.IEssCube;
import com.essbase.api.datasource.IEssOlapServer;
import com.saxifrages.vess.h2.essconn.EssConnectionManager;

public class RunCalc {

	public static Boolean runCalc(Connection connection, String calcName) {
		System.out.println("Running calculation!");
		System.out.println("Connection is: " + connection);
		try {
			System.out.println("Props: " + connection.getClientInfo());
		} catch (SQLException e) {
			System.out.println("SQLE: " + e.getMessage());
		}
		return true;
	}
	
	public static Boolean runCalc2(String username, String calcName) {
		try {
			IEssOlapServer server = EssConnectionManager.getInstance().getForUser(username);
			String[] appAndDb = server.getActive();
			IEssCube cube = server.getApplication(appAndDb[0]).getCube(appAndDb[1]);
			cube.calculate(false, calcName);
			return true;
		} catch (EssException e) {
			return false;
		}
	}
	
}

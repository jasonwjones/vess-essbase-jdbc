package com.saxifrages.vess.h2.procs;

import java.sql.Connection;
import java.sql.SQLException;

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
	
}

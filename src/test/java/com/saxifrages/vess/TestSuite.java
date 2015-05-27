package com.saxifrages.vess;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;



public class TestSuite {

	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		String embeddedSampleBasic = "jdbc:essbase:embedded://admin:password@epm11123:1000/Sample.Basic;app=Sample;cube=Basic";
		//Class.forName("com.saxifrages.vess.VessDriver");

		//DriverManager.registerDriver(new VessDriver());
		Connection conn = DriverManager.getConnection(embeddedSampleBasic);
		
		DatabaseMetaData md = conn.getMetaData();
		
		//conn.createState
	}

}

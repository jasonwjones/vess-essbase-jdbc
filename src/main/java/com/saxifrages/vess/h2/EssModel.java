package com.saxifrages.vess.h2;

import java.sql.SQLException;

import org.h2.engine.Session;
import org.h2.result.Row;
import org.h2.table.Column;

import com.saxifrages.vess.h2.essresults.EssResultSet;

public interface EssModel {

	public Column[] getColumns();
	
	public void addRealRow(Session session, Row row) throws SQLException;

	public void endDataLoad();

	/**
	 * Responsible for fetching data from the Essbase server and converting it
	 * into our JDBC ResultSet-like structure.
	 * 
	 * @return EssResultSet created data set
	 * @throws SQLException
	 */
	public EssResultSet get() throws SQLException;

}

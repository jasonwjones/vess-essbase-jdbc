package com.saxifrages.vess.resultset;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class StringResultSetTest {

	private StringResultSet resultSet;
	
	@Before
	public void setUp() throws Exception {
		String[] columnNames = new String[] {"TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "TABLE_TYPE", "REMARKS", "TYPE_CAT",
				"TYPE_SCHEM", "TYPE_NAME", "SELF_REFERENCING_COL_NAME", "REF_GENERATION"};
		int[] columnLengths = new int[] {50, 50, 50, 50, 50, 50, 50, 50, 50, 50};
		
		String[] table1 = new String[]{null, null, "VARIABLES", "VIEW", "Var remarks", null, null, null, null, null}; 
		
		List<String[]> tables = new ArrayList<String[]>();
		tables.add(table1);
		resultSet = new StringResultSet(columnNames, columnLengths, tables);
	}

	@Test
	public void testVarArgConstructor() throws SQLException {
		StringResultSet srs = StringResultSet.create("CAT_NAME", 50, "Foo", "Bar", "Baz");
		
		while (srs.next()) {
			System.out.println("Row" + srs.getString(1));
		}
	}
	
	@Test
	@Ignore
	public void testNext() throws SQLException {
		while (resultSet.next()) {
			System.out.println("Row");
			for (int i = 1; i <= 10; i++) {
				System.out.println("Col: " + resultSet.getString(i));
			}
		}
	}

}

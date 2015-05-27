package com.saxifrages.vess.resultset;

import static org.junit.Assert.fail;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class ResultSetBuilderTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	@Ignore
	public void testBuild() {
		String[] columnNames = new String[] { "FIRST_NAME", "LAST_NAME", "AGE" };
		Class<?>[] columNTypes = new Class<?>[] { String.class, String.class, Integer.class };

		List<Object[]> data = new ArrayList<Object[]>();
		Object[] person1 = new Object[] { "Jason", "Jones", 32 };
		Object[] person2 = new Object[] { "Peter", "Nitschke", 30 };

		data.add(person1);
		data.add(person2);

		ResultSetBuilder builder = new ResultSetBuilder();
		ResultSet resultSet = builder
				.columnNames(columnNames)
				.columnTypes(columNTypes)
				.data(data)
				.build();

		fail("Not yet implemented");
	}

}

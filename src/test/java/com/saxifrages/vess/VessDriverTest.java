package com.saxifrages.vess;

import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class VessDriverTest {

	private String embeddedSampleBasic = "jdbc:essbase:embedded://epm11123:1000/Sample.Basic;app=Sample;cube=Basic";
	
	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testConnect() throws SQLException {
		VessDriver driver = new VessDriver();
		
		Properties props = new Properties();
		props.setProperty("user", "admin");
		props.setProperty("password", "Way2GoGuys");

		// should supply at least user/password as props
		Connection conn = driver.connect(embeddedSampleBasic, props);
		//fail("Not yet implemented");
	}

	@Test
	@Ignore
	public void testAcceptsURL() {
		fail("Not yet implemented");
	}

	@Test
	@Ignore
	public void testGetPropertyInfo() {
		fail("Not yet implemented");
	}

}

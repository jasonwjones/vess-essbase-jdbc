package com.saxifrages.vess.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class EssbaseUrlTest {

	//private static String embeddedSampleBasic = "jdbc:essbase:embedded://admin:password@epm11123:1000/Sample.Basic;app=Sample;cube=Basic";
	private static String embeddedSampleBasic = "jdbc:essbase://192.168.1.231:9000/Sample.Basic;app=Sample;cube=Basic";
	
	private static String test2 = "jdbc:essbase://192.168.1.231:9000/Sample.Basic;app=Sample;cube=Basic";
	private EssbaseUrl url;
	
	// jdbc:essbase:embedded://admin:password@epm1112;app=Sample;cube=Basic
	// jdbc:essbase://user:foo@epm1123:9000/aps/JAPI;app=Sample;cube=Basic
	
	@Before
	public void setUp() throws Exception {
		url = EssbaseUrl.parse(embeddedSampleBasic);
	}

	@Test
	public void testParse() {
		assertTrue(url.isEmbedded());
		assertEquals("epm11123", url.getHostname());
	}
	
	@Test
	public void testParseProperties() {
		Map<String, String> properties = url.getProperties();
	}

	@Test
	public void testAppName() {
		System.out.println(url.getDatabase());
	}
	
}

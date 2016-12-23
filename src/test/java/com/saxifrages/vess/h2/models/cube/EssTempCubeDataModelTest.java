package com.saxifrages.vess.h2.models.cube;

import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class EssTempCubeDataModelTest {

	private EssTempCubeDataModel model;
	
	@Before
	public void setUp() throws Exception {
		model = new EssTempCubeDataModel(null, "Sample", "Basic");
	}

	@Test
	@Ignore
	public void testGetIEssCube() {
		fail("Not yet implemented");
	}

}

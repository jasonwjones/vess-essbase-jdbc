package com.jasonwjones.vess;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jasonwjones.jacombi.CartesianIndex;

public class VessProjectionTest {

	private static final Logger logger = LoggerFactory.getLogger(VessProjectionTest.class);
	
	private List<String> scenario;
	
	private List<String> time;
	
	private List<String> years;
	
	@Before
	public void setUp() throws Exception {
		scenario = Arrays.asList("Actual", "Budget");
		time = Arrays.asList("Jan", "Feb", "Mar");
		years = Arrays.asList("FY15", "FY16");
	}

	@Test
	public void test() {
		List<List<String>> lists = Arrays.asList(scenario, time, years);
		CartesianIndex<String> ci = new CartesianIndex(lists);
		
		logger.info("Count: {}", ci.count());
		for (List<String> combi : ci) {
			logger.info("Combo: {}", combi);
		}
		
		for (long index = 0; index < ci.count(); index++) {
			logger.info("Thign: {}", ci.positions(index));
		}
		
		for (long index = 0; index < ci.count(); index++) {
			logger.info("Product for {}: {}", index, ci.productForIndex(index));
		}
		
		for (long index = 0; index < ci.count(); index++) {
			int[] positions = ci.positionsForIndex(index);
			logger.info("Array pos for index {}: {}", index, positions);
			List<String> product = ci.productForIndex(positions);
			logger.info("Back to product: {}", product);
			logger.info("Product for {}: {}", index, ci.productForIndex(index));
		}
		
		
		
		
	}

}

package com.jasonwjones.vess;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.jasonwjones.jacombi.CartesianIndex;

public class CartesianTestBase {

	protected List<String> scenario;
	
	protected List<String> time;
	
	protected List<String> years;
	
	protected List<List<String>> lists; 
	 
	protected CartesianIndex<String> ci;

	
	@Before
	public void setUp() throws Exception {
		scenario = Arrays.asList("Actual", "Budget");
		time = Arrays.asList("Jan", "Feb", "Mar");
		years = Arrays.asList("FY15", "FY16");
		
		lists = Arrays.asList(scenario, time, years);
		ci = new CartesianIndex<String>(lists);
	}
		
}

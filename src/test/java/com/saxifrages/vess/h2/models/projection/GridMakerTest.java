package com.saxifrages.vess.h2.models.projection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class GridMakerTest {

	private List<String> years;
	
	private List<String> months;
	
	private List<String> states;
	
	@Before
	public void setUp() throws Exception {
		years = Arrays.asList("FY15", "FY16");
		months = Arrays.asList("Jan", "Feb", "Mar");
		states = Arrays.asList("Washington", "Oregon", "California");
	}

	@Test
	@Ignore
	public void testCreateGrid() {
		GridMaker<String> maker = new GridMaker<String>();
		
		
		List<List<String>> left = new ArrayList<List<String>>();
		left.add(years);
		left.add(months);
		
		List<List<String>> top = new ArrayList<List<String>>();
		top.add(states);
		
		Object[][] arr = maker.createGrid(left, top);
		
		for (Object[] row : arr) {
			for (Object item : row) {
				System.out.print(item + "    ");
			}
			System.out.println();
		}
		
		//fail("Not yet implemented");
	}

}

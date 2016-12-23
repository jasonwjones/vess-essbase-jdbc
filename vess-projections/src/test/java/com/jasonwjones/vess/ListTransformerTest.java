package com.jasonwjones.vess;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class ListTransformerTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testFilter() {
		List<String> items = Arrays.asList("Item 1", "Item 2", "Item 3");
		List<Integer> filteredIndices = ListTransformer.filter(items, Arrays.asList("Item 2"));
		assertEquals(1, filteredIndices.get(0).intValue());

		List<Integer> filteredIndices2 = ListTransformer.filter(items, Arrays.asList("Item 2", "Item 3"));
		assertEquals(2, filteredIndices2.get(1).intValue());
	}

}

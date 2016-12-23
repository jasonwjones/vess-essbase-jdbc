package com.jasonwjones.vess;

import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jasonwjones.jacombi.CartesianIndex;

public class ListTrans extends CartesianTestBase {

	private static final Logger logger = LoggerFactory.getLogger(ListTrans.class);
	
	@Test
	public void test() {
		List<List<Integer>> ints = ListTransformer.createIntegerLists(lists);
		
		ints.get(0).remove(1);
		CartesianIndex<Integer> intCi = new CartesianIndex<Integer>(ints);
		
		for (List<Integer> curIntList : intCi) {
			logger.info("Cur list: {}", curIntList);
			logger.info("Original: {}", ci.productForIndex(curIntList));
		}
	}
	
}

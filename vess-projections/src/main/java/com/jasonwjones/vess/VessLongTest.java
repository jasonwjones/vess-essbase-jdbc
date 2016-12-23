package com.jasonwjones.vess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jasonwjones.jacombi.CartesianIndex;

public class VessLongTest {

	private static final Logger logger = LoggerFactory.getLogger(VessLongTest.class);
	
	public static void main(String[] args) {
		
		List<Long> long1 = Arrays.asList(0L, 1L, 2L);
		List<Long> long2 = Arrays.asList(0L, 1L, 2L, 3L);
		List<Long> long3 = Arrays.asList(0L, 1L, 2L, 3L, 4L);
		
		List<List<Long>> longs = new ArrayList<List<Long>>();
		longs.add(long1);
		longs.add(long2);
		longs.add(long3);
		
		CartesianIndex<Long> ci = new CartesianIndex<Long>(longs);
		
		for (List<Long> item : ci) {
			logger.info("Long combo: {}", item);
		}
		
		// TODO Auto-generated method stub

	}

}

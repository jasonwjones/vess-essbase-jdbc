package com.jasonwjones.vess;

import java.util.List;

public class LongIterableFactory {

	public Iterable<Long> create() {
		return null;
	}
	
	public List<Long> createList() {
		return null;
	}
	
	// LongList.with(index1, index2)
	// LongList.withIndices(List<Long>)
	
	public static class LongRange {
		
		private Long start;
		
		private Long end;
		
	}
	
}

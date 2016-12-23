package com.saxifrages.vess.h2.models.projection;

import java.util.List;

public class GridMaker<E> {
	
	public Object[][] createGrid(List<List<E>> left, List<List<E>> top) {
		return null;
		/*
		CartesianIndex<E> leftIndex = new CartesianIndex<E>(left, false);
		CartesianIndex<E> topIndex = new CartesianIndex<E>(top, false);
		
		int totalWidth = left.size() + (int) topIndex.count();
		int totalHeight = top.size() + (int) leftIndex.count();

		Object[][] arr = new Object[totalHeight][totalWidth];
		
		int col = left.size();
		for (List<E> item : topIndex) {
			for (int row = 0; row < item.size(); row++) {
				arr[row][col] = item.get(row);	
			}
			col++;
		}
		
		int row = top.size();
		for (List<E> item : leftIndex) {
			for (int colIndex = 0; colIndex < item.size(); colIndex++) {
				arr[row][colIndex] = item.get(colIndex);
			}
			row++;
		}	
		return arr;
		*/
	}
	
	
}

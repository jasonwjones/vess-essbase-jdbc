package com.jasonwjones.vess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class ListTransformer {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
	public static <E> List<Integer> filter(Iterable<E> items, Collection<E> allowed) {
		List<Integer> allowedIndices = new ArrayList<Integer>();
		
		int index = 0;
		for (E item : items) {
			if (allowed.contains(item)) {
				allowedIndices.add(index);
			}
			index++;
		}
		return allowedIndices;
	}

	public static <E> List<List<Integer>> createIntegerList(List<List<E>> lists) {
		List<List<Integer>> intLists = new ArrayList<List<Integer>>();
		for (List<E> list : lists) {
			intLists.add(new FakeIntList(list.size()));
		}
		return intLists;
	}

	public static <E> List<List<Integer>> createIntegerLists(List<List<E>> lists) {
		List<List<Integer>> intLists = new ArrayList<List<Integer>>();
		for (List<E> list : lists) {
			List<Integer> intList = new ArrayList<Integer>();
			int currentInt = 0;
			for (E item : list) {
				intList.add(currentInt++);
			}
			intLists.add(intList);
		}
		return intLists;
	}
	
	private static class IntListIterator implements Iterator<Integer> {

		private int size;
		
		private int current = -1;
		
		public IntListIterator(int size) {
			this.size = size;
		}
		
		public boolean hasNext() {
			return current < size;
		}

		public Integer next() {
			return ++current;
		}
		
	}
	
	private static class FakeIntList implements List<Integer> {
		
		private FakeIntListData data = new FakeIntListData();

		public FakeIntList(int length) {
			this.data.length = length;
		}

		public int size() {
			return data.length;
		}

		public boolean isEmpty() {
			return data.length > 0;
		}

		public boolean contains(Object o) {
			if (o instanceof Integer) {
				return (Integer) o < data.length;
			}
			return false;
		}

		public Iterator<Integer> iterator() {			
			return new IntListIterator(data.length);
		}

		public Object[] toArray() {
			throw new UnsupportedOperationException("toArray()");
		}

		public <T> T[] toArray(T[] a) {
			throw new UnsupportedOperationException("toArray()");
		}

		public boolean add(Integer e) {
			throw new UnsupportedOperationException("add");
		}

		public boolean remove(Object o) {
			throw new UnsupportedOperationException("remove");
		}

		public boolean containsAll(Collection<?> c) {
			throw new UnsupportedOperationException("containsAll");
		}

		public boolean addAll(Collection<? extends Integer> c) {
			throw new UnsupportedOperationException("addAll");
		}

		public boolean addAll(int index, Collection<? extends Integer> c) {
			throw new UnsupportedOperationException("addAll2");
		}

		public boolean removeAll(Collection<?> c) {
			throw new UnsupportedOperationException("removeAll");
		}

		public boolean retainAll(Collection<?> c) {
			throw new UnsupportedOperationException("retainAll");
		}

		public void clear() {
			throw new UnsupportedOperationException("clear");
		}

		public Integer get(int index) {
			return index;
		}

		public Integer set(int index, Integer element) {
			throw new UnsupportedOperationException("set");
		}

		public void add(int index, Integer element) {
			throw new UnsupportedOperationException("add");
		}

		public Integer remove(int index) {
			throw new UnsupportedOperationException("remove");
		}

		public int indexOf(Object o) {
			throw new UnsupportedOperationException("indexOf");
		}

		public int lastIndexOf(Object o) {
			throw new UnsupportedOperationException("lastIndexOf");
		}

		public ListIterator<Integer> listIterator() {
			// TODO Auto-generated method stub
			return null;
		}

		public ListIterator<Integer> listIterator(int index) {
			// TODO Auto-generated method stub
			return null;
		}

		public List<Integer> subList(int fromIndex, int toIndex) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	
}

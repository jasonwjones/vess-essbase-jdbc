package com.saxifrages.vess.h2.datatable;

import java.util.List;

import org.h2.index.Cursor;
import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.value.Value;
import org.h2.value.ValueLong;
import org.h2.value.ValueNull;
import org.h2.value.ValueString;

import com.jasonwjones.jacombi.CartesianIndex;

public class EssDataTableCursor implements Cursor {

	private CartesianIndex<String> dataCombinations;

	private long currentRow = -1;

	public EssDataTableCursor(CartesianIndex<String> dataCombinations) {
		this.dataCombinations = dataCombinations;
	}

	@Override
	public Row get() {
		int dimensionCount = dataCombinations.getSourceCount();
		List<String> dimensionValues = dataCombinations.productForIndex(currentRow);
		Value[] values = new Value[dimensionCount + 2];
		values[0] = ValueLong.get(currentRow);
		for (int i = 1; i <= dimensionCount; i++) {
			values[i] = ValueString.get(dimensionValues.get(i - 1));
		}
		values[dimensionCount + 1] = ValueNull.INSTANCE;
		return new CartesianRow(values);
	}

	@Override
	public SearchRow getSearchRow() {
		return get();
	}

	@Override
	public boolean next() {
		return ++currentRow < dataCombinations.count();
	}

	/**
	 * Copied from IndexCursor.
	 */
	@Override
	public boolean previous() {
		throw DbException.throwInternalError();
	}

	private class CartesianRow extends Row {
		
		public CartesianRow(Value[] values) {
			super(values, 0);
		}
		
	}
	
}

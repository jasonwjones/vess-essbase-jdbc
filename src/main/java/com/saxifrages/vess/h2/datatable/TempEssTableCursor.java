package com.saxifrages.vess.h2.datatable;

import org.h2.index.Cursor;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.value.Value;
import org.h2.value.ValueLong;
import org.h2.value.ValueString;

import com.saxifrages.vess.h2.StringUtil;

public class TempEssTableCursor implements Cursor {

	private int count = 10;
	
	private int current = -1;
	
	private static Value[] createValues(int index) {
		Value[] var = new Value[4];
		var[0] = ValueLong.get(index);
		//var[0] = ValueString.get(StringUtil.emptyIfNull("Value 1"));
		var[1] = ValueString.get(StringUtil.emptyIfNull("Value 2"));
		var[2] = ValueString.get(StringUtil.emptyIfNull("Value 3"));
		var[3] = ValueString.get(StringUtil.emptyIfNull("Value 4"));
		return var;
	}
	
	@Override
	public Row get() {
		return new EssDataTableRow(createValues(current), 0);
	}

	@Override
	public SearchRow getSearchRow() {
		return get();
	}

	@Override
	public boolean next() {
		return ++current < count;
	}

	@Override
	public boolean previous() {
		return false;
	}

}

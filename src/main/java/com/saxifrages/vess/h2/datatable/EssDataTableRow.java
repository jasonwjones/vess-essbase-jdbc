package com.saxifrages.vess.h2.datatable;

import org.h2.result.Row;
import org.h2.value.Value;

public class EssDataTableRow extends Row {

	public EssDataTableRow(Value[] data, int memory) {
		super(data, memory);
	}

}

package com.saxifrages.vess.tableengine;

import org.h2.api.TableEngine;
import org.h2.command.ddl.CreateTableData;
import org.h2.table.Table;

/**
 * Tables wanting to use this use the fully qualified name to instantiate the
 * table.
 * 
 * @author jasonwjones
 *
 */
public class EssTableEngine implements TableEngine {

	@Override
	public Table createTable(CreateTableData data) {
		// TODO Auto-generated method stub
		return null;
	}

}

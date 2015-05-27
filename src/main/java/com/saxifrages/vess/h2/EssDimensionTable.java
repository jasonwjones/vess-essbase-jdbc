package com.saxifrages.vess.h2;

import org.h2.schema.Schema;

import com.essbase.api.base.EssException;
import com.essbase.api.datasource.IEssCube;

public class EssDimensionTable extends EssTable {

	private IEssCube cube;
	
	public EssDimensionTable(Schema schema, int id, String name, EssModel model) {
		super(schema, id, name, model);
	}
	
	public void foo() throws EssException {
		cube.openMemberSelection("default");
		
	}

}

package com.saxifrages.vess.h2.models;

import com.essbase.api.base.EssException;
import com.essbase.api.dataquery.IEssURLDrillThrough;
import com.essbase.api.datasource.IEssCube;

public class EssCubeDrillthroughModel {

	public void get(IEssCube cube) throws EssException {
		cube.listDrillThroughURLs();
		IEssURLDrillThrough d;
		//d.
		// returns IEssURLDrillThrough[]
	}
	
}

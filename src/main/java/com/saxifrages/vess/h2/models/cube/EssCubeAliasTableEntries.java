package com.saxifrages.vess.h2.models.cube;

import com.essbase.api.base.EssException;
import com.essbase.api.datasource.IEssCube;
import com.saxifrages.vess.h2.EssService;
import com.saxifrages.vess.h2.essresults.EssResultSet;
import com.saxifrages.vess.h2.models.AbstractEssbaseCubeModel;

public class EssCubeAliasTableEntries extends AbstractEssbaseCubeModel {

	public EssCubeAliasTableEntries(EssService service, String application, String cube) {
		super(service, application, cube);
		// TODO Auto-generated constructor stub
	}

	@Override
	public EssResultSet get(IEssCube cube) throws EssException {
		//cube.getAli
		// TODO Auto-generated method stub
		return null;
	}

}

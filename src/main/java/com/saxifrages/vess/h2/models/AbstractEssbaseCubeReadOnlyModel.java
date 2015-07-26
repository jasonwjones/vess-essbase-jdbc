package com.saxifrages.vess.h2.models;

import java.sql.SQLException;

import com.essbase.api.base.EssException;
import com.essbase.api.datasource.IEssCube;
import com.saxifrages.vess.h2.EssService;
import com.saxifrages.vess.h2.essresults.EssResultSet;

public abstract class AbstractEssbaseCubeReadOnlyModel extends AbstractEssbaseCubeModel {

	public AbstractEssbaseCubeReadOnlyModel(EssService service, String application, String cube) {
		super(service, application, cube);
	}
	
}

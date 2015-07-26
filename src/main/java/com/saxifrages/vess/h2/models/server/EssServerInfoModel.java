package com.saxifrages.vess.h2.models.server;

import org.h2.value.Value;
import org.h2.value.ValueBoolean;
import org.h2.value.ValueString;

import com.essbase.api.base.EssException;
import com.essbase.api.datasource.IEssOlapServer;
import com.saxifrages.vess.h2.ColumnBuilder;
import com.saxifrages.vess.h2.EssService;
import com.saxifrages.vess.h2.StringUtil;
import com.saxifrages.vess.h2.essresults.ArrayListEssResultSet;
import com.saxifrages.vess.h2.essresults.EssResultSet;
import com.saxifrages.vess.h2.models.AbstractEssbaseServerModel;

public class EssServerInfoModel extends AbstractEssbaseServerModel {

	public EssServerInfoModel(EssService service) {
		super(service);
		columns = new ColumnBuilder()
			.addString("NAME")
			.addString("LOCALE")
			.addString("DESCRIPTION")
			.addBoolean("UNICODE_ENABLED")
			.build();
	}

	@Override
	protected EssResultSet get(IEssOlapServer server) throws EssException {
		Value[] values = new Value[4];
		values[0] = ValueString.get(StringUtil.emptyIfNull(server.getName()));
		values[1] = ValueString.get(StringUtil.emptyIfNull(server.getLocale()));
		values[2] = ValueString.get(StringUtil.emptyIfNull(server.getDescription()));
		values[3] = ValueBoolean.get(server.isUnicodeEnabled());
		return new ArrayListEssResultSet(values);
	}

}

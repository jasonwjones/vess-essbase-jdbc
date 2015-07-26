package com.saxifrages.vess.h2.models.server;

import org.h2.value.Value;
import org.h2.value.ValueString;

import com.essbase.api.base.EssException;
import com.essbase.api.datasource.IEssOlapServer;
import com.saxifrages.vess.h2.ColumnBuilder;
import com.saxifrages.vess.h2.EssService;
import com.saxifrages.vess.h2.StringUtil;
import com.saxifrages.vess.h2.essresults.EssResultSet;
import com.saxifrages.vess.h2.models.AbstractEssbaseServerModel;

public class EssServerCustomFunctions extends AbstractEssbaseServerModel {

	public EssServerCustomFunctions(EssService service) {
		super(service);
		columns = new ColumnBuilder()
			.addString("NAME")
			.addString("SPECIFICATION")
			.addString("COMMENT")
			.addString("TYPE")
			.addString("SCOPE")
			.build();
	}

	@Override
	protected EssResultSet get(IEssOlapServer server) throws EssException {
		return mapper(server.getCDM_CDF(), new CdfMapper());
	}

	public static class CdfMapper implements StringArrayMapper {

		/**
		 * Result [x][0] represents the Name Result [x][1] represents the
		 * Specification Result [x][2] represents the Comment Result [x][3]
		 * represents the Type either a value "cdf" or "cdm" Result [x][4]
		 * represents the Scope which is will be "global"
		 */
		@Override
		public void convert(String[] row, Value[] values) throws EssException {
			values[0] = ValueString.get(StringUtil.emptyIfNull(row[0]));
			values[1] = ValueString.get(StringUtil.emptyIfNull(row[1]));
			values[2] = ValueString.get(StringUtil.emptyIfNull(row[2]));
			values[3] = ValueString.get(StringUtil.emptyIfNull(row[3]));
			values[4] = ValueString.get(StringUtil.emptyIfNull(row[4]));
		}

	}

}

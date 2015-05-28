package com.saxifrages.vess.h2.models;

import org.h2.value.Value;
import org.h2.value.ValueLong;
import org.h2.value.ValueString;

import com.essbase.api.base.EssException;
import com.essbase.api.datasource.IEssOlapServer;
import com.essbase.api.datasource.IEssOlapUser;
import com.saxifrages.vess.h2.ColumnBuilder;
import com.saxifrages.vess.h2.EssService;
import com.saxifrages.vess.h2.essresults.EssResultSet;

public class EssServerUsersModel extends AbstractEssbaseServerModel {

	private final static UserMapper USER_MAPPER = new UserMapper();

	public EssServerUsersModel(EssService service) {
		super(service);
		columns = new ColumnBuilder()
				.addString("USER_NAME")
				.key()
				.notNullable()
				.addLong("LAST_LOGIN_DATE")
				.build();
	}

	@Override
	protected EssResultSet get(IEssOlapServer server) throws EssException {
		return mapper(server.getOlapUsers(null, null, null, false), USER_MAPPER);
	}

	public static class UserMapper implements IterMapper<IEssOlapUser> {
		@Override
		public void convert(IEssOlapUser user, Value[] values) throws EssException {
			values[0] = ValueString.get(user.getName());
			values[1] = ValueLong.get(user.getLastLoginDate());
		}
	}

}

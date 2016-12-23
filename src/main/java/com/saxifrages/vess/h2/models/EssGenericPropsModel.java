package com.saxifrages.vess.h2.models;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.h2.result.Row;
import org.h2.value.Value;
import org.h2.value.ValueString;

import com.essbase.api.base.EssException;
import com.essbase.api.base.IEssProperties;
import com.saxifrages.vess.h2.ColumnBuilder;
import com.saxifrages.vess.h2.EssService;
import com.saxifrages.vess.h2.StringUtil;
import com.saxifrages.vess.h2.essresults.ArrayListEssResultSet;
import com.saxifrages.vess.h2.essresults.EssResultSet;

/**
 * Generic table model for translating the IEssProperties interface for various
 * objects. Many objects in the Essbase Java API implement this interface, such
 * as apps, cubes, servers, and more.
 * 
 * @author jasonwjones
 *
 */
public class EssGenericPropsModel extends AbstractEssbaseModel {

	private IEssProperties props;

	public EssGenericPropsModel(EssService service, IEssProperties props) {
		super(service);
		this.props = props;
		columns = new ColumnBuilder()
				.addString("PROPERTY_NAME")
				.precision(255)
				.key()
				.addString("PROPERTY_VALUE")
				.precision(255)
				.build();
	}

	@Override
	public void endDataLoad() {
		// TODO Auto-generated method stub
	}

	@Override
	public EssResultSet get() throws SQLException {
		try {
			List<Row> rows = new ArrayList<Row>();

			for (String propName : props.getPropertyNames()) {
				Value[] var = new Value[getColumnCount()];
				var[0] = ValueString.get(propName);
				var[1] = ValueString.get(StringUtil.emptyIfNull(props.getPropertyValueAny(propName).toString()));
				rows.add(new Row(var, 0));
			}

			return new ArrayListEssResultSet(rows, 2);
		} catch (EssException e) {
			throw new SQLException("Error fetching Essbase properties", e);
		}
	}

}

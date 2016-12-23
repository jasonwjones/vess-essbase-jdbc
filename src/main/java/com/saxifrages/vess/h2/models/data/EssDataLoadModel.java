package com.saxifrages.vess.h2.models.data;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.h2.engine.Session;
import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.table.Column;
import org.h2.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.saxifrages.vess.h2.EssModel;
import com.saxifrages.vess.h2.EssService;
import com.saxifrages.vess.h2.EssbaseBufferingRowLoader;
import com.saxifrages.vess.h2.essresults.ArrayListEssResultSet;
import com.saxifrages.vess.h2.essresults.EssResultSet;

public class EssDataLoadModel implements EssModel {

	private static final Logger logger = LoggerFactory.getLogger(EssDataLoadModel.class);
	
	private Column[] columns;
	
	private EssbaseBufferingRowLoader loader;
	
	private EssService service;
	
	private String app;
	
	private String cube;
	
	public EssDataLoadModel(Column[] columns, EssService service, String app, String cube) {
		this.columns = columns;
		this.service = service;
		this.app = app;
		this.cube = cube;
	}
	
	/** We can use essSendString mechanism and buffer up to 32KB, will need
	 * delegate method to close on done with flush.
	 * @param session
	 * @param row
	 */
	public void addRealRow(Session session, Row row) {
		try {
			if (loader == null) {
				loader = new EssbaseBufferingRowLoader(service.getCube(app, cube));
				loader.startProcessingRows();
				System.out.println("Created new buffering row loader");
			}
			List<String> rowStrings = rowToStrings(row);
			// flushes (sends to Essbase) if needed
			loader.processRow(rowStrings);
		} catch (Exception e) {
			throw DbException.convert(e);
		}
	}
	
	private static List<String> rowToStrings(Row row) {
		List<String> members = new ArrayList<String>();
		for (int i = 0; i < row.getColumnCount(); i++) {
			members.add(row.getValue(i).getString());
		}
		return members;
	}
	
	public void endDataLoad() {
		try {
			loader.doneProcessingRows();
			loader = null;
		} catch (Exception e) {
			throw DbException.convert(e);
		}
	}
	
	public String createUpdateLine(Row row) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < row.getColumnCount(); i++) {
			Value value = row.getValue(i);
			buffer.append(value.getString());
			buffer.append(" ");
		}
		return buffer.toString();
	}
	
	@Override
	public Column[] getColumns() {
		return columns;
	}

	@Override
	public EssResultSet get() {
		return new ArrayListEssResultSet(Collections.<Row>emptyList(), columns.length);
	}

	@Override
	public void removeRealRow(Session session, Row row) throws SQLException {
		logger.info("Delete not supported for data models");
	}

}

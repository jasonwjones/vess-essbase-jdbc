package com.saxifrages.vess.h2.models;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.h2.engine.Session;
import org.h2.result.Row;
import org.h2.table.Column;
import org.h2.value.Value;

import com.essbase.api.base.EssException;
import com.essbase.api.base.IEssIterator;
import com.saxifrages.vess.h2.EssModel;
import com.saxifrages.vess.h2.EssService;
import com.saxifrages.vess.h2.essresults.ArrayListEssResultSet;
import com.saxifrages.vess.h2.essresults.EssResultSet;

public abstract class AbstractEssbaseModel implements EssModel {
	
	protected Column[] columns;
	
	protected EssService service;
	
	public interface IterMapper<E> {
		public void convert(E object, Value[] values) throws EssException;
	}
	
	public interface ArrayMapper<E> {
		public void convert(E[] objects, Value[] values) throws EssException;
	}
	
	public interface StringArrayMapper {
		public void convert(String[] row, Value[] values) throws EssException;
	}
	
	public AbstractEssbaseModel(EssService service) {
		this.service = service;
	}
	
	@Override
	public Column[] getColumns() {
		return columns;
	}
	
	public int getColumnCount() {
		return this.getColumns().length;
	}
	
	@Override
	public void addRealRow(Session session, Row row) throws SQLException {
		// default implementation is to do nothing
	}
	
	@SuppressWarnings("unchecked")
	public <E> EssResultSet mapper(IEssIterator iterator, IterMapper<E> mapper) throws EssException {
		List<Row> rows = new ArrayList<Row>();
		for (int i = 0; i < iterator.getCount(); i++) {
			Value[] values = new Value[getColumnCount()];
			mapper.convert((E) iterator.getAt(i), values);
			rows.add(new Row(values, 0));
		}
		return new ArrayListEssResultSet(rows, getColumnCount());
	}
	
	public EssResultSet mapper(String[][] records, StringArrayMapper mapper) throws EssException {
		List<Row> rows = new ArrayList<Row>();
		for (String[] row : records) {
			Value[] values = new Value[getColumnCount()];
			mapper.convert(row, values);
			rows.add(new Row(values, 0));
		}
		return new ArrayListEssResultSet(rows, getColumnCount());
	}
	
	public <E> EssResultSet mapper(Iterable<E> iterator, IterMapper<E> mapper) throws EssException {
		List<Row> rows = new ArrayList<Row>();
		for (E item : iterator) {
			Value[] values = new Value[getColumnCount()];
			mapper.convert(item, values);
			rows.add(new Row(values, 0));
		}
		return new ArrayListEssResultSet(rows, getColumnCount());
	}
	
	
	
//	public <E> EssResultSet mapperCustom(IEssIterator iterator, IterMapper<E> mapper) throws EssException {
//	}
	
	// TODO: MAPPER WHERE WE DEFINE THE WHOLE RESULT SET
	
}

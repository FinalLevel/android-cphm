package com.finallevel.cphm;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Contract
{
//	private static final String LOG_TAG = "Contract";

	private final Field[] _fields;
	private final String[] _projection;

	public Contract(Class cls)
	{
//		Method f2c = cls.getMethod("fieldToColumnName", String.class);
//		Log.v(LOG_TAG, f2c.toString());

		final java.lang.reflect.Field[] fields = cls.getFields();

		final List<Field> list = new ArrayList<>(fields.length);
		list.add(null);
		for (java.lang.reflect.Field f : fields) {
			final Field field = Field.createNew(f);
			if (field != null) {
				if (field.name.equals(BaseColumns.CN_ID)) {
					list.set(0, field);
				} else {
					list.add(field);
				}
			}
		}
		if (list.get(0) == null) {
			throw new IllegalArgumentException("Column \"_id\" not exists");
		}

		//noinspection ToArrayCallWithZeroLengthArrayArgument
		_fields = list.toArray(new Field[list.size()]);

		_projection = new String[_fields.length + 1];
		for (int i = 0; i < _fields.length; i++) {
			_projection[i] = _fields[i].name;
		}
		_projection[_projection.length - 1] = "'" + this.hashCode() + "'";
	}

	public Iterable<String> getColumnList()
	{
		final List<String> list = new ArrayList<>();
		for (Field field : _fields) {
//			if (!field.name.equals(BaseColumns.CN_ID)) {
			list.add(field.name);
//			}
		}

		return list;
	}

	public Map<String, String> getColumnDefinitions()
	{
		final Map<String, String> list = new HashMap<>(_fields.length);
		for (Field field : _fields) {
//			if (!field.name.equals(BaseColumns.CN_ID)) {
			list.put(field.name, field.getDefinition());
//			}
		}

		return list;
	}

	public Map<String, String> getColumnDefaults()
	{
		final Map<String, String> list = new HashMap<>(_fields.length);
		for (Field field : _fields) {
			list.put(field.name, field.getDefaults());
		}

		return list;
	}

	public String[] getProjection()
	{
		return _projection;
	}

	public String[] getProjection(String withTable)
	{
		final String[] result = new String[_projection.length];

		int i = 0;
		for (; i < _projection.length - 1; i++) {
			result[i] = withTable + '.' + _projection[i];
		}
		result[i] = _projection[i];

		return result;
	}

	public void populateModel(Object result, Cursor cursor, int columnsShift)
		throws IllegalAccessException
	{
		if (cursor.getInt(columnsShift + _projection.length - 1) != this.hashCode()) {
			throw new IllegalArgumentException("Use getProjection() as projection");
		}

		for (int i = 0; i < _fields.length; i++) {
			_fields[i].set(result, cursor, columnsShift + i);
		}
	}

	public ContentValues toContentValues(Object model, Collection<String> exclude, Collection<String> only)
		throws IllegalAccessException
	{
		final ContentValues cv;
		if (exclude != null || only != null) {
			if (only != null) {
				cv = new ContentValues(only.size() + 1);
			} else {
				cv = new ContentValues(_fields.length - exclude.size() + 1);
			}

			for (Field field : _fields) {
				if (exclude != null && exclude.contains(field.name)) {
					continue;
				} else if (only != null && !only.contains(field.name)) {
					continue;
				}

				field.getContentValue(model, cv);
			}
		} else {
			cv = new ContentValues(_fields.length + 1);

			for (Field field : _fields) {
				field.getContentValue(model, cv);
			}
		}

		return cv;
	}

	public void setModelId(Object model, long id)
		throws IllegalAccessException
	{
		_fields[0].set(model, id);
	}

	public long getModelId(Object model)
		throws IllegalAccessException
	{
		return _fields[0].getLong(model);
	}

}

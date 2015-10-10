package com.finallevel.cphm;

import android.content.ContentValues;
import android.database.Cursor;

import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;

class Field
{
	// INTEGER
	private static final int TYPE_INT = 0;
	private static final int TYPE_INTEGER_OBJ = 1;
	private static final int TYPE_LONG = 2;
	private static final int TYPE_LONG_OBJ = 3;
	// REAL
	private static final int TYPE_FLOAT = 4;
	private static final int TYPE_FLOAT_OBJ = 5;
	private static final int TYPE_DOUBLE = 6;
	private static final int TYPE_DOUBLE_OBJ = 7;
	// TEXT
	private static final int TYPE_STRING = 8;
	// BLOB
	private static final int TYPE_BLOB = 9;
	// BYTE
	private static final int TYPE_BYTE = 10;
	private static final int TYPE_BYTE_OBJ = 11;

	private static final String[] SQLITE_TYPES = {
		"INTEGER", "INTEGER", "INTEGER", "INTEGER",
		"REAL", "REAL", "REAL", "REAL",
		"TEXT",
		"BLOB",
		"INTEGER", "INTEGER",
	};
	private static final boolean[] NOT_NULL = {
		true, false, true, false,
		true, false, true, false,
		false,
		false,
		true, false,
	};
	private static final String[] DEFAULTS = {
		"0", null, "0", null,
		"0", null, "0", null,
		null,
		null,
		"0", null,
	};

	public String name;

	private int _type;
//	public boolean notNull;

	private java.lang.reflect.Field _field;

	public static Field createNew(java.lang.reflect.Field field)
	{
//		Log.v(LOG_TAG, "  ==  " + field.toString());

		final int modifiers = field.getModifiers();
		if ((modifiers & (Modifier.STATIC | Modifier.FINAL)) != 0) {
			return null;
		}

//		NonNull nonNull = field.getAnnotations(NonNull.class);
//		Log.d(LOG_TAG, "NonNull: " + "abc".;
//		if (id != null) {
//			try {
//				idField = (String) field.get(null);
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}

		final Field result = new Field();

		final Class t = field.getType();
		if (t == int.class) {
			result._type = TYPE_INT;
		} else if (t == String.class) {
			result._type = TYPE_STRING;
		} else if (t == long.class) {
			result._type = TYPE_LONG;
		} else if (t == Integer.class) {
			result._type = TYPE_INTEGER_OBJ;
		} else if (t == Long.class) {
			result._type = TYPE_LONG_OBJ;
		} else if (t == byte[].class) {
			result._type = TYPE_BLOB;
		} else if (t == byte.class) {
			result._type = TYPE_BYTE;
		} else if (t == float.class) {
			result._type = TYPE_FLOAT;
		} else if (t == double.class) {
			result._type = TYPE_DOUBLE;
		} else if (t == Byte.class) {
			result._type = TYPE_BYTE_OBJ;
		} else if (t == Float.class) {
			result._type = TYPE_FLOAT_OBJ;
		} else if (t == Double.class) {
			result._type = TYPE_DOUBLE_OBJ;
		} else {
			return null;
		}

		result.name = field.getName();
		result._field = field;

//		Log.d(LOG_TAG, "Name: " + result.name + "; Type: " + result._type + "; NotNull: " + result.notNull);

		return result;
	}

	public String getDefinition()
	{
		return "\"" + name + "\" " + SQLITE_TYPES[_type] + (NOT_NULL[_type] ? " NOT NULL" : "");
	}

	public void set(Object model, Cursor cursor, int position)
		throws IllegalAccessException
	{
		switch (_type) {
			case TYPE_INT:
				_field.setInt(model, cursor.getInt(position));
				break;
			case TYPE_STRING:
				_field.set(model, cursor.isNull(position) ? null : cursor.getString(position));
				break;
			case TYPE_LONG:
				_field.setLong(model, cursor.getLong(position));
				break;
			case TYPE_INTEGER_OBJ:
				_field.set(model, cursor.isNull(position) ? null : cursor.getInt(position));
				break;
			case TYPE_LONG_OBJ:
				_field.set(model, cursor.isNull(position) ? null : cursor.getLong(position));
				break;
			case TYPE_BLOB:
				_field.set(model, cursor.isNull(position) ? null : cursor.getBlob(position));
				break;
			case TYPE_BYTE:
				_field.setByte(model, (byte) cursor.getInt(position));
				break;
			case TYPE_FLOAT:
				_field.setFloat(model, cursor.getFloat(position));
				break;
			case TYPE_DOUBLE:
				_field.setDouble(model, cursor.getDouble(position));
				break;
			case TYPE_BYTE_OBJ:
				_field.set(model, cursor.isNull(position) ? null : (byte) cursor.getInt(position));
				break;
			case TYPE_FLOAT_OBJ:
				_field.set(model, cursor.isNull(position) ? null : cursor.getFloat(position));
				break;
			case TYPE_DOUBLE_OBJ:
				_field.set(model, cursor.isNull(position) ? null : cursor.getDouble(position));
				break;
			default:
				throw new IllegalArgumentException("Unknown field _type: " + _type);
		}
	}

	public void set(Object model, long value)
		throws IllegalAccessException
	{
		switch (_type) {
			case TYPE_INT:
				_field.setInt(model, (int) value);
				break;
			case TYPE_STRING:
				_field.set(model, String.valueOf(value));
				break;
			case TYPE_LONG:
				_field.setLong(model, value);
				break;
			case TYPE_INTEGER_OBJ:
				_field.set(model, (int) value);
				break;
			case TYPE_LONG_OBJ:
				_field.set(model, value);
				break;
			case TYPE_BLOB:
				_field.set(model, null);
				break;
			case TYPE_BYTE:
				_field.setByte(model, (byte) value);
				break;
			case TYPE_FLOAT:
				_field.setFloat(model, (float) value);
				break;
			case TYPE_DOUBLE:
				_field.setDouble(model, (double) value);
				break;
			case TYPE_BYTE_OBJ:
				_field.set(model, (byte) value);
				break;
			case TYPE_FLOAT_OBJ:
				_field.set(model, (float) value);
				break;
			case TYPE_DOUBLE_OBJ:
				_field.set(model, (double) value);
				break;
			default:
				throw new IllegalArgumentException("Unknown field _type: " + _type);
		}
	}

	public void getContentValue(Object model, ContentValues contentValues)
		throws IllegalAccessException
	{
		// PRIMITIVES

		switch (_type) {
			case TYPE_INT:
				contentValues.put(name, _field.getInt(model));
				return;
			case TYPE_LONG:
				contentValues.put(name, _field.getLong(model));
				return;
			case TYPE_BYTE:
				contentValues.put(name, _field.getByte(model));
				return;
			case TYPE_FLOAT:
				contentValues.put(name, _field.getFloat(model));
				return;
			case TYPE_DOUBLE:
				contentValues.put(name, _field.getDouble(model));
				return;
		}

		// OBJECTS

		final Object val = _field.get(model);
		if (val == null) {
			contentValues.putNull(name);
			return;
		}

		switch (_type) {
			case TYPE_STRING:
				contentValues.put(name, (String) val);
				break;
			case TYPE_INTEGER_OBJ:
				contentValues.put(name, (Integer) val);
				break;
			case TYPE_LONG_OBJ:
				contentValues.put(name, (Long) val);
				break;
			case TYPE_BLOB:
				contentValues.put(name, (byte[]) val);
				break;
			case TYPE_BYTE_OBJ:
				contentValues.put(name, (Byte) val);
				break;
			case TYPE_FLOAT_OBJ:
				contentValues.put(name, (Float) val);
				break;
			case TYPE_DOUBLE_OBJ:
				contentValues.put(name, (Double) val);
				break;
			default:
				throw new IllegalArgumentException("Unknown field _type: " + _type);
		}
	}

	public Long getLong(Object model)
		throws IllegalAccessException
	{
		// PRIMITIVES

		switch (_type) {
			case TYPE_INT:
				return (long) _field.getInt(model);
			case TYPE_LONG:
				return _field.getLong(model);
			case TYPE_BYTE:
				return (long) _field.getByte(model);
			case TYPE_FLOAT:
				return (long) _field.getFloat(model);
			case TYPE_DOUBLE:
				return (long) _field.getDouble(model);
		}

		// OBJECTS

		final Object val = _field.get(model);
		if (val == null) {
			return null;
		}

		switch (_type) {
			case TYPE_STRING:
				return Long.parseLong((String) val);
			case TYPE_INTEGER_OBJ:
				return ((Integer) val).longValue();
			case TYPE_LONG_OBJ:
				return (Long) val;
			case TYPE_BLOB:
				return ByteBuffer.wrap((byte[]) val).getLong();
			case TYPE_BYTE_OBJ:
				return ((Byte) val).longValue();
			case TYPE_FLOAT_OBJ:
				return ((Float) val).longValue();
			case TYPE_DOUBLE_OBJ:
				return ((Double) val).longValue();
			default:
				throw new IllegalArgumentException("Unknown field _type: " + _type);
		}
	}

	public String getDefaults()
	{
		return DEFAULTS[_type];
	}
}

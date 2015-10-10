package com.finallevel.cphm;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class BaseSQLiteOpenHelper extends SQLiteOpenHelper
{
	private static final String LOG_TAG = "BaseSQLiteOpenHelper";

	private final BaseStructure _structure;

	public BaseSQLiteOpenHelper(Context context, String databaseFile, SQLiteDatabase.CursorFactory factory, int version)
	{
		super(context, databaseFile, factory, version);

		_structure = getStructure();
	}

	public abstract BaseStructure getStructure();

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		Log.v(LOG_TAG, "onCreate()");

		final Map<String, BaseStructure.TableHolder> tableHolders = _structure.getTableHolders();
		for (BaseStructure.TableHolder tableHolder : tableHolders.values()) {
			_createTable(
				db,
				tableHolder.tableName,
				tableHolder.contract.get(),
				tableHolder.pkOnConflict,
				tableHolder.constraints
			);
		}

		final Iterable<BaseStructure.IndexHolder> indexHolders = _structure.getIndexHolders();
		for (BaseStructure.IndexHolder indexHolder : indexHolders) {
			final String sql = indexHolder.getDefinition();

			Log.d(LOG_TAG, sql);
			db.execSQL(sql);
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		Log.d(LOG_TAG, "onUpgrade(" + oldVersion + ", " + newVersion + ")");

		final String[] PROJECTION = {
			"type",
			"name",
			"tbl_name",
		};
		final int CI_TYPE = 0;
		final int CI_NAME = 1;
		final int CI_TABLE = 2;

		final Set<String> tables = new HashSet<>();
		final Map<String, Set<String>> tablesIndexes = new HashMap<>();

		final Cursor cursor = db.query("sqlite_master", PROJECTION, null, null, null, null, null);
		//noinspection TryFinallyCanBeTryWithResources
		try {
			while (cursor.moveToNext()) {
				final String type = cursor.getString(CI_TYPE);
				final String name = cursor.getString(CI_NAME);
				final String table = cursor.getString(CI_TABLE);

				switch (type) {
					case "table": {
						tables.add(table);
						break;
					}
					case "index": {
						if (!tablesIndexes.containsKey(table)) {
							final Set<String> tmp = new HashSet<>();
							tmp.add(name);
							tablesIndexes.put(table, tmp);
						} else {
							tablesIndexes.get(table).add(name);
						}
						break;
					}
					default: {
						Log.w(LOG_TAG, "Unknown type in sqlite_master table: " + type);
					}
				}
			}
		} finally {
			cursor.close();
		}

		final Map<String, BaseStructure.TableHolder> tableHolders = _structure.getTableHolders();
		for (BaseStructure.TableHolder tableHolder : tableHolders.values()) {
			if (tables.contains(tableHolder.tableName)) {
				_alterTable(
					db,
					tableHolder.tableName,
					tableHolder.contract.get()
				);
			} else {
				_createTable(
					db, tableHolder.tableName,
					tableHolder.contract.get(),
					tableHolder.pkOnConflict,
					tableHolder.constraints
				);
			}
		}

		final Iterable<BaseStructure.IndexHolder> indexHolders = _structure.getIndexHolders();
		for (BaseStructure.IndexHolder indexHolder : indexHolders) {
			if (
				tablesIndexes.containsKey(indexHolder.tableName)
					&& tablesIndexes.get(indexHolder.tableName).contains(indexHolder.getName())
				) {
				continue;
			}

			final String sql = indexHolder.getDefinition();

			Log.d(LOG_TAG, sql);
			db.execSQL(sql);
		}
	}

	private void _alterTable(SQLiteDatabase db, String tableName, Contract contract)
	{
		final Set<String> exists = _getTableColumns(db, tableName);

		final Set<String> addColumns = new HashSet<>();
		final Iterable<String> columnsList = contract.getColumnList();
		for (String column : columnsList) {
			if (!exists.contains(column)) {
				addColumns.add(column);
			}
		}

		if (addColumns.isEmpty()) {
			return;
		}

		final Map<String, String> columnsDefinition = contract.getColumnDefinitions();
		for (String column : addColumns) {
			final String sql = "ALTER TABLE \"" + tableName + "\" ADD COLUMN "
				+ columnsDefinition.get(column) + ";";

			Log.d(LOG_TAG, sql);
			db.execSQL(sql);
		}
	}

	private Set<String> _getTableColumns(SQLiteDatabase db, String tableName)
	{
		final Cursor cursor = db.rawQuery("PRAGMA table_info(\"" + tableName + "\");", null);
		final Set<String> columns = new HashSet<>(cursor.getCount());
		try {
			final int ciName = cursor.getColumnIndex("name");
			while (cursor.moveToNext()) {
				columns.add(cursor.getString(ciName));
			}
		} finally {
			cursor.close();
		}

		return columns;
	}

	private void _createTable(SQLiteDatabase db, String tableName, Contract contract, BaseStructure.OnConflict pkOnConflict, List<BaseStructure.ConstraintHolder> constraints)
	{
		final StringBuilder builder = new StringBuilder(
			"CREATE TABLE IF NOT EXISTS \"" + tableName + "\" ( "
				+ BaseColumns.CN_ID + " INTEGER NOT NULL PRIMARY KEY"
		);
		if (pkOnConflict != null) {
			builder.append(" ON CONFLICT ").append(pkOnConflict);
		}
		builder.append(" AUTOINCREMENT");

		final Map<String, String> columns = contract.getColumnDefinitions();
		for (Map.Entry<String, String> column : columns.entrySet()) {
			if (!column.getKey().equals(BaseColumns.CN_ID)) {
				builder.append(", ").append(column.getValue());
			}
		}

		if (constraints != null) {
			for (BaseStructure.ConstraintHolder constraint : constraints) {
				builder.append(", ").append(constraint.getDefinition());
			}
		}

		builder.append(" );");

		final String sql = builder.toString();

		Log.d(LOG_TAG, sql);
		db.execSQL(sql);
	}

	protected boolean _recreateTable(SQLiteDatabase db, String tableName)
	{
		final Map<String, BaseStructure.TableHolder> tableHolders = _structure.getTableHolders();

		final BaseStructure.TableHolder tableHolder = tableHolders.get(tableName);
		if (tableHolder == null) {
			return false;
		}

		final Contract contract = tableHolder.contract.get();

		final Set<String> exists = _getTableColumns(db, tableName);
		final Map<String, String> columnsList = contract.getColumnDefaults();
		final List<String> columnNames = new ArrayList<>(exists.size());
		final List<String> columnValues = new ArrayList<>(exists.size());
		for (Map.Entry<String, String> column : columnsList.entrySet()) {
			if (exists.contains(column.getKey())) {
				columnNames.add(column.getKey());
				if (column.getValue() != null) {
					columnValues.add("IFNULL(\"" + column.getKey() + "\", " + column.getValue() + ")");
				} else {
					columnValues.add('"' + column.getKey() + '"');
				}
			} else if (column.getValue() != null) {
				columnNames.add(column.getKey());
				columnValues.add(column.getValue());
			}
		}

		try {
			db.beginTransaction();

			final String tmpTable = tableName + "_temporary_table";

			db.execSQL("DROP TABLE IF EXISTS \"" + tmpTable + "\";");
			db.execSQL("ALTER TABLE \"" + tableName + "\" RENAME TO \"" + tmpTable + "\";");

			_createTable(db, tableName, contract, tableHolder.pkOnConflict, tableHolder.constraints);

			if (!columnNames.isEmpty()) {
				final String cols = "\"" + TextUtils.join("\", \"", columnNames) + "\"";
				final String vals = TextUtils.join(", ", columnValues);
				final String sql = "INSERT INTO \"" + tableName + "\" (" + cols + ") "
					+ "SELECT " + vals + " FROM \"" + tmpTable + "\";";

				Log.d(LOG_TAG, sql);
				db.execSQL(sql);
			}

			db.execSQL("DROP TABLE \"" + tmpTable + "\";");

			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}

		return true;
	}

}

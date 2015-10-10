package com.finallevel.cphm;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import java.util.List;

public abstract class BaseProvider extends ContentProvider
{
	public static final String PARAM_ON_CONFLICT = "_ON_CONFLICT";
	public static final String PARAM_JOIN_ON = "_JOIN_ON";

	private static final String LOG_TAG = "BaseProvider";

	protected BaseStructure _structure;
	protected SQLiteOpenHelper _openHelper;

	public abstract BaseStructure getStructure();

	public abstract SQLiteOpenHelper getOpenHelper();

	@Override
	public boolean onCreate()
	{
		_structure = getStructure();

		_openHelper = getOpenHelper();

		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
	{
		Log.d(LOG_TAG, "QUERY: " + uri.toString());

		final Pair<String[], Long> type = _getType(uri);
		if (type == null) {
			throw new IllegalArgumentException("Unknown uri " + uri);
		}

		final SQLiteDatabase db = _openHelper.getReadableDatabase();

		final Cursor cursor;
		if (type.second != null) {
			if (type.first.length > 1) {
				throw new IllegalArgumentException("Unknown uri " + uri);
			}

			if (TextUtils.isEmpty(selection)) {
				cursor = db.query(
					type.first[0],
					projection,
					BaseColumns.CN_ID + " = " + type.second,
					null,
					null,
					null,
					sortOrder
				);
			} else {
				cursor = db.query(
					type.first[0],
					projection,
					"(" + selection + ") AND " + BaseColumns.CN_ID + " = " + type.second,
					selectionArgs,
					null,
					null,
					sortOrder
				);
			}
		} else if (type.first.length > 1) {
			final String queryParameter = uri.getQueryParameter(PARAM_JOIN_ON);
			final String[] joinOn = (TextUtils.isEmpty(queryParameter) ? null : queryParameter.split(","));
			final StringBuilder stringBuilder = new StringBuilder(type.first[0]);
			for (int i = 1, j = 0; i < type.first.length; i++, j++) {
				stringBuilder.append(" JOIN ").append(type.first[i]);
				if (joinOn != null && j < joinOn.length && !TextUtils.isEmpty(joinOn[j])) {
					stringBuilder.append(" ON (").append(joinOn[j]).append(")");
				}
			}

			final SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
			queryBuilder.setTables(stringBuilder.toString());

			cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
		} else {
			cursor = db.query(type.first[0], projection, selection, selectionArgs, null, null, sortOrder);
		}

		cursor.setNotificationUri(getContext().getContentResolver(), uri); // NOTE: check if join

		return cursor;
	}

	@Override
	public String getType(Uri uri)
	{
		final Pair<String[], Long> type = _getType(uri);
		if (type == null) {
			throw new IllegalArgumentException("Unknown uri " + uri);
		}

		if (type.second != null) {
			return "vnd.android.cursor.item/vdn." + TextUtils.join(".", type.first);
		} else {
			return "vnd.android.cursor.dir/vdn." + TextUtils.join(".", type.first);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values)
	{
		Log.d(LOG_TAG, "INSERT: " + uri.toString());

		final Pair<String[], Long> type = _getType(uri);
		if (type == null || type.second != null || type.first.length > 1) {
			throw new IllegalArgumentException("Unknown uri " + uri);
		}

		final SQLiteDatabase db = _openHelper.getWritableDatabase();

		int onConflict = SQLiteDatabase.CONFLICT_NONE;
		final String queryConflict = uri.getQueryParameter(PARAM_ON_CONFLICT);
		if (!TextUtils.isEmpty(queryConflict)) {
			onConflict = Integer.parseInt(queryConflict);
		}
		final long id = db.insertWithOnConflict(type.first[0], null, values, onConflict);

		if (id > 0 && !db.inTransaction()) {
			getContext().getContentResolver().notifyChange(uri, null);
		}

		return ContentUris.withAppendedId(uri, id);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs)
	{
		Log.d(LOG_TAG, "DELETE: " + uri.toString());

		final Pair<String[], Long> type = _getType(uri);
		if (type == null || type.first.length > 1) {
			throw new IllegalArgumentException("Unknown uri " + uri);
		}

		final SQLiteDatabase db = _openHelper.getWritableDatabase();

		final int rowsAffected;
		if (type.second != null) {
			if (TextUtils.isEmpty(selection)) {
				rowsAffected = db.delete(type.first[0], BaseColumns.CN_ID + " = " + type.second, null);
			} else {
				rowsAffected = db.delete(
					type.first[0],
					"(" + selection + ") AND " + BaseColumns.CN_ID + " = " + type.second,
					selectionArgs
				);
			}
		} else {
			rowsAffected = db.delete(type.first[0], selection, selectionArgs);
		}

		if ((selection == null || rowsAffected != 0) && !db.inTransaction()) {
			getContext().getContentResolver().notifyChange(uri, null);
		}

		return rowsAffected;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
	{
		Log.d(LOG_TAG, "UPDATE: " + uri.toString());

		final Pair<String[], Long> type = _getType(uri);
		if (type == null || type.first.length > 1) {
			throw new IllegalArgumentException("Unknown uri " + uri);
		}

		final SQLiteDatabase db = _openHelper.getWritableDatabase();

		int onConflict = SQLiteDatabase.CONFLICT_NONE;
		final String queryConflict = uri.getQueryParameter(PARAM_ON_CONFLICT);
		if (!TextUtils.isEmpty(queryConflict)) {
			onConflict = Integer.parseInt(queryConflict);
		}
		final int rowsAffected;
		if (type.second != null) {
			if (TextUtils.isEmpty(selection)) {
				rowsAffected = db.updateWithOnConflict(
					type.first[0],
					values,
					BaseColumns.CN_ID + " = " + type.second,
					null,
					onConflict
				);
			} else {
				rowsAffected = db.updateWithOnConflict(
					type.first[0],
					values,
					"(" + selection + ") AND " + BaseColumns.CN_ID + " = " + type.second,
					selectionArgs,
					onConflict
				);
			}
		} else {
			rowsAffected = db.updateWithOnConflict(type.first[0], values, selection, selectionArgs, onConflict);
		}

		if (rowsAffected != 0 && !db.inTransaction()) {
			getContext().getContentResolver().notifyChange(uri, null);
		}

		return rowsAffected;
	}

	private Pair<String[], Long> _getType(Uri uri)
	{
		final List<String> segments = uri.getPathSegments();

		final String[] tables = segments.get(0).split(",");

		if (tables.length < 1) {
			return null;
		}

		for (final String table : tables) {
			if (!_structure.containsTable(table)) {
				return null;
			}
		}

		if (segments.size() > 1) {
			final String id = segments.get(1);
			if (!id.isEmpty()) {
				return Pair.create(tables, Long.parseLong(id));
			}
		}

		return Pair.create(tables, null);
	}
}

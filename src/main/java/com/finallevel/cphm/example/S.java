/**
 * Singleton which describe structure of database
 */

package com.finallevel.cphm.example;

import android.database.Cursor;
import android.net.Uri;
import com.finallevel.cphm.BaseColumns;
import com.finallevel.cphm.BaseStructure;

public class S
{
	public static final String CONTENT_AUTHORITY = "com.finallevel.cphm.example";
	public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

	private static final BaseStructure _instance = new BaseStructure();

	static {
		_instance.addTable(Book.TABLE, Book.class, BaseStructure.OnConflict.REPLACE);
		_instance.addIndex(Book.TABLE, false, new String[] { Book.CN_TITLE });
		_instance.addConstraint(
			Book.TABLE,
			BaseStructure.Constraint.UNIQUE,
			new String[] { Book.CN_AUTHOR, Book.CN_TITLE },
			BaseStructure.OnConflict.REPLACE
		);
	}

	private S()
	{
	}

	public static BaseStructure instance()
	{
		return _instance;
	}

	public static String[] projection(Class<? extends BaseColumns> cls)
	{
		return _instance.getProjection(cls);
	}

	public static <T extends BaseColumns> T model(Class<T> cls, Cursor cursor)
	{
		return _instance.getModel(cls, cursor, 0);
	}

	public static Uri getContentUri(String tableName, long id)
	{
		return BASE_CONTENT_URI.buildUpon().appendPath(tableName).appendEncodedPath(String.valueOf(id)).build();
	}

	public static Uri getContentUri(String tableName)
	{
		return BASE_CONTENT_URI.buildUpon().appendPath(tableName).build();
	}

}

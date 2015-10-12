package com.finallevel.cphm.example;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.finallevel.cphm.BaseProvider;
import com.finallevel.cphm.BaseSQLiteOpenHelper;
import com.finallevel.cphm.BaseStructure;

import java.util.HashSet;
import java.util.Set;

public class ContentProvider extends BaseProvider
{
	public static final String DATABASE_FILE = "example.db";
	public static final int VERSION = 5;

	@Override
	public BaseStructure getStructure()
	{
		return S.instance();
	}

	@Override
	public SQLiteOpenHelper getOpenHelper()
	{
		return new OpenHelper(getContext(), DATABASE_FILE, null, VERSION);
	}

}

class OpenHelper extends BaseSQLiteOpenHelper
{
	public OpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version)
	{
		super(context, name, factory, version);
	}

	@Override
	public BaseStructure getStructure()
	{
		return S.instance();
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		final Set<String> tablesToRecreate = new HashSet<>();

		if (oldVersion <= 4) {
			tablesToRecreate.add(Book.TABLE);
		}

		for (String table : tablesToRecreate) {
			_recreateTable(db, table); // if you need full recreate table, all data will be saved
		}

		super.onUpgrade(db, oldVersion, newVersion); // update tables structure to described in models

		if (oldVersion <= 4) {
			// post action: populate table for example
		}
	}

	@Override
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		onUpgrade(db, oldVersion, newVersion);
	}

}

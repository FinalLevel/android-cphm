package com.finallevel.cphm.example;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;

public class MainActivity extends Activity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		final Book book = S.instance().findFirst(
				S.getContentUri(Book.TABLE, 1),
				Book.class,
				this
		);

		final Cursor cursor = getContentResolver().query(
				S.getContentUri(Book.TABLE),
				S.projection(Book.class), // if you want to extract model you must use projection() to get projection fields
				null,
				null,
				null
		);
		try {
			while (cursor.moveToNext()) {
				final Book bookItem = S.model(Book.class, cursor);
			}
		} finally {
			cursor.close();
		}

		final Book newBook = new Book();
		newBook.author = "misaret";
		newBook.title = "CPHM Library";
		newBook.year = 2015;

		final ContentValues contentValues = S.instance().toContentValues(newBook);

		S.instance().insertOrUpdate(getContentResolver(), S.getContentUri(Book.TABLE), contentValues);

	}

}

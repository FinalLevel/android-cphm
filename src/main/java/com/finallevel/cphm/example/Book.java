/**
 *
 * CPHM generate this SQL query for model described below:
 *
 * CREATE TABLE IF NOT EXISTS "station" (
 *   _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
 *   "author" TEXT,
 *   "title" TEXT,
 *   "year" INTEGER NOT NULL,
 *   "price" INTEGER
 *   );
 *
 */

package com.finallevel.cphm.example;

import com.finallevel.cphm.BaseColumns;

public class Book extends BaseColumns
{
	public static final String TABLE = "book";

	public long _id; // always INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT

	public static final String CN_AUTHOR = "author";
	public String author;

	public static final String CN_TITLE = "title";
	public String title;

	public int year; // all primitive types converted to required (NOT NULL) fields

	public Integer price; // this is optional (NULL) field


	// only public, non static and non final members will be field in table
	private int _otherMember; // skipped

}

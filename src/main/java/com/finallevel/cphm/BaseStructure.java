package com.finallevel.cphm;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BaseStructure
{
	private static final String LOG_TAG = "BaseStructure";

	private final Map<Class, ContractHolder> _contracts = new HashMap<>();
	private final Map<String, TableHolder> _tables = new HashMap<>();
	private final List<IndexHolder> _indexes = new ArrayList<>();

	public void addTable(String tableName, Class<? extends BaseColumns> contractClass, OnConflict pkOnConflict)
	{
		ContractHolder contract = _contracts.get(contractClass);
		if (contract == null) {
			contract = new ContractHolder(contractClass);

			_contracts.put(contractClass, contract);
		}

		_tables.put(tableName, new TableHolder(tableName, contract, pkOnConflict));
	}

	public void addConstraint(String tableName, Constraint constraint, String[] columns, OnConflict onConflict)
	{
		final TableHolder tableHolder = _tables.get(tableName);
		if (tableHolder == null) {
			throw new IllegalArgumentException("Constraint to unknown table");
		}

		tableHolder.constraints.add(new ConstraintHolder(constraint, columns, onConflict));
	}

	public void addIndex(String tableName, boolean unique, String[] columns)
	{
		_indexes.add(new IndexHolder(tableName, unique, columns));
	}

	public boolean containsTable(String tableName)
	{
		return _tables.containsKey(tableName);
	}

	public String[] getProjection(Class<? extends BaseColumns> cls)
	{
		final ContractHolder holder = _contracts.get(cls);
		if (holder == null) {
			throw new IllegalArgumentException("Unknown model " + cls);
		}

		return holder.get().getProjection();
	}

	public String[] getProjection(Class<? extends BaseColumns> cls, String withTable)
	{
		final ContractHolder holder = _contracts.get(cls);
		if (holder == null) {
			throw new IllegalArgumentException("Unknown model " + cls);
		}

		return holder.get().getProjection(withTable);
	}

	public <T extends BaseColumns> T getModel(Class<T> cls, Cursor cursor, int columnsShift)
	{
		final ContractHolder holder = _contracts.get(cls);
		if (holder == null) {
			throw new IllegalArgumentException("Unknown model " + cls);
		}

		//noinspection TryWithIdenticalCatches
		try {
			final T result = cls.newInstance();

			holder.get().populateModel(result, cursor, columnsShift);

			return result;
		} catch (InstantiationException e) {
			Log.w(LOG_TAG, e);

			return null;
		} catch (IllegalAccessException e) {
			Log.w(LOG_TAG, e);

			return null;
		}
	}

	public <T extends BaseColumns> ContentValues toContentValues(T model)
	{
		return toContentValues(model, null, null);
	}

	public <T extends BaseColumns> ContentValues toContentValues(T model, Collection<String> exclude, Collection<String> only)
	{
		final ContractHolder holder = _contracts.get(model.getClass());
		if (holder == null) {
			throw new IllegalArgumentException("Unknown model " + model.getClass());
		}

		try {
			return holder.get().toContentValues(model, exclude, only);
		} catch (IllegalAccessException e) {
			Log.w(LOG_TAG, e);

			return null;
		}
	}

	public <T extends BaseColumns> T findFirst(Uri uri, Class<T> cls, Context context)
	{
		final ContractHolder holder = _contracts.get(cls);
		if (holder == null) {
			throw new IllegalArgumentException("Unknown model " + cls);
		}

		final Contract contract = holder.get();

		final Cursor cursor = context.getContentResolver().query(
			uri,
			contract.getProjection(),
			null,
			null,
			null
		);
		if (cursor == null) {
			return null;
		}

		final T result;
		//noinspection TryWithIdenticalCatches
		try {
			if (!cursor.moveToFirst()) {
				return null;
			}

			result = cls.newInstance();

			contract.populateModel(result, cursor, 0);
		} catch (InstantiationException e) {
//			e.printStackTrace();
			return null;
		} catch (IllegalAccessException e) {
//			e.printStackTrace();
			return null;
		} finally {
			cursor.close();
		}

		return result;
	}

	public <T extends BaseColumns> boolean create(Uri uri, T model, Context context)
	{
		final ContractHolder holder = _contracts.get(model.getClass());
		if (holder == null) {
			throw new IllegalArgumentException("Unknown model " + model.getClass());
		}

		final Contract contract = holder.get();
		try {
			long id = contract.getModelId(model);

			final ContentValues cv = toContentValues(model);
			if (id <= 0) {
				cv.remove(BaseColumns.CN_ID);
			}

			final Uri resultUri = context.getContentResolver().insert(uri, cv);
			id = ContentUris.parseId(resultUri);

			contract.setModelId(model, id);

			return (id > 0);
		} catch (IllegalAccessException e) {
			Log.w(LOG_TAG, e);

			return false;
		}
	}

	public void insertOrUpdate(Collection<ContentProviderOperation> operations, Uri contentUri, ContentValues cv)
	{
		insertOrUpdate(operations, contentUri, cv, null);
	}

	public void insertOrUpdate(
		Collection<ContentProviderOperation> operations,
		Uri contentUri,
		ContentValues cv,
		Collection<String> excludeFromUpdate
	)
	{
		operations.add(
			ContentProviderOperation.newInsert(contentUri)
				.withValues(cv)
				.build()
		);
		if (excludeFromUpdate != null) {
			for (String field : excludeFromUpdate) {
				cv.remove(field);
			}
		}
		operations.add(
			ContentProviderOperation.newUpdate(contentUri)
				.withValues(cv)
				.build()
		);
	}

	@SuppressWarnings("UnusedReturnValue")
	public boolean insertOrUpdate(ContentResolver contentResolver, Uri contentUri, ContentValues cv)
	{
		return insertOrUpdate(contentResolver, contentUri, cv, null);
	}

	public boolean insertOrUpdate(
		ContentResolver contentResolver,
		Uri contentUri,
		ContentValues cv,
		String[] excludeFromUpdate
	)
	{
		try {
			final Uri uri = contentResolver.insert(contentUri, cv);
			final long id = ContentUris.parseId(uri);
			if (id > 0) {
				return true;
			}
		} catch (SQLException ignored) {
		}

		if (excludeFromUpdate != null) {
			for (String field : excludeFromUpdate) {
				cv.remove(field);
			}
		}

		contentResolver.update(contentUri, cv, null, null);

		return true;
	}

	Map<String, TableHolder> getTableHolders()
	{
		return _tables;
	}

	Iterable<IndexHolder> getIndexHolders()
	{
		return _indexes;
	}

	public enum Constraint
	{
		UNIQUE("UNIQUE"),
		CHECK("CHECK"),
		FOREIGN_KEY("FOREIGN_KEY");

		Constraint(@SuppressWarnings("unused") String value)
		{
		}
	}

	public enum OnConflict
	{
		ROLLBACK("ROLLBACK"),
		ABORT("ABORT"),
		FAIL("FAIL"),
		IGNORE("IGNORE"),
		REPLACE("REPLACE");

		OnConflict(@SuppressWarnings("unused") String value)
		{
		}
	}

	class ContractHolder
	{
		private final Class _class;
		private Contract _contract;

		ContractHolder(Class cls)
		{
			_class = cls;
		}

		Contract get()
		{
			if (_contract == null) {
				synchronized (this) {
					if (_contract == null) {
						_contract = new Contract(_class);
					}
				}
			}

			return _contract;
		}
	}

	class TableHolder
	{
		public final String tableName;
		public final ContractHolder contract;
		public final OnConflict pkOnConflict;
		public final List<ConstraintHolder> constraints = new ArrayList<>();

		TableHolder(String name, ContractHolder contract, OnConflict pkOnConflict)
		{
			this.tableName = name;
			this.contract = contract;
			this.pkOnConflict = pkOnConflict;
		}
	}

	class ConstraintHolder
	{
		public final Constraint constraint;
		public final String[] columns;
		public final OnConflict onConflict;

		ConstraintHolder(Constraint constraint, String[] columns, OnConflict onConflict)
		{
			this.constraint = constraint;
			this.columns = columns;
			this.onConflict = onConflict;
		}

		String getDefinition()
		{
			final StringBuilder builder = new StringBuilder()
				.append("CONSTRAINT \"const_").append(TextUtils.join("_", columns)).append("\" ")
				.append(constraint).append(" (\"").append(TextUtils.join("\", \"", columns)).append("\")");

			if (onConflict != null) {
				switch (constraint) {
					case UNIQUE:
						builder.append(" ON CONFLICT ").append(onConflict);
						break;
					default:
						throw new IllegalArgumentException("CONSTRAINT not realised: " + constraint);
				}
			}

			return builder.toString();
		}
	}

	class IndexHolder
	{
		public final String tableName;
		public final boolean unique;
		public final String[] columns;

		IndexHolder(String tableName, boolean unique, String[] columns)
		{
			this.tableName = tableName;
			this.unique = unique;
			this.columns = columns;
		}

		String getDefinition()
		{
			return "CREATE" + (unique ? " UNIQUE" : "") + " INDEX IF NOT EXISTS \""
				+ getName() + "\" ON \"" + tableName + "\" (\""
				+ TextUtils.join("\", \"", columns) + "\");";
		}

		String getName()
		{
			return "idx_" + tableName + "_" + TextUtils.join("_", columns);
		}
	}

}

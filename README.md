android-cphm
==========

Content Provider, SQLite Helper and Model together.

Just see [examples](src/main/java/com/finallevel/cphm/example)

```java
Book book = S.instance().findFirst(
		S.getContentUri(Book.TABLE, 12),
		Book.class,
		context
);

Book bookItem = S.model(Book.class, cursor);

ContentValues contentValues = S.instance().toContentValues(newBook);
```

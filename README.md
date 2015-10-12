android-cphm
==========

Content Provider, SQLite Helper and Model together.

#### Features:
- automatic create and update table schema relative to Model;
- indexes and contraints managing;
- creating Model from Cursor;
- creating ContentValues from Model;
- and many more...

#### Just see [examples](src/main/java/com/finallevel/cphm/example)

```java
Book book = S.instance().findFirst(
	S.getContentUri(Book.TABLE, 12),
	Book.class,
	context
);

Book bookItem = S.model(Book.class, cursor);

ContentValues contentValues = S.instance().toContentValues(newBook);
```

## Instalation

In project root folder:
```
git clone https://github.com/FinalLevel/android-cphm.git
```
Append to **settings.gradle**
```
include ':app', ':android-cphm'
```
and to **app/build.gradle**
```
dependencies {
	...
	compile project(':android-cphm')
}
```

package iq.haji.ssssssy;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "haji_cashier.db";
    private static final int DB_VERSION = 2;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createOrdersTable(db);
        createMenuTables(db);
        seedDefaultMenu(db);
    }

    private void createOrdersTable(SQLiteDatabase db) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS orders (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "order_number INTEGER NOT NULL," +
            "items TEXT NOT NULL," +
            "total INTEGER NOT NULL," +
            "created_at TEXT NOT NULL)"
        );
    }

    private void createMenuTables(SQLiteDatabase db) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS categories (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "name TEXT NOT NULL UNIQUE," +
            "position INTEGER NOT NULL DEFAULT 0)"
        );

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS dishes (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "name TEXT NOT NULL," +
            "price INTEGER NOT NULL CHECK(price >= 0)," +
            "category_id INTEGER NOT NULL," +
            "FOREIGN KEY(category_id) REFERENCES categories(id) " +
            "ON DELETE CASCADE)"
        );
    }

    private void seedDefaultMenu(SQLiteDatabase db) {
        insertCategory(db, "العروضات", 0);
        insertCategory(db, "البيتزا", 1);
        insertCategory(db, "البرغر", 2);
        insertCategory(db, "الشاورما", 3);
        insertCategory(db, "المشروبات", 4);

        long pizza = getCategoryId(db, "البيتزا");
        long burger = getCategoryId(db, "البرغر");
        long shawarma = getCategoryId(db, "الشاورما");
        long drinks = getCategoryId(db, "المشروبات");

        insertDishIfMissing(db, "بيتزا", 5000, pizza);
        insertDishIfMissing(db, "برغر", 4000, burger);
        insertDishIfMissing(db, "شاورما", 3000, shawarma);
        insertDishIfMissing(db, "بيبسي", 1000, drinks);
    }

    private void insertCategory(SQLiteDatabase db, String name, int position) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("position", position);
        db.insertWithOnConflict(
            "categories", null, values, SQLiteDatabase.CONFLICT_IGNORE
        );
    }

    private long getCategoryId(SQLiteDatabase db, String name) {
        Cursor cursor = db.rawQuery(
            "SELECT id FROM categories WHERE name = ?",
            new String[]{name}
        );

        long id = -1;
        if (cursor.moveToFirst()) {
            id = cursor.getLong(0);
        }
        cursor.close();
        return id;
    }

    private void insertDishIfMissing(
        SQLiteDatabase db, String name, int price, long categoryId
    ) {
        if (categoryId < 0) return;

        Cursor cursor = db.rawQuery(
            "SELECT id FROM dishes WHERE name = ? AND category_id = ?",
            new String[]{name, String.valueOf(categoryId)}
        );

        boolean exists = cursor.moveToFirst();
        cursor.close();

        if (!exists) {
            ContentValues values = new ContentValues();
            values.put("name", name);
            values.put("price", price);
            values.put("category_id", categoryId);
            db.insert("dishes", null, values);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            createOrdersTable(db);
            createMenuTables(db);
            seedDefaultMenu(db);
        }
    }

    public Cursor getCategories() {
        return getReadableDatabase().rawQuery(
            "SELECT id, name, position FROM categories " +
            "ORDER BY position ASC, id ASC",
            null
        );
    }

    public Cursor getDishesByCategory(long categoryId) {
        return getReadableDatabase().rawQuery(
            "SELECT id, name, price, category_id FROM dishes " +
            "WHERE category_id = ? ORDER BY name COLLATE NOCASE ASC",
            new String[]{String.valueOf(categoryId)}
        );
    }

    public long addCategory(String name) {
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isEmpty()) return -1;

        SQLiteDatabase db = getWritableDatabase();

        Cursor cursor = db.rawQuery(
            "SELECT COALESCE(MAX(position), -1) + 1 FROM categories",
            null
        );

        int position = 0;
        if (cursor.moveToFirst()) {
            position = cursor.getInt(0);
        }
        cursor.close();

        ContentValues values = new ContentValues();
        values.put("name", cleanName);
        values.put("position", position);

        return db.insertWithOnConflict(
            "categories", null, values, SQLiteDatabase.CONFLICT_IGNORE
        );
    }

    public long addDish(String name, int price, long categoryId) {
        String cleanName = name == null ? "" : name.trim();

        if (cleanName.isEmpty() || price < 0 || !categoryExists(categoryId)) {
            return -1;
        }

        ContentValues values = new ContentValues();
        values.put("name", cleanName);
        values.put("price", price);
        values.put("category_id", categoryId);

        return getWritableDatabase().insert("dishes", null, values);
    }

    public int updateDish(long id, String name, int price) {
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isEmpty() || price < 0) return 0;

        ContentValues values = new ContentValues();
        values.put("name", cleanName);
        values.put("price", price);

        return getWritableDatabase().update(
            "dishes", values, "id = ?",
            new String[]{String.valueOf(id)}
        );
    }

    public int deleteDish(long id) {
        return getWritableDatabase().delete(
            "dishes", "id = ?", new String[]{String.valueOf(id)}
        );
    }

    public int deleteCategory(long id) {
        if (isOffersCategory(id)) return 0;

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();

        try {
            db.delete(
                "dishes", "category_id = ?",
                new String[]{String.valueOf(id)}
            );

            int result = db.delete(
                "categories", "id = ?",
                new String[]{String.valueOf(id)}
            );

            db.setTransactionSuccessful();
            return result;
        } finally {
            db.endTransaction();
        }
    }

    private boolean categoryExists(long id) {
        Cursor cursor = getReadableDatabase().rawQuery(
            "SELECT id FROM categories WHERE id = ?",
            new String[]{String.valueOf(id)}
        );

        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    private boolean isOffersCategory(long id) {
        Cursor cursor = getReadableDatabase().rawQuery(
            "SELECT name FROM categories WHERE id = ?",
            new String[]{String.valueOf(id)}
        );

        boolean protectedCategory = false;
        if (cursor.moveToFirst()) {
            protectedCategory = "العروضات".equals(cursor.getString(0));
        }
        cursor.close();
        return protectedCategory;
    }

    public long saveOrder(String items, int total) {
        if (items == null || items.trim().isEmpty() || total < 0) {
            return -1;
        }

        SQLiteDatabase db = getWritableDatabase();

        int nextNumber = 1;
        Cursor cursor = db.rawQuery(
            "SELECT COALESCE(MAX(order_number), 0) + 1 FROM orders",
            null
        );

        if (cursor.moveToFirst()) {
            nextNumber = cursor.getInt(0);
        }
        cursor.close();

        String date = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss", Locale.getDefault()
        ).format(new Date());

        ContentValues values = new ContentValues();
        values.put("order_number", nextNumber);
        values.put("items", items);
        values.put("total", total);
        values.put("created_at", date);

        return db.insert("orders", null, values);
    }

    public Cursor getRecentOrders() {
        return getReadableDatabase().rawQuery(
            "SELECT id, order_number, items, total, created_at " +
            "FROM orders ORDER BY id DESC LIMIT 30",
            null
        );
    }

    public int getOrdersCount() {
        Cursor cursor = getReadableDatabase().rawQuery(
            "SELECT COUNT(*) FROM orders", null
        );

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }
}


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
    private static final int DB_VERSION = 3;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createOrdersTable(db);
        createMenuTables(db);
        createLogsTable(db);
        seedDefaultMenu(db);
    }

    private void createOrdersTable(SQLiteDatabase db) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS orders (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "order_number INTEGER NOT NULL," +
            "items TEXT NOT NULL," +
            "total INTEGER NOT NULL," +
            "created_at TEXT NOT NULL," +
            "customer_phone TEXT NOT NULL DEFAULT ''," +
            "status TEXT NOT NULL DEFAULT 'مكتمل'," +
            "cancel_reason TEXT NOT NULL DEFAULT '')"
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

    private void createLogsTable(SQLiteDatabase db) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS operation_logs (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "action TEXT NOT NULL," +
            "details TEXT NOT NULL DEFAULT ''," +
            "created_at TEXT NOT NULL)"
        );
    }

    private void ensureColumn(
        SQLiteDatabase db, String table, String column, String definition
    ) {
        Cursor cursor = db.rawQuery("PRAGMA table_info(" + table + ")", null);
        boolean found = false;

        try {
            int nameIndex = cursor.getColumnIndex("name");
            while (cursor.moveToNext()) {
                if (column.equalsIgnoreCase(cursor.getString(nameIndex))) {
                    found = true;
                    break;
                }
            }
        } finally {
            cursor.close();
        }

        if (!found) {
            db.execSQL(
                "ALTER TABLE " + table + " ADD COLUMN " +
                column + " " + definition
            );
        }
    }

    @Override
    public void onUpgrade(
        SQLiteDatabase db, int oldVersion, int newVersion
    ) {
        db.beginTransaction();

        try {
            createOrdersTable(db);
            createMenuTables(db);
            createLogsTable(db);

            ensureColumn(
                db, "orders", "customer_phone",
                "TEXT NOT NULL DEFAULT ''"
            );
            ensureColumn(
                db, "orders", "status",
                "TEXT NOT NULL DEFAULT 'مكتمل'"
            );
            ensureColumn(
                db, "orders", "cancel_reason",
                "TEXT NOT NULL DEFAULT ''"
            );

            seedDefaultMenu(db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private String now() {
        return new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss", Locale.getDefault()
        ).format(new Date());
    }

    private void log(SQLiteDatabase db, String action, String details) {
        ContentValues values = new ContentValues();
        values.put("action", action);
        values.put("details", details == null ? "" : details);
        values.put("created_at", now());
        db.insert("operation_logs", null, values);
    }

    public void logOperation(String action, String details) {
        SQLiteDatabase db = getWritableDatabase();
        log(db, action, details);
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

    private void insertCategory(
        SQLiteDatabase db, String name, int position
    ) {
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
        try {
            if (cursor.moveToFirst()) id = cursor.getLong(0);
        } finally {
            cursor.close();
        }
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

        boolean exists;
        try {
            exists = cursor.moveToFirst();
        } finally {
            cursor.close();
        }

        if (!exists) {
            ContentValues values = new ContentValues();
            values.put("name", name);
            values.put("price", price);
            values.put("category_id", categoryId);
            db.insert("dishes", null, values);
        }
    }

    public Cursor getCategories() {
        return getReadableDatabase().rawQuery(
            "SELECT id, name, position FROM categories " +
            "ORDER BY position ASC, id ASC", null
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
        try {
            if (cursor.moveToFirst()) position = cursor.getInt(0);
        } finally {
            cursor.close();
        }

        ContentValues values = new ContentValues();
        values.put("name", cleanName);
        values.put("position", position);

        long result = db.insertWithOnConflict(
            "categories", null, values, SQLiteDatabase.CONFLICT_IGNORE
        );

        if (result != -1) log(db, "إضافة قسم", cleanName);
        return result;
    }

    public long addDish(String name, int price, long categoryId) {
        String cleanName = name == null ? "" : name.trim();

        if (cleanName.isEmpty() || price < 0 ||
            !categoryExists(categoryId)) {
            return -1;
        }

        ContentValues values = new ContentValues();
        values.put("name", cleanName);
        values.put("price", price);
        values.put("category_id", categoryId);

        SQLiteDatabase db = getWritableDatabase();
        long result = db.insert("dishes", null, values);

        if (result != -1) {
            log(db, "إضافة طبق", cleanName + " بسعر " + price);
        }
        return result;
    }

    public int updateDish(long id, String name, int price) {
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isEmpty() || price < 0) return 0;

        ContentValues values = new ContentValues();
        values.put("name", cleanName);
        values.put("price", price);

        SQLiteDatabase db = getWritableDatabase();
        int result = db.update(
            "dishes", values, "id = ?",
            new String[]{String.valueOf(id)}
        );

        if (result > 0) {
            log(db, "تعديل طبق", cleanName + " بسعر " + price);
        }
        return result;
    }

    public int deleteDish(long id) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT name FROM dishes WHERE id = ?",
            new String[]{String.valueOf(id)}
        );

        String name = "";
        try {
            if (cursor.moveToFirst()) name = cursor.getString(0);
        } finally {
            cursor.close();
        }

        int result = db.delete(
            "dishes", "id = ?", new String[]{String.valueOf(id)}
        );

        if (result > 0) log(db, "حذف طبق من المنيو", name);
        return result;
    }

    public int deleteCategory(long id) {
        if (isOffersCategory(id)) return 0;

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();

        try {
            Cursor cursor = db.rawQuery(
                "SELECT name FROM categories WHERE id = ?",
                new String[]{String.valueOf(id)}
            );

            String name = "";
            try {
                if (cursor.moveToFirst()) name = cursor.getString(0);
            } finally {
                cursor.close();
            }

            db.delete(
                "dishes", "category_id = ?",
                new String[]{String.valueOf(id)}
            );

            int result = db.delete(
                "categories", "id = ?",
                new String[]{String.valueOf(id)}
            );

            if (result > 0) log(db, "حذف قسم", name);
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

        try {
            return cursor.moveToFirst();
        } finally {
            cursor.close();
        }
    }

    private boolean isOffersCategory(long id) {
        Cursor cursor = getReadableDatabase().rawQuery(
            "SELECT name FROM categories WHERE id = ?",
            new String[]{String.valueOf(id)}
        );

        try {
            return cursor.moveToFirst() &&
                "العروضات".equals(cursor.getString(0));
        } finally {
            cursor.close();
        }
    }

    public long saveOrder(String items, int total) {
        return saveOrder(items, total, "");
    }

    public long saveOrder(
        String items, int total, String customerPhone
    ) {
        if (items == null || items.trim().isEmpty() || total < 0) {
            return -1;
        }

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();

        try {
            Cursor cursor = db.rawQuery(
                "SELECT COALESCE(MAX(order_number), 0) + 1 FROM orders",
                null
            );

            int nextNumber = 1;
            try {
                if (cursor.moveToFirst()) nextNumber = cursor.getInt(0);
            } finally {
                cursor.close();
            }

            ContentValues values = new ContentValues();
            values.put("order_number", nextNumber);
            values.put("items", items);
            values.put("total", total);
            values.put("created_at", now());
            values.put(
                "customer_phone",
                customerPhone == null ? "" : customerPhone.trim()
            );
            values.put("status", "مكتمل");
            values.put("cancel_reason", "");

            long id = db.insert("orders", null, values);

            if (id != -1) {
                log(
                    db, "حفظ طلب",
                    "رقم الطلب " + nextNumber + " والإجمالي " + total
                );
                db.setTransactionSuccessful();
            }

            return id;
        } finally {
            db.endTransaction();
        }
    }

    public int cancelOrder(long orderId, String reason) {
        String cleanReason = reason == null ? "" : reason.trim();
        if (cleanReason.isEmpty()) return 0;

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();

        try {
            ContentValues values = new ContentValues();
            values.put("status", "ملغي");
            values.put("cancel_reason", cleanReason);

            int result = db.update(
                "orders", values,
                "id = ? AND status != 'ملغي'",
                new String[]{String.valueOf(orderId)}
            );

            if (result > 0) {
                log(
                    db, "إلغاء طلب",
                    "معرّف الطلب " + orderId + " والسبب: " + cleanReason
                );
            }

            db.setTransactionSuccessful();
            return result;
        } finally {
            db.endTransaction();
        }
    }

    public Cursor getRecentOrders() {
        return getReadableDatabase().rawQuery(
            "SELECT id, order_number, items, total, created_at " +
            "FROM orders ORDER BY id DESC LIMIT 30", null
        );
    }

    public Cursor getRecentOrdersDetailed() {
        return getReadableDatabase().rawQuery(
            "SELECT id, order_number, items, total, created_at, " +
            "customer_phone, status, cancel_reason " +
            "FROM orders ORDER BY id DESC LIMIT 100", null
        );
    }

    public Cursor getTodayOrders() {
        return getReadableDatabase().rawQuery(
            "SELECT id, order_number, items, total, created_at, " +
            "customer_phone, status, cancel_reason FROM orders " +
            "WHERE substr(created_at, 1, 10) = ? ORDER BY id DESC",
            new String[]{new SimpleDateFormat(
                "yyyy-MM-dd", Locale.getDefault()
            ).format(new Date())}
        );
    }

    public int getTodaySales() {
        Cursor cursor = getReadableDatabase().rawQuery(
            "SELECT COALESCE(SUM(total), 0) FROM orders " +
            "WHERE substr(created_at, 1, 10) = ? AND status != 'ملغي'",
            new String[]{new SimpleDateFormat(
                "yyyy-MM-dd", Locale.getDefault()
            ).format(new Date())}
        );

        try {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            cursor.close();
        }
    }

    public int getTodayOrdersCount() {
        Cursor cursor = getReadableDatabase().rawQuery(
            "SELECT COUNT(*) FROM orders " +
            "WHERE substr(created_at, 1, 10) = ?",
            new String[]{new SimpleDateFormat(
                "yyyy-MM-dd", Locale.getDefault()
            ).format(new Date())}
        );

        try {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            cursor.close();
        }
    }

    public int getOrdersCount() {
        Cursor cursor = getReadableDatabase().rawQuery(
            "SELECT COUNT(*) FROM orders", null
        );

        try {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            cursor.close();
        }
    }

    public Cursor getOperationLogs() {
        return getReadableDatabase().rawQuery(
            "SELECT id, action, details, created_at " +
            "FROM operation_logs ORDER BY id DESC LIMIT 500", null
        );
    }

    public int clearTodayOrdersAfterExport() {
        SQLiteDatabase db = getWritableDatabase();
        String today = new SimpleDateFormat(
            "yyyy-MM-dd", Locale.getDefault()
        ).format(new Date());

        db.beginTransaction();
        try {
            int result = db.delete(
                "orders", "substr(created_at, 1, 10) = ?",
                new String[]{today}
            );
            log(db, "تصفير طلبات اليوم", "عدد الطلبات المحذوفة: " + result);
            db.setTransactionSuccessful();
            return result;
        } finally {
            db.endTransaction();
        }
    }
}

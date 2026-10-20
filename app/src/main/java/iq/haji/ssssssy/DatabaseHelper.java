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
    private static final int DB_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
            "CREATE TABLE orders (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "order_number INTEGER NOT NULL," +
            "items TEXT NOT NULL," +
            "total INTEGER NOT NULL," +
            "created_at TEXT NOT NULL)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // حافظ على البيانات الموجودة عند تحديث التطبيق.
    }

    public long saveOrder(String items, int total) {
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
            "yyyy-MM-dd HH:mm:ss",
            Locale.getDefault()
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
            "SELECT COUNT(*) FROM orders",
            null
        );

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }
}

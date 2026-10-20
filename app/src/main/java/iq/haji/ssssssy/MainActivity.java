
package iq.haji.ssssssy;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;

public class MainActivity extends Activity {

    private final int ORANGE = Color.rgb(232, 93, 4);
    private final int RED = Color.rgb(214, 40, 40);
    private final int BACKGROUND = Color.rgb(255, 248, 240);
    private final int DARK = Color.rgb(41, 35, 33);
    private final int MUTED = Color.rgb(119, 107, 101);
    private final int WHITE = Color.WHITE;
    private final int GREEN = Color.rgb(35, 134, 54);

    private static final int EXPORT_REQUEST = 7001;

    private DatabaseHelper database;
    private LinearLayout root;
    private LinearLayout categoryList;
    private LinearLayout dishList;
    private LinearLayout cartList;
    private TextView totalText;
    private TextView countText;
    private TextView categoryTitle;
    private EditText phoneInput;

    private long selectedCategoryId = -1;
    private String selectedCategoryName = "";

    private final ArrayList<Category> categories = new ArrayList<>();
    private final LinkedHashMap<Long, CartItem> cart = new LinkedHashMap<>();

    private static class Category {
        long id;
        String name;

        Category(long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private static class CartItem {
        long dishId;
        String name;
        int price;
        int quantity;

        CartItem(long dishId, String name, int price) {
            this.dishId = dishId;
            this.name = name;
            this.price = price;
            this.quantity = 1;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = new DatabaseHelper(this);
        buildScreen();
        loadCategories();
        updateOrderCount();
    }

    private void buildScreen() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackgroundColor(BACKGROUND);
        root.setPadding(dp(10), dp(8), dp(10), dp(8));
        setContentView(root);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(12), dp(7), dp(12), dp(7));
        header.setBackground(round(ORANGE, 14));

        TextView brand = text("🍕 مطعم الحجي", 22, WHITE, true);
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));

        countText = text("الطلبات: 0", 14, WHITE, true);
        header.addView(countText);

        root.addView(header, margins(-1, -2, 0, 0, 0, 7));

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);

        Button addCategory = button("+ قسم", DARK);
        addCategory.setOnClickListener(v -> showAddCategoryDialog());

        Button addDish = button("+ طبق", ORANGE);
        addDish.setOnClickListener(v -> showAddDishDialog());

        Button manageDish = button("تعديل المنيو", RED);
        manageDish.setOnClickListener(v -> showManageDishesDialog());

        Button report = button("تقرير اليوم", GREEN);
        report.setOnClickListener(v -> showDailyReport());

        toolbar.addView(addCategory, new LinearLayout.LayoutParams(0, dp(40), 1));
        toolbar.addView(addDish, margins(0, dp(40), 4, 0, 4, 0));
        toolbar.addView(manageDish, new LinearLayout.LayoutParams(0, dp(40), 1));
        toolbar.addView(report, margins(0, dp(40), 4, 0, 0, 0));

        root.addView(toolbar, margins(-1, -2, 0, 0, 0, 7));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout menuPanel = panel();
        menuPanel.setOrientation(LinearLayout.VERTICAL);
        menuPanel.setPadding(dp(8), dp(8), dp(8), dp(8));

        categoryTitle = text("الأطباق", 18, DARK, true);
        menuPanel.addView(categoryTitle, margins(-1, -2, 0, 0, 0, 5));

        HorizontalScrollView categoryScroll = new HorizontalScrollView(this);
        categoryScroll.setHorizontalScrollBarEnabled(false);
        categoryList = new LinearLayout(this);
        categoryList.setOrientation(LinearLayout.HORIZONTAL);
        categoryScroll.addView(categoryList);
        menuPanel.addView(categoryScroll, margins(-1, -2, 0, 0, 0, 5));

        ScrollView dishScroll = new ScrollView(this);
        dishList = new LinearLayout(this);
        dishList.setOrientation(LinearLayout.VERTICAL);
        dishScroll.addView(dishList);
        menuPanel.addView(dishScroll, new LinearLayout.LayoutParams(-1, 0, 1));

        content.addView(menuPanel, new LinearLayout.LayoutParams(0, -1, 1.2f));

        LinearLayout cartPanel = panel();
        cartPanel.setOrientation(LinearLayout.VERTICAL);
        cartPanel.setPadding(dp(8), dp(8), dp(8), dp(8));

        cartPanel.addView(
            text("الطلب الحالي", 18, DARK, true),
            margins(-1, -2, 0, 0, 0, 4)
        );

        phoneInput = new EditText(this);
        phoneInput.setSingleLine(true);
        phoneInput.setTextSize(15);
        phoneInput.setInputType(InputType.TYPE_CLASS_PHONE);
        phoneInput.setHint("رقم الزبون");
        phoneInput.setTextDirection(View.TEXT_DIRECTION_LTR);
        phoneInput.setPadding(dp(10), dp(4), dp(10), dp(4));
        cartPanel.addView(phoneInput, margins(-1, dp(46), 0, 0, 0, 5));

        ScrollView cartScroll = new ScrollView(this);
        cartList = new LinearLayout(this);
        cartList.setOrientation(LinearLayout.VERTICAL);
        cartScroll.addView(cartList);
        cartPanel.addView(cartScroll, new LinearLayout.LayoutParams(-1, 0, 1));

        totalText = text("الإجمالي: 0 د.ع", 18, ORANGE, true);
        totalText.setGravity(Gravity.CENTER);
        cartPanel.addView(totalText, margins(-1, -2, 0, 5, 0, 5));

        Button save = button("حفظ الطلب", GREEN);
        save.setOnClickListener(v -> saveCurrentOrder());
        cartPanel.addView(save, margins(-1, dp(44), 0, 0, 0, 5));

        Button recent = button("الطلبات وسجل الإلغاء", DARK);
        recent.setOnClickListener(v -> showRecentOrders());
        cartPanel.addView(recent, margins(-1, dp(40), 0, 0, 0, 5));

        Button export = button("تصدير طلبات اليوم CSV", ORANGE);
        export.setOnClickListener(v -> startExport());
        cartPanel.addView(export, margins(-1, dp(40), 0, 0, 0, 0));

        content.addView(cartPanel, new LinearLayout.LayoutParams(0, -1, 0.9f));
    }

    private void loadCategories() {
        categories.clear();
        Cursor cursor = database.getCategories();

        try {
            while (cursor.moveToNext()) {
                categories.add(new Category(cursor.getLong(0), cursor.getString(1)));
            }
        } finally {
            cursor.close();
        }

        renderCategories();

        if (categories.isEmpty()) {
            selectedCategoryId = -1;
            selectedCategoryName = "";
            renderDishes();
            return;
        }

        Category selected = null;
        for (Category category : categories) {
            if (category.id == selectedCategoryId) {
                selected = category;
                break;
            }
        }

        if (selected == null) {
            selected = categories.get(0);
            for (Category category : categories) {
                if ("العروضات".equals(category.name)) {
                    selected = category;
                    break;
                }
            }
        }

        selectCategory(selected);
    }

    private void renderCategories() {
        categoryList.removeAllViews();

        for (Category category : categories) {
            Button b = button(
                category.name,
                category.id == selectedCategoryId ? ORANGE : DARK
            );
            b.setTextSize(13);
            b.setOnClickListener(v -> selectCategory(category));
            categoryList.addView(b, margins(-2, dp(38), 0, 0, 4, 0));
        }
    }

    private void selectCategory(Category category) {
        selectedCategoryId = category.id;
        selectedCategoryName = category.name;
        categoryTitle.setText("أطباق " + category.name);
        renderCategories();
        renderDishes();
    }

    private void renderDishes() {
        dishList.removeAllViews();

        if (selectedCategoryId < 0) {
            dishList.addView(text("أضف قسماً أولاً.", 15, MUTED, false));
            return;
        }

        Cursor cursor = database.getDishesByCategory(selectedCategoryId);
        boolean found = false;

        try {
            while (cursor.moveToNext()) {
                found = true;
                long id = cursor.getLong(0);
                String name = cursor.getString(1);
                int price = cursor.getInt(2);

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(8), dp(5), dp(8), dp(5));
                row.setBackground(round(WHITE, 10));
                dishList.addView(row, margins(-1, -2, 0, 0, 0, 5));

                LinearLayout labels = new LinearLayout(this);
                labels.setOrientation(LinearLayout.VERTICAL);
                labels.addView(text(name, 16, DARK, true));
                labels.addView(text(
                    String.format(Locale.getDefault(), "%,d د.ع", price),
                    13, MUTED, false
                ));
                row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));

                Button add = button("+", ORANGE);
                add.setTextSize(20);
                add.setOnClickListener(v -> addToCart(id, name, price));
                row.addView(add, new LinearLayout.LayoutParams(dp(44), dp(40)));
            }
        } finally {
            cursor.close();
        }

        if (!found) {
            TextView empty = text("القسم فارغ. استخدم زر + طبق.", 15, MUTED, false);
            empty.setGravity(Gravity.CENTER);
            dishList.addView(empty, margins(-1, -2, 0, 18, 0, 0));
        }
    }

    private void addToCart(long id, String name, int price) {
        CartItem item = cart.get(id);
        if (item == null) cart.put(id, new CartItem(id, name, price));
        else item.quantity++;
        renderCart();
    }

    private void renderCart() {
        cartList.removeAllViews();
        long total = 0;

        for (CartItem item : cart.values()) {
            total += (long) item.price * item.quantity;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            cartList.addView(row, margins(-1, -2, 0, 0, 0, 4));

            TextView details = text(
                item.name + "\n" + item.quantity + " × " + item.price +
                " = " + ((long) item.quantity * item.price) + " د.ع",
                13, DARK, true
            );
            row.addView(details, new LinearLayout.LayoutParams(0, -2, 1));

            Button minus = button("−", RED);
            minus.setOnClickListener(v -> {
                if (item.quantity <= 1) cart.remove(item.dishId);
                else item.quantity--;
                renderCart();
            });
            row.addView(minus, new LinearLayout.LayoutParams(dp(38), dp(38)));
        }

        totalText.setText("الإجمالي: " +
            String.format(Locale.getDefault(), "%,d", total) + " د.ع");
    }

    private void showAddCategoryDialog() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("اسم القسم");

        new AlertDialog.Builder(this)
            .setTitle("إضافة قسم")
            .setView(input)
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("حفظ", (d, w) -> {
                String name = input.getText().toString().trim();
                if (name.isEmpty()) {
                    toast("اكتب اسم القسم");
                    return;
                }

                long id = database.addCategory(name);
                if (id == -1) {
                    toast("تعذرت الإضافة. ربما الاسم موجود.");
                } else {
                    loadCategories();
                    for (Category c : categories) {
                        if (c.id == id) {
                            selectCategory(c);
                            break;
                        }
                    }
                    toast("تم حفظ القسم");
                }
            }).show();
    }

    private void showAddDishDialog() {
        if (selectedCategoryId < 0) {
            toast("أضف قسماً أولاً");
            return;
        }

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(18), dp(6), dp(18), dp(4));

        EditText name = new EditText(this);
        name.setSingleLine(true);
        name.setHint("اسم الطبق");
        form.addView(name);

        EditText price = new EditText(this);
        price.setSingleLine(true);
        price.setInputType(InputType.TYPE_CLASS_NUMBER);
        price.setHint("السعر بالدينار");
        form.addView(price);

        new AlertDialog.Builder(this)
            .setTitle("إضافة طبق — " + selectedCategoryName)
            .setView(form)
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("حفظ", (d, w) -> {
                String n = name.getText().toString().trim();
                String p = price.getText().toString().trim();

                if (n.isEmpty() || p.isEmpty()) {
                    toast("اكتب الاسم والسعر");
                    return;
                }

                try {
                    int value = Integer.parseInt(p);
                    if (value < 0) {
                        toast("السعر غير صحيح");
                        return;
                    }

                    if (database.addDish(n, value, selectedCategoryId) == -1) {
                        toast("تعذر حفظ الطبق");
                    } else {
                        renderDishes();
                        toast("تم حفظ الطبق");
                    }
                } catch (NumberFormatException e) {
                    toast("السعر غير صحيح");
                }
            }).show();
    }

    private void showManageDishesDialog() {
        if (selectedCategoryId < 0) {
            toast("اختر قسماً أولاً");
            return;
        }

        ArrayList<Long> ids = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();
        Cursor cursor = database.getDishesByCategory(selectedCategoryId);

        try {
            while (cursor.moveToNext()) {
                ids.add(cursor.getLong(0));
                names.add(cursor.getString(1) + " — " + cursor.getInt(2) + " د.ع");
            }
        } finally {
            cursor.close();
        }

        if (ids.isEmpty()) {
            toast("ماكو أطباق بهذا القسم");
            return;
        }

        new AlertDialog.Builder(this)
            .setTitle("اختار الطبق")
            .setItems(names.toArray(new String[0]),
                (d, which) -> showDishActions(ids.get(which)))
            .setNegativeButton("إغلاق", null)
            .show();
    }

    private void showDishActions(long id) {
        Cursor cursor = database.getReadableDatabase().rawQuery(
            "SELECT name, price FROM dishes WHERE id = ?",
            new String[]{String.valueOf(id)}
        );

        if (!cursor.moveToFirst()) {
            cursor.close();
            toast("الطبق غير موجود");
            return;
        }

        String name = cursor.getString(0);
        int price = cursor.getInt(1);
        cursor.close();

        new AlertDialog.Builder(this)
            .setTitle(name)
            .setItems(new String[]{"تعديل الاسم والسعر", "حذف من المنيو"},
                (d, which) -> {
                    if (which == 0) showEditDishDialog(id, name, price);
                    else confirmDeleteDish(id, name);
                })
            .setNegativeButton("رجوع", null)
            .show();
    }

    private void showEditDishDialog(long id, String oldName, int oldPrice) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(18), dp(6), dp(18), dp(4));

        EditText name = new EditText(this);
        name.setSingleLine(true);
        name.setText(oldName);
        name.setHint("اسم الطبق");
        form.addView(name);

        EditText price = new EditText(this);
        price.setSingleLine(true);
        price.setInputType(InputType.TYPE_CLASS_NUMBER);
        price.setText(String.valueOf(oldPrice));
        price.setHint("السعر");
        form.addView(price);

        new AlertDialog.Builder(this)
            .setTitle("تعديل الطبق")
            .setView(form)
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("حفظ", (d, w) -> {
                String n = name.getText().toString().trim();
                String p = price.getText().toString().trim();

                if (n.isEmpty() || p.isEmpty()) {
                    toast("أكمل الاسم والسعر");
                    return;
                }

                try {
                    int value = Integer.parseInt(p);
                    if (value < 0) {
                        toast("السعر غير صحيح");
                        return;
                    }

                    if (database.updateDish(id, n, value) > 0) {
                        CartItem item = cart.get(id);
                        if (item != null) {
                            item.name = n;
                            item.price = value;
                            renderCart();
                        }
                        renderDishes();
                        toast("تم تعديل الطبق");
                    } else {
                        toast("لم يتم تعديل الطبق");
                    }
                } catch (NumberFormatException e) {
                    toast("السعر غير صحيح");
                }
            }).show();
    }

    private void confirmDeleteDish(long id, String name) {
        new AlertDialog.Builder(this)
            .setTitle("حذف الطبق من المنيو")
            .setMessage("سيختفي الطبق من المنيو. تفاصيل الطلبات المحفوظة تبقى كما هي.\n\n" + name)
            .setNegativeButton("رجوع", null)
            .setPositiveButton("حذف", (d, w) -> {
                int result = database.deleteDish(id);
                cart.remove(id);
                renderCart();
                renderDishes();
                toast(result > 0 ? "تم حذف الطبق من المنيو" : "تعذر الحذف");
            }).show();
    }

    private void saveCurrentOrder() {
        if (cart.isEmpty()) {
            toast("أضف طبقاً للطلب أولاً");
            return;
        }

        StringBuilder items = new StringBuilder();
        long total = 0;

        for (CartItem item : cart.values()) {
            long lineTotal = (long) item.price * item.quantity;
            total += lineTotal;

            items.append(item.name)
                .append(" × ").append(item.quantity)
                .append(" = ").append(lineTotal).append(" د.ع\n");
        }

        if (total > Integer.MAX_VALUE) {
            toast("الإجمالي أكبر من الحد المسموح");
            return;
        }

        String phone = phoneInput.getText().toString().trim();
        long orderId = database.saveOrder(items.toString(), (int) total, phone);

        if (orderId == -1) {
            toast("تعذر حفظ الطلب");
            return;
        }

        cart.clear();
        phoneInput.setText("");
        renderCart();
        updateOrderCount();

        new AlertDialog.Builder(this)
            .setTitle("تم حفظ الطلب")
            .setMessage("رقم الزبون: " + (phone.isEmpty() ? "غير مسجل" : phone) +
                "\nالإجمالي: " + String.format(Locale.getDefault(), "%,d", total) +
                " د.ع\nتم حفظ الطلب داخل الجهاز.")
            .setPositiveButton("حسناً", null)
            .show();
    }

    private void showRecentOrders() {
        Cursor cursor = database.getRecentOrdersDetailed();
        ArrayList<Long> ids = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        StringBuilder all = new StringBuilder();

        try {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                int number = cursor.getInt(1);
                String items = cursor.getString(2);
                int total = cursor.getInt(3);
                String date = cursor.getString(4);
                String phone = cursor.getString(5);
                String status = cursor.getString(6);
                String reason = cursor.getString(7);

                String label = "طلب #" + number + " | " +
                    String.format(Locale.getDefault(), "%,d", total) +
                    " د.ع | " + status;

                ids.add(id);
                labels.add(label);

                all.append(label).append("\n")
                    .append(date).append("\n")
                    .append("رقم الزبون: ")
                    .append(phone.isEmpty() ? "غير مسجل" : phone)
                    .append("\n").append(items);

                if ("ملغي".equals(status)) {
                    all.append("سبب الإلغاء: ").append(reason).append("\n");
                }
                all.append("-------------------------\n");
            }
        } finally {
            cursor.close();
        }

        if (ids.isEmpty()) {
            toast("ماكو طلبات محفوظة");
            return;
        }

        new AlertDialog.Builder(this)
            .setTitle("الطلبات — اضغط على طلب لإجراء عملية")
            .setItems(labels.toArray(new String[0]), (d, which) -> {
                showOrderActions(ids.get(which));
            })
            .setNeutralButton("عرض التفاصيل", (d, w) -> showOrdersText(all.toString()))
            .setNegativeButton("إغلاق", null)
            .show();
    }

    private void showOrdersText(String text) {
        ScrollView scroll = new ScrollView(this);
        TextView body = text(text, 14, DARK, false);
        body.setPadding(dp(12), dp(8), dp(12), dp(8));
        scroll.addView(body);

        new AlertDialog.Builder(this)
            .setTitle("تفاصيل الطلبات")
            .setView(scroll)
            .setPositiveButton("إغلاق", null)
            .show();
    }

    private void showOrderActions(long id) {
        Cursor cursor = database.getReadableDatabase().rawQuery(
            "SELECT order_number, total, status FROM orders WHERE id = ?",
            new String[]{String.valueOf(id)}
        );

        if (!cursor.moveToFirst()) {
            cursor.close();
            toast("الطلب غير موجود");
            return;
        }

        int number = cursor.getInt(0);
        int total = cursor.getInt(1);
        String status = cursor.getString(2);
        cursor.close();

        if ("ملغي".equals(status)) {
            toast("هذا الطلب ملغي مسبقاً");
            return;
        }

        new AlertDialog.Builder(this)
            .setTitle("طلب رقم " + number)
            .setMessage("الإجمالي: " +
                String.format(Locale.getDefault(), "%,d", total) + " د.ع")
            .setPositiveButton("إلغاء الطلب", (d, w) -> showCancelReason(id, number))
            .setNegativeButton("رجوع", null)
            .show();
    }

    private void showCancelReason(long id, int number) {
        EditText reason = new EditText(this);
        reason.setHint("اكتب سبب إلغاء الطلب");
        reason.setMinLines(2);

        new AlertDialog.Builder(this)
            .setTitle("إلغاء الطلب رقم " + number)
            .setMessage("سيبقى الطلب محفوظاً في السجل.")
            .setView(reason)
            .setNegativeButton("رجوع", null)
            .setPositiveButton("تأكيد الإلغاء", (d, w) -> {
                String value = reason.getText().toString().trim();

                if (value.isEmpty()) {
                    toast("سبب الإلغاء مطلوب");
                    return;
                }

                int result = database.cancelOrder(id, value);
                if (result > 0) {
                    database.logOperation(
                        "تنبيه: إلغاء من الواجهة",
                        "تم إلغاء الطلب رقم " + number
                    );
                    updateOrderCount();
                    toast("تم تسجيل إلغاء الطلب");
                } else {
                    toast("تعذر إلغاء الطلب");
                }
            }).show();
    }

    private void showDailyReport() {
        int sales = database.getTodaySales();
        int count = database.getTodayOrdersCount();

        Cursor cursor = database.getTodayOrders();
        int cancelled = 0;

        try {
            while (cursor.moveToNext()) {
                if ("ملغي".equals(cursor.getString(6))) cancelled++;
            }
        } finally {
            cursor.close();
        }

        String date = new SimpleDateFormat(
            "yyyy-MM-dd", Locale.getDefault()
        ).format(new Date());

        String message = "التاريخ: " + date +
            "\nعدد الطلبات: " + count +
            "\nالطلبات الملغاة: " + cancelled +
            "\nصافي المبيعات للطلبات غير الملغاة: " +
            String.format(Locale.getDefault(), "%,d", sales) + " د.ع" +
            "\n\nصدّر بيانات اليوم قبل أي تصفير.";

        new AlertDialog.Builder(this)
            .setTitle("تقرير اليوم")
            .setMessage(message)
            .setPositiveButton("تصدير CSV", (d, w) -> startExport())
            .setNegativeButton("إغلاق", null)
            .show();
    }

    private void startExport() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(
            Intent.EXTRA_TITLE,
            "haji-orders-" +
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date()) + ".csv"
        );
        startActivityForResult(intent, EXPORT_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != EXPORT_REQUEST || resultCode != RESULT_OK ||
            data == null || data.getData() == null) {
            return;
        }

        exportTodayCsv(data.getData());
    }

    private void exportTodayCsv(Uri uri) {
        Cursor cursor = database.getTodayOrders();

        try (OutputStream output = getContentResolver().openOutputStream(uri)) {
            if (output == null) {
                toast("تعذر فتح الملف");
                return;
            }

            String header = "رقم الطلب,التاريخ,رقم الزبون,الحالة,الإجمالي,التفاصيل,سبب الإلغاء\n";
            output.write(header.getBytes(StandardCharsets.UTF_8));

            while (cursor.moveToNext()) {
                String[] fields = {
                    String.valueOf(cursor.getInt(1)),
                    cursor.getString(4),
                    cursor.getString(5),
                    cursor.getString(6),
                    String.valueOf(cursor.getInt(3)),
                    cursor.getString(2),
                    cursor.getString(7)
                };

                StringBuilder line = new StringBuilder();
                for (int i = 0; i < fields.length; i++) {
                    if (i > 0) line.append(',');
                    line.append(csvEscape(fields[i]));
                }
                line.append("\r\n");
                output.write(line.toString().getBytes(StandardCharsets.UTF_8));
            }

            output.flush();
            toast("تم تصدير بيانات اليوم");
        } catch (Exception e) {
            toast("فشل التصدير: " + e.getMessage());
        } finally {
            cursor.close();
        }
    }

    private String csvEscape(String value) {
        if (value == null) value = "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private void updateOrderCount() {
        if (countText != null) {
            countText.setText("الطلبات: " + database.getOrdersCount());
        }
    }

    private LinearLayout panel() {
        LinearLayout view = new LinearLayout(this);
        view.setBackground(round(WHITE, 14));
        view.setElevation(dp(2));
        return view;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTextDirection(View.TEXT_DIRECTION_RTL);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setTypeface(
            Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL
        );
        return view;
    }

    private Button button(String value, int color) {
        Button view = new Button(this);
        view.setText(value);
        view.setTextSize(13);
        view.setTextColor(WHITE);
        view.setAllCaps(false);
        view.setMinHeight(0);
        view.setMinimumHeight(0);
        view.setPadding(dp(5), dp(3), dp(5), dp(3));
        view.setBackground(round(color, 9));
        return view;
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private LinearLayout.LayoutParams margins(
        int width, int height,
        int left, int top, int right, int bottom
    ) {
        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(width, height);
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private int dp(int value) {
        return (int) (value *
            getResources().getDisplayMetrics().density + 0.5f);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        if (database != null) database.close();
        super.onDestroy();
    }
}

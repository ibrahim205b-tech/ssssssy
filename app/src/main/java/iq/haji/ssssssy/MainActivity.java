package iq.haji.ssssssy;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.database.Cursor;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    private final int ORANGE = Color.rgb(232, 93, 4);
    private final int RED = Color.rgb(214, 40, 40);
    private final int BACKGROUND = Color.rgb(255, 248, 240);
    private final int DARK = Color.rgb(41, 35, 33);
    private final int MUTED = Color.rgb(119, 107, 101);
    private final int WHITE = Color.WHITE;
    private final int GREEN = Color.rgb(35, 134, 54);

    private DatabaseHelper database;

    private LinearLayout root;
    private LinearLayout categoryList;
    private LinearLayout dishList;
    private LinearLayout cartList;

    private TextView totalText;
    private TextView countText;
    private TextView categoryTitle;

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
        header.setPadding(dp(14), dp(8), dp(14), dp(8));
        header.setBackground(round(ORANGE, 14));

        TextView brand = text("🍕 مطعم الحجي", 23, WHITE, true);
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));

        countText = text("الطلبات: 0", 14, WHITE, true);
        header.addView(countText);

        root.addView(header, margins(-1, -2, 0, 0, 0, 8));

        LinearLayout management = new LinearLayout(this);
        management.setOrientation(LinearLayout.HORIZONTAL);
        management.setGravity(Gravity.CENTER_VERTICAL);

        Button addCategoryButton = button("+ قسم", DARK);
        addCategoryButton.setOnClickListener(v -> showAddCategoryDialog());

        Button addDishButton = button("+ طبق", ORANGE);
        addDishButton.setOnClickListener(v -> showAddDishDialog());

        Button manageDishButton = button("تعديل المنيو", RED);
        manageDishButton.setOnClickListener(v -> showManageDishesDialog());

        management.addView(addCategoryButton, new LinearLayout.LayoutParams(0, dp(42), 1));
        management.addView(addDishButton, margins(0, dp(42), 5, 0, 5, 0));
        management.addView(manageDishButton, new LinearLayout.LayoutParams(0, dp(42), 1));

        root.addView(management, margins(-1, -2, 0, 0, 0, 8));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));

        // لوحة الأطباق
        LinearLayout menuPanel = panel();
        menuPanel.setOrientation(LinearLayout.VERTICAL);
        menuPanel.setPadding(dp(9), dp(9), dp(9), dp(9));

        categoryTitle = text("الأطباق", 19, DARK, true);
        menuPanel.addView(categoryTitle, margins(-1, -2, 0, 0, 0, 6));

        HorizontalScrollView categoryScroll = new HorizontalScrollView(this);
        categoryScroll.setHorizontalScrollBarEnabled(false);

        categoryList = new LinearLayout(this);
        categoryList.setOrientation(LinearLayout.HORIZONTAL);
        categoryList.setPadding(0, 0, 0, dp(4));

        categoryScroll.addView(categoryList);
        menuPanel.addView(categoryScroll, margins(-1, -2, 0, 0, 0, 6));

        ScrollView dishScroll = new ScrollView(this);
        dishList = new LinearLayout(this);
        dishList.setOrientation(LinearLayout.VERTICAL);
        dishScroll.addView(dishList);

        menuPanel.addView(dishScroll, new LinearLayout.LayoutParams(-1, 0, 1));

        content.addView(menuPanel, new LinearLayout.LayoutParams(0, -1, 1.2f));

        // لوحة الطلب الحالي
        LinearLayout cartPanel = panel();
        cartPanel.setOrientation(LinearLayout.VERTICAL);
        cartPanel.setPadding(dp(9), dp(9), dp(9), dp(9));

        TextView cartTitle = text("الطلب الحالي", 19, DARK, true);
        cartPanel.addView(cartTitle, margins(-1, -2, 0, 0, 0, 6));

        ScrollView cartScroll = new ScrollView(this);
        cartList = new LinearLayout(this);
        cartList.setOrientation(LinearLayout.VERTICAL);
        cartScroll.addView(cartList);

        cartPanel.addView(cartScroll, new LinearLayout.LayoutParams(-1, 0, 1));

        totalText = text("الإجمالي: 0 د.ع", 19, ORANGE, true);
        totalText.setGravity(Gravity.CENTER);
        cartPanel.addView(totalText, margins(-1, -2, 0, 8, 0, 8));

        Button saveButton = button("حفظ الطلب", GREEN);
        saveButton.setOnClickListener(v -> saveCurrentOrder());
        cartPanel.addView(saveButton, margins(-1, dp(46), 0, 0, 0, 5));

        Button recentButton = button("آخر الطلبات", DARK);
        recentButton.setOnClickListener(v -> showRecentOrders());
        cartPanel.addView(recentButton, margins(-1, dp(42), 0, 0, 0, 0));

        content.addView(cartPanel, new LinearLayout.LayoutParams(0, -1, 0.9f));

        updateOrderCount();
    }

    private void loadCategories() {
        categories.clear();

        Cursor cursor = database.getCategories();
        try {
            while (cursor.moveToNext()) {
                categories.add(new Category(
                    cursor.getLong(0),
                    cursor.getString(1)
                ));
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

        Category offers = categories.get(0);
        for (Category category : categories) {
            if ("العروضات".equals(category.name)) {
                offers = category;
                break;
            }
        }

        selectCategory(offers);
    }

    private void renderCategories() {
        categoryList.removeAllViews();

        for (Category category : categories) {
            Button categoryButton = button(category.name, 
                category.id == selectedCategoryId ? ORANGE : DARK);

            categoryButton.setTextSize(14);
            categoryButton.setOnClickListener(v -> selectCategory(category));

            categoryList.addView(categoryButton, margins(-2, dp(40), 0, 0, 5, 0));
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
            dishList.addView(text("أضف قسماً حتى تبدأ بإضافة الأطباق.", 16, MUTED, false));
            return;
        }

        Cursor cursor = database.getDishesByCategory(selectedCategoryId);
        boolean hasDishes = false;

        try {
            while (cursor.moveToNext()) {
                hasDishes = true;

                long id = cursor.getLong(0);
                String name = cursor.getString(1);
                int price = cursor.getInt(2);

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(9), dp(7), dp(9), dp(7));
                row.setBackground(round(WHITE, 12));

                dishList.addView(row, margins(-1, -2, 0, 0, 0, 6));

                LinearLayout labels = new LinearLayout(this);
                labels.setOrientation(LinearLayout.VERTICAL);

                labels.addView(text(name, 17, DARK, true));
                labels.addView(text(
                    String.format(Locale.getDefault(), "%,d د.ع", price),
                    14, MUTED, false
                ));

                row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));

                Button addButton = button("+", ORANGE);
                addButton.setTextSize(21);
                addButton.setOnClickListener(v -> addToCart(id, name, price));

                row.addView(addButton, new LinearLayout.LayoutParams(dp(48), dp(42)));
            }
        } finally {
            cursor.close();
        }

        if (!hasDishes) {
            TextView empty = text(
                "هذا القسم فارغ حالياً.\nاستخدم زر + طبق لإضافة أول طبق.",
                16, MUTED, false
            );
            empty.setGravity(Gravity.CENTER);
            dishList.addView(empty, margins(-1, -2, 0, 20, 0, 0));
        }
    }

    private void addToCart(long id, String name, int price) {
        CartItem item = cart.get(id);

        if (item == null) {
            cart.put(id, new CartItem(id, name, price));
        } else {
            item.quantity++;
        }

        renderCart();
    }

    private void renderCart() {
        cartList.removeAllViews();
        int total = 0;

        for (CartItem item : cart.values()) {
            total += item.price * item.quantity;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            cartList.addView(row, margins(-1, -2, 0, 0, 0, 5));

            TextView details = text(
                item.name + "\n" + item.quantity + " × " +
                String.format(Locale.getDefault(), "%,d", item.price) +
                " = " + String.format(Locale.getDefault(), "%,d",
                item.quantity * item.price) + " د.ع",
                14, DARK, true
            );

            row.addView(details, new LinearLayout.LayoutParams(0, -2, 1));

            Button minus = button("−", RED);
            minus.setOnClickListener(v -> {
                if (item.quantity <= 1) {
                    cart.remove(item.dishId);
                } else {
                    item.quantity--;
                }
                renderCart();
            });

            row.addView(minus, new LinearLayout.LayoutParams(dp(40), dp(40)));
        }

        totalText.setText("الإجمالي: " +
            String.format(Locale.getDefault(), "%,d", total) + " د.ع");
    }

    private void showAddCategoryDialog() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("اسم القسم الجديد");

        new AlertDialog.Builder(this)
            .setTitle("إضافة قسم")
            .setView(input)
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("حفظ", (dialog, which) -> {
                String name = input.getText().toString().trim();

                if (name.isEmpty()) {
                    toast("اكتب اسم القسم");
                    return;
                }

                long result = database.addCategory(name);

                if (result == -1) {
                    toast("تعذر إضافة القسم. قد يكون الاسم موجوداً.");
                } else {
                    loadCategories();
                    for (Category category : categories) {
                        if (category.id == result) {
                            selectCategory(category);
                            break;
                        }
                    }
                    toast("تم حفظ القسم");
                }
            })
            .show();
    }

    private void showAddDishDialog() {
        if (selectedCategoryId < 0) {
            toast("أضف قسماً أولاً");
            return;
        }

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(8), dp(20), dp(4));

        TextView category = text("القسم: " + selectedCategoryName, 15, MUTED, true);
        form.addView(category, margins(-1, -2, 0, 0, 0, 8));

        EditText nameInput = new EditText(this);
        nameInput.setSingleLine(true);
        nameInput.setHint("اسم الطبق");
        form.addView(nameInput);

        EditText priceInput = new EditText(this);
        priceInput.setSingleLine(true);
        priceInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        priceInput.setHint("السعر بالدينار العراقي");
        form.addView(priceInput);

        new AlertDialog.Builder(this)
            .setTitle("إضافة طبق")
            .setView(form)
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("حفظ", (dialog, which) -> {
                String name = nameInput.getText().toString().trim();
                String priceString = priceInput.getText().toString().trim();

                if (name.isEmpty() || priceString.isEmpty()) {
                    toast("اكتب اسم الطبق والسعر");
                    return;
                }

                try {
                    int price = Integer.parseInt(priceString);
                    long result = database.addDish(name, price, selectedCategoryId);

                    if (result == -1) {
                        toast("تعذر حفظ الطبق");
                    } else {
                        renderDishes();
                        toast("تم حفظ الطبق");
                    }
                } catch (NumberFormatException exception) {
                    toast("السعر غير صحيح");
                }
            })
            .show();
    }

    private void showManageDishesDialog() {
        if (selectedCategoryId < 0) {
            toast("اختر قسماً أولاً");
            return;
        }

        ArrayList<Long> ids = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        Cursor cursor = database.getDishesByCategory(selectedCategoryId);
        try {
            while (cursor.moveToNext()) {
                ids.add(cursor.getLong(0));
                labels.add(cursor.getString(1) + " — " +
                    String.format(Locale.getDefault(), "%,d", cursor.getInt(2)) +
                    " د.ع");
            }
        } finally {
            cursor.close();
        }

        if (ids.isEmpty()) {
            toast("هذا القسم لا يحتوي أطباقاً");
            return;
        }

        new AlertDialog.Builder(this)
            .setTitle("اختر الطبق للتعديل أو الحذف")
            .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                showDishActions(ids.get(which));
            })
            .setNegativeButton("إغلاق", null)
            .show();
    }

    private void showDishActions(long dishId) {
        Cursor cursor = database.getReadableDatabase().rawQuery(
            "SELECT name, price FROM dishes WHERE id = ?",
            new String[]{String.valueOf(dishId)}
        );

        if (!cursor.moveToFirst()) {
            cursor.close();
            toast("الطبق غير موجود");
            renderDishes();
            return;
        }

        String name = cursor.getString(0);
        int price = cursor.getInt(1);
        cursor.close();

        new AlertDialog.Builder(this)
            .setTitle(name)
            .setItems(new String[]{"تعديل الاسم والسعر", "حذف الطبق"}, (dialog, which) -> {
                if (which == 0) {
                    showEditDishDialog(dishId, name, price);
                } else {
                    confirmDeleteDish(dishId, name);
                }
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void showEditDishDialog(long id, String oldName, int oldPrice) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(8), dp(20), dp(4));

        EditText nameInput = new EditText(this);
        nameInput.setSingleLine(true);
        nameInput.setHint("اسم الطبق");
        nameInput.setText(oldName);
        form.addView(nameInput);

        EditText priceInput = new EditText(this);
        priceInput.setSingleLine(true);
        priceInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        priceInput.setHint("السعر بالدينار");
        priceInput.setText(String.valueOf(oldPrice));
        form.addView(priceInput);

        new AlertDialog.Builder(this)
            .setTitle("تعديل الطبق")
            .setView(form)
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("حفظ التعديل", (dialog, which) -> {
                String name = nameInput.getText().toString().trim();
                String priceText = priceInput.getText().toString().trim();

                if (name.isEmpty() || priceText.isEmpty()) {
                    toast("أكمل الاسم والسعر");
                    return;
                }

                try {
                    int price = Integer.parseInt(priceText);
                    int result = database.updateDish(id, name, price);

                    if (result > 0) {
                        renderDishes();
                        toast("تم تعديل الطبق");
                    } else {
                        toast("لم يتم تعديل الطبق");
                    }
                } catch (NumberFormatException exception) {
                    toast("السعر غير صحيح");
                }
            })
            .show();
    }

    private void confirmDeleteDish(long id, String name) {
        new AlertDialog.Builder(this)
            .setTitle("حذف الطبق")
            .setMessage("تريد تحذف \"" + name + "\" من المنيو؟")
            .setNegativeButton("رجوع", null)
            .setPositiveButton("حذف", (dialog, which) -> {
                int result = database.deleteDish(id);
                cart.remove(id);
                renderCart();
                renderDishes();

                if (result > 0) {
                    toast("تم حذف الطبق من المنيو");
                } else {
                    toast("تعذر حذف الطبق");
                }
            })
            .show();
    }

    private void saveCurrentOrder() {
        if (cart.isEmpty()) {
            toast("أضف طبقاً للطلب أولاً");
            return;
        }

        StringBuilder details = new StringBuilder();
        int total = 0;

        for (CartItem item : cart.values()) {
            int lineTotal = item.price * item.quantity;
            total += lineTotal;

            details.append(item.name)
                .append(" × ").append(item.quantity)
                .append(" = ").append(lineTotal)
                .append(" د.ع\n");
        }

        long result = database.saveOrder(details.toString(), total);

        if (result != -1) {
            cart.clear();
            renderCart();
            updateOrderCount();

            new AlertDialog.Builder(this)
                .setTitle("تم حفظ الطلب")
                .setMessage("الإجمالي: " +
                    String.format(Locale.getDefault(), "%,d", total) +
                    " د.ع\nتم حفظ الطلب داخل الجهاز.")
                .setPositiveButton("حسناً", null)
                .show();
        } else {
            toast("تعذر حفظ الطلب");
        }
    }

    private void showRecentOrders() {
        Cursor cursor = database.getRecentOrders();
        StringBuilder result = new StringBuilder();

        try {
            while (cursor.moveToNext()) {
                result.append("طلب رقم ")
                    .append(cursor.getInt(1)).append("\n")
                    .append(cursor.getString(4)).append("\n")
                    .append(cursor.getString(2))
                    .append("الإجمالي: ")
                    .append(String.format(Locale.getDefault(), "%,d", cursor.getInt(3)))
                    .append(" د.ع\n")
                    .append("-------------------------\n");
            }
        } finally {
            cursor.close();
        }

        if (result.length() == 0) {
            result.append("ماكو طلبات محفوظة لحد الآن.");
        }

        ScrollView scroll = new ScrollView(this);
        TextView body = text(result.toString(), 15, DARK, false);
        body.setPadding(dp(12), dp(8), dp(12), dp(8));
        scroll.addView(body);

        new AlertDialog.Builder(this)
            .setTitle("آخر الطلبات")
            .setView(scroll)
            .setPositiveButton("إغلاق", null)
            .show();
    }

    private void updateOrderCount() {
        countText.setText("الطلبات: " + database.getOrdersCount());
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
        view.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        return view;
    }

    private Button button(String value, int color) {
        Button view = new Button(this);
        view.setText(value);
        view.setTextSize(14);
        view.setTextColor(WHITE);
        view.setAllCaps(false);
        view.setMinHeight(0);
        view.setMinimumHeight(0);
        view.setPadding(dp(8), dp(4), dp(8), dp(4));
        view.setBackground(round(color, 10));
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
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        if (database != null) {
            database.close();
        }
        super.onDestroy();
    }
}

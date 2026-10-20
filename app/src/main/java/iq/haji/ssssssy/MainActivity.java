package iq.haji.ssssssy;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
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

    private LinearLayout root;
    private LinearLayout menuList;
    private LinearLayout cartList;
    private TextView totalText;
    private TextView countText;

    private DatabaseHelper database;

    private final LinkedHashMap<String, Integer> menu = new LinkedHashMap<>();
    private final LinkedHashMap<String, Integer> cart = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        database = new DatabaseHelper(this);

        // أسعار تجريبية بالدينار العراقي. غيّرها حسب منيو المطعم.
        menu.put("بيتزا", 5000);
        menu.put("برغر", 4000);
        menu.put("شاورما", 3000);
        menu.put("فروج", 7000);
        menu.put("بطاطا", 1500);
        menu.put("بيبسي", 1000);

        buildScreen();
    }

    private void buildScreen() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackgroundColor(BACKGROUND);
        root.setPadding(dp(12), dp(8), dp(12), dp(8));

        setContentView(root);

        // الشريط العلوي
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(dp(14), dp(10), dp(14), dp(10));
        header.setBackground(round(ORANGE, 16));

        TextView brand = text("🍕 مطعم الحجي", 24, WHITE, true);
        header.addView(brand, new LinearLayout.LayoutParams(
            0, -2, 1
        ));

        countText = text("الطلبات: " + database.getOrdersCount(), 15, WHITE, true);
        header.addView(countText);

        root.addView(header, params(-1, -2, 0, 0, 0, 8));

        // محتوى الشاشة: الأطباق والسلة
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        root.addView(content, new LinearLayout.LayoutParams(
            -1, 0, 1
        ));

        // جهة قائمة الأطباق
        LinearLayout menuPanel = panel();
        menuPanel.setOrientation(LinearLayout.VERTICAL);
        menuPanel.setPadding(dp(10), dp(10), dp(10), dp(10));

        TextView menuTitle = text("الأطباق والأسعار", 20, DARK, true);
        menuPanel.addView(menuTitle, params(-1, -2, 0, 0, 0, 8));

        ScrollView menuScroll = new ScrollView(this);
        menuList = new LinearLayout(this);
        menuList.setOrientation(LinearLayout.VERTICAL);
        menuScroll.addView(menuList);

        menuPanel.addView(menuScroll, new LinearLayout.LayoutParams(
            -1, 0, 1
        ));

        content.addView(menuPanel, new LinearLayout.LayoutParams(
            0, -1, 1.2f
        ));

        // جهة السلة
        LinearLayout cartPanel = panel();
        cartPanel.setOrientation(LinearLayout.VERTICAL);
        cartPanel.setPadding(dp(10), dp(10), dp(10), dp(10));

        TextView cartTitle = text("الطلب الحالي", 20, DARK, true);
        cartPanel.addView(cartTitle, params(-1, -2, 0, 0, 0, 8));

        ScrollView cartScroll = new ScrollView(this);
        cartList = new LinearLayout(this);
        cartList.setOrientation(LinearLayout.VERTICAL);
        cartScroll.addView(cartList);

        cartPanel.addView(cartScroll, new LinearLayout.LayoutParams(
            -1, 0, 1
        ));

        totalText = text("الإجمالي: 0 د.ع", 20, ORANGE, true);
        totalText.setGravity(Gravity.CENTER);
        cartPanel.addView(totalText, params(-1, -2, 0, 8, 0, 8));

        Button saveButton = button("حفظ الطلب", ORANGE);
        saveButton.setOnClickListener(v -> saveCurrentOrder());
        cartPanel.addView(saveButton, params(-1, dp(48), 0, 0, 0, 6));

        Button recentButton = button("آخر الطلبات", DARK);
        recentButton.setOnClickListener(v -> showRecentOrders());
        cartPanel.addView(recentButton, params(-1, dp(44), 0, 0, 0, 0));

        content.addView(cartPanel, new LinearLayout.LayoutParams(
            0, -1, 0.9f
        ));

        renderMenu();
        renderCart();
    }

    private void renderMenu() {
        menuList.removeAllViews();

        for (Map.Entry<String, Integer> entry : menu.entrySet()) {
            String name = entry.getKey();
            int price = entry.getValue();

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(10), dp(8), dp(10), dp(8));
            row.setBackground(round(WHITE, 12));

            LinearLayout.LayoutParams rowParams =
                params(-1, -2, 0, 0, 0, 6);
            menuList.addView(row, rowParams);

            LinearLayout labels = new LinearLayout(this);
            labels.setOrientation(LinearLayout.VERTICAL);

            TextView nameText = text(name, 18, DARK, true);
            TextView priceText = text(
                String.format(Locale.getDefault(), "%,d د.ع", price),
                14, MUTED, false
            );

            labels.addView(nameText);
            labels.addView(priceText);

            row.addView(labels, new LinearLayout.LayoutParams(
                0, -2, 1
            ));

            Button addButton = button("+", ORANGE);
            addButton.setTextSize(22);
            addButton.setOnClickListener(v -> {
                int quantity = cart.containsKey(name) ? cart.get(name) : 0;
                cart.put(name, quantity + 1);
                renderCart();
            });

            row.addView(addButton, new LinearLayout.LayoutParams(
                dp(52), dp(44)
            ));
        }
    }

    private void renderCart() {
        cartList.removeAllViews();
        int total = 0;

        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            String name = entry.getKey();
            int quantity = entry.getValue();
            int price = menu.get(name);
            total += price * quantity;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(6), dp(8), dp(6), dp(8));

            cartList.addView(row, params(-1, -2, 0, 0, 0, 4));

            TextView itemText = text(
                name + "\n" + quantity + " × " + price + " = " +
                (quantity * price) + " د.ع",
                14, DARK, true
            );

            row.addView(itemText, new LinearLayout.LayoutParams(
                0, -2, 1
            ));

            Button minus = button("−", RED);
            minus.setTextSize(18);
            minus.setOnClickListener(v -> {
                int current = cart.containsKey(name) ? cart.get(name) : 0;
                if (current <= 1) {
                    cart.remove(name);
                } else {
                    cart.put(name, current - 1);
                }
                renderCart();
            });

            row.addView(minus, new LinearLayout.LayoutParams(
                dp(42), dp(40)
            ));
        }

        totalText.setText(
            "الإجمالي: " + String.format(Locale.getDefault(), "%,d", total) + " د.ع"
        );
    }

    private void saveCurrentOrder() {
        if (cart.isEmpty()) {
            Toast.makeText(this, "أضف طبقاً للطلب أولاً", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder details = new StringBuilder();
        int total = 0;

        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            String name = entry.getKey();
            int quantity = entry.getValue();
            int price = menu.get(name);

            details.append(name)
                .append(" × ")
                .append(quantity)
                .append(" = ")
                .append(quantity * price)
                .append(" د.ع\n");

            total += quantity * price;
        }

        long result = database.saveOrder(details.toString(), total);

        if (result != -1) {
            cart.clear();
            renderCart();
            countText.setText("الطلبات: " + database.getOrdersCount());

            new AlertDialog.Builder(this)
                .setTitle("تم حفظ الطلب")
                .setMessage("الإجمالي: " + total + " د.ع\nتم حفظ الطلب داخل الجهاز.")
                .setPositiveButton("حسناً", (dialog, which) -> {})
                .show();
        } else {
            Toast.makeText(this, "تعذر حفظ الطلب", Toast.LENGTH_LONG).show();
        }
    }

    private void showRecentOrders() {
        android.database.Cursor cursor = database.getRecentOrders();
        StringBuilder result = new StringBuilder();

        try {
            while (cursor.moveToNext()) {
                int number = cursor.getInt(1);
                String items = cursor.getString(2);
                int total = cursor.getInt(3);
                String date = cursor.getString(4);

                result.append("طلب رقم ").append(number).append("\n")
                    .append(date).append("\n")
                    .append(items)
                    .append("الإجمالي: ").append(total).append(" د.ع\n")
                    .append("-------------------------\n");
            }
        } finally {
            cursor.close();
        }

        if (result.length() == 0) {
            result.append("ماكو طلبات محفوظة لحد الآن.");
        }

        TextView body = text(result.toString(), 16, DARK, false);
        body.setPadding(dp(12), dp(8), dp(12), dp(8));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(body);

        new AlertDialog.Builder(this)
            .setTitle("آخر الطلبات")
            .setView(scroll)
            .setPositiveButton("إغلاق", (dialog, which) -> {})
            .show();
    }

    private LinearLayout panel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setBackground(round(WHITE, 16));
        panel.setElevation(dp(2));
        return panel;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setTextDirection(View.TEXT_DIRECTION_RTL);
        view.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        return view;
    }

    private Button button(String value, int color) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(15);
        button.setTextColor(WHITE);
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(8), dp(4), dp(8), dp(4));
        button.setBackground(round(color, 10));
        return button;
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private LinearLayout.LayoutParams params(
        int width, int height,
        int left, int top, int right, int bottom
    ) {
        LinearLayout.LayoutParams p =
            new LinearLayout.LayoutParams(width, height);
        p.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return p;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        if (database != null) {
            database.close();
        }
        super.onDestroy();
    }
}

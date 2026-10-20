
package iq.haji.ssssssy;

import android.app.role.RoleManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class CallEnabledMainActivity extends MainActivity {

    private static final int REQUEST_SCREENING_ROLE = 8091;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private EditText customerPhone;
    private Button activationButton;

    private boolean receiverRegistered = false;
    private boolean controlsAdded = false;
    private boolean watcherAdded = false;
    private boolean updatingIncomingNumber = false;

    private String lastSeenStoredNumber = "";

    private final BroadcastReceiver callerReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(
                        Context context,
                        Intent intent
                ) {
                    if (intent == null) {
                        return;
                    }

                    String number = intent.getStringExtra(
                            CallerScreeningService.EXTRA_CALLER_NUMBER
                    );

                    if (number != null && !number.trim().isEmpty()) {
                        showIncomingNumber(number.trim());
                    }
                }
            };

    private final Runnable refreshPendingNumber = new Runnable() {
        @Override
        public void run() {
            syncStoredNumber();
            handler.postDelayed(this, 700);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!receiverRegistered) {
            IntentFilter filter = new IntentFilter(
                    CallerScreeningService.ACTION_CALLER_NUMBER
            );

            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(
                        callerReceiver,
                        filter,
                        Context.RECEIVER_NOT_EXPORTED
                );
            } else {
                registerReceiver(callerReceiver, filter);
            }

            receiverRegistered = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        findCustomerPhoneField();

        if (customerPhone != null) {
            attachPhoneWatcher();
            addCallControls();
            applyCompactLayout(getWindow().getDecorView());
        }

        syncStoredNumber();
        updateActivationButton();

        handler.removeCallbacks(refreshPendingNumber);
        handler.post(refreshPendingNumber);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(refreshPendingNumber);
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (receiverRegistered) {
            try {
                unregisterReceiver(callerReceiver);
            } catch (IllegalArgumentException ignored) {
            }

            receiverRegistered = false;
        }

        super.onStop();
    }

    private void findCustomerPhoneField() {
        customerPhone = findPhoneInput(
                getWindow().getDecorView()
        );
    }

    private EditText findPhoneInput(View view) {
        if (view instanceof EditText) {
            EditText editText = (EditText) view;
            CharSequence hint = editText.getHint();

            if (hint != null &&
                    "رقم الزبون".contentEquals(hint)) {
                return editText;
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;

            for (int i = 0; i < group.getChildCount(); i++) {
                EditText result = findPhoneInput(
                        group.getChildAt(i)
                );

                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    private SharedPreferences callPreferences() {
        return getSharedPreferences(
                CallerScreeningService.PREFS_NAME,
                Context.MODE_PRIVATE
        );
    }

    private void syncStoredNumber() {
        if (customerPhone == null) {
            findCustomerPhoneField();
        }

        if (customerPhone == null) {
            return;
        }

        String number = callPreferences().getString(
                CallerScreeningService.KEY_PENDING_NUMBER,
                ""
        );

        if (number == null || number.trim().isEmpty()) {
            return;
        }

        number = number.trim();

        if (!number.equals(lastSeenStoredNumber)) {
            showIncomingNumber(number);
        }
    }

    private void showIncomingNumber(String number) {
        if (customerPhone == null) {
            findCustomerPhoneField();
        }

        if (customerPhone == null ||
                number == null ||
                number.trim().isEmpty()) {
            return;
        }

        lastSeenStoredNumber = number.trim();

        updatingIncomingNumber = true;

        customerPhone.setText(lastSeenStoredNumber);
        customerPhone.setSelection(
                customerPhone.getText().length()
        );

        updatingIncomingNumber = false;
    }

    private void attachPhoneWatcher() {
        if (customerPhone == null || watcherAdded) {
            return;
        }

        customerPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after
            ) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count
            ) {
                // عند إكمال الطلب ومسح الحقل أو مسحه يدوياً،
                // لا نعيد تعبئة الرقم القديم مرة أخرى.
                if (!updatingIncomingNumber &&
                        s != null &&
                        s.length() == 0 &&
                        !lastSeenStoredNumber.isEmpty()) {

                    callPreferences().edit()
                            .remove(
                                    CallerScreeningService.KEY_PENDING_NUMBER
                            )
                            .remove(
                                    CallerScreeningService.KEY_INCOMING_TIME
                            )
                            .apply();

                    lastSeenStoredNumber = "";
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        watcherAdded = true;
    }

    private void addCallControls() {
        if (customerPhone == null || controlsAdded) {
            return;
        }

        ViewParent viewParent = customerPhone.getParent();

        if (!(viewParent instanceof LinearLayout)) {
            return;
        }

        LinearLayout parent = (LinearLayout) viewParent;

        // منع تكرار الأزرار عند العودة إلى التطبيق.
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);

            if ("haji_call_controls".equals(child.getTag())) {
                controlsAdded = true;
                return;
            }
        }

        LinearLayout row = new LinearLayout(this);
        row.setTag("haji_call_controls");
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        activationButton = createActionButton(
                "تفعيل الاتصال",
                Color.rgb(232, 93, 4)
        );

        activationButton.setOnClickListener(
                v -> requestScreeningRole()
        );

        Button transferButton = createActionButton(
                "ترحيل الرقم",
                Color.rgb(214, 40, 40)
        );

        transferButton.setOnClickListener(v -> transferNumber());

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        0,
                        dp(36),
                        1
                );

        buttonParams.setMargins(dp(2), 0, dp(2), 0);

        row.addView(activationButton, buttonParams);

        LinearLayout.LayoutParams transferParams =
                new LinearLayout.LayoutParams(
                        0,
                        dp(36),
                        1
                );

        transferParams.setMargins(dp(2), 0, dp(2), 0);

        row.addView(transferButton, transferParams);

        int phoneIndex = parent.indexOfChild(customerPhone);

        LinearLayout.LayoutParams rowParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(40)
                );

        rowParams.setMargins(0, 0, 0, dp(4));

        parent.addView(row, phoneIndex + 1, rowParams);

        controlsAdded = true;
    }

    private Button createActionButton(
            String title,
            int backgroundColor
    ) {
        Button button = new Button(this);

        button.setText(title);
        button.setTextSize(11);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(4), 0, dp(4), 0);

        GradientDrawable background = new GradientDrawable();
        background.setColor(backgroundColor);
        background.setCornerRadius(dp(8));

        button.setBackground(background);

        return button;
    }

    private void transferNumber() {
        if (customerPhone == null) {
            findCustomerPhoneField();
        }

        updatingIncomingNumber = true;

        if (customerPhone != null) {
            customerPhone.setText("");
        }

        updatingIncomingNumber = false;

        callPreferences().edit()
                .remove(CallerScreeningService.KEY_PENDING_NUMBER)
                .remove(CallerScreeningService.KEY_INCOMING_TIME)
                .apply();

        lastSeenStoredNumber = "";

        Toast.makeText(
                this,
                "تم ترحيل الرقم دون إنشاء طلب",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void requestScreeningRole() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Toast.makeText(
                    this,
                    "يتطلب استقبال المكالمات أندرويد 10 أو أحدث",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        RoleManager roleManager = (RoleManager)
                getSystemService(Context.ROLE_SERVICE);

        if (roleManager == null ||
                !roleManager.isRoleAvailable(
                        RoleManager.ROLE_CALL_SCREENING
                )) {
            Toast.makeText(
                    this,
                    "خدمة فحص المكالمات غير متاحة على هذا الجهاز",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        if (roleManager.isRoleHeld(
                RoleManager.ROLE_CALL_SCREENING
        )) {
            updateActivationButton();

            Toast.makeText(
                    this,
                    "استقبال رقم المتصل مفعّل",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        startActivityForResult(
                roleManager.createRequestRoleIntent(
                        RoleManager.ROLE_CALL_SCREENING
                ),
                REQUEST_SCREENING_ROLE
        );
    }

    private void updateActivationButton() {
        if (activationButton == null) {
            return;
        }

        boolean enabled = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = (RoleManager)
                    getSystemService(Context.ROLE_SERVICE);

            enabled = roleManager != null &&
                    roleManager.isRoleHeld(
                            RoleManager.ROLE_CALL_SCREENING
                    );
        }

        activationButton.setText(
                enabled
                        ? "الاتصال مفعّل"
                        : "تفعيل الاتصال"
        );
    }

    private void applyCompactLayout(View view) {
        if (view instanceof Button) {
            Button button = (Button) view;

            button.setTextSize(
                    Math.min(button.getTextSize() /
                            getResources().getDisplayMetrics().scaledDensity,
                            13)
            );

            button.setPadding(dp(4), dp(2), dp(4), dp(2));

            ViewGroup.LayoutParams params = button.getLayoutParams();

            if (params != null &&
                    params.height > dp(38) &&
                    params.height <= dp(48)) {
                params.height = dp(36);
                button.setLayoutParams(params);
            }
        } else if (view instanceof TextView &&
                !(view instanceof EditText)) {

            TextView textView = (TextView) view;

            float size = textView.getTextSize() /
                    getResources().getDisplayMetrics().scaledDensity;

            if (size > 20) {
                textView.setTextSize(20);
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;

            for (int i = 0; i < group.getChildCount(); i++) {
                applyCompactLayout(group.getChildAt(i));
            }
        }
    }

    private int dp(int value) {
        return (int) (
                value * getResources().getDisplayMetrics().density
                        + 0.5f
        );
    }
}

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

    private static final String SETUP_PREFS = "ssssssy_call_setup";
    private static final String KEY_FIRST_ROLE_PROMPT_DONE =
            "first_role_prompt_done";

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private EditText customerPhone;

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

        // طلب صلاحية استقبال رقم المتصل عند أول تشغيل فقط.
        handler.postDelayed(
                this::requestScreeningRoleOnFirstLaunch,
                1000
        );
    }

    private void requestScreeningRoleOnFirstLaunch() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return;
        }

        SharedPreferences setupPreferences = getSharedPreferences(
                SETUP_PREFS,
                Context.MODE_PRIVATE
        );

        if (setupPreferences.getBoolean(
                KEY_FIRST_ROLE_PROMPT_DONE,
                false
        )) {
            return;
        }

        RoleManager roleManager = (RoleManager)
                getSystemService(Context.ROLE_SERVICE);

        if (roleManager == null) {
            setupPreferences.edit()
                    .putBoolean(KEY_FIRST_ROLE_PROMPT_DONE, true)
                    .apply();
            return;
        }

        if (roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
            setupPreferences.edit()
                    .putBoolean(KEY_FIRST_ROLE_PROMPT_DONE, true)
                    .apply();
            return;
        }

        if (!roleManager.isRoleAvailable(
                RoleManager.ROLE_CALL_SCREENING
        )) {
            setupPreferences.edit()
                    .putBoolean(KEY_FIRST_ROLE_PROMPT_DONE, true)
                    .apply();
            return;
        }

        // نسجل ظهور الطلب حتى لا يتكرر عند تشغيل التطبيق لاحقاً.
        setupPreferences.edit()
                .putBoolean(KEY_FIRST_ROLE_PROMPT_DONE, true)
                .apply();

        try {
            startActivityForResult(
                    roleManager.createRequestRoleIntent(
                            RoleManager.ROLE_CALL_SCREENING
                    ),
                    REQUEST_SCREENING_ROLE
            );
        } catch (Exception ignored) {
            // لا نوقف الكاشير إذا تعذر عرض نافذة الصلاحية.
        }
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

        Button transferButton = createActionButton(
                "ترحيل الرقم",
                Color.rgb(214, 40, 40)
        );

        transferButton.setOnClickListener(v -> transferNumber());

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(36)
                );

        buttonParams.setMargins(dp(2), 0, dp(2), 0);
        row.addView(transferButton, buttonParams);

        int phoneIndex = parent.indexOfChild(customerPhone);

        LinearLayout.LayoutParams rowParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(40)
                    }
                );

        rowParams.setMargins(0, 0, 0, dp(4));

        parent.addView(row, phoneIndex + 

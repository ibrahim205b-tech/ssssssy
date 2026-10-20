
package iq.haji.ssssssy;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.telecom.Call;
import android.telecom.CallScreeningService;

public class CallerScreeningService extends CallScreeningService {

    public static final String ACTION_CALLER_NUMBER =
            "iq.haji.ssssssy.ACTION_CALLER_NUMBER";

    public static final String EXTRA_CALLER_NUMBER =
            "caller_number";

    public static final String PREFS_NAME = "haji_calls";
    public static final String KEY_PENDING_NUMBER =
            "pending_incoming_phone";
    public static final String KEY_INCOMING_TIME =
            "incoming_time";

    @Override
    public void onScreenCall(Call.Details details) {
        if (details == null) {
            return;
        }

        CallResponse response = new CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build();

        // السماح للمكالمة بالاستمرار بشكل طبيعي.
        respondToCall(details, response);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return;
        }

        try {
            if (details.getCallDirection()
                    != Call.Details.DIRECTION_INCOMING) {
                return;
            }

            Uri handle = details.getHandle();

            if (handle == null ||
                    !"tel".equalsIgnoreCase(handle.getScheme())) {
                return;
            }

            String number = cleanNumber(
                    handle.getSchemeSpecificPart()
            );

            if (number.isEmpty()) {
                return;
            }

            SharedPreferences preferences =
                    getSharedPreferences(
                            PREFS_NAME,
                            MODE_PRIVATE
                    );

            preferences.edit()
                    .putString(KEY_PENDING_NUMBER, number)
                    .putLong(
                            KEY_INCOMING_TIME,
                            System.currentTimeMillis()
                    )
                    .apply();

            // إرسال الرقم إلى واجهة الكاشير إذا كانت فعالة.
            Intent intent = new Intent(ACTION_CALLER_NUMBER);
            intent.setPackage(getPackageName());
            intent.putExtra(EXTRA_CALLER_NUMBER, number);
            sendBroadcast(intent);

        } catch (Exception exception) {
            android.util.Log.e(
                    "HajiCallScreening",
                    "Unable to process incoming call",
                    exception
            );
        }
    }

    private String cleanNumber(String number) {
        if (number == null) {
            return "";
        }

        String value = number.trim();
        StringBuilder cleaned = new StringBuilder();

        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);

            if (Character.isDigit(character)) {
                cleaned.append(character);
            } else if (character == '+' &&
                    cleaned.length() == 0) {
                cleaned.append(character);
            }
        }

        return cleaned.toString();
    }
}

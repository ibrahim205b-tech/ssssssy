
package iq.haji.ssssssy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;

public class IncomingCallReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null ||
            !TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(
                intent.getAction())) {
            return;
        }

        String state = intent.getStringExtra(
            TelephonyManager.EXTRA_STATE
        );

        if (!TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
            return;
        }

        String number = intent.getStringExtra(
            TelephonyManager.EXTRA_INCOMING_NUMBER
        );

        if (number == null || number.trim().isEmpty()) {
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(
            "haji_calls", Context.MODE_PRIVATE
        );

        prefs.edit()
            .putString("pending_incoming_phone", number.trim())
            .putLong("incoming_time", System.currentTimeMillis())
            .apply();
    }
}

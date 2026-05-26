package com.arriva.touristguideapp.sos.manager;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import androidx.core.content.ContextCompat;
import com.arriva.touristguideapp.sos.utils.Logger;

public class SMSHelper {
    public static boolean sendSMS(Context context, String phoneNumber, String message) {
        if (context == null || phoneNumber == null || phoneNumber.isEmpty()) {
            Logger.e("SMS Failed: Context or phone number is null");
            return false;
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Logger.e("SMS Failed: SEND_SMS permission not granted");
            return false;
        }

        try {
            SmsManager smsManager = SmsManager.getDefault();
            if (smsManager == null) {
                Logger.e("SmsManager is null");
                return false;
            }
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Logger.i("SMS sent successfully to " + phoneNumber);
            return true;
        } catch (Exception e) {
            Logger.e("Failed to send SMS due to exception", e);
            return false;
        }
    }
}

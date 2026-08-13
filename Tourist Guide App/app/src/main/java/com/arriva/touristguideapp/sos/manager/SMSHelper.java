package com.arriva.touristguideapp.sos.manager;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import androidx.core.content.ContextCompat;
import com.arriva.touristguideapp.sos.utils.Logger;

import java.util.ArrayList;

public class SMSHelper {
    public static boolean sendSMS(Context context, String phoneNumber, String message) {
        if (context == null || phoneNumber == null || phoneNumber.isEmpty()) {
            Logger.e("SMS Failed: Context or phone number is null");
            return false;
        }

        if (message == null || message.trim().isEmpty()) {
            Logger.e("SMS Failed: Message is empty");
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

            ArrayList<String> parts = smsManager.divideMessage(message);
            if (parts == null || parts.isEmpty()) {
                Logger.e("SMS Failed: Could not divide message");
                return false;
            }

            if (parts.size() > SOSMessageBuilder.MAX_SMS_PARTS) {
                Logger.e("SMS Failed: Message exceeds maximum SMS parts");
                return false;
            }

            if (parts.size() == 1) {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            } else {
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null);
            }
            Logger.i("SMS sent successfully to " + phoneNumber);
            return true;
        } catch (Exception e) {
            Logger.e("Failed to send SMS due to exception", e);
            return false;
        }
    }
}

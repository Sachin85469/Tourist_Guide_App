package com.arriva.touristguideapp.sos.manager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import androidx.core.content.ContextCompat;
import com.arriva.touristguideapp.sos.utils.Logger;

public class CallHelper {
    public static void makeCall(Context context, String phoneNumber) {
        if (context == null || phoneNumber == null || phoneNumber.isEmpty()) {
            Logger.e("Cannot call: Context or phone number is null");
            return;
        }

        try {
            Intent intent;
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                Logger.d("Permission granted for ACTION_CALL");
                intent = new Intent(Intent.ACTION_CALL);
            } else {
                Logger.d("Permission NOT granted for ACTION_CALL, using ACTION_DIAL");
                intent = new Intent(Intent.ACTION_DIAL);
            }
            
            intent.setData(Uri.parse("tel:" + phoneNumber));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            context.startActivity(intent);
            Logger.i("Call initiated to " + phoneNumber);
        } catch (Exception e) {
            Logger.e("Failed to initiate call", e);
        }
    }
}

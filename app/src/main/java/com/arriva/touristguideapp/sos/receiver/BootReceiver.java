package com.arriva.touristguideapp.sos.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import androidx.core.content.ContextCompat;
import com.arriva.touristguideapp.sos.manager.SOSPreferences;
import com.arriva.touristguideapp.sos.service.FloatingSOSService;
import com.arriva.touristguideapp.sos.utils.Logger;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;
        
        Logger.d("BootReceiver onReceive: " + intent.getAction());
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            try {
                SOSPreferences prefs = new SOSPreferences(context);
                if (prefs.isFloatingButtonEnabled()) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        if (!Settings.canDrawOverlays(context)) {
                            Logger.d("Boot: Skipping floating service (no permission)");
                            return;
                        }
                    }
                    Intent serviceIntent = new Intent(context, FloatingSOSService.class);
                    ContextCompat.startForegroundService(context, serviceIntent);
                    Logger.d("Boot: Floating service started");
                }
            } catch (Exception e) {
                Logger.e("Error in BootReceiver", e);
            }
        }
    }
}

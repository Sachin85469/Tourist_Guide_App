package com.arriva.touristguideapp.sos.manager;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.arriva.touristguideapp.sos.service.SOSForegroundService;
import com.arriva.touristguideapp.sos.service.FloatingSOSService;
import com.arriva.touristguideapp.sos.utils.DebounceHelper;
import com.arriva.touristguideapp.sos.utils.Logger;

public class SOSManager {
    private static SOSManager instance;
    private final Context context;
    private final SOSPreferences preferences;
    private final DebounceHelper debounceHelper;
    private boolean isTriggering = false;

    private SOSManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = new SOSPreferences(this.context);
        this.debounceHelper = new DebounceHelper(10000); // 10s cooldown
    }

    public static synchronized SOSManager getInstance(Context context) {
        if (instance == null) {
            instance = new SOSManager(context);
        }
        return instance;
    }

    public void initialize() {
        Logger.d("SOSManager initializing");
        try {
            if (preferences.isFloatingButtonEnabled()) {
                startFloatingService();
            }
        } catch (Exception e) {
            Logger.e("Error during SOSManager initialization", e);
        }
    }

    private void startFloatingService() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(context)) {
                    Logger.d("Skipping floating service: No overlay permission");
                    return;
                }
            }
            Intent serviceIntent = new Intent(context, FloatingSOSService.class);
            ContextCompat.startForegroundService(context, serviceIntent);
            Logger.d("Floating service started");
        } catch (Exception e) {
            Logger.e("Failed to start floating service", e);
        }
    }

    public void triggerSOS(String source) {
        if (isTriggering) {
            Logger.d("SOS already in progress");
            return;
        }

        if (!debounceHelper.isAllowed()) {
            Logger.d("SOS triggered too soon. Cooldown active.");
            try {
                Toast.makeText(context, "Please wait before triggering SOS again", Toast.LENGTH_SHORT).show();
            } catch (Exception ignored) {}
            return;
        }

        isTriggering = true;
        Logger.i("SOS Triggered from: " + source);

        try {
            // Start foreground service to keep app alive
            Intent serviceIntent = new Intent(context, SOSForegroundService.class);
            ContextCompat.startForegroundService(context, serviceIntent);
        } catch (Exception e) {
            Logger.e("Failed to start SOSForegroundService", e);
        }

        // Proceed with SOS flow
        executeSOSFlow(source);
    }

    private void executeSOSFlow(String source) {
        try {
            String contact = preferences.getContactNumber();
            if (contact == null || contact.isEmpty()) {
                Logger.e("No emergency contact set!");
                Toast.makeText(context, "Emergency contact not set!", Toast.LENGTH_LONG).show();
                stopSOS();
                return;
            }

            LocationHelper locationHelper = new LocationHelper(context);
            locationHelper.getLastLocation(mapsLink -> {
                try {
                    String message = "EMERGENCY! I need help. My location: " + 
                        (mapsLink != null ? mapsLink : "Location unavailable");
                    
                    boolean smsSent = SMSHelper.sendSMS(context, contact, message);
                    
                    if (preferences.isCallAfterSMS()) {
                        CallHelper.makeCall(context, contact);
                    }

                    preferences.addLog("SOS triggered from " + source + ". Location: " + (mapsLink != null) + ". SMS: " + smsSent);
                    
                    try {
                        com.arriva.touristguideapp.data.notifications.NotificationRepository notificationRepository = 
                            new com.arriva.touristguideapp.data.notifications.NotificationRepository(context);
                        notificationRepository.addNotification(
                            "SOS Alert Activated",
                            "Emergency SOS alert was sent successfully to " + contact + ".",
                            com.arriva.touristguideapp.data.notifications.NotificationModel.TYPE_SOS
                        );
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    Logger.e("Error in SOS flow callback", e);
                } finally {
                    new Handler(Looper.getMainLooper()).postDelayed(this::stopSOS, 5000);
                }
            });
        } catch (Exception e) {
            Logger.e("Critical error in executeSOSFlow", e);
            stopSOS();
        }
    }

    private void stopSOS() {
        isTriggering = false;
        try {
            Intent serviceIntent = new Intent(context, SOSForegroundService.class);
            context.stopService(serviceIntent);
        } catch (Exception e) {
            Logger.e("Error stopping SOS service", e);
        }
    }
}

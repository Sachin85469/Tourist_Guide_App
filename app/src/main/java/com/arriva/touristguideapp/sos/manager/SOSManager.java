package com.arriva.touristguideapp.sos.manager;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.arriva.touristguideapp.data.sos.SOSContact;
import com.arriva.touristguideapp.sos.model.SOSEvent;
import com.arriva.touristguideapp.sos.service.SOSForegroundService;
import com.arriva.touristguideapp.sos.utils.DebounceHelper;
import com.arriva.touristguideapp.sos.utils.Logger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

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
                if (!android.provider.Settings.canDrawOverlays(context)) {
                    Logger.d("Skipping floating service: No overlay permission");
                    return;
                }
            }
            Intent serviceIntent = new Intent(context, com.arriva.touristguideapp.sos.service.FloatingSOSService.class);
            ContextCompat.startForegroundService(context, serviceIntent);
            Logger.d("Floating service started");
        } catch (Exception e) {
            Logger.e("Failed to start floating service", e);
        }
    }

    public void startSOSFlow(Context activeContext, String source) {
        if (preferences.isRequireConfirmation() && activeContext instanceof android.app.Activity) {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(activeContext)
                .setTitle("Emergency SOS")
                .setMessage("Are you sure you want to activate SOS?")
                .setPositiveButton("Activate SOS", (dialog, which) -> {
                    triggerSOS(source);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
        } else {
            triggerSOS(source);
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
            List<SOSContact> contacts = preferences.getContacts();
            
            // Check if emergency contacts are empty
            if (contacts == null || contacts.isEmpty()) {
                Logger.e("No emergency contacts set!");
                Toast.makeText(context, "No emergency contacts set! Calling 112 directly.", Toast.LENGTH_LONG).show();
                
                // Immediately call 112 even with empty contacts
                CallHelper.makeCall(context, "112");
                
                // Save empty trigger to log history
                logSOSEvent("Location unavailable", "No emergency contacts notified");
                stopSOS();
                return;
            }

            // Fetch live GPS location
            LocationHelper locationHelper = new LocationHelper(context);
            locationHelper.getLastLocation(mapsLink -> {
                try {
                    String locationText = (mapsLink != null) ? mapsLink : "Location unavailable";
                    
                    // Compose the message template precisely
                    String message = "EMERGENCY ALERT\n\n" +
                                     "I may need immediate assistance.\n\n" +
                                     "My current location:\n\n" +
                                     locationText + "\n\n" +
                                     "Sent from Smart Tourist Guide App";
                    
                    List<String> namesNotified = new ArrayList<>();
                    boolean anySmsSent = false;

                    // Send SMS automatically to all saved contacts
                    if (preferences.isSendSMSAutomatically()) {
                        for (SOSContact contact : contacts) {
                            if (contact.getPhone() != null && !contact.getPhone().trim().isEmpty()) {
                                boolean success = SMSHelper.sendSMS(context, contact.getPhone(), message);
                                if (success) {
                                    anySmsSent = true;
                                }
                                namesNotified.add(contact.getName() + " (" + contact.getPhone() + ")");
                            }
                        }
                    }

                    // Dial Emergency Services (112)
                    CallHelper.makeCall(context, "112");

                    // Join notified contacts list into a string
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < namesNotified.size(); i++) {
                        sb.append(namesNotified.get(i));
                        if (i < namesNotified.size() - 1) {
                            sb.append(", ");
                        }
                    }
                    String contactsNotifiedStr = sb.toString();
                    if (contactsNotifiedStr.isEmpty()) {
                        contactsNotifiedStr = "No emergency contacts notified";
                    }

                    // Save event log
                    logSOSEvent(locationText, contactsNotifiedStr);

                    // Add Notification to system
                    generateSystemNotification();

                    preferences.addLog("SOS triggered from " + source + ". Location: " + locationText + ". SMS Sent: " + anySmsSent);

                } catch (Exception e) {
                    Logger.e("Error in SOS flow location callback", e);
                } finally {
                    new Handler(Looper.getMainLooper()).postDelayed(this::stopSOS, 5000);
                }
            });

        } catch (Exception e) {
            Logger.e("Critical error in executeSOSFlow", e);
            stopSOS();
        }
    }

    private void logSOSEvent(String location, String contactsNotified) {
        try {
            SimpleDateFormat sdfDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            SimpleDateFormat sdfTime = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Date now = new Date();

            SOSEvent event = new SOSEvent(
                UUID.randomUUID().toString(),
                sdfDate.format(now),
                sdfTime.format(now),
                location,
                contactsNotified
            );
            preferences.addSosEvent(event);
        } catch (Exception e) {
            Logger.e("Failed to save SOSEvent log", e);
        }
    }

    private void generateSystemNotification() {
        try {
            com.arriva.touristguideapp.data.notifications.NotificationRepository notificationRepository = 
                new com.arriva.touristguideapp.data.notifications.NotificationRepository(context);
            notificationRepository.addNotification(
                "SOS activated successfully",
                "Emergency SOS alert was activated successfully. Dialed 112 and notified emergency contacts.",
                com.arriva.touristguideapp.data.notifications.NotificationModel.TYPE_SOS
            );
        } catch (Exception e) {
            Logger.e("Failed to post system notification for SOS", e);
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

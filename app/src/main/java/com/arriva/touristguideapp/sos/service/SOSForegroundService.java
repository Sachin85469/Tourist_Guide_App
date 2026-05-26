package com.arriva.touristguideapp.sos.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.arriva.touristguideapp.R;
import com.arriva.touristguideapp.sos.utils.Logger;

public class SOSForegroundService extends Service {
    private static final String CHANNEL_ID = "SOS_PROTECTION_CHANNEL";
    private static final int NOTIFICATION_ID = 101;

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.d("SOSForegroundService onCreate");
        createNotificationChannel();
        startForegroundSafe();
    }

    private void startForegroundSafe() {
        try {
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(getString(R.string.sos_notification_title))
                    .setContentText("Emergency monitoring is active")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setOngoing(true)
                    .build();

            startForeground(NOTIFICATION_ID, notification);
            Logger.d("startForeground called successfully");
        } catch (Exception e) {
            Logger.e("Failed to start foreground service", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.d("SOSForegroundService onStartCommand");
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.sos_notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

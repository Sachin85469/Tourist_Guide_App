package com.arriva.touristguideapp.sos.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.arriva.touristguideapp.R;
import com.arriva.touristguideapp.sos.manager.SOSManager;
import com.arriva.touristguideapp.sos.utils.Logger;

public class FloatingSOSService extends Service {
    private static final String CHANNEL_ID = "SOS_FLOATING_CHANNEL";
    private static final int NOTIFICATION_ID = 102;
    private WindowManager windowManager;
    private View floatingView;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.d("FloatingSOSService onCreate");
        createNotificationChannel();
        startForegroundSafe();
        initFloatingView();
    }

    private void startForegroundSafe() {
        try {
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("SOS Floating Button Active")
                    .setContentText("Tap to trigger emergency SOS")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build();
            startForeground(NOTIFICATION_ID, notification);
        } catch (Exception e) {
            Logger.e("Failed to start foreground for FloatingSOSService", e);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initFloatingView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Logger.e("Cannot show floating button: Overlay permission missing");
            stopSelf();
            return;
        }

        try {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            if (windowManager == null) {
                Logger.e("WindowManager is null");
                stopSelf();
                return;
            }

            floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_sos, null);

            int layoutType;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                layoutType = WindowManager.LayoutParams.TYPE_PHONE;
            }

            final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    layoutType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);

            params.gravity = Gravity.TOP | Gravity.START;
            params.x = 0;
            params.y = 200;

            windowManager.addView(floatingView, params);

            ImageView sosButton = floatingView.findViewById(R.id.sos_floating_icon);
            if (sosButton != null) {
                sosButton.setOnClickListener(v -> {
                    Logger.d("Floating button clicked");
                    SOSManager.getInstance(this).triggerSOS("Floating Button");
                });

                sosButton.setOnTouchListener(new View.OnTouchListener() {
                    private int initialX;
                    private int initialY;
                    private float initialTouchX;
                    private float initialTouchY;

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (windowManager == null || floatingView == null) return false;
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                initialX = params.x;
                                initialY = params.y;
                                initialTouchX = event.getRawX();
                                initialTouchY = event.getRawY();
                                return true;
                            case MotionEvent.ACTION_MOVE:
                                params.x = initialX + (int) (event.getRawX() - initialTouchX);
                                params.y = initialY + (int) (event.getRawY() - initialTouchY);
                                try {
                                    windowManager.updateViewLayout(floatingView, params);
                                } catch (Exception e) {
                                    Logger.e("Error updating floating view layout", e);
                                }
                                return true;
                            case MotionEvent.ACTION_UP:
                                int Xdiff = (int) (event.getRawX() - initialTouchX);
                                int Ydiff = (int) (event.getRawY() - initialTouchY);
                                if (Math.abs(Xdiff) < 10 && Math.abs(Ydiff) < 10) {
                                    v.performClick();
                                }
                                return true;
                        }
                        return false;
                    }
                });
            }
        } catch (Exception e) {
            Logger.e("Crash while creating floating SOS button", e);
            stopSelf();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "SOS Floating Button",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (floatingView != null && windowManager != null) {
                windowManager.removeView(floatingView);
            }
        } catch (Exception e) {
            Logger.e("Error removing floating view", e);
        }
    }
}

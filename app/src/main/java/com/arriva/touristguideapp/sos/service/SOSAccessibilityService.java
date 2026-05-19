package com.arriva.touristguideapp.sos.service;

import android.accessibilityservice.AccessibilityService;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import com.arriva.touristguideapp.sos.manager.SOSManager;
import com.arriva.touristguideapp.sos.manager.SOSPreferences;
import com.arriva.touristguideapp.sos.utils.Logger;

import java.util.ArrayList;
import java.util.List;

public class SOSAccessibilityService extends AccessibilityService {
    private final List<Long> pressTimestamps = new ArrayList<>();
    private static final int PRESS_COUNT_THRESHOLD = 3;
    private static final long TIME_WINDOW_MS = 1500;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Logger.d("SOSAccessibilityService connected");
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (event == null) return false;

        try {
            int keyCode = event.getKeyCode();
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    
                    SOSPreferences prefs = new SOSPreferences(this);
                    if (!prefs.isVolumeTriggerEnabled()) {
                        return super.onKeyEvent(event);
                    }

                    long currentTime = System.currentTimeMillis();
                    pressTimestamps.add(currentTime);

                    // Remove timestamps outside the window
                    while (!pressTimestamps.isEmpty() && currentTime - pressTimestamps.get(0) > TIME_WINDOW_MS) {
                        pressTimestamps.remove(0);
                    }

                    if (pressTimestamps.size() >= PRESS_COUNT_THRESHOLD) {
                        Logger.i("SOS triggered via Accessibility Service (Volume Buttons)");
                        pressTimestamps.clear();
                        SOSManager.getInstance(this).triggerSOS("Volume Buttons");
                    }
                }
            }
        } catch (Exception e) {
            Logger.e("Error handling key event in SOSAccessibilityService", e);
        }
        return super.onKeyEvent(event);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

    @Override
    public void onDestroy() {
        super.onDestroy();
        Logger.d("SOSAccessibilityService destroyed");
    }
}

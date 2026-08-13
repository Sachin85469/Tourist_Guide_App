package com.arriva.touristguideapp.sos.utils;

public class DebounceHelper {
    private long lastTriggerTime = 0;
    private final long cooldownMillis;

    public DebounceHelper(long cooldownMillis) {
        this.cooldownMillis = cooldownMillis;
    }

    public synchronized boolean isAllowed() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTriggerTime >= cooldownMillis) {
            lastTriggerTime = currentTime;
            return true;
        }
        return false;
    }
}

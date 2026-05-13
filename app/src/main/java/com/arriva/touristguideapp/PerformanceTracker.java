package com.arriva.touristguideapp;

import android.util.Log;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility for tracking app performance metrics.
 */
public class PerformanceTracker {
    private static final String TAG = "PerformanceTracker";
    private static final Map<String, Long> startTimeMap = new HashMap<>();

    public static void startTimer(String key) {
        startTimeMap.put(key, System.currentTimeMillis());
    }

    public static void endTimer(String key) {
        Long start = startTimeMap.remove(key);
        if (start != null) {
            long duration = System.currentTimeMillis() - start;
            Log.i(TAG, "PERFORMANCE_METRICS: " + key + " took " + duration + "ms");
        }
    }

    public static void logMetric(String key, String value) {
        Log.i(TAG, "PERFORMANCE_METRICS: " + key + " = " + value);
    }
}

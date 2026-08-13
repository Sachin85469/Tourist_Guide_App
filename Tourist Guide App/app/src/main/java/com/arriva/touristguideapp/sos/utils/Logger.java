package com.arriva.touristguideapp.sos.utils;

import android.util.Log;

public class Logger {
    private static final String TAG = "SOS_DEBUG";

    public static void d(String message) {
        Log.d(TAG, message != null ? message : "null");
    }

    public static void e(String message) {
        Log.e(TAG, message != null ? message : "null");
    }

    public static void e(String message, Throwable t) {
        Log.e(TAG, message != null ? message : "null", t);
    }

    public static void i(String message) {
        Log.i(TAG, message != null ? message : "null");
    }
}

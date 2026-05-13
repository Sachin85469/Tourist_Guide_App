package com.arriva.touristguideapp.data.notifications;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages notification tokens and preferences.
 */
public class NotificationRepository {
    private static final String TAG = "NotificationRepository";
    private static final String PREFS_NAME = "notification_prefs";
    private static final String KEY_FCM_TOKEN = "fcm_token";
    private static final String KEY_HISTORY = "notification_history";

    public static final String TYPE_NEARBY = "nearby";
    public static final String TYPE_TRENDING = "trending";
    public static final String TYPE_REVIEWS = "reviews";
    public static final String TYPE_FAVORITES = "favorites";
    public static final String TYPE_ADMIN = "admin";

    private final Context context;
    private final FirebaseFirestore db;
    private final com.google.gson.Gson gson;

    public NotificationRepository(Context context) {
        this.context = context.getApplicationContext();
        this.db = FirebaseFirestore.getInstance();
        this.gson = new com.google.gson.Gson();
    }

    public void registerToken(String token) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply();

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            db.collection("users").document(uid)
                    .update("fcmToken", token)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM_TOKEN_REGISTERED"))
                    .addOnFailureListener(e -> Log.e(TAG, "FAILED_TO_REGISTER_TOKEN", e));
        }
    }

    public void saveToHistory(NotificationModel notification) {
        java.util.List<NotificationModel> history = getHistory();
        history.add(0, notification);
        if (history.size() > 50) {
            history = history.subList(0, 50);
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_HISTORY, gson.toJson(history)).apply();
    }

    public java.util.List<NotificationModel> getHistory() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_HISTORY, null);
        if (json == null) return new java.util.ArrayList<>();
        java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<java.util.List<NotificationModel>>() {}.getType();
        return gson.fromJson(json, type);
    }

    public void setPreference(String type, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean("pref_" + type, enabled).apply();
    }

    public boolean isEnabled(String type) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean("pref_" + type, true);
    }

    public void logNotificationOpened(String notificationId) {
        Log.d(TAG, "NOTIFICATION_OPENED: " + notificationId);
        // In a real app, you might send this to analytics as well
    }
}

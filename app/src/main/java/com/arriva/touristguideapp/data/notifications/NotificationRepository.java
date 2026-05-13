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

    private final Context context;
    private final FirebaseFirestore db;

    public NotificationRepository(Context context) {
        this.context = context.getApplicationContext();
        this.db = FirebaseFirestore.getInstance();
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

    public void setPreference(String type, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean("pref_" + type, enabled).apply();
    }

    public boolean isEnabled(String type) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean("pref_" + type, true);
    }
}

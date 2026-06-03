package com.arriva.touristguideapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {
    private String currentLanguage;

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences("settings", Context.MODE_PRIVATE);
        String langCode = prefs.getString("language", "en");
        Context context = LocaleHelper.setLocale(newBase, langCode);
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = getSharedPreferences("settings", Context.MODE_PRIVATE);
        currentLanguage = prefs.getString("language", "en");
    }

    public static String getDeviceId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("device_settings", Context.MODE_PRIVATE);
        String deviceId = prefs.getString("device_id", null);
        if (deviceId == null) {
            deviceId = java.util.UUID.randomUUID().toString();
            prefs.edit().putString("device_id", deviceId).apply();
        }
        return deviceId;
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences("settings", Context.MODE_PRIVATE);
        String lang = prefs.getString("language", "en");
        if (!lang.equals(currentLanguage)) {
            recreate();
        }

        // Active session check for remote logout
        com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null && 
            !(this instanceof LoginActivity) && 
            !(this instanceof SignupActivity) && 
            !(this instanceof SplashActivity)) {
            
            String uid = auth.getCurrentUser().getUid();
            String deviceId = getDeviceId(this);
            
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("devices")
                .document(deviceId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().exists()) {
                        // The device was signed out remotely
                        auth.signOut();
                        
                        // Clear user profile caches
                        getSharedPreferences("user_profile_cache", Context.MODE_PRIVATE).edit().clear().apply();
                        getSharedPreferences("favorites", Context.MODE_PRIVATE).edit().clear().apply();
                        
                        android.widget.Toast.makeText(this, "Session terminated from this device.", android.widget.Toast.LENGTH_LONG).show();
                        
                        android.content.Intent intent = new android.content.Intent(this, LoginActivity.class);
                        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                });
        }
    }
}

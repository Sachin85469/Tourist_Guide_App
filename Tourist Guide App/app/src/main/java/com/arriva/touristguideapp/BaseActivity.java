package com.arriva.touristguideapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;

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

        if (requiresAuthentication()
                && FirebaseAuth.getInstance().getCurrentUser() == null) {
            redirectToLogin();
        }
    }

    protected boolean requiresAuthentication() {
        return !(this instanceof LoginActivity)
                && !(this instanceof SignupActivity)
                && !(this instanceof SplashActivity);
    }

    protected void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public static boolean isGoogleUser(@Nullable FirebaseUser user) {
        return hasProvider(user, GoogleAuthProvider.PROVIDER_ID);
    }

    public static boolean isEmailPasswordUser(@Nullable FirebaseUser user) {
        return hasProvider(user, EmailAuthProvider.PROVIDER_ID);
    }

    public static boolean canEnterMainActivity(@Nullable FirebaseUser user) {
        if (user == null) {
            return false;
        }
        if (isGoogleUser(user)) {
            return true;
        }
        return isEmailPasswordUser(user) && user.isEmailVerified();
    }

    private static boolean hasProvider(@Nullable FirebaseUser user, String providerId) {
        if (user == null) {
            return false;
        }
        for (UserInfo provider : user.getProviderData()) {
            if (providerId.equals(provider.getProviderId())) {
                return true;
            }
        }
        return false;
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
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (requiresAuthentication() && auth.getCurrentUser() == null) {
            redirectToLogin();
            return;
        }

        if (auth.getCurrentUser() != null && requiresAuthentication()) {
            
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
                        
                        redirectToLogin();
                    }
                });
        }
    }
}

package com.arriva.touristguideapp;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

/**
 * Application entry used to configure Firestore before Activities use it.
 * Persistence is enabled explicitly; if settings were already applied, we log and continue.
 */
public class TouristGuideApp extends Application {

    private static final String TAG = "TouristGuideApp";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Phase 12: Performance Tracking
        PerformanceTracker.startTimer("APP_STARTUP");
        
        // Global Crash Handler Simulation (Requirement 4)
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            android.util.Log.e("TouristGuideApp", "CRASH_DETECTED in thread " + thread.getName(), throwable);
            // Re-throw or exit to prevent zombie state
            System.exit(1);
        });

        FirebaseApp.initializeApp(this);
        configureFirestorePersistence();
        
        PerformanceTracker.endTimer("APP_STARTUP");
    }

    private void configureFirestorePersistence() {
        try {
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build();
            firestore.setFirestoreSettings(settings);
            Log.d(TAG, "Firestore persistence enabled");
        } catch (IllegalStateException e) {
            Log.w(TAG, "Firestore settings already locked (persistence left as default): " + e.getMessage());
        }
    }
}

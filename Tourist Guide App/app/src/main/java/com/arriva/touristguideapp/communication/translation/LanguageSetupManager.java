package com.arriva.touristguideapp.communication.translation;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.arriva.touristguideapp.communication.languages.LanguageConfig;
import com.arriva.touristguideapp.communication.languages.LanguageRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages the first-time setup of translation models and language tools.
 */
public class LanguageSetupManager {

    private static final String TAG = "LanguageSetupManager";
    private static final String PREF_NAME = "language_setup_prefs";
    private static final String KEY_SETUP_COMPLETE = "setup_complete";

    public interface SetupCallback {
        void onProgress(int percentage, String message);
        void onComplete();
        void onError(String message);
    }

    private final Context context;
    private final TranslationManager translationManager;
    private final SharedPreferences prefs;

    public LanguageSetupManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.translationManager = TranslationManager.getInstance(this.context);
        this.prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isSetupComplete() {
        return prefs.getBoolean(KEY_SETUP_COMPLETE, false);
    }

    public void startSetup(@NonNull SetupCallback callback) {
        List<LanguageConfig> required = new ArrayList<>();
        required.add(LanguageRegistry.requireFromCode("en"));
        required.add(LanguageRegistry.requireFromCode("mr"));
        required.add(LanguageRegistry.requireFromCode("hi"));

        final int total = required.size();
        final AtomicInteger completed = new AtomicInteger(0);

        callback.onProgress(0, "Initializing setup…");

        for (LanguageConfig lang : required) {
            translationManager.getModelManager().ensurePairReady(
                LanguageRegistry.requireFromCode("en"), 
                lang, 
                new TranslationModelManager.ModelReadyCallback() {
                    @Override
                    public void onReady() {
                        int current = completed.incrementAndGet();
                        int percentage = (current * 100) / total;
                        callback.onProgress(percentage, "Prepared " + lang.getDisplayName());
                        
                        if (current == total) {
                            prefs.edit().putBoolean(KEY_SETUP_COMPLETE, true).apply();
                            callback.onComplete();
                        }
                    }

                    @Override
                    public void onProgress(@NonNull String message) {
                        callback.onProgress((completed.get() * 100) / total, message);
                    }

                    @Override
                    public void onFailed(@NonNull String message) {
                        Log.e(TAG, "Setup failed for " + lang.getDisplayName() + ": " + message);
                        callback.onError("Failed to download " + lang.getDisplayName() + ". Please check your internet connection.");
                    }
                });
        }
    }
}

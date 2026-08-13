package com.arriva.touristguideapp.translator;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for ML Kit Translation.
 * Handles translator lifecycle, model downloads, and text translation.
 */
public class TranslationHelper {
    private static final String TAG = "TranslationHelper";

    public interface TranslationCallback {
        void onSuccess(String translatedText);
        void onFailure(Exception e);
        void onModelDownloading();
    }

    private final Map<String, Translator> translators = new HashMap<>();

    /**
     * Translates text from source language to English.
     * Handles model download if necessary.
     */
    public void translate(String text, String sourceLangCode, TranslationCallback callback) {
        if (text == null || text.trim().isEmpty()) {
            Log.w(TAG, "translate() ignored: empty text");
            return;
        }

        String targetLangCode = TranslateLanguage.ENGLISH;
        Translator translator = getTranslator(sourceLangCode, targetLangCode);

        // Conditions for downloading the model.
        // We removed .requireWifi() to allow download on mobile data if WiFi is unavailable, improving UX.
        DownloadConditions conditions = new DownloadConditions.Builder()
                .build();

        Log.d(TAG, "Ensuring model is downloaded for: " + sourceLangCode);
        
        // Notify the UI that we are checking/downloading the model.
        callback.onModelDownloading();

        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Model ready. Proceeding with translation.");
                    performTranslation(translator, text, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Model download failed", e);
                    callback.onFailure(e);
                });
    }

    private void performTranslation(Translator translator, String text, TranslationCallback callback) {
        Log.d(TAG, "Translating text: " + text);
        translator.translate(text)
                .addOnSuccessListener(translatedText -> {
                    Log.d(TAG, "Translation successful: " + translatedText);
                    callback.onSuccess(translatedText);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Translation execution failed", e);
                    callback.onFailure(e);
                });
    }

    @NonNull
    private Translator getTranslator(String sourceLang, String targetLang) {
        String key = sourceLang + "_" + targetLang;
        Translator cachedTranslator = translators.get(key);
        if (cachedTranslator == null) {
            Log.d(TAG, "Creating new translator for " + key);
            TranslatorOptions options = new TranslatorOptions.Builder()
                    .setSourceLanguage(sourceLang)
                    .setTargetLanguage(targetLang)
                    .build();
            cachedTranslator = Translation.getClient(options);
            translators.put(key, cachedTranslator);
        }
        return cachedTranslator;
    }

    /**
     * Closes all active translators to release native resources.
     */
    public void close() {
        Log.d(TAG, "Closing translators");
        for (Translator translator : translators.values()) {
            translator.close();
        }
        translators.clear();
    }
}

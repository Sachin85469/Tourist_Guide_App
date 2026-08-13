package com.arriva.touristguideapp.translator;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.TranslateRemoteModel;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Robust Translation Manager for ML Kit.
 * Supports English, Marathi, and Hindi inter-translation.
 * Handles model lifecycle, download states, and caching.
 */
public class TranslationManager {
    private static final String TAG = "TranslationManager";

    public enum ModelStatus {
        NOT_DOWNLOADED,
        DOWNLOADING,
        READY,
        FAILED
    }

    public interface TranslationCallback {
        void onSuccess(String translatedText);
        void onFailure(Exception e);
    }

    public interface ModelStatusCallback {
        void onStatusChanged(ModelStatus status);
    }

    public interface ModelDownloadCallback extends ModelStatusCallback {
        void onError(Exception e);
    }

    private final Map<String, Translator> translatorCache = new HashMap<>();
    private final RemoteModelManager modelManager = RemoteModelManager.getInstance();

    /**
     * Translates text between two languages.
     * Ensures models are downloaded before translating.
     */
    public void translate(String text, String sourceLang, String targetLang, TranslationCallback callback) {
        if (text == null || text.trim().isEmpty()) {
            callback.onSuccess("");
            return;
        }

        if (sourceLang.equals(targetLang)) {
            callback.onSuccess(text);
            return;
        }

        Translator translator = getTranslator(sourceLang, targetLang);
        DownloadConditions conditions = new DownloadConditions.Builder().build();

        Log.d(TAG, "Translating " + sourceLang + " -> " + targetLang);

        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Model is ready, translating...");
                    translator.translate(text)
                            .addOnSuccessListener(callback::onSuccess)
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Translation failed", e);
                                callback.onFailure(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Model download failed", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Explicitly download models for a language pair.
     */
    public void downloadModels(String sourceLang, String targetLang, ModelDownloadCallback callback) {
        callback.onStatusChanged(ModelStatus.DOWNLOADING);

        DownloadConditions conditions = new DownloadConditions.Builder().build();
        
        // Ensure both source and target models are downloaded
        downloadModel(sourceLang, conditions, new ModelDownloadCallback() {
            @Override
            public void onStatusChanged(ModelStatus status) {
                if (status == ModelStatus.READY) {
                    downloadModel(targetLang, conditions, callback);
                } else {
                    callback.onStatusChanged(status);
                }
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    private void downloadModel(String langCode, DownloadConditions conditions, ModelDownloadCallback callback) {
        if (langCode.equals(TranslateLanguage.ENGLISH)) {
            callback.onStatusChanged(ModelStatus.READY);
            return;
        }

        TranslateRemoteModel model = new TranslateRemoteModel.Builder(langCode).build();
        modelManager.download(model, conditions)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Model downloaded: " + langCode);
                    callback.onStatusChanged(ModelStatus.READY);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to download model: " + langCode, e);
                    callback.onStatusChanged(ModelStatus.FAILED);
                    callback.onError(e);
                });
    }

    public void checkModelStatus(String langCode, ModelStatusCallback callback) {
        if (langCode.equals(TranslateLanguage.ENGLISH)) {
            callback.onStatusChanged(ModelStatus.READY);
            return;
        }

        TranslateRemoteModel model = new TranslateRemoteModel.Builder(langCode).build();
        modelManager.isModelDownloaded(model)
                .addOnSuccessListener(downloaded -> callback.onStatusChanged(downloaded ? ModelStatus.READY : ModelStatus.NOT_DOWNLOADED))
                .addOnFailureListener(e -> callback.onStatusChanged(ModelStatus.FAILED));
    }

    @NonNull
    private Translator getTranslator(String sourceLang, String targetLang) {
        String key = sourceLang + "_" + targetLang;
        Translator translator = translatorCache.get(key);
        if (translator == null) {
            TranslatorOptions options = new TranslatorOptions.Builder()
                    .setSourceLanguage(sourceLang)
                    .setTargetLanguage(targetLang)
                    .build();
            translator = Translation.getClient(options);
            translatorCache.put(key, translator);
        }
        return translator;
    }

    public void close() {
        for (Translator translator : translatorCache.values()) {
            translator.close();
        }
        translatorCache.clear();
    }
}

package com.arriva.touristguideapp.communication.translation;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.arriva.touristguideapp.communication.languages.LanguageConfig;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cached ML Kit translators with on-demand model download, timeout, retry, and cancellation.
 */
public class TranslationManager {

    private static final String TAG = "TranslationManager";
    private static final long TRANSLATION_TIMEOUT_MS = 20_000L;
    private static final int MAX_RETRIES = 2;

    @Nullable
    private static TranslationManager instance;

  private final TranslationModelManager modelManager;
    private final TranslationCache cache;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final Map<String, Translator> translators = new ConcurrentHashMap<>();
    private final AtomicInteger operationToken = new AtomicInteger(0);

    public TranslationManager(@NonNull Context context) {
        modelManager = new TranslationModelManager();
        cache = new TranslationCache(context);
    }

    @NonNull
    public static synchronized TranslationManager getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new TranslationManager(context.getApplicationContext());
        }
        return instance;
    }

    @NonNull
    public TranslationCache getCache() {
        return cache;
    }

    @NonNull
    public TranslationModelManager getModelManager() {
        return modelManager;
    }

    public void translate(@NonNull String text,
                          @NonNull LanguageConfig source,
                          @NonNull LanguageConfig target,
                          @Nullable String cacheKey,
                          @NonNull TranslationCallback callback) {
        final String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            callback.onSuccess("");
            return;
        }
        if (source.getLanguageCode().equals(target.getLanguageCode())) {
            callback.onSuccess(trimmed);
            return;
        }

        if (cacheKey != null) {
            String cached = cache.get(cacheKey);
            if (cached != null) {
                callback.onSuccess(cached);
                return;
            }
        }

        int token = operationToken.incrementAndGet();
        AtomicBoolean completed = new AtomicBoolean(false);
        Runnable timeoutRunnable = () -> {
            if (completed.compareAndSet(false, true)) {
                callback.onFailure("Translation timed out. Check your connection and try again.");
            }
        };
        mainHandler.postDelayed(timeoutRunnable, TRANSLATION_TIMEOUT_MS);

        modelManager.ensurePairReady(source, target, new TranslationModelManager.ModelReadyCallback() {
            @Override
            public void onReady() {
                if (token != operationToken.get()) {
                    return;
                }
                runTranslateWithRetry(trimmed, source, target, cacheKey, 0, callback, completed, timeoutRunnable);
            }

            @Override
            public void onProgress(@NonNull String message) {
                callback.onProgress(message);
            }

            @Override
            public void onFailed(@NonNull String message) {
                finish(completed, timeoutRunnable);
                callback.onFailure(message);
            }
        });
    }

    private void runTranslateWithRetry(@NonNull String text,
                                       @NonNull LanguageConfig source,
                                       @NonNull LanguageConfig target,
                                       @Nullable String cacheKey,
                                       int attempt,
                                       @NonNull TranslationCallback callback,
                                       @NonNull AtomicBoolean completed,
                                       @NonNull Runnable timeoutRunnable) {
        callback.onProgress("Translating…");
        Translator translator = getOrCreateTranslator(source, target);
        translator.translate(text)
                .addOnSuccessListener(result -> {
                    finish(completed, timeoutRunnable);
                    if (cacheKey != null && result != null) {
                        cache.put(cacheKey, result);
                    }
                    callback.onSuccess(result != null ? result : "");
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "translate failed attempt=" + attempt, e);
                    if (attempt < MAX_RETRIES) {
                        modelManager.retry(source);
                        modelManager.retry(target);
                        runTranslateWithRetry(text, source, target, cacheKey, attempt + 1, callback, completed, timeoutRunnable);
                    } else {
                        finish(completed, timeoutRunnable);
                        callback.onFailure(e.getMessage() != null
                                ? e.getMessage()
                                : "Translation failed");
                    }
                });
    }

    @NonNull
    private Translator getOrCreateTranslator(@NonNull LanguageConfig source,
                                               @NonNull LanguageConfig target) {
        String pairKey = pairKey(source, target);
        return translators.computeIfAbsent(pairKey, key -> {
            TranslatorOptions options = new TranslatorOptions.Builder()
                    .setSourceLanguage(source.getMlKitLanguageCode())
                    .setTargetLanguage(target.getMlKitLanguageCode())
                    .build();
            return Translation.getClient(options);
        });
    }

    @NonNull
    public static String pairKey(@NonNull LanguageConfig source, @NonNull LanguageConfig target) {
        return source.getLanguageCode() + "->" + target.getLanguageCode();
    }

    public void cancelAll() {
        operationToken.incrementAndGet();
    }

    public void closeAll() {
        cancelAll();
        for (Translator translator : translators.values()) {
            try {
                translator.close();
            } catch (Exception e) {
                Log.w(TAG, "translator.close failed", e);
            }
        }
        translators.clear();
    }

    private void finish(@NonNull AtomicBoolean completed, @NonNull Runnable timeoutRunnable) {
        if (completed.compareAndSet(false, true)) {
            mainHandler.removeCallbacks(timeoutRunnable);
        }
    }
}

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
 * Cached ML Kit translators with on-demand model download, separate timeouts, retry, and cancellation.
 */
public class TranslationManager {

    private static final String TAG = "TranslationManager";
    /** Applies only after language packs are ready. */
    private static final long TRANSLATION_TIMEOUT_MS = 10_000L;
    private static final int MAX_TRANSLATION_RETRIES = 2;

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
                Log.d(TAG, "translation cache hit: " + cacheKey);
                callback.onSuccess(cached);
                return;
            }
        }

        final int token = operationToken.incrementAndGet();
        final String pair = pairKey(source, target);
        Log.i(TAG, "translation requested " + pair + " len=" + trimmed.length());

        final Translator translator = getOrCreateTranslator(source, target);

        modelManager.ensurePairReady(source, target, new TranslationModelManager.ModelReadyCallback() {
            @Override
            public void onReady() {
                if (token != operationToken.get()) {
                    Log.d(TAG, "translation cancelled (stale token after models ready)");
                    return;
                }
                verifyTranslatorModelsAndTranslate(
                        token, trimmed, source, target, cacheKey, translator, callback);
            }

            @Override
            public void onProgress(@NonNull String message) {
                callback.onProgress(message);
            }

            @Override
            public void onFailed(@NonNull String message) {
                if (token != operationToken.get()) {
                    return;
                }
                Log.e(TAG, "translation aborted (models not ready): " + message);
                callback.onFailure(message);
            }
        });
    }

    /**
     * After remote models are on disk, confirm the cached {@link Translator} is synced then translate.
     */
    private void verifyTranslatorModelsAndTranslate(int token,
                                                      @NonNull String text,
                                                      @NonNull LanguageConfig source,
                                                      @NonNull LanguageConfig target,
                                                      @Nullable String cacheKey,
                                                      @NonNull Translator translator,
                                                      @NonNull TranslationCallback callback) {
        callback.onProgress("Preparing translator…");
        Log.i(TAG, "translator.downloadModelIfNeeded started for " + pairKey(source, target));

        translator.downloadModelIfNeeded()
                .addOnSuccessListener(unused -> {
                    if (token != operationToken.get()) {
                        return;
                    }
                    modelManager.markReady(source);
                    modelManager.markReady(target);
                    Log.i(TAG, "translator models ready for " + pairKey(source, target));
                    runTranslateWithTimeout(token, text, source, target, cacheKey, translator, 0, callback);
                })
                .addOnFailureListener(e -> {
                    if (token != operationToken.get()) {
                        return;
                    }
                    Log.e(TAG, "translator.downloadModelIfNeeded failed", e);
                    callback.onFailure(formatError("Could not prepare translator", e));
                });
    }

    private void runTranslateWithTimeout(int token,
                                         @NonNull String text,
                                         @NonNull LanguageConfig source,
                                         @NonNull LanguageConfig target,
                                         @Nullable String cacheKey,
                                         @NonNull Translator translator,
                                         int attempt,
                                         @NonNull TranslationCallback callback) {
        AtomicBoolean completed = new AtomicBoolean(false);
        Runnable translationTimeout = () -> {
            if (completed.compareAndSet(false, true)) {
                Log.e(TAG, "translation timed out after " + TRANSLATION_TIMEOUT_MS + "ms, pair="
                        + pairKey(source, target));
                callback.onFailure("Translation timed out after "
                        + (TRANSLATION_TIMEOUT_MS / 1000) + " seconds. Tap Retry to try again.");
            }
        };

        mainHandler.postDelayed(translationTimeout, TRANSLATION_TIMEOUT_MS);
        callback.onProgress("Translating…");
        Log.i(TAG, "translation started attempt=" + attempt + " pair=" + pairKey(source, target));

        translator.translate(text)
                .addOnSuccessListener(result -> {
                    if (token != operationToken.get()) {
                        mainHandler.removeCallbacks(translationTimeout);
                        return;
                    }
                    if (completed.compareAndSet(false, true)) {
                        mainHandler.removeCallbacks(translationTimeout);
                        String output = result != null ? result : "";
                        if (cacheKey != null && !output.isEmpty()) {
                            cache.put(cacheKey, output);
                        }
                        Log.i(TAG, "translation completed pair=" + pairKey(source, target)
                                + " outLen=" + output.length());
                        callback.onSuccess(output);
                    }
                })
                .addOnFailureListener(e -> {
                    if (token != operationToken.get()) {
                        mainHandler.removeCallbacks(translationTimeout);
                        return;
                    }
                    Log.w(TAG, "translation failed attempt=" + attempt, e);
                    if (attempt < MAX_TRANSLATION_RETRIES && completed.compareAndSet(false, true)) {
                        mainHandler.removeCallbacks(translationTimeout);
                        Log.i(TAG, "translation retry attempt=" + (attempt + 1));
                        runTranslateWithTimeout(
                                token, text, source, target, cacheKey, translator, attempt + 1, callback);
                    } else if (completed.compareAndSet(false, true)) {
                        mainHandler.removeCallbacks(translationTimeout);
                        callback.onFailure(formatError("Translation failed", e));
                    }
                });
    }

    @NonNull
    private Translator getOrCreateTranslator(@NonNull LanguageConfig source,
                                               @NonNull LanguageConfig target) {
        String key = pairKey(source, target);
        return translators.computeIfAbsent(key, unused -> {
            Log.i(TAG, "creating translator instance for " + key);
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

    @NonNull
    private static String formatError(@NonNull String prefix, @NonNull Exception e) {
        String detail = e.getMessage();
        if (detail != null && !detail.isEmpty()) {
            return prefix + ": " + detail;
        }
        return prefix;
    }

    public void cancelAll() {
        operationToken.incrementAndGet();
        Log.d(TAG, "cancelAll: operations cancelled");
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
        Log.i(TAG, "closeAll: translators closed");
    }
}

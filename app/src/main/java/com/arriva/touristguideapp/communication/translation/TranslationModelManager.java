package com.arriva.touristguideapp.communication.translation;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.arriva.touristguideapp.communication.languages.LanguageConfig;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.nl.translate.TranslateRemoteModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tracks per-language ML Kit model download state and coordinates on-demand downloads.
 */
public class TranslationModelManager {

    private static final String TAG = "TranslationModelMgr";
    /** First-time language pack download can be slow on mobile networks. */
    public static final long MODEL_DOWNLOAD_TIMEOUT_MS = 45_000L;

    public interface ModelReadyCallback {
        void onReady();

        void onProgress(@NonNull String message);

        void onFailed(@NonNull String message);
    }

    private final RemoteModelManager remoteModelManager = RemoteModelManager.getInstance();
    private final DownloadConditions downloadConditions = new DownloadConditions.Builder().build();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final Map<String, TranslationModelState> languageStates = new ConcurrentHashMap<>();
    /** Dedupes parallel requests for the same language model. */
    private final Map<String, Task<Void>> inFlightDownloads = new ConcurrentHashMap<>();

    @NonNull
    public TranslationModelState getState(@NonNull LanguageConfig language) {
        return languageStates.getOrDefault(language.getLanguageCode(), TranslationModelState.NOT_DOWNLOADED);
    }

    public void ensurePairReady(@NonNull LanguageConfig source,
                                @NonNull LanguageConfig target,
                                @NonNull ModelReadyCallback callback) {
        if (source.getLanguageCode().equals(target.getLanguageCode())) {
            Log.d(TAG, "ensurePairReady: same language, skip download");
            callback.onReady();
            return;
        }

        Log.i(TAG, "ensurePairReady: source=" + source.getLanguageCode()
                + " target=" + target.getLanguageCode());

        List<Task<Void>> tasks = new ArrayList<>();
        tasks.add(ensureLanguageDownloaded(source, callback));
        tasks.add(ensureLanguageDownloaded(target, callback));

        AtomicBoolean finished = new AtomicBoolean(false);
        Runnable downloadTimeout = () -> {
            if (finished.compareAndSet(false, true)) {
                Log.e(TAG, "ensurePairReady: model download timed out after "
                        + MODEL_DOWNLOAD_TIMEOUT_MS + "ms");
                callback.onFailed("Language pack download timed out. Check your connection and tap Retry.");
            }
        };
        mainHandler.postDelayed(downloadTimeout, MODEL_DOWNLOAD_TIMEOUT_MS);

        Tasks.whenAll(tasks)
                .addOnSuccessListener(unused -> {
                    if (finished.compareAndSet(false, true)) {
                        mainHandler.removeCallbacks(downloadTimeout);
                        Log.i(TAG, "ensurePairReady: all models ready");
                        callback.onReady();
                    }
                })
                .addOnFailureListener(e -> {
                    if (finished.compareAndSet(false, true)) {
                        mainHandler.removeCallbacks(downloadTimeout);
                        String message = e.getMessage() != null
                                ? e.getMessage()
                                : "Model download failed";
                        Log.e(TAG, "ensurePairReady: model download failure: " + message, e);
                        callback.onFailed("Could not download language pack: " + message);
                    }
                });
    }

    @NonNull
    private Task<Void> ensureLanguageDownloaded(@NonNull LanguageConfig language,
                                                @NonNull ModelReadyCallback callback) {
        String code = language.getLanguageCode();
        TranslationModelState state = languageStates.getOrDefault(code, TranslationModelState.NOT_DOWNLOADED);

        if (state == TranslationModelState.READY) {
            Log.d(TAG, "model already downloaded: " + code);
            return Tasks.forResult(null);
        }

        Task<Void> inFlight = inFlightDownloads.get(code);
        if (inFlight != null) {
            Log.d(TAG, "model download already in flight, joining: " + code);
            return inFlight;
        }

        languageStates.put(code, TranslationModelState.DOWNLOADING);
        Log.i(TAG, "model download started: " + code + " (" + language.getDisplayName() + ")");
        callback.onProgress("Downloading " + language.getDisplayName() + " language pack…");

        TranslateRemoteModel model = new TranslateRemoteModel.Builder(language.getMlKitLanguageCode()).build();
        Task<Void> downloadTask = remoteModelManager.isModelDownloaded(model)
                .continueWithTask(checkTask -> {
                    if (!checkTask.isSuccessful()) {
                        Exception ex = checkTask.getException();
                        Log.e(TAG, "isModelDownloaded failed for " + code, ex);
                        return Tasks.forException(ex != null ? ex : new Exception("Could not check model status"));
                    }
                    if (Boolean.TRUE.equals(checkTask.getResult())) {
                        Log.i(TAG, "model already on device (remote check): " + code);
                        languageStates.put(code, TranslationModelState.READY);
                        return Tasks.forResult(null);
                    }
                    Log.i(TAG, "model download requesting from server: " + code);
                    return remoteModelManager.download(model, downloadConditions);
                })
                .addOnSuccessListener(unused -> {
                    languageStates.put(code, TranslationModelState.READY);
                    Log.i(TAG, "model download success: " + code);
                })
                .addOnFailureListener(e -> {
                    languageStates.put(code, TranslationModelState.FAILED);
                    Log.e(TAG, "model download failure: " + code + " — "
                            + (e.getMessage() != null ? e.getMessage() : "unknown"), e);
                })
                .continueWith(task -> {
                    inFlightDownloads.remove(code);
                    if (task.isSuccessful()) {
                        return Tasks.forResult(null);
                    }
                    Exception ex = task.getException();
                    return Tasks.forException(ex != null ? ex : new Exception("Download failed for " + code));
                });

        inFlightDownloads.put(code, downloadTask);
        return downloadTask;
    }

    public void retry(@NonNull LanguageConfig language) {
        String code = language.getLanguageCode();
        Log.i(TAG, "model retry requested: " + code);
        languageStates.put(code, TranslationModelState.NOT_DOWNLOADED);
        inFlightDownloads.remove(code);
    }

    public void markReady(@NonNull LanguageConfig language) {
        languageStates.put(language.getLanguageCode(), TranslationModelState.READY);
    }
}

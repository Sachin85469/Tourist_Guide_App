package com.arriva.touristguideapp.communication.translation;

import android.util.Log;

import androidx.annotation.NonNull;

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

/**
 * Tracks per-language ML Kit model download state and coordinates on-demand downloads.
 */
public class TranslationModelManager {

    private static final String TAG = "TranslationModelMgr";

    public interface ModelReadyCallback {
        void onReady();

        void onProgress(@NonNull String message);

        void onFailed(@NonNull String message);
    }

    private final RemoteModelManager remoteModelManager = RemoteModelManager.getInstance();
    private final DownloadConditions downloadConditions = new DownloadConditions.Builder().build();

    private final Map<String, TranslationModelState> languageStates = new ConcurrentHashMap<>();

    @NonNull
    public TranslationModelState getState(@NonNull LanguageConfig language) {
        return languageStates.getOrDefault(language.getLanguageCode(), TranslationModelState.NOT_DOWNLOADED);
    }

    public void ensurePairReady(@NonNull LanguageConfig source,
                                @NonNull LanguageConfig target,
                                @NonNull ModelReadyCallback callback) {
        if (source.getLanguageCode().equals(target.getLanguageCode())) {
            callback.onReady();
            return;
        }
        List<Task<Void>> tasks = new ArrayList<>();
        tasks.add(ensureLanguageDownloaded(source, callback));
        tasks.add(ensureLanguageDownloaded(target, callback));

        Tasks.whenAll(tasks)
                .addOnSuccessListener(unused -> callback.onReady())
                .addOnFailureListener(e -> {
                    Log.e(TAG, "ensurePairReady failed", e);
                    callback.onFailed(e.getMessage() != null ? e.getMessage() : "Model download failed");
                });
    }

    @NonNull
    private Task<Void> ensureLanguageDownloaded(@NonNull LanguageConfig language,
                                                @NonNull ModelReadyCallback callback) {
        String code = language.getLanguageCode();
        TranslationModelState state = languageStates.getOrDefault(code, TranslationModelState.NOT_DOWNLOADED);
        if (state == TranslationModelState.READY) {
            return Tasks.forResult(null);
        }

        languageStates.put(code, TranslationModelState.DOWNLOADING);
        callback.onProgress("Downloading " + language.getDisplayName() + " language pack…");

        TranslateRemoteModel model = new TranslateRemoteModel.Builder(language.getMlKitLanguageCode()).build();
        return remoteModelManager.isModelDownloaded(model)
                .continueWithTask(task -> {
                    if (task.isSuccessful() && Boolean.TRUE.equals(task.getResult())) {
                        languageStates.put(code, TranslationModelState.READY);
                        return Tasks.forResult(null);
                    }
                    return remoteModelManager.download(model, downloadConditions)
                            .continueWithTask(downloadTask -> {
                                if (downloadTask.isSuccessful()) {
                                    languageStates.put(code, TranslationModelState.READY);
                                } else {
                                    languageStates.put(code, TranslationModelState.FAILED);
                                }
                                return downloadTask;
                            });
                });
    }

    public void retry(@NonNull LanguageConfig language) {
        languageStates.put(language.getLanguageCode(), TranslationModelState.NOT_DOWNLOADED);
    }
}

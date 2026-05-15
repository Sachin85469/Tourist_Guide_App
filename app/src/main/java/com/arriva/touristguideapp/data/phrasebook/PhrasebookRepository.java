package com.arriva.touristguideapp.data.phrasebook;

import android.content.Context;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads phrases from Firestore with local fallback and offline disk cache.
 */
public class PhrasebookRepository {

    private static final String TAG = "PhrasebookRepository";

    public enum DataOrigin {
        FIRESTORE,
        OFFLINE_CACHE,
        LOCAL_FALLBACK
    }

    public interface PhrasesListener {
        void onPhrasesLoaded(@NonNull List<Phrase> phrases, @NonNull DataOrigin origin);

        void onError(@NonNull String message);
    }

    private final FirestorePhrasebookDataSource remote;
    private final LocalPhrasebookCatalog local;
    private final PhrasebookCache cache;

    public PhrasebookRepository(@NonNull Context context) {
        this(new FirestorePhrasebookDataSource(), new LocalPhrasebookCatalog(), new PhrasebookCache(context));
    }

    public PhrasebookRepository(@NonNull FirestorePhrasebookDataSource remote,
                                @NonNull LocalPhrasebookCatalog local,
                                @NonNull PhrasebookCache cache) {
        this.remote = remote;
        this.local = local;
        this.cache = cache;
    }

    @MainThread
    public void startListening(@NonNull PhrasesListener listener) {
        remote.listenToPhrases(new FirestorePhrasebookDataSource.PhrasesSnapshotListener() {
            @Override
            public void onPhrasesUpdated(@NonNull List<PhraseDto> phrases) {
                if (phrases.isEmpty()) {
                    deliverFallback(listener, "empty_remote");
                    return;
                }
                List<Phrase> mapped = PhraseMapper.toPhrases(phrases);
                if (mapped.isEmpty()) {
                    deliverFallback(listener, "mapping_failed");
                    return;
                }
                cache.save(mapped);
                listener.onPhrasesLoaded(mapped, DataOrigin.FIRESTORE);
            }

            @Override
            public void onError(@NonNull String message) {
                Log.w(TAG, "Firestore listener error: " + message);
                deliverFallback(listener, message);
            }
        });
    }

    public void stopListening() {
        remote.removeListener();
    }

    private void deliverFallback(@NonNull PhrasesListener listener, @Nullable String reason) {
        List<Phrase> cached = cache.load();
        if (!cached.isEmpty()) {
            Log.d(TAG, "Using offline cache, reason=" + reason);
            listener.onPhrasesLoaded(cached, DataOrigin.OFFLINE_CACHE);
            return;
        }
        List<Phrase> bundled = local.getAllPhrases();
        Log.d(TAG, "Using bundled catalog, reason=" + reason);
        listener.onPhrasesLoaded(bundled, DataOrigin.LOCAL_FALLBACK);
    }

    @NonNull
    public static List<Phrase> filter(@NonNull List<Phrase> source,
                                      @Nullable String category,
                                      @Nullable String query,
                                      boolean favoritesOnly,
                                      @NonNull java.util.Set<String> favoriteIds) {
        List<Phrase> filtered = new ArrayList<>();
        for (Phrase phrase : source) {
            if (favoritesOnly && !favoriteIds.contains(phrase.getId())) {
                continue;
            }
            if (!phrase.matchesCategory(category)) {
                continue;
            }
            if (!phrase.matchesQuery(query)) {
                continue;
            }
            filtered.add(phrase);
        }
        return filtered;
    }
}

package com.arriva.touristguideapp.data.phrasebook;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Uploads bundled phrases with translations into Firestore.
 */
public final class PhrasebookMigrationHelper {

    private static final String TAG = "PhrasebookMigration";

    public interface MigrationCallback {
        void onMigrationFinished(int successCount, int failureCount);
    }

    private PhrasebookMigrationHelper() {
    }

    public static void migratePhrasesToFirestore(@NonNull Context context,
                                                 @Nullable MigrationCallback callback) {
        List<Phrase> phrases = new LocalPhrasebookCatalog().getAllPhrases();
        if (phrases.isEmpty()) {
            if (callback != null) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onMigrationFinished(0, 0));
            }
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failure = new AtomicInteger(0);
        AtomicInteger pending = new AtomicInteger(phrases.size());

        for (Phrase phrase : phrases) {
            Map<String, Object> fields = new HashMap<>();
            fields.put(PhrasebookFirestoreContract.FIELD_ENGLISH_TEXT, phrase.getEnglishText());
            fields.put(PhrasebookFirestoreContract.FIELD_HINDI_TEXT, phrase.getHindiText());
            fields.put(PhrasebookFirestoreContract.FIELD_MARATHI_TEXT, phrase.getMarathiText());
            fields.put(PhrasebookFirestoreContract.FIELD_CATEGORY, phrase.getCategory());

            db.collection(PhrasebookFirestoreContract.COLLECTION_PHRASEBOOK)
                    .document(phrase.getId())
                    .set(fields)
                    .addOnSuccessListener(unused -> {
                        success.incrementAndGet();
                        if (pending.decrementAndGet() == 0 && callback != null) {
                            new Handler(Looper.getMainLooper()).post(() ->
                                    callback.onMigrationFinished(success.get(), failure.get()));
                        }
                    })
                    .addOnFailureListener(e -> {
                        failure.incrementAndGet();
                        Log.e(TAG, "Upload failed id=" + phrase.getId(), e);
                        if (pending.decrementAndGet() == 0 && callback != null) {
                            new Handler(Looper.getMainLooper()).post(() ->
                                    callback.onMigrationFinished(success.get(), failure.get()));
                        }
                    });
        }
    }
}

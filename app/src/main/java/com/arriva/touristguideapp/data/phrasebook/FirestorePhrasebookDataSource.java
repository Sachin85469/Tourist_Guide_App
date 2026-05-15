package com.arriva.touristguideapp.data.phrasebook;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Real-time Firestore reads for the phrasebook collection.
 */
public class FirestorePhrasebookDataSource {

    private static final String TAG = "PhrasebookFirestore";

    public interface PhrasesSnapshotListener {
        void onPhrasesUpdated(@NonNull List<PhraseDto> phrases);

        void onError(@NonNull String message);
    }

    private final FirebaseFirestore db;
    @Nullable
    private ListenerRegistration registration;

    public FirestorePhrasebookDataSource() {
        this(FirebaseFirestore.getInstance());
    }

    public FirestorePhrasebookDataSource(@NonNull FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * Live listener ordered by category for stable scrolling.
     */
    public void listenToPhrases(@NonNull PhrasesSnapshotListener listener) {
        removeListener();
        Query query = db.collection(PhrasebookFirestoreContract.COLLECTION_PHRASEBOOK)
                .orderBy(PhrasebookFirestoreContract.FIELD_CATEGORY);

        registration = query.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "listenToPhrases failed", error);
                listener.onError(error.getMessage() != null ? error.getMessage() : "unknown_error");
                return;
            }
            listener.onPhrasesUpdated(mapSnapshot(snapshot));
        });
    }

    public void listenByCategory(@NonNull String category, @NonNull PhrasesSnapshotListener listener) {
        removeListener();
        Query query = db.collection(PhrasebookFirestoreContract.COLLECTION_PHRASEBOOK)
                .whereEqualTo(PhrasebookFirestoreContract.FIELD_CATEGORY, category)
                .orderBy(PhrasebookFirestoreContract.FIELD_ENGLISH);

        registration = query.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "listenByCategory failed", error);
                listener.onError(error.getMessage() != null ? error.getMessage() : "unknown_error");
                return;
            }
            listener.onPhrasesUpdated(mapSnapshot(snapshot));
        });
    }

    public void removeListener() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    @NonNull
    private static List<PhraseDto> mapSnapshot(@Nullable QuerySnapshot snapshot) {
        List<PhraseDto> dtos = new ArrayList<>();
        if (snapshot == null || snapshot.isEmpty()) {
            return dtos;
        }
        snapshot.getDocuments().forEach(doc -> {
            PhraseDto dto = PhraseMapper.fromSnapshot(doc);
            if (dto != null) {
                dtos.add(dto);
            }
        });
        return dtos;
    }
}

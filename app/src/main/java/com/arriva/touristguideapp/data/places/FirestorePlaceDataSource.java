package com.arriva.touristguideapp.data.places;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Low-level Firestore reads for the places collection.
 * Does not apply fallback; {@link PlaceRepository} owns that policy.
 */
public class FirestorePlaceDataSource {

    private static final String TAG = "FirestorePlaceDataSource";

    private final FirebaseFirestore db;

    public FirestorePlaceDataSource() {
        this(FirebaseFirestore.getInstance());
    }

    public FirestorePlaceDataSource(@NonNull FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * All documents with {@link PlacesFirestoreContract#STATUS_PUBLISHED}.
     */
    @NonNull
    public Task<List<PlaceDto>> fetchPublishedPlaces() {
        Query query = db.collection(PlacesFirestoreContract.COLLECTION_PLACES)
                .whereEqualTo(PlacesFirestoreContract.FIELD_STATUS, PlacesFirestoreContract.STATUS_PUBLISHED);

        return query.get().continueWith(task -> mapSnapshotToDtos(task, "fetchPublishedPlaces"));
    }

    /**
     * Published places flagged as top pick (requires composite index: status + isTopPick).
     */
    @NonNull
    public Task<List<PlaceDto>> fetchTopPicks() {
        Query query = db.collection(PlacesFirestoreContract.COLLECTION_PLACES)
                .whereEqualTo(PlacesFirestoreContract.FIELD_STATUS, PlacesFirestoreContract.STATUS_PUBLISHED)
                .whereEqualTo(PlacesFirestoreContract.FIELD_IS_TOP_PICK, true);

        return query.get().continueWith(task -> mapSnapshotToDtos(task, "fetchTopPicks"));
    }

    /**
     * Published places for a category (requires composite index: status + category).
     */
    @NonNull
    public Task<List<PlaceDto>> fetchPublishedByCategory(@NonNull String category) {
        Query query = db.collection(PlacesFirestoreContract.COLLECTION_PLACES)
                .whereEqualTo(PlacesFirestoreContract.FIELD_STATUS, PlacesFirestoreContract.STATUS_PUBLISHED)
                .whereEqualTo(PlacesFirestoreContract.FIELD_CATEGORY, category);

        return query.get().continueWith(task -> mapSnapshotToDtos(task, "fetchPublishedByCategory"));
    }

    @NonNull
    private List<PlaceDto> mapSnapshotToDtos(@NonNull com.google.android.gms.tasks.Task<QuerySnapshot> task,
                                             @NonNull String operation) {
        if (!task.isSuccessful()) {
            Exception e = task.getException();
            Log.e(TAG, operation + " query failed", e);
            throw e != null ? new RuntimeException(e) : new RuntimeException(operation + " failed");
        }
        QuerySnapshot snapshot = task.getResult();
        if (snapshot == null) {
            Log.w(TAG, operation + " returned null snapshot");
            return new ArrayList<>();
        }

        List<PlaceDto> out = new ArrayList<>();
        snapshot.getDocuments().forEach(doc -> {
            try {
                PlaceDto dto = PlaceDto.fromSnapshot(doc);
                if (dto != null) {
                    out.add(dto);
                } else {
                    Log.w(TAG, operation + ": skipped invalid document id=" + doc.getId());
                }
            } catch (Exception ex) {
                Log.e(TAG, operation + ": mapping exception for id=" + doc.getId(), ex);
            }
        });

        Log.d(TAG, operation + " success count=" + out.size());
        return out;
    }
}

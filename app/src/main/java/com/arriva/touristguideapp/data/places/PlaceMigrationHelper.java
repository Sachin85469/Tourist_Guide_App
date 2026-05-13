package com.arriva.touristguideapp.data.places;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.arriva.touristguideapp.DataProvider;
import com.arriva.touristguideapp.Place;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TEMPORARY one-shot migration: uploads hardcoded {@link DataProvider} rows into the
 * {@link PlacesFirestoreContract#COLLECTION_PLACES} collection.
 * 
 * Updated to remove legacy drawable mapping.
 */
public final class PlaceMigrationHelper {

    private static final String TAG = "PlaceMigrationHelper";

    public interface MigrationCallback {
        void onMigrationFinished(int successCount, int failureCount);
    }

    private PlaceMigrationHelper() {
    }

    public static void migratePlacesToFirestore(@NonNull Context context,
                                                @Nullable MigrationCallback callback) {
        Context app = context.getApplicationContext();
        List<Place> places = DataProvider.getAllPlaces();
        if (places.isEmpty()) {
            Log.w(TAG, "migratePlacesToFirestore: no local places to upload");
            if (callback != null) {
                postMain(app, () -> callback.onMigrationFinished(0, 0));
            }
            return;
        }

        LinkedHashMap<String, Place> unique = new LinkedHashMap<>();
        AtomicInteger preflightFailures = new AtomicInteger(0);
        for (Place place : places) {
            String docId = resolveDocumentId(place);
            if (docId == null || docId.isEmpty()) {
                preflightFailures.incrementAndGet();
                Log.e(TAG, "UPLOAD_FAILURE reason=invalid_doc_id name=" + place.getName());
                continue;
            }
            if (unique.containsKey(docId)) {
                preflightFailures.incrementAndGet();
                Log.e(TAG, "UPLOAD_SKIPPED reason=duplicate_doc_id docId=" + docId
                        + " keptName=" + unique.get(docId).getName() + " droppedName=" + place.getName());
                continue;
            }
            unique.put(docId, place);
        }

        if (unique.isEmpty()) {
            Log.w(TAG, "migratePlacesToFirestore: no valid document ids after de-duplication");
            if (callback != null) {
                postMain(app, () -> callback.onMigrationFinished(0, preflightFailures.get()));
            }
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failure = new AtomicInteger(preflightFailures.get());
        AtomicInteger pending = new AtomicInteger(unique.size());

        Log.i(TAG, "migratePlacesToFirestore: START sourceRows=" + places.size());

        Runnable onOneFinished = () -> {
            if (pending.decrementAndGet() != 0) {
                return;
            }
            int ok = success.get();
            int bad = failure.get();
            Log.i(TAG, "migratePlacesToFirestore: COMPLETE totalSuccess=" + ok + " totalFailure=" + bad);
            if (callback != null) {
                postMain(app, () -> callback.onMigrationFinished(ok, bad));
            }
        };

        for (Map.Entry<String, Place> e : unique.entrySet()) {
            String docId = e.getKey();
            Place place = e.getValue();

            Map<String, Object> payload = buildDocument(place);
            Task<Void> write = db.collection(PlacesFirestoreContract.COLLECTION_PLACES)
                    .document(docId)
                    .set(payload);

            write.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    success.incrementAndGet();
                    Log.i(TAG, "UPLOAD_SUCCESS docId=" + docId + " name=" + place.getName());
                } else {
                    failure.incrementAndGet();
                    Exception ex = task.getException();
                    Log.e(TAG, "UPLOAD_FAILURE docId=" + docId + " name=" + place.getName(), ex);
                }
                onOneFinished.run();
            });
        }
    }

    private static void postMain(@NonNull Context app, @NonNull Runnable r) {
        new Handler(app.getMainLooper()).post(r);
    }

    @Nullable
    private static String resolveDocumentId(@NonNull Place place) {
        String legacy = place.getLegacyCatalogId();
        if (legacy != null) {
            legacy = legacy.trim();
            if (!legacy.isEmpty()) {
                return legacy;
            }
        }
        String id = place.getId();
        if (id == null) {
            return null;
        }
        id = id.trim();
        return id.isEmpty() ? null : id;
    }

    @NonNull
    private static Map<String, Object> buildDocument(@NonNull Place place) {
        Map<String, Object> map = new HashMap<>();

        map.put(PlacesFirestoreContract.FIELD_NAME, place.getName());
        map.put(PlacesFirestoreContract.FIELD_STATUS, PlacesFirestoreContract.STATUS_PUBLISHED);
        map.put(PlacesFirestoreContract.FIELD_CATEGORY, place.getCategory());
        map.put(PlacesFirestoreContract.FIELD_CITY, place.getCity());
        map.put(PlacesFirestoreContract.FIELD_DESCRIPTION, place.getDescription());
        map.put(PlacesFirestoreContract.FIELD_LATITUDE, place.getLatitude());
        map.put(PlacesFirestoreContract.FIELD_LONGITUDE, place.getLongitude());
        map.put(PlacesFirestoreContract.FIELD_IS_TOP_PICK, place.isTopPick());
        map.put(PlacesFirestoreContract.FIELD_RATING_AVG, place.getRating());

        map.put(PlacesFirestoreContract.FIELD_LOCATION,
                new GeoPoint(place.getLatitude(), place.getLongitude()));

        map.put(PlacesFirestoreContract.FIELD_BUDGET, place.getBudget());
        map.put(PlacesFirestoreContract.FIELD_CROWD_LEVEL, place.getCrowdLevel());
        map.put(PlacesFirestoreContract.FIELD_BEST_TIME, place.getBestTime());
        map.put(PlacesFirestoreContract.FIELD_TIPS, place.getTips());
        map.put(PlacesFirestoreContract.FIELD_FUN_FACT, place.getFunFact());
        map.put(PlacesFirestoreContract.FIELD_NEAREST_STATION, place.getNearestStation());
        map.put(PlacesFirestoreContract.FIELD_TAG, place.getTag());
        map.put(PlacesFirestoreContract.FIELD_LEGACY_ID, place.getId());

        // Note: imageUrl and galleryUrls are NOT mapped here as they are manually added to Firestore
        // or provided by the data source if already remote-first.
        
        return map;
    }
}

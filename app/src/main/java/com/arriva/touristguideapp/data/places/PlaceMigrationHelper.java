package com.arriva.touristguideapp.data.places;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.arriva.touristguideapp.DataProvider;
import com.arriva.touristguideapp.Place;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TEMPORARY one-shot migration: uploads hardcoded {@link DataProvider} rows into the
 * {@link PlacesFirestoreContract#COLLECTION_PLACES} collection. Safe to delete once Firestore is seeded.
 */
public final class PlaceMigrationHelper {

    private static final String TAG = "PlaceMigrationHelper";

    public interface MigrationCallback {
        void onMigrationFinished(int successCount, int failureCount);
    }

    private PlaceMigrationHelper() {
    }

    /**
     * Reads {@link DataProvider#getAllPlaces()} and writes each document with {@link Task} {@code set()}
     * (full replace per document id). Logs per-document outcome and a final summary.
     */
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

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failure = new AtomicInteger(0);
        AtomicInteger pending = new AtomicInteger(places.size());

        Log.i(TAG, "migratePlacesToFirestore: START count=" + places.size());

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

        for (Place place : places) {
            String docId = resolveDocumentId(place);
            if (docId == null || docId.isEmpty()) {
                failure.incrementAndGet();
                Log.e(TAG, "UPLOAD_FAILURE reason=invalid_doc_id name=" + place.getName());
                onOneFinished.run();
                continue;
            }

            Map<String, Object> payload = buildDocument(app, place);
            Task<Void> write = db.collection(PlacesFirestoreContract.COLLECTION_PLACES)
                    .document(docId)
                    .set(payload);

            write.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    success.incrementAndGet();
                    Log.i(TAG, "UPLOAD_SUCCESS docId=" + docId + " name=" + place.getName());
                } else {
                    failure.incrementAndGet();
                    Exception e = task.getException();
                    Log.e(TAG, "UPLOAD_FAILURE docId=" + docId + " name=" + place.getName()
                            + " message=" + (e != null ? e.getMessage() : "unknown"), e);
                }
                onOneFinished.run();
            });
        }
    }

    private static void postMain(@NonNull Context app, @NonNull Runnable r) {
        new Handler(app.getMainLooper()).post(r);
    }

    /**
     * Uses {@link Place#getId()} so numeric legacy ids ("1", "2", "3", …) become Firestore document ids.
     */
    @Nullable
    private static String resolveDocumentId(@NonNull Place place) {
        String id = place.getId();
        if (id == null) {
            return null;
        }
        id = id.trim();
        return id.isEmpty() ? null : id;
    }

    @NonNull
    private static Map<String, Object> buildDocument(@NonNull Context context, @NonNull Place place) {
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

        map.put(PlacesFirestoreContract.FIELD_DRAWABLE_ASSET_KEY,
                drawableAssetKey(context, place.getImageResId()));
        map.put(PlacesFirestoreContract.FIELD_GALLERY_DRAWABLE_KEYS,
                galleryDrawableKeys(context, place.getGalleryImages()));

        return map;
    }

    @NonNull
    private static String drawableAssetKey(@NonNull Context context, int resId) {
        if (resId == 0) {
            return "";
        }
        try {
            return context.getResources().getResourceEntryName(resId);
        } catch (Resources.NotFoundException e) {
            Log.w(TAG, "drawableAssetKey: NotFoundException resId=0x" + Integer.toHexString(resId));
            return "missing_drawable_" + resId;
        }
    }

    @NonNull
    private static List<String> galleryDrawableKeys(@NonNull Context context,
                                                    @Nullable List<Integer> resIds) {
        if (resIds == null || resIds.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> ordered = new LinkedHashSet<>();
        for (Integer id : resIds) {
            if (id == null || id == 0) {
                continue;
            }
            ordered.add(drawableAssetKey(context, id));
        }
        return new ArrayList<>(ordered);
    }
}

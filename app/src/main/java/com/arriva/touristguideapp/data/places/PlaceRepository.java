package com.arriva.touristguideapp.data.places;

import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.arriva.touristguideapp.Place;

import java.util.List;

/**
 * Single entry point for loading places from Firestore with automatic fallback to {@link LocalPlaceCatalog}.
 * Activities are not wired yet; instantiate where needed in a later phase.
 */
public class PlaceRepository {

    private static final String TAG = "PlaceRepository";

    public enum DataOrigin {
        FIRESTORE,
        LOCAL_FALLBACK
    }

    public interface PlacesLoadCallback {
        /**
         * @param places      never null; may be empty if both remote and local are empty (extremely unlikely for local)
         * @param origin      whether results came from Firestore or static fallback
         * @param message     optional debug detail (e.g. exception message when falling back)
         */
        void onPlacesLoaded(@NonNull List<Place> places, @NonNull DataOrigin origin, @Nullable String message);
    }

    private final FirestorePlaceDataSource remote;
    private final LocalPlaceCatalog local;

    public PlaceRepository() {
        this(new FirestorePlaceDataSource(), new LocalPlaceCatalog());
    }

    public PlaceRepository(@NonNull FirestorePlaceDataSource remote, @NonNull LocalPlaceCatalog local) {
        this.remote = remote;
        this.local = local;
    }

    /**
     * Loads published places. On empty remote list or any failure, returns {@link LocalPlaceCatalog#getAllPlaces()}.
     */
    @MainThread
    public void fetchPublishedPlaces(@NonNull PlacesLoadCallback callback) {
        remote.fetchPublishedPlaces()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Exception ex = task.getException();
                        Log.w(TAG, "fetchPublishedPlaces: Firestore failed, activating LOCAL_FALLBACK", ex);
                        deliver(local.getAllPlaces(), DataOrigin.LOCAL_FALLBACK,
                                ex != null ? ex.getMessage() : "unknown_error", callback);
                        return;
                    }
                    List<PlaceDto> dtos = task.getResult();
                    if (dtos == null || dtos.isEmpty()) {
                        Log.w(TAG, "fetchPublishedPlaces: empty remote result, activating LOCAL_FALLBACK");
                        deliver(local.getAllPlaces(), DataOrigin.LOCAL_FALLBACK, "empty_remote", callback);
                        return;
                    }
                    List<Place> mapped = PlaceMapper.toPlaces(dtos);
                    if (mapped.isEmpty()) {
                        Log.w(TAG, "fetchPublishedPlaces: all DTOs failed mapping, activating LOCAL_FALLBACK");
                        deliver(local.getAllPlaces(), DataOrigin.LOCAL_FALLBACK, "mapping_failed_all", callback);
                        return;
                    }
                    Log.d(TAG, "fetchPublishedPlaces: Firestore success count=" + mapped.size()
                            + " firstId=" + mapped.get(0).getId());
                    deliver(mapped, DataOrigin.FIRESTORE, null, callback);
                });
    }

    /**
     * Loads published top picks. Falls back to {@link LocalPlaceCatalog#getTopPicks()} on failure or empty/mapped-empty.
     */
    @MainThread
    public void fetchTopPicks(@NonNull PlacesLoadCallback callback) {
        remote.fetchTopPicks()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Exception ex = task.getException();
                        Log.w(TAG, "fetchTopPicks: Firestore failed, activating LOCAL_FALLBACK", ex);
                        deliver(local.getTopPicks(), DataOrigin.LOCAL_FALLBACK,
                                ex != null ? ex.getMessage() : "unknown_error", callback);
                        return;
                    }
                    List<PlaceDto> dtos = task.getResult();
                    if (dtos == null || dtos.isEmpty()) {
                        Log.w(TAG, "fetchTopPicks: empty remote result, activating LOCAL_FALLBACK");
                        deliver(local.getTopPicks(), DataOrigin.LOCAL_FALLBACK, "empty_remote", callback);
                        return;
                    }
                    List<Place> mapped = PlaceMapper.toPlaces(dtos);
                    if (mapped.isEmpty()) {
                        Log.w(TAG, "fetchTopPicks: all DTOs failed mapping, activating LOCAL_FALLBACK");
                        deliver(local.getTopPicks(), DataOrigin.LOCAL_FALLBACK, "mapping_failed_all", callback);
                        return;
                    }
                    Log.d(TAG, "fetchTopPicks: Firestore success count=" + mapped.size()
                            + " firstId=" + mapped.get(0).getId());
                    deliver(mapped, DataOrigin.FIRESTORE, null, callback);
                });
    }

    /**
     * Loads published places for a category (exact category string match as stored in Firestore).
     */
    @MainThread
    public void fetchPublishedByCategory(@NonNull String category, @NonNull PlacesLoadCallback callback) {
        remote.fetchPublishedByCategory(category)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Exception ex = task.getException();
                        Log.w(TAG, "fetchPublishedByCategory: Firestore failed, activating LOCAL_FALLBACK category="
                                + category, ex);
                        deliver(local.getPlacesByCategory(category), DataOrigin.LOCAL_FALLBACK,
                                ex != null ? ex.getMessage() : "unknown_error", callback);
                        return;
                    }
                    List<PlaceDto> dtos = task.getResult();
                    if (dtos == null || dtos.isEmpty()) {
                        Log.w(TAG, "fetchPublishedByCategory: empty remote result, activating LOCAL_FALLBACK category="
                                + category);
                        deliver(local.getPlacesByCategory(category), DataOrigin.LOCAL_FALLBACK, "empty_remote", callback);
                        return;
                    }
                    List<Place> mapped = PlaceMapper.toPlaces(dtos);
                    if (mapped.isEmpty()) {
                        Log.w(TAG, "fetchPublishedByCategory: all DTOs failed mapping, activating LOCAL_FALLBACK");
                        deliver(local.getPlacesByCategory(category), DataOrigin.LOCAL_FALLBACK, "mapping_failed_all", callback);
                        return;
                    }
                    Log.d(TAG, "fetchPublishedByCategory: Firestore success category=" + category
                            + " count=" + mapped.size() + " firstId=" + mapped.get(0).getId());
                    deliver(mapped, DataOrigin.FIRESTORE, null, callback);
                });
    }

    private void deliver(@NonNull List<Place> places,
                         @NonNull DataOrigin origin,
                         @Nullable String message,
                         @NonNull PlacesLoadCallback callback) {
        if (origin == DataOrigin.LOCAL_FALLBACK) {
            Log.i(TAG, "LOCAL_FALLBACK delivery size=" + places.size() + (message != null ? (" detail=" + message) : ""));
        } else {
            Log.d(TAG, "FIRESTORE delivery size=" + places.size());
        }
        callback.onPlacesLoaded(places, origin, message);
    }
}

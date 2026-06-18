package com.arriva.touristguideapp.data.places;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.arriva.touristguideapp.PerformanceTracker;
import com.arriva.touristguideapp.Place;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Single entry point for loading places from Firestore with fallback options.
 */
public class PlaceRepository {

    private static final String TAG = "PlaceRepository";
    private static final ExecutorService CACHE_EXECUTOR = Executors.newSingleThreadExecutor();

    public enum DataOrigin {
        FIRESTORE,
        LOCAL_FALLBACK,
        ROOM_CACHE
    }

    public interface PlacesLoadCallback {
        /**
         * @param places  never null; may be empty if both remote and local are empty
         * @param origin  where results came from
         * @param message optional debug detail
         */
        void onPlacesLoaded(@NonNull List<Place> places, @NonNull DataOrigin origin, @Nullable String message);
    }

    public interface OfflineFirstPlacesCallback {
        /**
         * @param places     never null
         * @param origin     Firestore when live, Room when saved data is served
         * @param cacheEmpty true only when offline cache has no places to return
         * @param message    optional debug detail
         */
        void onPlacesLoaded(@NonNull List<Place> places,
                            @NonNull DataOrigin origin,
                            boolean cacheEmpty,
                            @Nullable String message);
    }

    private final FirestorePlaceDataSource remote;
    private final LocalPlaceCatalog local;
    @Nullable
    private final Context appContext;
    @Nullable
    private final PlaceDao placeDao;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public PlaceRepository() {
        this(null, new FirestorePlaceDataSource(), new LocalPlaceCatalog());
    }

    public PlaceRepository(@NonNull Context context) {
        this(context, new FirestorePlaceDataSource(), new LocalPlaceCatalog());
    }

    public PlaceRepository(@NonNull FirestorePlaceDataSource remote, @NonNull LocalPlaceCatalog local) {
        this(null, remote, local);
    }

    public PlaceRepository(@Nullable Context context,
                           @NonNull FirestorePlaceDataSource remote,
                           @NonNull LocalPlaceCatalog local) {
        this.remote = remote;
        this.local = local;
        this.appContext = context != null ? context.getApplicationContext() : null;
        this.placeDao = this.appContext != null ? AppDatabase.getInstance(this.appContext).placeDao() : null;
    }

    /**
     * Loads published places. On empty remote list or any failure, returns {@link LocalPlaceCatalog#getAllPlaces()}.
     */
    @MainThread
    public void fetchPublishedPlaces(@NonNull PlacesLoadCallback callback) {
        PerformanceTracker.startTimer("FETCH_PLACES_REMOTE");
        remote.fetchPublishedPlaces()
                .addOnCompleteListener(task -> {
                    PerformanceTracker.endTimer("FETCH_PLACES_REMOTE");
                    if (!task.isSuccessful()) {
                        Exception ex = task.getException();
                        Log.w(TAG, "fetchPublishedPlaces: Firestore QUERY failed, activating LOCAL_FALLBACK"
                                + " exception=" + (ex != null ? ex.getMessage() : "unknown"), ex);
                        deliver(local.getAllPlaces(), DataOrigin.LOCAL_FALLBACK,
                                ex != null ? ex.getMessage() : "unknown_error", callback);
                        return;
                    }
                    try {
                        List<PlaceDto> dtos = task.getResult();
                        if (dtos == null || dtos.isEmpty()) {
                            Log.w(TAG, "fetchPublishedPlaces: empty remote result, activating LOCAL_FALLBACK");
                            deliver(local.getAllPlaces(), DataOrigin.LOCAL_FALLBACK, "empty_remote", callback);
                            return;
                        }
                        List<Place> mapped = PlaceMapper.toPlaces(dtos);
                        if (mapped.isEmpty()) {
                            Log.w(TAG, "fetchPublishedPlaces: all " + dtos.size()
                                    + " DTOs failed mapping, activating LOCAL_FALLBACK");
                            deliver(local.getAllPlaces(), DataOrigin.LOCAL_FALLBACK, "mapping_failed_all", callback);
                            return;
                        }
                        cachePlacesInBackground(mapped, true);
                        Log.d(TAG, "fetchPublishedPlaces: FIRESTORE success count=" + mapped.size()
                                + " (from " + dtos.size() + " DTOs)"
                                + " firstId=" + mapped.get(0).getId());
                        deliver(mapped, DataOrigin.FIRESTORE, null, callback);
                    } catch (Exception resultEx) {
                        Log.e(TAG, "fetchPublishedPlaces: task.getResult() threw, activating LOCAL_FALLBACK"
                                + " exception=" + resultEx.getMessage(), resultEx);
                        deliver(local.getAllPlaces(), DataOrigin.LOCAL_FALLBACK,
                                "result_exception: " + resultEx.getMessage(), callback);
                    }
                });
    }

    /**
     * Loads published places with Room as the deliberate offline source.
     */
    @MainThread
    public void getPlacesOfflineFirst(@Nullable String city,
                                      @Nullable List<String> categories,
                                      @NonNull OfflineFirstPlacesCallback callback) {
        if (appContext == null || placeDao == null) {
            fetchPublishedPlaces((places, origin, message) ->
                    callback.onPlacesLoaded(filterPlaces(places, city, categories), origin, false, message));
            return;
        }

        if (isOnline()) {
            fetchRemoteForOfflineFirst(city, categories, callback);
        } else {
            loadFromRoom(city, categories, "offline", callback);
        }
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
                        Log.w(TAG, "fetchTopPicks: Firestore QUERY failed, activating LOCAL_FALLBACK"
                                + " exception=" + (ex != null ? ex.getMessage() : "unknown"), ex);
                        deliver(local.getTopPicks(), DataOrigin.LOCAL_FALLBACK,
                                ex != null ? ex.getMessage() : "unknown_error", callback);
                        return;
                    }
                    try {
                        List<PlaceDto> dtos = task.getResult();
                        if (dtos == null || dtos.isEmpty()) {
                            Log.w(TAG, "fetchTopPicks: empty remote result, activating LOCAL_FALLBACK");
                            deliver(local.getTopPicks(), DataOrigin.LOCAL_FALLBACK, "empty_remote", callback);
                            return;
                        }
                        List<Place> mapped = PlaceMapper.toPlaces(dtos);
                        if (mapped.isEmpty()) {
                            Log.w(TAG, "fetchTopPicks: all " + dtos.size()
                                    + " DTOs failed mapping, activating LOCAL_FALLBACK");
                            deliver(local.getTopPicks(), DataOrigin.LOCAL_FALLBACK, "mapping_failed_all", callback);
                            return;
                        }
                        Log.d(TAG, "fetchTopPicks: FIRESTORE success count=" + mapped.size()
                                + " (from " + dtos.size() + " DTOs)"
                                + " firstId=" + mapped.get(0).getId());
                        deliver(mapped, DataOrigin.FIRESTORE, null, callback);
                    } catch (Exception resultEx) {
                        Log.e(TAG, "fetchTopPicks: task.getResult() threw, activating LOCAL_FALLBACK"
                                + " exception=" + resultEx.getMessage(), resultEx);
                        deliver(local.getTopPicks(), DataOrigin.LOCAL_FALLBACK,
                                "result_exception: " + resultEx.getMessage(), callback);
                    }
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
                        Log.w(TAG, "fetchPublishedByCategory: Firestore QUERY failed, activating LOCAL_FALLBACK"
                                + " category=" + category
                                + " exception=" + (ex != null ? ex.getMessage() : "unknown"), ex);
                        deliver(local.getPlacesByCategory(category), DataOrigin.LOCAL_FALLBACK,
                                ex != null ? ex.getMessage() : "unknown_error", callback);
                        return;
                    }
                    try {
                        List<PlaceDto> dtos = task.getResult();
                        if (dtos == null || dtos.isEmpty()) {
                            Log.w(TAG, "fetchPublishedByCategory: empty remote result, activating LOCAL_FALLBACK"
                                    + " category=" + category);
                            deliver(local.getPlacesByCategory(category), DataOrigin.LOCAL_FALLBACK, "empty_remote", callback);
                            return;
                        }
                        List<Place> mapped = PlaceMapper.toPlaces(dtos);
                        if (mapped.isEmpty()) {
                            Log.w(TAG, "fetchPublishedByCategory: all " + dtos.size()
                                    + " DTOs failed mapping, activating LOCAL_FALLBACK category=" + category);
                            deliver(local.getPlacesByCategory(category), DataOrigin.LOCAL_FALLBACK, "mapping_failed_all", callback);
                            return;
                        }
                        Log.d(TAG, "fetchPublishedByCategory: FIRESTORE success category=" + category
                                + " count=" + mapped.size() + " (from " + dtos.size() + " DTOs)"
                                + " firstId=" + mapped.get(0).getId());
                        deliver(mapped, DataOrigin.FIRESTORE, null, callback);
                    } catch (Exception resultEx) {
                        Log.e(TAG, "fetchPublishedByCategory: task.getResult() threw, activating LOCAL_FALLBACK"
                                + " category=" + category
                                + " exception=" + resultEx.getMessage(), resultEx);
                        deliver(local.getPlacesByCategory(category), DataOrigin.LOCAL_FALLBACK,
                                "result_exception: " + resultEx.getMessage(), callback);
                    }
                });
    }

    private void fetchRemoteForOfflineFirst(@Nullable String city,
                                            @Nullable List<String> categories,
                                            @NonNull OfflineFirstPlacesCallback callback) {
        PerformanceTracker.startTimer("FETCH_PLACES_OFFLINE_FIRST_REMOTE");
        remote.fetchPublishedPlaces()
                .addOnCompleteListener(task -> {
                    PerformanceTracker.endTimer("FETCH_PLACES_OFFLINE_FIRST_REMOTE");
                    if (!task.isSuccessful()) {
                        Exception ex = task.getException();
                        Log.w(TAG, "getPlacesOfflineFirst: Firestore failed, trying ROOM_CACHE"
                                + " exception=" + (ex != null ? ex.getMessage() : "unknown"), ex);
                        loadFromRoom(city, categories, ex != null ? ex.getMessage() : "remote_failed", callback);
                        return;
                    }
                    try {
                        List<PlaceDto> dtos = task.getResult();
                        List<Place> allPlaces = PlaceMapper.toPlaces(dtos != null ? dtos : Collections.emptyList());
                        if (allPlaces.isEmpty()) {
                            Log.w(TAG, "getPlacesOfflineFirst: empty remote result, trying ROOM_CACHE");
                            loadFromRoom(city, categories, "empty_remote", callback);
                            return;
                        }

                        cachePlacesInBackground(allPlaces, true);
                        List<Place> filteredPlaces = filterPlaces(allPlaces, city, categories);
                        deliverOfflineFirst(filteredPlaces, DataOrigin.FIRESTORE, false, null, callback);
                    } catch (Exception e) {
                        Log.w(TAG, "getPlacesOfflineFirst: result handling failed, trying ROOM_CACHE", e);
                        loadFromRoom(city, categories, "result_exception: " + e.getMessage(), callback);
                    }
                });
    }

    private void loadFromRoom(@Nullable String city,
                              @Nullable List<String> categories,
                              @Nullable String message,
                              @NonNull OfflineFirstPlacesCallback callback) {
        CACHE_EXECUTOR.execute(() -> {
            if (placeDao == null) {
                mainHandler.post(() -> callback.onPlacesLoaded(new ArrayList<>(), DataOrigin.ROOM_CACHE, true, message));
                return;
            }

            int cacheCount = placeDao.getCount();
            if (cacheCount == 0) {
                deliverOfflineFirst(new ArrayList<>(), DataOrigin.ROOM_CACHE, true, message, callback);
                return;
            }

            List<PlaceEntity> entities = loadCachedEntities(city, categories);
            List<Place> places = new ArrayList<>();
            for (PlaceEntity entity : entities) {
                if (entity != null) {
                    places.add(entity.toPlace());
                }
            }
            List<Place> filteredPlaces = filterPlaces(places, city, categories);
            deliverOfflineFirst(filteredPlaces, DataOrigin.ROOM_CACHE, false, message, callback);
        });
    }

    @NonNull
    private List<PlaceEntity> loadCachedEntities(@Nullable String city, @Nullable List<String> categories) {
        if (placeDao == null) {
            return new ArrayList<>();
        }

        boolean hasCity = hasUsableCity(city);
        List<String> cleanedCategories = cleanCategories(categories);
        boolean hasCategories = !cleanedCategories.isEmpty();

        if (hasCity) {
            return placeDao.getByCity(city.trim());
        }

        if (hasCategories) {
            List<PlaceEntity> byCategory = placeDao.getByCategories(cleanedCategories);
            if (!byCategory.isEmpty()) {
                return byCategory;
            }
        }

        return placeDao.getAll();
    }

    private void cachePlacesInBackground(@NonNull List<Place> places, boolean pruneStale) {
        if (appContext == null || placeDao == null || places.isEmpty()) {
            return;
        }
        CACHE_EXECUTOR.execute(() -> {
            long timestamp = System.currentTimeMillis();
            List<PlaceEntity> entities = PlaceSyncManager.toEntities(places, timestamp);
            placeDao.insertAll(entities);
            if (pruneStale) {
                placeDao.deleteOlderThan(timestamp);
            }
            SharedPreferences prefs = appContext.getSharedPreferences(PlaceSyncManager.PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putLong(PlaceSyncManager.KEY_LAST_SYNCED_AT, timestamp).apply();
            Log.d(TAG, "Updated local place cache count=" + entities.size());
        });
    }

    private boolean isOnline() {
        if (appContext == null) {
            return true;
        }
        ConnectivityManager connectivityManager =
                (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) {
            return false;
        }
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        return capabilities != null
                && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    @NonNull
    private List<Place> filterPlaces(@NonNull List<Place> places,
                                     @Nullable String city,
                                     @Nullable List<String> categories) {
        List<String> cleanedCategories = cleanCategories(categories);
        boolean hasCity = hasUsableCity(city);
        boolean hasCategories = !cleanedCategories.isEmpty();
        if (!hasCity && !hasCategories) {
            return new ArrayList<>(places);
        }

        List<Place> filtered = new ArrayList<>();
        for (Place place : places) {
            if (place == null) {
                continue;
            }
            if (hasCity && !matchesCity(place, city)) {
                continue;
            }
            if (hasCategories && !matchesAnyCategory(place, cleanedCategories)) {
                continue;
            }
            filtered.add(place);
        }
        return filtered;
    }

    private boolean matchesCity(@NonNull Place place, @Nullable String city) {
        if (!hasUsableCity(city)) {
            return true;
        }
        String placeCity = normalize(place.getCity());
        String targetCity = normalize(city);
        return !placeCity.isEmpty()
                && (placeCity.equals(targetCity)
                || placeCity.contains(targetCity)
                || targetCity.contains(placeCity));
    }

    private boolean matchesAnyCategory(@NonNull Place place, @NonNull List<String> categories) {
        String placeCategory = normalizeCategory(place.getCategory());
        String placeCategoryId = normalizeCategory(place.getCategoryId());
        for (String category : categories) {
            String target = normalizeCategory(category);
            if (target.isEmpty()) {
                continue;
            }
            if (target.equals(placeCategory)
                    || target.equals(placeCategoryId)
                    || (!placeCategory.isEmpty() && placeCategory.contains(target))
                    || (!placeCategory.isEmpty() && target.contains(placeCategory))) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    private List<String> cleanCategories(@Nullable List<String> categories) {
        List<String> cleaned = new ArrayList<>();
        if (categories == null) {
            return cleaned;
        }
        for (String category : categories) {
            if (category == null) {
                continue;
            }
            String trimmed = category.trim();
            if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("All") || trimmed.equalsIgnoreCase("Mixed")) {
                continue;
            }
            cleaned.add(trimmed);
        }
        return cleaned;
    }

    private boolean hasUsableCity(@Nullable String city) {
        if (city == null) {
            return false;
        }
        String trimmed = city.trim();
        return !trimmed.isEmpty()
                && !trimmed.equalsIgnoreCase("All")
                && !trimmed.equalsIgnoreCase("Your Current Location");
    }

    @NonNull
    private String normalizeCategory(@Nullable String value) {
        String normalized = normalize(value);
        if (normalized.equals("history") || normalized.equals("historic") || normalized.equals("fort")) {
            return "historical";
        }
        if (normalized.equals("spiritual") || normalized.equals("religion")
                || normalized.equals("temple") || normalized.equals("temples")) {
            return "religious";
        }
        return normalized;
    }

    @NonNull
    private String normalize(@Nullable String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.US);
    }

    private void deliver(@NonNull List<Place> places,
                         @NonNull DataOrigin origin,
                         @Nullable String message,
                         @NonNull PlacesLoadCallback callback) {
        if (origin == DataOrigin.LOCAL_FALLBACK) {
            Log.i(TAG, "LOCAL_FALLBACK delivery size=" + places.size() + (message != null ? (" detail=" + message) : ""));
        } else {
            Log.d(TAG, origin + " delivery size=" + places.size());
        }
        callback.onPlacesLoaded(places, origin, message);
    }

    private void deliverOfflineFirst(@NonNull List<Place> places,
                                     @NonNull DataOrigin origin,
                                     boolean cacheEmpty,
                                     @Nullable String message,
                                     @NonNull OfflineFirstPlacesCallback callback) {
        Runnable delivery = () -> {
            Log.d(TAG, "OFFLINE_FIRST delivery origin=" + origin
                    + " size=" + places.size()
                    + " cacheEmpty=" + cacheEmpty
                    + (message != null ? (" detail=" + message) : ""));
            callback.onPlacesLoaded(places, origin, cacheEmpty, message);
        };

        if (Looper.myLooper() == Looper.getMainLooper()) {
            delivery.run();
        } else {
            mainHandler.post(delivery);
        }
    }
}

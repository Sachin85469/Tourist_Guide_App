package com.arriva.touristguideapp.data.places;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.arriva.touristguideapp.Place;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlaceSyncManager {

    private static final String TAG = "PlaceSyncManager";
    static final String PREFS_NAME = "place_cache";
    static final String KEY_LAST_SYNCED_AT = "last_synced_at";
    private static final long SIX_HOURS_MS = 6L * 60L * 60L * 1000L;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private final Context appContext;
    private final FirestorePlaceDataSource remote;
    private final PlaceDao placeDao;
    private final SharedPreferences prefs;

    public PlaceSyncManager(@NonNull Context context) {
        this(context, new FirestorePlaceDataSource(), AppDatabase.getInstance(context).placeDao());
    }

    PlaceSyncManager(@NonNull Context context,
                     @NonNull FirestorePlaceDataSource remote,
                     @NonNull PlaceDao placeDao) {
        this.appContext = context.getApplicationContext();
        this.remote = remote;
        this.placeDao = placeDao;
        this.prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void syncIfNeeded() {
        EXECUTOR.execute(() -> {
            if (!isOnline()) {
                return;
            }
            if (!shouldSync()) {
                return;
            }
            syncNow();
        });
    }

    @WorkerThread
    public boolean shouldSync() {
        long lastSyncedAt = prefs.getLong(KEY_LAST_SYNCED_AT, 0L);
        return lastSyncedAt == 0L
                || System.currentTimeMillis() - lastSyncedAt > SIX_HOURS_MS
                || placeDao.getCount() == 0;
    }

    @WorkerThread
    private void syncNow() {
        long syncTimestamp = System.currentTimeMillis();
        try {
            List<PlaceDto> dtos = Tasks.await(remote.fetchPublishedPlaces());
            List<Place> places = PlaceMapper.toPlaces(dtos != null ? dtos : new ArrayList<>());
            List<PlaceEntity> entities = toEntities(places, syncTimestamp);
            placeDao.insertAll(entities);
            placeDao.deleteOlderThan(syncTimestamp);
            prefs.edit().putLong(KEY_LAST_SYNCED_AT, syncTimestamp).apply();
            Log.i(TAG, "Synced " + entities.size() + " places to local cache");
        } catch (Exception e) {
            Log.w(TAG, "Place cache sync failed: " + e.getMessage(), e);
        }
    }

    @NonNull
    static List<PlaceEntity> toEntities(@NonNull List<Place> places, long syncTimestamp) {
        List<PlaceEntity> entities = new ArrayList<>();
        for (Place place : places) {
            if (place == null) {
                continue;
            }
            entities.add(PlaceEntity.fromPlace(place, syncTimestamp));
        }
        return entities;
    }

    private boolean isOnline() {
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
}

package com.arriva.touristguideapp;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FavoritesManager {

    private static final String PREF_NAME = "favorites";

    public static boolean isFavorite(Context context, String placeId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(placeId, false);
    }

    public static void toggleFavorite(Context context, String placeId) {
        Place targetPlace = null;
        for (Place p : DataProvider.getAllPlaces()) {
            if (p.getId().equals(placeId)) {
                targetPlace = p;
                break;
            }
        }
        toggleFavorite(context, placeId, targetPlace);
    }

    public static void toggleFavorite(Context context, Place place) {
        if (place == null) return;
        toggleFavorite(context, place.getId(), place);
    }

    private static void toggleFavorite(Context context, String placeId, Place place) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean current = prefs.getBoolean(placeId, false);
        boolean next = !current;

        // 1. Save locally for offline support
        prefs.edit().putBoolean(placeId, next).apply();

        String placeName = place != null ? place.getName() : "Destination";

        // Track Analytics
        if (next) {
            new com.arriva.touristguideapp.data.analytics.AnalyticsRepository().trackFavoriteAdded(placeId);
            com.arriva.touristguideapp.profile.ProfileActivityTracker.log(
                    context,
                    com.arriva.touristguideapp.profile.ProfileActivityTracker.Action.DESTINATION_SAVED,
                    placeName
            );
        } else {
            com.arriva.touristguideapp.profile.ProfileActivityTracker.log(
                    context,
                    com.arriva.touristguideapp.profile.ProfileActivityTracker.Action.DESTINATION_REMOVED,
                    placeName
            );
        }

        // 2. Sync with Firestore top-level "favorites" collection
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String uid = auth.getCurrentUser().getUid();
            String favId = uid + "_" + placeId;
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            if (next) {
                Map<String, Object> favData = new HashMap<>();
                favData.put("favoriteId", favId);
                favData.put("userId", uid);
                favData.put("destinationId", placeId);
                favData.put("destinationName", placeName);
                if (place != null) {
                    favData.put("city", place.getCity());
                    favData.put("latitude", place.getLatitude());
                    favData.put("longitude", place.getLongitude());
                }
                favData.put("savedAt", System.currentTimeMillis());

                db.collection("favorites")
                        .document(favId)
                        .set(favData);
            } else {
                db.collection("favorites")
                        .document(favId)
                        .delete();
            }
        }

        // 3. Generate notifications
        try {
            com.arriva.touristguideapp.data.notifications.NotificationRepository notificationRepository = 
                new com.arriva.touristguideapp.data.notifications.NotificationRepository(context);
            if (next) {
                notificationRepository.addNotification(
                    "Destination Saved",
                    placeName + " added to Saved Places",
                    com.arriva.touristguideapp.data.notifications.NotificationModel.TYPE_FAVORITE
                );
            } else {
                notificationRepository.addNotification(
                    "Destination Removed",
                    placeName + " removed from Saved Places",
                    com.arriva.touristguideapp.data.notifications.NotificationModel.TYPE_FAVORITE
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Set<String> getFavoriteIds(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();
        Set<String> favorites = new HashSet<>();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if (entry.getValue() instanceof Boolean && (Boolean) entry.getValue()) {
                favorites.add(entry.getKey());
            }
        }
        return favorites;
    }

    public static int getFavoritesCount(Context context) {
        return getFavoriteIds(context).size();
    }

    public static void syncFavoritesFromFirestore(Context context) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        FirebaseFirestore.getInstance().collection("favorites")
                .whereEqualTo("userId", uid)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();

                        // Clear existing favorite flags
                        Map<String, ?> all = prefs.getAll();
                        for (Map.Entry<String, ?> entry : all.entrySet()) {
                            if (entry.getValue() instanceof Boolean) {
                                editor.remove(entry.getKey());
                            }
                        }

                        // Set the current ones
                        for (com.google.firebase.firestore.DocumentSnapshot doc : task.getResult().getDocuments()) {
                            String destId = doc.getString("destinationId");
                            if (destId != null) {
                                editor.putBoolean(destId, true);
                            }
                        }
                        editor.apply();

                        // Notify parent activity if it is MainActivity or FavoritesActivity
                        if (context instanceof MainActivity) {
                            ((MainActivity) context).refreshQuickStatsOnly();
                        } else if (context instanceof FavoritesActivity) {
                            ((FavoritesActivity) context).updateCount();
                        }
                    }
                });
    }
}

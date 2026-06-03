package com.arriva.touristguideapp;

import android.content.Context;
import android.content.SharedPreferences;
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
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean current = prefs.getBoolean(placeId, false);
        boolean next = !current;
        prefs.edit().putBoolean(placeId, next).apply();

        if (next) {
            new com.arriva.touristguideapp.data.analytics.AnalyticsRepository().trackFavoriteAdded(placeId);
        }

        // Get place name from DataProvider
        String placeName = "Destination";
        for (Place p : DataProvider.getAllPlaces()) {
            if (p.getId().equals(placeId)) {
                placeName = p.getName();
                break;
            }
        }

        // Generate Favorite Notification
        try {
            com.arriva.touristguideapp.data.notifications.NotificationRepository notificationRepository = 
                new com.arriva.touristguideapp.data.notifications.NotificationRepository(context);
            if (next) {
                notificationRepository.addNotification(
                    "Favorite Added",
                    placeName + " was added to your favorites successfully.",
                    com.arriva.touristguideapp.data.notifications.NotificationModel.TYPE_FAVORITE
                );
            } else {
                notificationRepository.addNotification(
                    "Favorite Removed",
                    placeName + " was removed from your favorites.",
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
}

package com.example.touristguideapp;

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
        prefs.edit().putBoolean(placeId, !current).apply();
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
}

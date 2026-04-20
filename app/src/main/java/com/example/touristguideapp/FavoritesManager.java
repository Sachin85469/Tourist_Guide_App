package com.example.touristguideapp;

import android.content.Context;
import android.content.SharedPreferences;

public class FavoritesManager {

    public static boolean isFavorite(Context context, String placeId) {
        SharedPreferences prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE);
        return prefs.getBoolean(placeId, false);
    }

    public static void toggleFavorite(Context context, String placeId) {
        SharedPreferences prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE);
        boolean current = prefs.getBoolean(placeId, false);
        prefs.edit().putBoolean(placeId, !current).apply();
    }
}

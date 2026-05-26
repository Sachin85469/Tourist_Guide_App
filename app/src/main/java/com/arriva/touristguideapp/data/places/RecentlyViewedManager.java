package com.arriva.touristguideapp.data.places;

import android.content.Context;
import android.content.SharedPreferences;
import com.arriva.touristguideapp.Place;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Optimized manager for tracking and persisting recently viewed tourist spots.
 * Requirements:
 * 1. Store last 8 viewed spots.
 * 2. Most recent first.
 * 3. No duplicates (move existing to top).
 * 4. Persist via SharedPreferences.
 */
public class RecentlyViewedManager {
    private static final String PREF_NAME = "recently_viewed_prefs";
    private static final String KEY_RECENT = "recently_viewed";
    private static final int MAX_RECENT = 8;

    private final SharedPreferences prefs;
    private final Gson gson;

    public RecentlyViewedManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    /**
     * Adds a spot to the recently viewed list.
     * Handles duplicates by moving the existing entry to the top.
     * Limits history to 8 items.
     */
    public void addRecentlyViewed(Place place) {
        if (place == null || place.getId() == null) return;
        
        List<Place> recent = getRecentlyViewed();
        
        // Remove duplicate if same spot already exists
        Place duplicate = null;
        for (Place p : recent) {
            if (p.getId() != null && p.getId().equals(place.getId())) {
                duplicate = p;
                break;
            }
        }
        if (duplicate != null) {
            recent.remove(duplicate);
        }

        // Add new spot at beginning of array (Most recent first)
        place.setViewedAt(System.currentTimeMillis());
        recent.add(0, place);

        // Keep only latest 8 entries
        if (recent.size() > MAX_RECENT) {
            recent = new ArrayList<>(recent.subList(0, MAX_RECENT));
        }

        // Save updated array back to storage
        prefs.edit().putString(KEY_RECENT, gson.toJson(recent)).apply();
    }

    /**
     * Fetches existing Recently Viewed array from persistence.
     */
    public List<Place> getRecentlyViewed() {
        String json = prefs.getString(KEY_RECENT, null);
        if (json == null) return new ArrayList<>();

        try {
            Type type = new TypeToken<List<Place>>() {}.getType();
            List<Place> list = gson.fromJson(json, type);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Clears all viewing history.
     */
    public void clearRecentlyViewed() {
        prefs.edit().remove(KEY_RECENT).apply();
    }

    // Keep old method names for backward compatibility during migration if needed
    @Deprecated
    public void addPlace(Place place) {
        addRecentlyViewed(place);
    }

    @Deprecated
    public List<Place> getRecentPlaces() {
        return getRecentlyViewed();
    }
}

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
 * Tracks recently viewed places.
 */
public class RecentlyViewedManager {
    private static final String PREF_NAME = "recently_viewed_prefs";
    private static final String KEY_RECENT = "recently_viewed";
    private static final int MAX_RECENT = 10;

    private final SharedPreferences prefs;
    private final Gson gson;

    public RecentlyViewedManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    public void addPlace(Place place) {
        if (place == null) return;
        List<Place> recent = getRecentPlaces();
        
        // Remove duplicate
        Place found = null;
        for (Place p : recent) {
            if (p.getId().equals(place.getId())) {
                found = p;
                break;
            }
        }
        if (found != null) recent.remove(found);

        recent.add(0, place);
        if (recent.size() > MAX_RECENT) {
            recent = recent.subList(0, MAX_RECENT);
        }

        prefs.edit().putString(KEY_RECENT, gson.toJson(recent)).apply();
    }

    public List<Place> getRecentPlaces() {
        String json = prefs.getString(KEY_RECENT, null);
        if (json == null) return new ArrayList<>();

        Type type = new TypeToken<List<Place>>() {}.getType();
        return gson.fromJson(json, type);
    }
}

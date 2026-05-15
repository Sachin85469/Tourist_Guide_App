package com.arriva.touristguideapp.data.phrasebook;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Favorite phrases and recently played phrase ids (most recent first).
 */
public class PhrasebookPreferences {

    private static final String PREFS = "phrasebook_prefs";
    private static final String KEY_FAVORITES = "favorite_ids";
    private static final String KEY_RECENT = "recent_ids";
    private static final int MAX_RECENT = 10;

    private final SharedPreferences prefs;

    public PhrasebookPreferences(@NonNull Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean isFavorite(@NonNull String phraseId) {
        return getFavoriteIds().contains(phraseId);
    }

    public void toggleFavorite(@NonNull String phraseId) {
        Set<String> favorites = new LinkedHashSet<>(getFavoriteIds());
        if (favorites.contains(phraseId)) {
            favorites.remove(phraseId);
        } else {
            favorites.add(phraseId);
        }
        prefs.edit().putStringSet(KEY_FAVORITES, favorites).apply();
    }

    @NonNull
    public Set<String> getFavoriteIds() {
        Set<String> stored = prefs.getStringSet(KEY_FAVORITES, null);
        if (stored == null) {
            return Collections.emptySet();
        }
        return new LinkedHashSet<>(stored);
    }

    public void recordPlayed(@NonNull String phraseId) {
        List<String> recent = new ArrayList<>(getRecentIds());
        recent.remove(phraseId);
        recent.add(0, phraseId);
        if (recent.size() > MAX_RECENT) {
            recent = recent.subList(0, MAX_RECENT);
        }
        prefs.edit().putString(KEY_RECENT, join(recent)).apply();
    }

    @NonNull
    public List<String> getRecentIds() {
        String raw = prefs.getString(KEY_RECENT, "");
        if (raw == null || raw.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> ids = new ArrayList<>();
        for (String part : raw.split(",")) {
            if (!part.isEmpty()) {
                ids.add(part);
            }
        }
        return ids;
    }

    @NonNull
    private static String join(@NonNull List<String> ids) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(ids.get(i));
        }
        return builder.toString();
    }
}

package com.arriva.touristguideapp.data.places;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages search history using SharedPreferences.
 */
public class SearchHistoryManager {
    private static final String PREF_NAME = "search_history_prefs";
    private static final String KEY_HISTORY = "search_history";
    private static final int MAX_HISTORY = 10;

    private final SharedPreferences prefs;
    private final Gson gson;

    public SearchHistoryManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    public void addSearch(String query) {
        if (query == null || query.trim().isEmpty()) return;
        query = query.trim();

        List<String> history = getHistory();
        history.remove(query); // Remove if exists to move to top
        history.add(0, query);

        if (history.size() > MAX_HISTORY) {
            history = history.subList(0, MAX_HISTORY);
        }

        prefs.edit().putString(KEY_HISTORY, gson.toJson(history)).apply();
    }

    public List<String> getHistory() {
        String json = prefs.getString(KEY_HISTORY, null);
        if (json == null) return new ArrayList<>();

        Type type = new TypeToken<List<String>>() {}.getType();
        return gson.fromJson(json, type);
    }

    public void clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply();
    }
}

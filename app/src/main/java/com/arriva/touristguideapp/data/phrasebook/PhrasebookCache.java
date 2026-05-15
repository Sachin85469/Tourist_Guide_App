package com.arriva.touristguideapp.data.phrasebook;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Persists the last successful Firestore phrase list for offline use.
 */
public class PhrasebookCache {

    private static final String TAG = "PhrasebookCache";
    private static final String PREFS = "phrasebook_cache";
    private static final String KEY_JSON = "phrases_json";

    private final SharedPreferences prefs;

    public PhrasebookCache(@NonNull Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void save(@NonNull List<Phrase> phrases) {
        try {
            JSONArray array = new JSONArray();
            for (Phrase phrase : phrases) {
                JSONObject obj = new JSONObject();
                obj.put("id", phrase.getId());
                obj.put("englishText", phrase.getEnglishText());
                obj.put("marathiText", phrase.getMarathiText());
                obj.put("hindiText", phrase.getHindiText());
                obj.put("category", phrase.getCategory());
                array.put(obj);
            }
            prefs.edit().putString(KEY_JSON, array.toString()).apply();
        } catch (Exception e) {
            Log.w(TAG, "save failed", e);
        }
    }

    @NonNull
    public List<Phrase> load() {
        String json = prefs.getString(KEY_JSON, null);
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        List<Phrase> phrases = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Phrase phrase = new Phrase(
                        obj.getString("id"),
                        obj.getString("englishText"),
                        obj.getString("marathiText"),
                        obj.getString("hindiText"),
                        obj.getString("category")
                );
                phrases.add(phrase);
            }
        } catch (Exception e) {
            Log.w(TAG, "load failed", e);
        }
        return phrases;
    }

    public void clear() {
        prefs.edit().remove(KEY_JSON).apply();
    }

    @Nullable
    public String lastSavedAtLabel() {
        if (!prefs.contains(KEY_JSON)) {
            return null;
        }
        return "Offline copy";
    }
}
